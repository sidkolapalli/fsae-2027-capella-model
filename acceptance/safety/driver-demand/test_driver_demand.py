"""Source-selection and model-draft contracts; native graph checks are in 40-verify-driver-demand.groovy."""
import hashlib
import json
from pathlib import Path
import unittest

import prepare_slice

HERE = Path(__file__).resolve().parent
SLICE = json.loads((HERE / "obligations.json").read_text(encoding="utf-8"))
GROUPS = {item["id"]: item for item in SLICE["obligations"]}
INVENTORY = json.loads((prepare_slice.RULES / "normalized-clauses.json").read_text(encoding="utf-8"))


class DriverDemandSourceTest(unittest.TestCase):
    def test_exact_selection_and_pinned_provenance(self):
        selected = [ref for item in GROUPS.values() for ref in item["source_refs"]]
        self.assertEqual(len(GROUPS), 15)
        self.assertEqual(len(selected), 57)
        self.assertEqual(len({ref["clause_id"] for ref in selected}), 57)
        expected_rules = {rule for spec in prepare_slice.GROUPS for rule in spec["rules"]}
        expected = {item["clause_id"]: item for item in INVENTORY["clauses"] if item["source_rule_id"] in expected_rules}
        self.assertEqual(set(expected), {ref["clause_id"] for ref in selected})
        for ref in selected:
            self.assertEqual(ref["source_sha256"], prepare_slice.SOURCE_HASH)
            self.assertEqual(ref["source_text_sha256"], expected[ref["clause_id"]]["source_text_sha256"])
            self.assertEqual(ref["source_pages"], expected[ref["clause_id"]]["source_pages"])
            self.assertTrue(ref["source_pages"])
            self.assertEqual(len(ref["source_text_sha256"]), 64)
        for excluded in ("T.4.2.6", "T.4.2.11", "T.4.3.5", "EV.4.5", "EV.4.6", "T.3.2.4", "EV.3.2.1"):
            self.assertNotIn(excluded, expected_rules)

    def test_every_obligation_has_owner_conditions_and_pending_evidence(self):
        for item in GROUPS.values():
            self.assertTrue(item["functions"])
            self.assertTrue(item["owner"])
            self.assertTrue(item["conditions"])
            self.assertTrue(item["checks"])
            self.assertEqual(item["coverage_status"], "prepared_not_applied")
            self.assertEqual(item["verification_status"], "not_verified")
            self.assertEqual(item["evidence_status"], "not_available")
            self.assertEqual(item["compliance_status"], "not_claimed")
            self.assertNotIn("source_text", item)

    def test_strict_thresholds_and_source_examples_keep_their_meaning(self):
        deviation, persistence, example = GROUPS["DD-05"]["numeric"]
        self.assertEqual((deviation["value"], deviation["operator"]), (10, ">"))
        self.assertEqual((persistence["value"], persistence["unit"], persistence["operator"]), (100, "ms", ">"))
        self.assertIn("source example", example["role"])
        trip, release = GROUPS["DD-10"]["numeric"]
        self.assertEqual((trip["value"], trip["operator"]), (25, ">"))
        self.assertEqual((release["value"], release["operator"]), (5, "<"))
        duration, power, current = GROUPS["DD-12"]["numeric"]
        self.assertEqual((duration["value"], duration["operator"]), (0.5, ">"))
        self.assertEqual((power["value"], power["unit"]), (5, "kW"))
        self.assertIn("not a measured-power comparator", power["role"])
        self.assertEqual(current["expression"], "5000 W / U_nominal")
        self.assertIn("unresolved", current["role"])
        self.assertIn("candidate interpretation", GROUPS["DD-02"]["numeric"][0]["role"])

    def test_generated_scripts_are_reproducible_and_use_native_guards(self):
        payload = [dict(group,
                        conditions_json=json.dumps(group["conditions"], ensure_ascii=False, separators=(",", ":")),
                        numeric_json=json.dumps(group["numeric"], ensure_ascii=False, separators=(",", ":")),
                        source_ids_json=json.dumps([ref["clause_id"] for ref in group["source_refs"]], separators=(",", ":")))
                   for group in SLICE["obligations"]]
        for name in ("30-driver-demand", "35-repair-driver-demand-native-semantics", "40-verify-driver-demand"):
            template = (HERE / f"{name}.template.groovy").read_text(encoding="utf-8")
            generated = (HERE / f"{name}.groovy").read_text(encoding="utf-8")
            self.assertEqual(generated, template.replace("/* OBLIGATIONS */", "def obligations = " + prepare_slice.groovy(payload)))
            self.assertIn("URI.segment(1)", generated)
            self.assertNotIn("groovy.json", generated)
        model = (HERE / "30-driver-demand.template.groovy").read_text(encoding="utf-8")
        self.assertIn("req.createModule(api.layer('SA'),'FSAE2027:DD:module'", model)
        self.assertIn("api.create('StateMachine'", model)
        self.assertIn("api.create('StateTransition'", model)
        self.assertIn("fn.availableInStates.add(states[stateName])", model)
        self.assertNotIn("availableAbstractFunctions.add", model)
        self.assertIn("feature.changeable && !feature.derived", model)
        verifier = (HERE / "40-verify-driver-demand.template.groovy").read_text(encoding="utf-8")
        self.assertIn("fnByKey['bspd-trip'][layer].availableInStates.toSet() == [requested].toSet()", verifier)
        self.assertIn("req.derive(derived,source)", model)
        self.assertIn("req.verify(source,verification)", model)
        self.assertIn("req.satisfy(it,source)", model)
        self.assertNotIn("source_module',", model)

    def test_native_semantic_repair_preserves_identity_and_uses_leaf_interfaces(self):
        repair = (HERE / "35-repair-driver-demand-native-semantics.template.groovy").read_text(encoding="utf-8")
        self.assertIn("(field == 'source' ? fn.outputs : fn.inputs).add(port)", repair)
        self.assertIn("sm.ownedRegions.add(region)", repair)
        self.assertIn("api.id(exchange.source) == before[api.id(exchange)].source", repair)
        self.assertNotIn("api.delete", repair)
        self.assertNotIn("remove(allocation", repair)
        self.assertIn("createFunctionalExchangeRealization()", repair)
        self.assertIn("createPortRealization()", repair)
        self.assertIn("lower[field].ownedPortRealizations.add(relation)", repair)
        self.assertNotIn("req.find(", repair)
        self.assertIn("req.requirements(sourceModule) + req.requirements(derivedModule)", repair)
        self.assertIn("new java.util.LinkedHashSet(ids)", repair)
        self.assertIn("addOwners(requirement,uniqueAdditions)", repair)
        model = (HERE / "30-driver-demand.template.groovy").read_text(encoding="utf-8")
        self.assertIn("name,[guardRegions[index]]", model)
        self.assertIn("functions['tractive-current'][layer]", model)


if __name__ == "__main__":
    unittest.main()
