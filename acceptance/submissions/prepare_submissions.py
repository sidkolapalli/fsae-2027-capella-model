"""Generate the competition-submission layer from submissions.json.

submissions.json holds the verified submission items as paraphrases and ids only. Model anchors were
frozen once from the verified prose into {class, layer, name} triples with capellambse (--freeze); every
later run reads the frozen file, checks it and writes the Groovy chunks and expected-manifest.json.
"""
import argparse
import csv
import datetime
import hashlib
import json
import math
import re
from collections import Counter, defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
RULES = HERE.parent / "rules"
MODEL = ROOT / "model/FSAE-2027-Electric-Arcadia/FSAE-2027-Electric-Arcadia.aird"
DATA = HERE / "submissions.json"
SOURCE_HASH = "f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e"
SLICE = "FSAE2027:submissions-v1"
PREFIX = "FSAE-2027-V1:"
MODULE_ID = "FSAE2027:SUB:module"
MODULE_TITLE = "Formula SAE 2027 competition submissions (candidate readiness)"
PREPARE = "FSAE2027:process Prepare and document competition entry"
INSPECT = "FSAE2027:process Inspect and present the vehicle"
CHUNK_ITEMS = 60
CHUNK_CHARS = 90_000
WORDS = 8

# Submission folders in build order: report name, format, penalty group and the Table PS rows they derive from.
# Format and penalty group are taken from each verified overview; due and cancellation dates are set on the
# event website, so they stay TBD until the team enters them.
SUBMISSIONS = [
    ("SES", "ses", "XLSX", "Tech", ["TABLE-PS-1:p002"]),
    ("IAD", "iad", "XLSX (part of the SES workbook)", "Tech", ["F.8.8.1:p001"]),
    ("ESF", "esf", "XLSX", "Tech", ["TABLE-PS-1:p006"]),
    ("DES", "design", "PDF briefing, XLSX spec sheet, PDF drawings", "Other",
     ["TABLE-PS-1:p011", "TABLE-PS-1:p012", "TABLE-PS-1:p013"]),
    ("COST", "cost", "Event Website cost tool", "Other", ["TABLE-PS-1:p008", "TABLE-PS-1:p009", "TABLE-PS-1:p010"]),
    ("PRES", "presentation", "TBD (set by the Presentation Concept)", "Other", ["TABLE-PS-1:p007"]),
    ("GEN", "general", "PDF or XLSX as Table PS-1 lists", "none", ["TABLE-PS-1:p001", "TABLE-PS-2:p001"]),
]
EXTRA_SATISFY = {"COST:ADD": INSPECT}  # the Cost Addendum is handed in at the event

VOCAB = {
    "submission_id": [s[0] for s in SUBMISSIONS],
    "submission_source_kind": ["model", "cad", "analysis", "test", "supplier", "admin"],
    "submission_model_status": ["present", "partial", "absent"],
    "submission_status": ["not_started", "in_progress", "ready_for_review", "submitted", "accepted", "not_applicable"],
    "submission_penalty_group": ["Tech", "Other", "none"],
}
DATE_ATTRIBUTES = ("submission_due", "submission_cancellation")
STATUS_COLUMNS = ["item_id", "status", "owner", "due", "value", "evidence_ref"]
LAYERS = ["SA", "PA", "LA", "OA", "EPBS"]  # preference when one name exists in several layers
ARCHITECTURES = {"OperationalAnalysis": "OA", "SystemAnalysis": "SA", "LogicalArchitecture": "LA",
                 "PhysicalArchitecture": "PA", "EPBSArchitecture": "EPBS"}


def load(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def write(path, text):
    Path(path).write_text(text, encoding="utf-8", newline="\n")


def as_json(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def groovy(value):
    if isinstance(value, dict):
        return "[" + ",".join(groovy(str(k)) + ":" + groovy(v) for k, v in value.items()) + "]" if value else "[:]"
    if isinstance(value, list):
        return "[" + ",".join(map(groovy, value)) + "]"
    if isinstance(value, str):
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "\\r").replace("\n", "\\n") + "'"
    return json.dumps(value)


# ---------------------------------------------------------------- rule references

def ledger_index(ledger):
    """Paragraph ids in ledger order, grouped by their rule id (the part before :pNNN)."""
    by_rule = defaultdict(list)
    for clause_id in ledger:
        by_rule[clause_id.removeprefix(PREFIX).rsplit(":", 1)[0]].append(clause_id)
    return by_rule


def normalize_ref(ref):
    """('clause', full paragraph id) for clause refs, short forms included; ('rule', rule id) otherwise."""
    ref = ref.strip()
    if ref.startswith(PREFIX):
        return "clause", ref
    if re.search(r":p\d{3}$", ref):
        return "clause", PREFIX + ref
    return "rule", ref


def resolve_refs(refs, ledger, by_rule, rule_ids):
    """Derive targets and text-only refs. A bare rule links all its paragraphs unless the item names some."""
    clauses, rules = [], []
    for ref in refs:
        kind, value = normalize_ref(ref)
        if kind == "clause":
            if value not in ledger:
                raise ValueError("Unknown paragraph id: " + ref)
            clauses.append(value)
        else:
            if value not in by_rule and value not in rule_ids:
                raise ValueError("Unknown rule id: " + ref)
            rules.append(value)
    named = {c.removeprefix(PREFIX).rsplit(":", 1)[0] for c in clauses}
    derive, text_only = set(clauses), []
    for rule in rules:
        if rule in named:
            continue
        if rule in by_rule:
            derive.update(by_rule[rule])
        elif rule not in text_only:
            text_only.append(rule)
    order = {c: i for i, c in enumerate(ledger)}
    return sorted(derive, key=order.get), text_only


# ---------------------------------------------------------------- anchor freeze (capellambse, run once)

def layer_of(element):
    node = element
    while node is not None and type(node).__name__ not in ARCHITECTURES:
        node = getattr(node, "parent", None)
    return ARCHITECTURES[type(node).__name__] if node is not None else None


def behavior_constraint(name):
    """Guard and INVARIANT constraints state behavior the model holds, so they get satisfy links."""
    return " guard" in name or name.endswith(" INVARIANT")


def link_kind(cls, name, uuid, verify_targets):
    """The anchor class rule: which link an anchor gets, or None for a text-only marker."""
    if cls == "Requirement":
        return "derive"
    if cls == "Constraint":
        if ":DEC-" in name:
            return "decision"
        if uuid in verify_targets:
            return "verify"
        if behavior_constraint(name):
            return "satisfy"
        return None  # OPEN/TBD and other marker constraints stay text only
    return "satisfy"


def freeze_anchor(text, index, requirement_ids, verify_targets):
    """Split verified anchor prose into links, decision ids and text that cannot be linked."""
    keys = sorted(set(index) | requirement_ids, key=len, reverse=True)
    anchors, decisions, text_parts = [], [], []
    for segment in (s.strip() for s in text.split(";")):
        if not segment:
            continue
        wildcard = segment.lower().startswith("none") or any(t in segment for t in ("...", " to ", "*", "<"))
        found, markers, position = [], [], 0
        while not wildcard:
            starts = [i for i in (segment.find("FSAE2027:", position), segment.find(PREFIX, position)) if i >= 0]
            if not starts:
                break
            start = min(starts)
            hit = next((k for k in keys if segment.startswith(k, start) and (
                start + len(k) == len(segment) or not (segment[start + len(k)].isalnum() or segment[start + len(k)] in "-_.:"))), None)
            if hit is None:
                raise ValueError("Unresolved model name in anchor: " + segment[start:start + 80])
            position = start + len(hit)
            if hit in requirement_ids:
                found.append(("derive", "Requirement", "SA", hit))
                continue
            words = {w.rstrip("s") for w in re.findall(r"[A-Za-z]+", segment[:start])}
            candidates = index[hit]
            if words & {c for c, _, _ in candidates}:
                candidates = [c for c in candidates if c[0] in words]
            layer = next(layer for layer in LAYERS if any(c[1] == layer for c in candidates))
            candidates = [c for c in candidates if c[1] == layer]
            if len(candidates) != 1:
                raise ValueError("Ambiguous model anchor: " + hit + " " + str(candidates))
            cls, layer, uuid = candidates[0]
            kind = link_kind(cls, hit, uuid, verify_targets)
            if kind is None:
                markers.append(hit)
            else:
                found.append((kind, cls, layer, hit))
        if not found:
            text_parts.append(segment)
            continue
        text_parts.extend(markers)
        for kind, cls, layer, name in found:
            if kind == "decision":
                decision = re.search(r"DEC-[A-Z]+", name).group(0)
                if decision not in decisions:
                    decisions.append(decision)
            else:
                anchor = {"link": kind, "class": cls, "layer": layer, "name": name}
                if anchor not in anchors:
                    anchors.append(anchor)
    return anchors, decisions, "; ".join(text_parts) or "none"


def freeze(verified_path, model_path=MODEL):
    import warnings
    import capellambse  # only needed for this one-off step
    warnings.simplefilter("ignore", FutureWarning)
    model = capellambse.MelodyModel(str(model_path))
    index = defaultdict(list)
    for element in model.search():
        name = getattr(element, "name", None)
        if name and type(element).__name__ != "Part":
            index[name].append((type(element).__name__, layer_of(element), element.uuid))
    requirement_ids = {r.identifier for r in model.search("Requirement")}
    verify_targets = {r.target.uuid for r in model.search("CapellaIncomingRelation") if r.type and r.type.long_name == "verify"}
    decisions = {re.search(r"DEC-[A-Z]+", c.name).group(0): c.name for c in model.search("Constraint") if (c.name or "").startswith("FSAE2027:DEC-")}
    source = next(m for m in model.sa.requirement_modules if m.identifier == "FSAE-2027-V1:source-module")
    coverage, verification = Counter(), Counter()
    for requirement in source.requirements:
        values = {a.definition.long_name: a.value for a in requirement.attributes}
        coverage[values["coverage_status"]] += 1
        verification[values["verification_status"]] += 1
    ledger = load(RULES / "clause-ledger.json")["clauses"]
    by_rule = ledger_index(ledger)
    rule_ids = {n["source_rule_id"] for n in load(RULES / "rule-inventory.json")["rule_nodes"]}
    verified = load(verified_path)
    assert len(verified) == len(SUBMISSIONS)
    # Reviewed fields in the current submissions.json win over the verified prose, so a re-freeze keeps the review.
    reviewed = load(DATA) if DATA.exists() else dict(submissions=[])
    kept = {s["id"]: s for s in reviewed["submissions"]}
    kept_items = {i["id"]: i for s in reviewed["submissions"] for i in s["items"]}
    submissions = []
    for (sid, report, fmt, group, rows), entry in zip(SUBMISSIONS, verified):
        items = []
        for item in entry["items"]:
            assert item["id"].startswith(sid + "-"), item["id"]
            anchors, anchor_decisions, anchor_text = freeze_anchor(item["model_anchor"], index, requirement_ids, verify_targets)
            blocking = list(item.get("blocking_decisions") or [])
            blocking += [d for d in anchor_decisions if d not in blocking]
            derive, text_only = resolve_refs(item["rule_refs"], ledger, by_rule, rule_ids)
            items.append(dict(id=item["id"], section=item["section"], item=item["item"], rule_refs=item["rule_refs"],
                              source_kind=item["source_kind"], model_status=item["in_model"], blocking_decisions=blocking,
                              note=item.get("note", ""), model_anchor=item["model_anchor"], anchor_text=anchor_text,
                              anchors=anchors, derive_clauses=derive, text_only_refs=text_only))
            old = kept_items.get(item["id"], {})
            items[-1].update({k: old[k] for k in ("model_status", "note", "status") if k in old})
        old = kept.get(sid, {})
        submissions.append(dict(id=sid, report=report, title=entry["submission"], overview=old.get("overview", entry["overview"]),
                                governing_rules=old.get("governing_rules", entry["governing_rules"]), format=fmt, penalty_group=group, due="TBD",
                                cancellation="TBD", table_rows=[PREFIX + r for r in rows], items=items))
    capella = Path(model_path).with_suffix(".capella")
    data = dict(schema_version=1, slice_id=SLICE, source_document_id="FSAE-2027-V1", source_sha256=SOURCE_HASH,
                model_capella_sha256=hashlib.sha256(capella.read_bytes()).hexdigest(),
                anchor_rule="Anchors were frozen once from the verified prose. Requirements get derive, verify-target "
                            "Constraints get verify, DEC Constraints only block, guard/INVARIANT Constraints and other "
                            "elements get satisfy at SA when the name exists there (else PA, LA, OA, EPBS). Parts, "
                            "OPEN/TBD marker Constraints, diagrams and wildcards stay text only.",
                model_baseline=dict(source_requirements=sum(coverage.values()), coverage_status=dict(sorted(coverage.items())),
                                    verification_status=dict(sorted(verification.items()))),
                decisions=dict(sorted(decisions.items())), submissions=submissions)
    write(DATA, json.dumps(data, indent=1, ensure_ascii=False) + "\n")
    return data


# ---------------------------------------------------------------- checks

def section_id(item_id):
    return item_id.rsplit("-", 1)[0]


def folder_id(section):
    return "FSAE2027:SUB:" + section.replace("-", ":", 1)


def item_identifier(item_id):
    return "FSAE2027:SUB:item:" + item_id


def cut(text, limit=90):
    if len(text) <= limit:
        return text
    return text[:limit - 3].rsplit(" ", 1)[0].rstrip(" ,;:-(") + "..."


def anchor_key(anchor):
    return anchor["layer"] + "|" + anchor["class"] + "|" + anchor["name"]


def sections_of(submission):
    """Section folders keyed by item prefix; a prefix with several chapter titles uses their shared lead."""
    titles = defaultdict(list)
    for item in submission["items"]:
        if item["section"] not in titles[section_id(item["id"])]:
            titles[section_id(item["id"])].append(item["section"])
    result = {}
    for prefix, names in titles.items():
        leads = {n.split(" - ", 1)[0] for n in names}
        if len(names) > 1 and (len(leads) != 1 or not all(" - " in n for n in names)):
            raise ValueError("Section prefix with unrelated titles: " + prefix)
        result[prefix] = names[0] if len(names) == 1 else leads.pop()
    return result


def words(text):
    text = text.lower()
    for old, new in (("\u2019", "'"), ("\u2018", "'"), ("\u201c", '"'), ("\u201d", '"'), ("\u2013", "-"), ("\u2014", "-")):
        text = text.replace(old, new)
    return [w for w in (re.sub(r"^\W+|\W+$", "", x) for x in text.split()) if w]


def strings(value, path=""):
    if isinstance(value, str):
        yield path, value
    elif isinstance(value, list):
        for i, v in enumerate(value):
            yield from strings(v, f"{path}[{i}]")
    elif isinstance(value, dict):
        for k, v in value.items():
            yield from strings(v, f"{path}.{k}")


def copied_runs(texts, paragraphs, size=WORDS):
    """(where, run) for every run of `size` words shared with the private source paragraphs."""
    shingles = set()
    for paragraph in paragraphs:
        w = words(paragraph)
        shingles.update(tuple(w[i:i + size]) for i in range(len(w) - size + 1))
    found = []
    for where, text in texts:
        w = words(text)
        for i in range(len(w) - size + 1):
            if tuple(w[i:i + size]) in shingles:
                found.append((where, " ".join(w[i:i + size])))
                break
    return found


def check(data, ledger, by_rule, rule_ids):
    """Uniqueness, vocabularies, frozen derive ids and the anchor class rule. Returns text-only refs."""
    assert data["source_sha256"] == SOURCE_HASH and data["slice_id"] == SLICE
    assert [s["id"] for s in data["submissions"]] == VOCAB["submission_id"], "Submission order or ids changed"
    ids, text_only = set(), []
    for submission in data["submissions"]:
        assert submission["penalty_group"] in VOCAB["submission_penalty_group"]
        for field in ("due", "cancellation"):
            assert valid_date(submission[field]), submission["id"] + " " + field
        assert all(r in ledger for r in submission["table_rows"])
        sections_of(submission)
        for item in submission["items"]:
            where = item["id"]
            assert re.fullmatch(submission["id"] + r"-[A-Z0-9]+-\d{2}", where), "Bad item id " + where
            assert where not in ids, "Duplicate item id " + where
            ids.add(where)
            assert item["source_kind"] in VOCAB["submission_source_kind"], where
            assert item["model_status"] in VOCAB["submission_model_status"], where
            assert item["item"].strip() and isinstance(item["note"], str), where
            assert item.get("status", "not_started") in BUILD_STATUSES, where + " build status"
            assert all(d in data["decisions"] for d in item["blocking_decisions"]), where + " unknown decision"
            assert len(set(item["blocking_decisions"])) == len(item["blocking_decisions"]), where
            derive, refs = resolve_refs(item["rule_refs"], ledger, by_rule, rule_ids)
            assert derive == item["derive_clauses"] and refs == item["text_only_refs"], where + " derive ids are stale"
            assert derive or not [r for r in item["rule_refs"] if r not in refs], where + " has refs but no derive link"
            keys = [(a["link"], anchor_key(a)) for a in item["anchors"]]
            assert len(keys) == len(set(keys)), where + " duplicate anchor"
            for anchor in item["anchors"]:
                assert anchor["layer"] in LAYERS, where
                if anchor["class"] == "Requirement":
                    assert anchor["link"] == "derive", where
                elif anchor["class"] == "Constraint":
                    assert anchor["link"] in ("verify", "satisfy") and ":DEC-" not in anchor["name"], where
                    assert anchor["link"] == "verify" or behavior_constraint(anchor["name"]), where
                else:
                    assert anchor["link"] == "satisfy", where
            if item["model_status"] != "absent":
                assert item["anchors"] or derive, where + " is present/partial but has no typed link"
            text_only += [(where, r) for r in refs]
    return text_only


def valid_date(value):
    if value == "TBD":
        return True
    try:
        return datetime.date.fromisoformat(value).isoformat() == value
    except ValueError:
        return False


# ---------------------------------------------------------------- generation

def item_links(item):
    derive = list(item["derive_clauses"])
    derive += [a["name"] for a in item["anchors"] if a["link"] == "derive" and a["name"] not in derive]
    return dict(derive=derive, verify=[anchor_key(a) for a in item["anchors"] if a["link"] == "verify"],
                satisfy=[anchor_key(a) for a in item["anchors"] if a["link"] == "satisfy"])


def item_attributes(sid, item):
    """Item-specific attributes; COMMON and ITEM_DEFAULTS complete the set."""
    return {"submission_id": sid, "submission_item_id": item["id"], "submission_rule_refs": as_json(item["rule_refs"]),
            "submission_source_kind": item["source_kind"], "submission_model_status": item["model_status"],
            "submission_model_anchor": item["anchor_text"], "submission_blocking_decisions": as_json(item["blocking_decisions"]),
            "submission_note": item["note"], "submission_status": item.get("status", "not_started")}


COMMON = {"slice_id": SLICE, "source_document_id": "FSAE-2027-V1", "source_sha256": SOURCE_HASH}
ITEM_DEFAULTS = {"submission_owner": "TBD", "submission_due": "TBD", "submission_value": "TBD", "submission_evidence_ref": "TBD"}
BUILD_STATUSES = ("not_started", "not_applicable")  # not_applicable only for rows the EV choice already rules out


def folders(data):
    """Submission and section folders in creation order."""
    result = []
    for submission in data["submissions"]:
        sid = submission["id"]
        sections = sections_of(submission)
        result.append(dict(identifier="FSAE2027:SUB:" + sid, parent=MODULE_ID, title="FSAE2027:SUB " + sid + " " + submission["title"],
                           text=submission["overview"], derive=submission["table_rows"], satisfy=["OA|OperationalActivity|" + PREPARE],
                           attributes={"submission_id": sid, "submission_governing_rules": as_json(submission["governing_rules"]),
                                       "submission_format": submission["format"], "submission_penalty_group": submission["penalty_group"],
                                       "submission_status": "not_started", "submission_due": submission["due"],
                                       "submission_cancellation": submission["cancellation"]}))
        for prefix, title in sections.items():
            extra = EXTRA_SATISFY.get(prefix.replace("-", ":", 1))
            result.append(dict(identifier=folder_id(prefix), parent="FSAE2027:SUB:" + sid, title="FSAE2027:SUB " + prefix + " " + title,
                               text="", derive=[], satisfy=["OA|OperationalActivity|" + extra] if extra else [],
                               attributes={"submission_id": sid}))
    return result


def chunks(items, limit=CHUNK_ITEMS):
    count = math.ceil(len(items) / limit)
    size = math.ceil(len(items) / count)
    return [items[i:i + size] for i in range(0, len(items), size)]


def render(template, **values):
    script = (HERE / template).read_text(encoding="utf-8")
    for key, value in values.items():
        script = script.replace("@@" + key + "@@", value if isinstance(value, str) else groovy(value))
    assert "@@" not in script, template + " has an unfilled placeholder"
    assert len(script) < CHUNK_CHARS, template + " output exceeds " + str(CHUNK_CHARS) + " characters"
    return script


def short(clause_id):
    return clause_id.removeprefix(PREFIX)


def manifest(data, ledger):
    folder_list = folders(data)
    items = []
    for submission in data["submissions"]:
        for item in submission["items"]:
            items.append(dict(identifier=item_identifier(item["id"]), folder=folder_id(section_id(item["id"])), **item_links(item)))
    targets = sorted({c for f in folder_list for c in f["derive"]} | {c for i in items for c in i["derive"] if c.startswith(PREFIX)})
    digest = hashlib.sha256("\n".join(c + "=" + ledger[c]["source_text_sha256"] for c in targets).encode("utf-8")).hexdigest()
    links = Counter()
    for entry in folder_list + items:
        for kind in ("derive", "verify", "satisfy"):
            links[kind] += len(entry.get(kind, []))
    return dict(schema_version=1, slice_id=SLICE, source_sha256=SOURCE_HASH, module=MODULE_ID,
                counts=dict(submission_folders=len(data["submissions"]), section_folders=len(folder_list) - len(data["submissions"]),
                            items=len(items), derive_targets=len(targets), links=dict(links)),
                derive_target_sha256_digest=digest, model_baseline=data["model_baseline"], decisions=data["decisions"],
                folders=[{k: f[k] for k in ("identifier", "parent", "derive", "satisfy")} for f in folder_list], items=items)


def generate(data, ledger, out=HERE):
    """Write every Groovy chunk and expected-manifest.json; returns {file name: size}."""
    for stale in list(out.glob("31-import-*.groovy")) + list(out.glob("32-trace-*.groovy")):
        stale.unlink()  # chunk counts can change between runs
    expected = manifest(data, ledger)
    written = {}

    def emit(name, script):
        write(out / name, script)
        written[name] = len(script)

    folder_list = folders(data)
    for f in folder_list:
        f["derive"] = [short(c) for c in f["derive"]]
    emit("30-create-submissions.groovy", render("30-create.template.groovy", MODULE_ID=groovy(MODULE_ID), MODULE_TITLE=groovy(MODULE_TITLE),
                                                COMMON=COMMON, FOLDERS=folder_list, ITEM_COUNT=str(expected["counts"]["items"])))
    total = expected["counts"]["items"]
    for submission in data["submissions"]:
        sid = submission["id"]
        parts = chunks(submission["items"])
        for number, part in enumerate(parts, 1):
            label = f"{sid.lower()}-{number}"
            sections = defaultdict(list)
            for item in part:
                sections[folder_id(section_id(item["id"]))].append(dict(
                    identifier=item_identifier(item["id"]), title="FSAE2027:SUB " + item["id"] + " " + cut(item["item"]),
                    text=item["item"], chapter=item["section"], attributes=item_attributes(sid, item)))
            emit(f"31-import-{label}.groovy", render("31-import.template.groovy", MODULE_ID=groovy(MODULE_ID), CHUNK=groovy(label),
                                                     COMMON=dict(COMMON, **ITEM_DEFAULTS),
                                                     SECTIONS=[dict(folder=k, records=v) for k, v in sections.items()],
                                                     CHUNK_COUNT=str(len(part))))
            trace = []
            for item in part:
                links = item_links(item)
                links["derive"] = [short(c) for c in links["derive"]]
                trace.append(dict(id=item_identifier(item["id"]), **links))
            emit(f"32-trace-{label}.groovy", render("32-trace.template.groovy", MODULE_ID=groovy(MODULE_ID), CHUNK=groovy(label),
                                                    ITEM_COUNT=str(total), ITEMS=trace))
    emit("40-verify-submissions.groovy", verify_script(data, expected))
    write(out / "expected-manifest.json", json.dumps(expected, indent=1, ensure_ascii=False) + "\n")
    return written


def link_digest(links):
    lines = sorted(["derive|" + c for c in links["derive"]] + ["verify|" + k for k in links["verify"]] +
                   ["satisfy|" + k for k in links["satisfy"]])
    return hashlib.sha256("\n".join(lines).encode("utf-8")).hexdigest()[:16]


def content_digest(identifier, title, text, chapter, attributes):
    lines = [identifier, title, text, chapter] + [k + "=" + v for k, v in sorted(attributes.items())]
    return hashlib.sha256("\n".join(lines).encode("utf-8")).hexdigest()[:16]


def verify_script(data, expected):
    rows = []
    for submission in data["submissions"]:
        for item in submission["items"]:
            identifier = item_identifier(item["id"])
            attributes = dict(COMMON, **ITEM_DEFAULTS, **item_attributes(submission["id"], item))
            title = "FSAE2027:SUB " + item["id"] + " " + cut(item["item"])
            rows.append([identifier[len("FSAE2027:SUB:item:"):], folder_id(section_id(item["id"]))[len("FSAE2027:SUB:"):],
                         link_digest(item_links(item)), content_digest(identifier, title, item["item"], item["section"], attributes),
                         item["model_status"], item.get("status", "not_started")])
    folder_rows = [[f["identifier"], f["parent"], [short(c) for c in f["derive"]], f["satisfy"],
                    content_digest(f["identifier"], f["title"], f["text"], "", dict(COMMON, **f["attributes"]))] for f in folders(data)]
    return render("40-verify.template.groovy", MODULE_ID=groovy(MODULE_ID), SOURCE_HASH=SOURCE_HASH, VOCAB=VOCAB,
                  ITEMS=rows, FOLDERS=folder_rows, DECISIONS=sorted(data["decisions"]),
                  BASELINE=data["model_baseline"], DIGEST=expected["derive_target_sha256_digest"],
                  TARGET_COUNT=str(expected["counts"]["derive_targets"]), ITEM_COUNT=str(expected["counts"]["items"]))


# ---------------------------------------------------------------- status batch

def read_status(path, data):
    """Validated rows from a status CSV; empty cells leave the model value unchanged."""
    known = {item["id"] for s in data["submissions"] for item in s["items"]}
    with open(path, newline="", encoding="utf-8-sig") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames != STATUS_COLUMNS:
            raise ValueError("Status CSV columns must be: " + ",".join(STATUS_COLUMNS))
        rows, seen = [], set()
        for line, row in enumerate(reader, 2):
            row = {k: (v or "").strip() for k, v in row.items()}
            where = f"line {line} ({row['item_id']})"
            if row["item_id"] not in known:
                raise ValueError(where + ": unknown item id")
            if row["item_id"] in seen:
                raise ValueError(where + ": item listed twice")
            seen.add(row["item_id"])
            if row["status"] and row["status"] not in VOCAB["submission_status"]:
                raise ValueError(where + ": status must be one of " + ", ".join(VOCAB["submission_status"]))
            if row["due"] and not valid_date(row["due"]):
                raise ValueError(where + ": due must be an ISO date (YYYY-MM-DD) or TBD")
            values = {"submission_" + k: row[k] for k in STATUS_COLUMNS[1:] if row[k]}
            if values:
                rows.append(dict(id=item_identifier(row["item_id"]), values=values))
    return rows


def status_script(rows):
    return render("50-update-status.template.groovy", MODULE_ID=groovy(MODULE_ID), STATUSES=VOCAB["submission_status"], ROWS=rows)


# ---------------------------------------------------------------- main

def prepare(private_source=None, data_path=DATA, out=HERE):
    data = load(data_path)
    ledger = load(RULES / "clause-ledger.json")["clauses"]
    by_rule = ledger_index(ledger)
    rule_ids = {n["source_rule_id"] for n in load(RULES / "rule-inventory.json")["rule_nodes"]}
    text_only = check(data, ledger, by_rule, rule_ids)
    if private_source:
        paragraphs = {p["clause_id"]: p["text"] for p in load(private_source)}
        expected = manifest(data, ledger)
        targets = {c for e in expected["folders"] + expected["items"] for c in e["derive"] if c.startswith(PREFIX)}
        for clause_id in targets:
            assert hashlib.sha256(paragraphs[clause_id].encode("utf-8")).hexdigest() == ledger[clause_id]["source_text_sha256"], clause_id
        texts = list(strings(data))
        for md in [HERE / "README.md"] + sorted((ROOT / "docs/submissions").glob("*.md")):
            if md.exists():
                texts.append((md.name, md.read_text(encoding="utf-8")))
        copied = copied_runs(texts, paragraphs.values())
        if copied:
            raise ValueError("Text copied from the rulebook (8+ words):\n" + "\n".join(f"{w}: {r}" for w, r in copied))
    written = generate(data, ledger, out)
    return data, text_only, written


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--private-source", type=Path, help="private-source-paragraphs.json: checks source hashes and the 8-word rule")
    parser.add_argument("--status", type=Path, help="status CSV: writes 50-update-status.groovy only")
    parser.add_argument("--freeze", type=Path, help="verified submissions JSON: re-freezes submissions.json from the model (capellambse)")
    args = parser.parse_args()
    if args.freeze:
        freeze(args.freeze)
    data = load(DATA)
    if args.status:
        rows = read_status(args.status, data)
        script = status_script(rows)
        write(HERE / "50-update-status.groovy", script)
        print(json.dumps({"50-update-status.groovy": len(script), "rows": len(rows)}))
        return
    if not args.private_source:
        print("No --private-source: the 8-word copy check and source hash check were skipped.")
    data, text_only, written = prepare(args.private_source)
    print("Text-only rule refs (headings without paragraphs):")
    for where, ref in text_only:
        print("  " + where + ": " + ref)
    print(json.dumps(written, indent=1))


if __name__ == "__main__":
    main()
