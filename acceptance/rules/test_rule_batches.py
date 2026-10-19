import copy
import hashlib
import json
import shutil
import unittest
import uuid
from pathlib import Path

import prepare_batches as batches


class RuleBatchTests(unittest.TestCase):
    def setUp(self):
        test_root = batches.ROOT.parent / ".build-tools/rule-batch-tests"
        test_root.mkdir(parents=True, exist_ok=True)
        self.directory = test_root / ("case-" + uuid.uuid4().hex)
        self.directory.mkdir()
        self.source = []
        self.rows = []
        for number in range(3):
            text = "Measured source paragraph " + str(number)
            identifier = "FSAE-2027-V1:EV.3.3." + str(number + 1) + ":p001"
            self.source.append({"clause_id": identifier, "text": text})
            self.rows.append({"clause_id": identifier, "source_document_id": "FSAE-2027-V1", "source_sha256": "pinned-pdf",
                              "source_text_sha256": batches.sha(text), "source_rule_id": "EV.3.3." + str(number + 1),
                              "source_pages": [93], "title": "Limits", "source_character_count": len(text),
                              "applicability": "applicable", "classification": "design", "numeric_facts": [],
                              "coverage_status": "not_modeled", "verification_status": "not_verified",
                              "model_element_ids": [], "verification_element_ids": []})
        self.inventory = {"source_document_id": "FSAE-2027-V1", "clauses": self.rows, "source_objects": []}
        self.output = self.directory / "drafts"
        self.ledger = self.directory / "ledger.json"

    def tearDown(self):
        expected_root = (batches.ROOT.parent / ".build-tools/rule-batch-tests").resolve()
        assert self.directory.resolve().is_relative_to(expected_root)
        shutil.rmtree(self.directory)

    def prepare(self, **arguments):
        return batches.prepare(self.inventory, self.source, [], self.output, self.ledger, **arguments)

    def test_every_clause_is_bounded_and_stays_unmodeled(self):
        index = self.prepare(max_records=2)
        self.assertEqual([2, 1], [len(item["clause_ids"]) for item in index["batches"]])
        ledger = batches.read_json(self.ledger)
        self.assertEqual(3, len(ledger["clauses"]))
        self.assertEqual({"not_modeled"}, {item["coverage_status"] for item in ledger["clauses"].values()})
        self.assertEqual({"not_verified"}, {item["verification_status"] for item in ledger["clauses"].values()})
        self.assertEqual("not_claimed", ledger["compliance_status"])

    def test_rerun_preserves_existing_decisions_and_trace_ids(self):
        self.prepare()
        ledger = batches.read_json(self.ledger)
        row = ledger["clauses"][self.rows[0]["clause_id"]]
        row.update({"coverage_status": "partially_modeled", "model_element_ids": ["real-model-id"], "applicability": "conditional"})
        batches.write_json(self.ledger, ledger)
        self.prepare(max_records=1)
        retained = batches.read_json(self.ledger)["clauses"][self.rows[0]["clause_id"]]
        self.assertEqual("partially_modeled", retained["coverage_status"])
        self.assertEqual(["real-model-id"], retained["model_element_ids"])
        self.assertEqual("conditional", retained["applicability"])

    def test_source_checksum_mismatch_stops_preparation(self):
        self.source[0]["text"] += " injected"
        with self.assertRaisesRegex(ValueError, "checksum mismatch"):
            self.prepare()
        self.assertFalse(self.ledger.exists())

    def test_seed_is_bound_to_the_exact_source_and_not_a_compliance_claim(self):
        seed = {"clause_id": self.rows[0]["clause_id"], "source_text_sha256": self.rows[0]["source_text_sha256"], "short_paraphrase": "Reviewed paraphrase"}
        index = batches.prepare(self.inventory, self.source, [seed], self.output, self.ledger)
        records = batches.read_json(self.output / (index["batches"][0]["batch_id"] + ".records.json"))
        self.assertEqual("Reviewed paraphrase", records[0]["text"])
        self.assertIsNone(records[1]["text"])
        self.assertEqual("not_modeled", records[0]["attributes"]["coverage_status"])
        seed["source_text_sha256"] = "different-edition"
        with self.assertRaisesRegex(ValueError, "Seed source checksum mismatch"):
            batches.prepare(self.inventory, self.source, [seed], self.output, self.ledger)

    def test_unassigned_or_duplicate_source_text_is_rejected(self):
        self.source.append(copy.deepcopy(self.source[0]))
        with self.assertRaisesRegex(ValueError, "match every inventory clause"):
            self.prepare()

    def test_private_source_cannot_be_written_inside_repository(self):
        with self.assertRaisesRegex(ValueError, "outside the repository"):
            batches.prepare(self.inventory, self.source, [], batches.ROOT / "test-models/unsafe-output", self.ledger)

    def test_single_oversized_paragraph_and_unbounded_parameters_rejected(self):
        self.source[0]["text"] = "x" * 1001
        self.rows[0]["source_text_sha256"] = batches.sha(self.source[0]["text"])
        with self.assertRaisesRegex(ValueError, "exceeds"):
            self.prepare(max_source_chars=1000)
        with self.assertRaisesRegex(ValueError, "Batch limits"):
            self.prepare(max_records=101)

    def test_literal_strings_cannot_interpolate_or_break_groovy_draft(self):
        self.assertEqual("'quote\\' \\n $name \\\\ tail'", batches.groovy("quote' \n $name \\ tail"))
        self.assertEqual("[:]", batches.groovy({}))
        self.assertEqual("null", batches.groovy(None))

    def test_unnumbered_objects_keep_null_source_rule_and_native_safe_attributes(self):
        object_row = self.rows.pop()
        object_row.update({"source_rule_id": None, "source_object_id": "TABLE-PS-1", "source_section_id": "PS", "source_object_type": "table"})
        self.inventory["source_objects"].append(object_row)
        index = self.prepare()
        records = batches.read_json(self.output / (index["batches"][0]["batch_id"] + ".records.json"))
        self.assertNotIn("source_rule_id", records[-1]["attributes"])
        self.assertEqual("TABLE-PS-1", records[-1]["attributes"]["source_object_id"])
        self.assertEqual("PS", records[-1]["chapter"])
        self.assertTrue(all(isinstance(value, (str, int, float, bool)) for value in records[-1]["attributes"].values()))

    def test_script_preserves_existing_native_records_and_requires_activated_viewpoint(self):
        index = self.prepare()
        script = (self.output / (index["batches"][0]["batch_id"] + ".groovy")).read_text(encoding="utf-8")
        self.assertIn("requirements.viewpointEnabled()", script)
        self.assertIn("requirements.find(it.identifier) == null", script)
        self.assertIn("requirements.importRequirements(module, pending)", script)
        self.assertNotIn("enableViewpoint()", script)


class PinnedInventoryTests(unittest.TestCase):
    def test_pinned_inventory_and_all_clause_provenance_are_consistent(self):
        directory = Path(__file__).parent
        inventory = batches.read_json(directory / "rule-inventory.json")
        clauses = batches.read_json(directory / "normalized-clauses.json")
        manifest = batches.read_json(directory / "source-manifest.json")
        nodes = {row["source_rule_id"]: row for row in inventory["rule_nodes"]}
        self.assertEqual(1825, len(nodes))
        self.assertEqual(3177, len(clauses["clauses"]))
        self.assertEqual(20, len(clauses["source_objects"]))
        self.assertEqual(0, manifest["validation"]["unassigned_body_line_count"])
        self.assertEqual([], manifest["validation"]["duplicate_rule_ids"])
        self.assertEqual(["D.8.1"], manifest["validation"]["missing_parent_rule_ids"])
        self.assertFalse(manifest["validation"]["semantic_review_complete"])
        self.assertFalse(manifest["validation"]["visual_review_complete"])
        all_rows = clauses["clauses"] + clauses["source_objects"]
        self.assertEqual(len(all_rows), len({row["clause_id"] for row in all_rows}))
        self.assertEqual({manifest["main_rulebook"]["sha256"]}, {row["source_sha256"] for row in all_rows})
        for row in all_rows:
            self.assertTrue(row["source_pages"])
            self.assertTrue(set(row.get("referenced_rule_ids", [])).issubset(nodes))
            self.assertEqual("not_modeled", row["coverage_status"])
            self.assertEqual("not_verified", row["verification_status"])
        for node in nodes.values():
            self.assertEqual(node["paragraph_count"], len(node["paragraph_ids"]))
        limits = {row["source_rule_id"]: row for row in clauses["clauses"] if row["source_rule_id"] in ("EV.3.3.1", "EV.3.3.2")}
        self.assertIn(("80", "kW"), [(fact["value"], fact["unit"]) for fact in limits["EV.3.3.1"]["numeric_facts"]])
        self.assertIn(("600", "V DC"), [(fact["value"], fact["unit"]) for fact in limits["EV.3.3.2"]["numeric_facts"]])


if __name__ == "__main__":
    unittest.main()
