"""Prepare-step contracts; native graph checks are in 40-verify-submissions.groovy."""
import json
import tempfile
import unittest
from collections import Counter
from pathlib import Path

import make_reports
import prepare_submissions as prep

HERE = Path(__file__).resolve().parent
DATA = prep.load(prep.DATA)
LEDGER = prep.load(prep.RULES / "clause-ledger.json")["clauses"]
BY_RULE = prep.ledger_index(LEDGER)
RULE_IDS = {n["source_rule_id"] for n in prep.load(prep.RULES / "rule-inventory.json")["rule_nodes"]}
ITEMS = [item for s in DATA["submissions"] for item in s["items"]]


def resolve(refs):
    return prep.resolve_refs(refs, LEDGER, BY_RULE, RULE_IDS)


class ReferenceTest(unittest.TestCase):
    def test_short_forms_get_the_source_prefix(self):
        self.assertEqual(prep.normalize_ref("TABLE-PS-1:p006"), ("clause", "FSAE-2027-V1:TABLE-PS-1:p006"))
        self.assertEqual(prep.normalize_ref("IN.5.1:p006"), ("clause", "FSAE-2027-V1:IN.5.1:p006"))
        self.assertEqual(prep.normalize_ref("EV.5.1.1:p004"), ("clause", "FSAE-2027-V1:EV.5.1.1:p004"))
        self.assertEqual(prep.normalize_ref("FSAE-2027-V1:F.2.2.1:p001"), ("clause", "FSAE-2027-V1:F.2.2.1:p001"))
        self.assertEqual(prep.normalize_ref("F.3.2.1"), ("rule", "F.3.2.1"))

    def test_bare_rule_links_all_paragraphs_unless_the_item_names_some(self):
        everything, _ = resolve(["F.3.2.1"])
        self.assertEqual(everything, BY_RULE["F.3.2.1"])
        self.assertGreater(len(everything), 1)
        named, _ = resolve(["F.3.2.1", "F.3.2.1:p003"])
        self.assertEqual(named, ["FSAE-2027-V1:F.3.2.1:p003"])

    def test_headings_stay_text_only_and_unknown_refs_fail(self):
        derive, text_only = resolve(["F.8", "T.3", "F.5.12"])
        self.assertEqual((derive, text_only), ([], ["F.8", "T.3", "F.5.12"]))
        with self.assertRaises(ValueError):
            resolve(["F.99.1"])
        with self.assertRaises(ValueError):
            resolve(["F.2.2.1:p099"])

    def test_items_without_derive_links_have_only_heading_refs(self):
        bare = [i for i in ITEMS if not i["derive_clauses"]]
        self.assertIn("DES-EVT-05", [i["id"] for i in bare])
        for item in bare:
            self.assertEqual(item["text_only_refs"], item["rule_refs"], item["id"])


class DataTest(unittest.TestCase):
    def test_verified_items_are_unique_and_inside_the_vocabularies(self):
        self.assertEqual(len(ITEMS), 588)
        self.assertEqual(len({i["id"] for i in ITEMS}), 588)
        self.assertEqual(Counter(s["id"] for s in DATA["submissions"] for _ in s["items"]),
                         Counter(SES=141, IAD=84, ESF=150, DES=79, COST=45, PRES=37, GEN=52))
        prep.check(DATA, LEDGER, BY_RULE, RULE_IDS)

    def test_duplicate_ids_and_bad_vocabulary_are_rejected(self):
        for change in ({"id": ITEMS[0]["id"]}, {"source_kind": "guess"}, {"model_status": "done"}):
            data = json.loads(json.dumps(DATA))
            data["submissions"][0]["items"][1].update(change)
            with self.assertRaises(AssertionError):
                prep.check(data, LEDGER, BY_RULE, RULE_IDS)

    def test_unknown_values_are_tbd_and_nothing_is_started(self):
        for submission in DATA["submissions"]:
            self.assertEqual((submission["due"], submission["cancellation"]), ("TBD", "TBD"))
        self.assertEqual(set(prep.ITEM_DEFAULTS.values()), {"TBD"})
        statuses = {i["id"]: prep.item_attributes("GEN", i)["submission_status"] for i in ITEMS}
        self.assertEqual(sorted(k for k, v in statuses.items() if v == "not_applicable"), ["GEN-PEN-09", "GEN-PS1-11"])
        self.assertEqual(set(statuses.values()), {"not_started", "not_applicable"})


class AnchorRuleTest(unittest.TestCase):
    INDEX = {
        "FSAE2027:powertrain Equipment": [("PhysicalComponent", "PA", "u1")],
        "FSAE2027:SD Remove motor torque on shutdown": [(c, l, "f" + l) for c, l in
                                                        (("OperationalActivity", "OA"), ("SystemFunction", "SA"),
                                                         ("LogicalFunction", "LA"), ("PhysicalFunction", "PA"))],
        "FSAE2027:DEC-CELLS UNRESOLVED": [("Constraint", "SA", "d1")],
        "FSAE2027:VP Apply tubing classes verification NOT_VERIFIED": [("Constraint", "SA", "v1")],
        "FSAE2027:VP Bounded slice dependencies and exclusions OPEN": [("Constraint", "SA", "o1")],
        "FSAE2027:DD guard Trip brake conflict": [("Constraint", "LA", "g1"), ("Constraint", "PA", "g2")],
        "FSAE2027:twice": [("PhysicalComponent", "PA", "t1"), ("PhysicalComponent", "PA", "t2")],
    }

    def freeze(self, text):
        return prep.freeze_anchor(text, self.INDEX, {"FSAE2027:VP:obligation:AIP", "FSAE-2027-V1:F.8.2.1:p001"}, {"v1"})

    def test_each_anchor_class_gets_its_link(self):
        anchors, decisions, text = self.freeze(
            "FSAE2027:VP:obligation:AIP; FSAE2027:powertrain Equipment; Constraint FSAE2027:DEC-CELLS UNRESOLVED; "
            "FSAE2027:VP Apply tubing classes verification NOT_VERIFIED; FSAE2027:SD Remove motor torque on shutdown; "
            "FSAE2027:DD guard Trip brake conflict; FSAE2027:VP Bounded slice dependencies and exclusions OPEN; "
            "Diagram 'FSAE 2027 LA logical subsystems (candidate)'")
        self.assertEqual([(a["link"], a["class"], a["layer"]) for a in anchors], [
            ("derive", "Requirement", "SA"), ("satisfy", "PhysicalComponent", "PA"), ("verify", "Constraint", "SA"),
            ("satisfy", "SystemFunction", "SA"), ("satisfy", "Constraint", "PA")])
        self.assertEqual(decisions, ["DEC-CELLS"])
        self.assertEqual(text, "FSAE2027:VP Bounded slice dependencies and exclusions OPEN; "
                               "Diagram 'FSAE 2027 LA logical subsystems (candidate)'")

    def test_explicit_class_wildcards_and_none_are_honoured(self):
        anchors, _, _ = self.freeze("OperationalActivity FSAE2027:SD Remove motor torque on shutdown")
        self.assertEqual((anchors[0]["class"], anchors[0]["layer"]), ("OperationalActivity", "OA"))
        for text in ("none (should hang off FSAE2027:powertrain Equipment)",
                     "Requirements FSAE-2027-V1:F.8.2.1:p001 to p005", "FSAE2027:VP <obligation> verification"):
            self.assertEqual(self.freeze(text), ([], [], text))

    def test_missing_or_ambiguous_names_fail(self):
        with self.assertRaises(ValueError):
            self.freeze("FSAE2027:unknown Equipment")
        with self.assertRaises(ValueError):
            self.freeze("FSAE2027:twice")

    def test_frozen_anchors_follow_the_rule(self):
        counts = Counter(a["link"] for i in ITEMS for a in i["anchors"])
        self.assertEqual(set(counts), {"derive", "verify", "satisfy"})
        for item in ITEMS:
            self.assertFalse([a for a in item["anchors"] if a["class"] == "Part" or ":DEC-" in a["name"]], item["id"])
            if item["model_status"] != "absent":
                self.assertTrue(item["anchors"] or item["derive_clauses"], item["id"])


class CopyCheckTest(unittest.TestCase):
    PARAGRAPHS = ["Each purple lantern shall be polished by two volunteers before the parade review closes."]

    def test_planted_copied_sentence_is_rejected(self):
        data = json.loads(json.dumps(DATA))
        self.assertEqual(prep.copied_runs(prep.strings(data), self.PARAGRAPHS), [])
        data["submissions"][2]["items"][4]["note"] = "Note: lantern shall be polished by two volunteers before the parade, see photos."
        found = prep.copied_runs(prep.strings(data), self.PARAGRAPHS)
        self.assertEqual(len(found), 1)
        self.assertIn("items[4].note", found[0][0])
        self.assertEqual(found[0][1], "lantern shall be polished by two volunteers before")

    def test_seven_words_or_changed_punctuation(self):
        self.assertEqual(prep.copied_runs([("x", "lantern shall be polished by two volunteers")], self.PARAGRAPHS), [])
        self.assertTrue(prep.copied_runs([("x", "Lantern shall be POLISHED, by two “volunteers” before the")], self.PARAGRAPHS))


class GenerationTest(unittest.TestCase):
    def test_generated_files_are_reproducible_and_bounded(self):
        with tempfile.TemporaryDirectory() as directory:
            written = prep.generate(DATA, LEDGER, Path(directory))
            for name, size in written.items():
                self.assertLess(size, prep.CHUNK_CHARS, name)
                self.assertEqual((Path(directory) / name).read_text(encoding="utf-8"), (HERE / name).read_text(encoding="utf-8"), name)
            manifest = (Path(directory) / "expected-manifest.json").read_text(encoding="utf-8")
            self.assertEqual(manifest, (HERE / "expected-manifest.json").read_text(encoding="utf-8"))
        imports = sorted(n for n in written if n.startswith("31-import-"))
        self.assertEqual([n.replace("31-import-", "32-trace-") for n in imports], sorted(n for n in written if n.startswith("32-trace-")))
        self.assertEqual(len(imports), 13)
        for submission in DATA["submissions"]:
            for part in prep.chunks(submission["items"]):
                self.assertLessEqual(len(part), prep.CHUNK_ITEMS)
        self.assertEqual(sorted(p.name for p in HERE.glob("3[12]-*-[0-9].groovy")), sorted(n for n in written if n[:3] in ("31-", "32-")))

    def test_manifest_counts_and_link_sets(self):
        manifest = prep.load(HERE / "expected-manifest.json")
        self.assertEqual(manifest["counts"]["items"], 588)
        self.assertEqual(manifest["counts"]["submission_folders"], 7)
        by_id = {i["identifier"]: i for i in manifest["items"]}
        tub = by_id["FSAE2027:SUB:item:SES-TUB-02"]
        self.assertIn("FSAE-2027-V1:F.3.2.1:p003", tub["derive"])
        self.assertFalse([c for c in tub["derive"] if c.startswith("FSAE-2027-V1:F.3.2.1:") and not c.endswith("p003")])
        folders = {f["identifier"]: f for f in manifest["folders"]}
        self.assertEqual(folders["FSAE2027:SUB:COST"]["derive"], ["FSAE-2027-V1:TABLE-PS-1:p008", "FSAE-2027-V1:TABLE-PS-1:p009", "FSAE-2027-V1:TABLE-PS-1:p010"])
        self.assertEqual(folders["FSAE2027:SUB:IAD"]["derive"], ["FSAE-2027-V1:F.8.8.1:p001"])
        self.assertEqual(folders["FSAE2027:SUB:COST:ADD"]["satisfy"], ["OA|OperationalActivity|" + prep.INSPECT])
        self.assertEqual(sum(f["satisfy"] == ["OA|OperationalActivity|" + prep.PREPARE] for f in manifest["folders"]), 7)

    def test_scripts_use_native_guards(self):
        create = (HERE / "30-create.template.groovy").read_text(encoding="utf-8")
        self.assertIn("isSuperTypeOf(folder.eClass())", create)
        self.assertIn("req.find(@@MODULE_ID@@) == null", create)
        trace = (HERE / "32-trace.template.groovy").read_text(encoding="utf-8")
        self.assertIn("hasNext()", trace)
        self.assertNotIn("req.find(", trace)
        self.assertIn("'Ambiguous model anchor '", trace)
        self.assertIn("importRequirements(folder", (HERE / "31-import.template.groovy").read_text(encoding="utf-8"))
        status = [line for line in (HERE / "50-update-status.template.groovy").read_text(encoding="utf-8").splitlines()
                  if not line.startswith("//")]
        status = " ".join(status)
        self.assertNotIn("satisfy(", status)
        self.assertNotIn("submission_model_status", status)


class StatusTest(unittest.TestCase):
    def status(self, rows):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "status.csv"
            path.write_text("item_id,status,owner,due,value,evidence_ref\n" + "".join(r + "\n" for r in rows), encoding="utf-8")
            return prep.read_status(path, DATA)

    def test_valid_rows_become_attribute_updates(self):
        rows = self.status(["SES-TUB-02,in_progress,Chassis lead,2027-01-15,,", "GEN-LOC-01,,,,,upload receipt 3"])
        self.assertEqual(rows[0], dict(id="FSAE2027:SUB:item:SES-TUB-02", values=dict(
            submission_status="in_progress", submission_owner="Chassis lead", submission_due="2027-01-15")))
        self.assertEqual(rows[1]["values"], dict(submission_evidence_ref="upload receipt 3"))
        script = prep.status_script(rows)
        self.assertIn("'FSAE2027:SUB:item:SES-TUB-02'", script)

    def test_vocabulary_dates_and_ids_are_checked_first(self):
        for row in ("SES-TUB-02,done,,,,", "SES-TUB-02,,,15/01/2027,,", "SES-XXX-99,in_progress,,,,"):
            with self.assertRaises(ValueError):
                self.status([row])
        with self.assertRaises(ValueError):
            self.status(["SES-TUB-02,in_progress,,,,", "SES-TUB-02,submitted,,,,"])


def fresh_model_view():
    """What make_reports.read_model returns right after the build, built from submissions.json."""
    submissions = []
    for s in DATA["submissions"]:
        sections = []
        for prefix, title in prep.sections_of(s).items():
            items = []
            for item in [i for i in s["items"] if prep.section_id(i["id"]) == prefix]:
                links = prep.item_links(item)
                attributes = dict(prep.COMMON, **prep.ITEM_DEFAULTS, **prep.item_attributes(s["id"], item))
                items.append(dict(id=prep.item_identifier(item["id"]), item=item["item"], section=item["section"], attributes=attributes,
                                  links=[dict(kind="derive", cls="Requirement", name=c) for c in links["derive"]] +
                                        [dict(kind=k, cls=key.split("|")[1], name=key.split("|")[2]) for k in ("verify", "satisfy") for key in links[k]]))
            sections.append(dict(id=prep.folder_id(prefix), title=title, items=items))
        folder = next(f for f in prep.folders(DATA) if f["identifier"] == "FSAE2027:SUB:" + s["id"])
        submissions.append(dict(id=s["id"], report=s["report"], title=s["title"], overview=s["overview"],
                                attributes=dict(prep.COMMON, **folder["attributes"]), links=[], sections=sections))
    return submissions


class ReportTest(unittest.TestCase):
    def test_pages_for_every_submission_and_index(self):
        pages = make_reports.render(fresh_model_view(), DATA["decisions"])
        self.assertEqual(sorted(pages), ["README.md", "cost.md", "design.md", "esf.md", "general.md", "iad.md", "presentation.md", "ses.md"])
        self.assertIn("0 % (0 of 141)", pages["ses.md"])
        self.assertIn("SES-TUB-02", pages["ses.md"])
        self.assertIn("F.3.2.1 p003", pages["ses.md"])
        self.assertIn("F.8 (text only)", pages["design.md"])
        self.assertIn("DEC-CHASSIS " + make_reports.BLOCKED, pages["ses.md"])
        self.assertIn("| [Electrical System Form (ESF)](esf.md) | 150 |", pages["README.md"])

    def test_check_finds_values_outside_the_vocabularies(self):
        submissions = fresh_model_view()
        self.assertEqual(make_reports.problems(submissions), [])
        item = submissions[0]["sections"][0]["items"][0]
        item["attributes"]["submission_status"] = "done"
        item["attributes"]["submission_due"] = "next week"
        self.assertEqual(len(make_reports.problems(submissions)), 2)


if __name__ == "__main__":
    unittest.main()
