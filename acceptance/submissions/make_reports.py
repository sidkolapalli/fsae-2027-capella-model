"""Write one readiness page per competition submission from the saved model.

Run after 40-verify-submissions passes on the reopened model (or after a status update). Reads the model with
capellambse and writes docs/submissions/<name>.md plus a README.md index. Pages hold paraphrases and ids only.
--check only validates the submission_* values against the vocabularies and exits non-zero on a problem.
"""
import argparse
import html
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

import prepare_submissions as prep

DOCS = prep.ROOT / "docs/submissions"
BLOCKED = "\u26d4"
QUESTIONS = {d["id"]: d["question"] for d in prep.load(prep.HERE.parent / "model-schema.json")["engineering_decisions"]}
GUIDE = "../MODEL-GUIDE.md"
DEVELOP = GUIDE + "#using-the-model-as-a-development-platform"
STATUS_HELP = "../../acceptance/submissions/README.md#updating-status"
LEGEND = [
    "How to read the tables:", "",
    "- **Model**: `present` means the candidate model already holds the content (an architecture element, state "
    "logic or a diagram) with nothing open; it does not mean the submitted value is final. `partial` means a model "
    "element or obligation stands for the item but holds no team value yet; items that only restate a rule value "
    "are `partial` too. `absent` means the model has nothing for the item, or cannot hold it (photos, raw test "
    "data, receipts, records).",
    "- **Source**: where the value comes from: `model` (this Capella model), `cad` (CAD and drawings), `analysis` "
    "(calculations), `test` (physical tests), `supplier` (datasheets and purchase records), `admin` (team "
    "administration).",
    "- **Status**: the team's progress. Conditional rows (aluminum, composite, monocoque, custom IA, digital "
    "APPS/BSE) count until you mark them `not_applicable` through the status CSV, once DEC-CHASSIS or the IA route "
    f"is chosen ([how to update status]({STATUS_HELP})).",
    f"- **Blocking**: {BLOCKED} marks a decision whose constraint name still ends in UNRESOLVED. "
    f"[Open design decisions]({GUIDE}#open-design-decisions) lists them, and step 2 of "
    f"[Using the model as a development platform]({DEVELOP}) says how to record and close one.",
    "- **Note** says why an item is partial or absent, which rows are conditional and what is still missing. "
    "**Evidence** is the drawing, file, photo or test id the team records.", ""]


def read_model(path=prep.MODEL):
    """Submissions as plain dicts: folders, items, attributes and the far end of every typed link."""
    import warnings
    import capellambse
    warnings.simplefilter("ignore", FutureWarning)
    model = capellambse.MelodyModel(str(path))
    module = next((m for m in model.sa.requirement_modules if m.identifier == prep.MODULE_ID), None)
    if module is None:
        raise SystemExit(prep.MODULE_ID + " is not in " + str(path) + ". Build the submission layer first (see README.md).")
    links = defaultdict(list)
    for cls in ("InternalRelation", "CapellaOutgoingRelation", "CapellaIncomingRelation"):
        for relation in model.search(cls):
            kind = relation.type.long_name if relation.type else None
            for end, other in ((relation.source, relation.target), (relation.target, relation.source)):
                if end is not None and other is not None:
                    links[end.uuid].append(dict(kind=kind, cls=type(other).__name__,
                                                name=getattr(other, "identifier", None) or other.name))
    decisions = {re.search(r"DEC-[A-Z]+", c.name).group(0): c.name for c in model.search("Constraint")
                 if (c.name or "").startswith("FSAE2027:DEC-")}

    def attributes(element):
        return {a.definition.long_name: str(a.value) for a in element.attributes if a.definition is not None}

    reports = {s[0]: s[1] for s in prep.SUBMISSIONS}
    submissions = []
    for folder in module.folders:
        values = attributes(folder)
        sections = []
        for section in folder.folders:
            items = [dict(id=r.identifier, title=r.long_name, item=html.unescape(str(r.text)), section=r.chapter_name or "",
                          attributes=attributes(r), links=links[r.uuid]) for r in section.requirements]
            sections.append(dict(id=section.identifier, title=section.long_name.split(" ", 2)[-1], items=items))
        submissions.append(dict(id=values.get("submission_id", ""), report=reports.get(values.get("submission_id"), folder.identifier),
                                title=folder.long_name.split(" ", 2)[-1], overview=html.unescape(str(folder.text)), attributes=values,
                                links=links[folder.uuid], sections=sections))
    return submissions, decisions


def problems(submissions):
    """Every submission_* value outside its vocabulary or date format."""
    found = []
    for submission in submissions:
        records = [(submission["id"] or "?", submission["attributes"])]
        records += [(item["id"], item["attributes"]) for s in submission["sections"] for item in s["items"]]
        for where, values in records:
            for key, allowed in prep.VOCAB.items():
                if key in values and values[key] not in allowed:
                    found.append(f"{where}: {key}={values[key]!r} is not one of {', '.join(allowed)}")
            for key in prep.DATE_ATTRIBUTES:
                if key in values and not prep.valid_date(values[key]):
                    found.append(f"{where}: {key}={values[key]!r} is not an ISO date or TBD")
    return found


def cell(text):
    return str(text).replace("|", "\\|").replace("\n", " ").strip() or " "


def rules_cell(item):
    """Derived paragraph ids grouped by rule, then refs that stay text only (headings)."""
    groups = defaultdict(list)
    for link in item["links"]:
        if link["kind"] == "derive" and link["name"].startswith(prep.PREFIX):
            rule, paragraph = link["name"].removeprefix(prep.PREFIX).rsplit(":", 1)
            if paragraph not in groups[rule]:
                groups[rule].append(paragraph)
    parts = [rule + " " + ",".join(sorted(p)) for rule, p in groups.items()]
    for ref in json.loads(item["attributes"].get("submission_rule_refs", "[]")):
        kind, value = prep.normalize_ref(ref)
        if kind == "rule" and value not in groups and value + " (text only)" not in parts:
            parts.append(value + " (text only)")
    return "; ".join(parts)


def anchors_cell(item):
    seen, parts = set(), []
    for link in item["links"]:
        if link["kind"] == "derive" and link["name"].startswith(prep.PREFIX):
            continue
        key = (link["cls"], link["name"])
        if key not in seen:
            seen.add(key)
            parts.append(f"{link['kind']}: {link['cls']} {link['name']}")
    text = item["attributes"].get("submission_model_anchor", "none")
    if text != "none":
        parts.append("text: " + text)
    return "; ".join(parts) or "none"


def blocking(item, decisions):
    ids = json.loads(item["attributes"].get("submission_blocking_decisions", "[]"))
    return [(d, decisions.get(d, "").endswith("UNRESOLVED")) for d in ids]


def readiness(items):
    status = Counter(i["attributes"].get("submission_status") for i in items)
    base = len(items) - status["not_applicable"]
    done = status["submitted"] + status["accepted"]
    return (f"{100 * done / base:.0f} % ({done} of {base})" if base else "n/a (all not applicable)"), status


def render_submission(submission, decisions):
    items = [i for s in submission["sections"] for i in s["items"]]
    values = submission["attributes"]
    ready, status = readiness(items)
    model = Counter(i["attributes"].get("submission_model_status") for i in items)
    blocked = sum(any(u for _, u in blocking(i, decisions)) for i in items)
    lines = [f"# {submission['title']}", "",
             "Generated by `acceptance/submissions/make_reports.py` from the saved model. Do not edit; change the model "
             "and regenerate. The model is a candidate architecture: a link shows where a value should come from, "
             "not that the submission is complete or compliant.", "",
             submission["overview"], "",
             "| Format | Penalty group | Official due | Cancellation | Folder status |", "|---|---|---|---|---|",
             f"| {cell(values.get('submission_format', 'TBD'))} | {cell(values.get('submission_penalty_group', 'TBD'))} | "
             f"{cell(values.get('submission_due', 'TBD'))} | {cell(values.get('submission_cancellation', 'TBD'))} | "
             f"{cell(values.get('submission_status', 'TBD'))} |", "",
             f"**Readiness:** {ready}, counting submitted and accepted items and leaving out not_applicable ones.", "",
             f"**Items:** {len(items)}. Model status: " + ", ".join(f"{k} {model[k]}" for k in prep.VOCAB["submission_model_status"]) +
             ". Team status: " + ", ".join(f"{k} {status[k]}" for k in prep.VOCAB["submission_status"] if status[k]) +
             f". Blocked by an unresolved decision: {blocked}.", ""] + LEGEND
    gated = Counter(d for i in items for d, _ in blocking(i, decisions))
    if gated:
        lines += ["## Blocking decisions", "", "| Decision | Question | Current constraint name | Items gated |", "|---|---|---|---|"]
        lines += [f"| {d} | {cell(QUESTIONS.get(d, 'TBD'))} | {cell(decisions.get(d, 'missing from the model'))} | {n} |"
                  for d, n in sorted(gated.items())]
        unresolved = [(n, d) for d, n in gated.items() if decisions.get(d, "").endswith("UNRESOLVED")]
        if unresolved:
            n, d = max(unresolved)
            lines += ["", f"**Next step:** resolve {d} first; it gates {n} of the {len(items)} items. Record the choice "
                      f"as step 2 of [Using the model as a development platform]({DEVELOP}) describes, then review the "
                      "model status of the items it gates."]
        lines += [""]
    governing = json.loads(values.get("submission_governing_rules", "[]"))
    if governing:
        lines += ["## Governing rules", ""] + [f"- {cell(g)}" for g in governing] + [""]
    for section in submission["sections"]:
        lines += [f"## {section['title']}", "",
                  "| ID | Item | Source | Model | Status | Owner | Due | Value | Evidence | Rules | Anchors | Blocking | Note |",
                  "|---|---|---|---|---|---|---|---|---|---|---|---|---|"]
        for item in section["items"]:
            a = item["attributes"]
            chapter = "" if item["section"] in (section["title"], "") else f"({item['section'].split(' - ', 1)[-1]}) "
            block = ", ".join(d + (" " + BLOCKED if unresolved else "") for d, unresolved in blocking(item, decisions))
            lines.append("| " + " | ".join(cell(x) for x in (
                a.get("submission_item_id", item["id"]), chapter + item["item"], a.get("submission_source_kind"),
                a.get("submission_model_status"), a.get("submission_status"), a.get("submission_owner"), a.get("submission_due"),
                a.get("submission_value"), a.get("submission_evidence_ref"), rules_cell(item), anchors_cell(item),
                block or "none", a.get("submission_note", ""))) + " |")
        lines.append("")
    return "\n".join(lines)


def render(submissions, decisions):
    """{file name: markdown} for every submission plus the index."""
    pages = {s["report"] + ".md": render_submission(s, decisions) for s in submissions}
    index = ["# Competition submissions", "",
             "One page per Formula SAE 2027 submission, generated from the model by "
             "`acceptance/submissions/make_reports.py`. Readiness counts submitted and accepted items and leaves out "
             f"not_applicable ones. An item is blocked while a decision it needs still has a constraint name ending in UNRESOLVED ({BLOCKED}).",
             "", "The ESO/ESA form (Electrical System Officer and Advisor details, PDF, Tech group) is a separate upload "
             "with no checklist of its own. Like the SES and ESF, missing it is grounds for removing the entry. It is "
             "tracked as GEN-PS1-02 on the [general page](general.md).", ""] + LEGEND + ["| Submission | Items | Present | Partial | Absent | Blocked | Readiness |", "|---|---|---|---|---|---|---|"]
    for s in submissions:
        items = [i for sec in s["sections"] for i in sec["items"]]
        model = Counter(i["attributes"].get("submission_model_status") for i in items)
        blocked = sum(any(u for _, u in blocking(i, decisions)) for i in items)
        index.append(f"| [{cell(s['title'])}]({s['report']}.md) | {len(items)} | {model['present']} | {model['partial']} | "
                     f"{model['absent']} | {blocked} | {readiness(items)[0]} |")
    pages["README.md"] = "\n".join(index + [""])
    return pages


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", type=Path, default=prep.MODEL, help="the .aird file to read")
    parser.add_argument("--out", type=Path, default=DOCS)
    parser.add_argument("--check", action="store_true", help="only check the submission_* values against the vocabularies")
    args = parser.parse_args()
    submissions, decisions = read_model(args.model)
    found = problems(submissions)
    if found:
        print("\n".join(found))
        sys.exit(1)
    if args.check:
        print("All submission_* values are inside their vocabularies.")
        return
    args.out.mkdir(parents=True, exist_ok=True)
    for name, text in render(submissions, decisions).items():
        prep.write(args.out / name, text)
    print("Wrote " + str(len(submissions) + 1) + " pages to " + str(args.out))


if __name__ == "__main__":
    main()
