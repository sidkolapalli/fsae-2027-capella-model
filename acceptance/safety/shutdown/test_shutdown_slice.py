import hashlib
import json
from pathlib import Path
import unittest
import build_shutdown_slice as builder

HERE = Path(__file__).parent


class ShutdownDraftTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.report = json.loads((HERE / "shutdown-obligations.json").read_text(encoding="utf-8"))
        cls.script = (HERE / "30-model-shutdown.groovy").read_text(encoding="utf-8")
        corpus = json.loads((builder.RULES / "normalized-clauses.json").read_text(encoding="utf-8"))
        cls.rows = {row["clause_id"]: row for row in corpus["clauses"]}

    def test_every_owned_record_retains_exact_pinned_provenance(self):
        owned = {key for key, row in self.rows.items() if (row.get("source_rule_id") or "").startswith("EV.8.") and not row["source_rule_id"].startswith("EV.8.7")}
        prose = [source for group in self.report["obligations"] for source in group["source_clauses"]]
        artifacts = self.report["figure_label_records"]
        self.assertEqual(135, len(owned))
        self.assertEqual(122, len(prose))
        self.assertEqual(13, len(artifacts))
        self.assertEqual(owned, {row["clause_id"] for row in prose + artifacts})
        self.assertEqual(len(prose), len({row["clause_id"] for row in prose}))
        for source in prose:
            original = self.rows[source["clause_id"]]
            self.assertEqual(builder.PINNED_SHA, source["source_sha256"])
            self.assertEqual(original["source_text_sha256"], source["source_text_sha256"])
            self.assertEqual(original["source_pages"], source["source_pages"])
        self.assertEqual([105, 106, 107, 108, 109, 110], self.report["prose_review_pages"])

    def test_figure_labels_and_bspd_internals_are_not_normative_allocations(self):
        assigned = {source["clause_id"] for group in self.report["obligations"] for source in group["source_clauses"]}
        self.assertNotIn("FSAE-2027-V1:EV.8.1.7:p002", assigned)
        self.assertFalse(any(key.startswith("FSAE-2027-V1:EV.8.7") for key in assigned))
        for number in range(4, 16):
            self.assertNotIn(f"FSAE-2027-V1:EV.8.2.3:p{number:03}", assigned)
        mixed = next(source for group in self.report["obligations"] for source in group["source_clauses"] if source["clause_id"] == "FSAE-2027-V1:EV.8.2.3:p003")
        self.assertIn("prose_only_rotated_figure_text_ignored", mixed["source_review_status"])

    def test_draft_keeps_verification_unperformed_and_does_not_claim_applied_coverage(self):
        self.assertEqual(39, len(self.report["obligations"]))
        for group in self.report["obligations"]:
            self.assertEqual("draft_not_applied", group["coverage_status"])
            self.assertEqual("not_verified", group["verification_status"])
            self.assertTrue(group["verification_obligation"])
        self.assertIn("req.setAttribute(requirement, 'verification_status', 'not_verified')", self.script)
        self.assertIn("req.satisfy(it, requirement)", self.script)
        self.assertIn("req.verify(requirement, verification)", self.script)
        self.assertIn("req.derive(derived, requirement)", self.script)

    def test_native_state_current_topology_and_stable_interfaces_are_concrete(self):
        self.assertIn("common.createStateMachine()", self.script)
        self.assertIn("common.createStateTransition()", self.script)
        self.assertIn("cs.createPhysicalLink()", self.script)
        self.assertIn("cs.createPhysicalLinkEnd()", self.script)
        self.assertIn("cs.createPhysicalPort()", self.script)
        self.assertIn("link.ownedPhysicalLinkEnds.add(end)", self.script)
        self.assertIn("Accept independent shutdown trips", self.script)
        self.assertIn("Remove motor torque on shutdown", self.script)
        self.assertIn("Only local manual reset releases latch", self.script)
        self.assertIn("Controls cannot bypass protective latch", self.script)

    def test_state_availability_uses_the_writable_function_side(self):
        self.assertIn("function.availableInStates.add(value)", self.script)
        self.assertNotIn(".availableAbstractFunctions.add", self.script)
        self.assertIn("available(states.Energized, activeFunctions)", self.script)
        self.assertIn("available(states.Discharging, ['discharge'])", self.script)

    def test_native_operational_actors_are_resolved_and_checked_before_mutation(self):
        guard = "assert actor != null && actor.actor == true"
        self.assertIn("api.find('Entity', 'FSAE2027:' + name + ' Operational actor')", self.script)
        self.assertNotIn("api.find('OperationalActor'", self.script)
        self.assertLess(self.script.index(guard), self.script.index("api.batch("))
        self.assertIn("api.allocate(activity, operationalActors[specification.actor])", self.script)

    def test_shutdown_specific_flows_use_leaf_sources_and_native_realizations(self):
        self.assertIn("api.link(functions['glv-supply'][layer], functions.glv[layer], 'FunctionalExchange')", self.script)
        self.assertNotIn("api.link(domains['low-voltage'][layer], functions.glv[layer]", self.script)
        self.assertIn("createFunctionalExchangeRealization()", self.script)
        self.assertIn("createPortRealization()", self.script)
        self.assertIn("from.ownedPortRealizations.add(portRealization)", self.script)
        groups = {group["key"]: group for group in self.report["obligations"]}
        self.assertIn("glv-supply", groups["TRIP-CAUSES"]["model_keys"])

    def test_additive_semantic_repair_retains_existing_exchange_ids_and_trace_evidence(self):
        repair = (HERE / "35-repair-shutdown-semantics.groovy").read_text(encoding="utf-8")
        self.assertIn("function.outputs.add(glv[layer].flow.source)", repair)
        self.assertIn("originalFlowIds ==", repair)
        self.assertIn("Repair already applied", repair)
        self.assertNotIn("api.delete", repair)
        self.assertIn("assert attrs.coverage_status == 'allocated' && attrs.verification_status == 'not_verified'", repair)
        self.assertIn("api.allocate(activity, vehicle)", repair)

    def test_semantic_repairs_snapshot_ids_and_split_structure_from_realizations(self):
        structure = (HERE / "35-repair-shutdown-semantics.groovy").read_text(encoding="utf-8")
        realizations = (HERE / "36-repair-shutdown-realizations.groovy").read_text(encoding="utf-8")
        self.assertNotIn("api.findById(", structure)
        self.assertNotIn("api.findById(", realizations)
        self.assertIn("existingById[stableId(key)]", structure)
        self.assertIn("existingIds.add(id)", structure)
        self.assertIn("existingIds.add(id)", realizations)
        self.assertNotIn("createFunctionalExchangeRealization()", structure)
        self.assertIn("createFunctionalExchangeRealization()", realizations)
        self.assertIn("Apply phase 35 first", realizations)
        self.assertIn("semantic structure v2 APPLIED", self.script)

    def test_stable_id_writes_use_the_synchronized_facade_and_verify_native_fragments(self):
        for file in ("30-model-shutdown.groovy", "35-repair-shutdown-semantics.groovy", "36-repair-shutdown-realizations.groovy"):
            draft = (HERE / file).read_text(encoding="utf-8")
            self.assertNotIn("value.id =", draft)
            self.assertNotIn("element.id =", draft)
            self.assertIn("'id', id)", draft)
        proof = (HERE / "40-verify-shutdown.groovy").read_text(encoding="utf-8")
        self.assertIn("value.eResource().getURIFragment(value) == api.id(value)", proof)
        self.assertIn("value.eResource().getEObject(api.id(value)) == value", proof)

    def test_additive_guards_and_module_keep_the_source_bijection(self):
        self.assertIn("assert attributes.coverage_status == 'not_modeled'", self.script)
        self.assertIn("FSAE2027:SD:derived-module", self.script)
        self.assertIn("sourceRequirements = req.requirements(module)", self.script)
        self.assertNotIn("req.createFolder(module,", self.script)
        self.assertIn("Shutdown slice already exists", self.script)
        self.assertIn("system.ownedSystemComponents.isEmpty()", self.script)
        self.assertIn("UUID.nameUUIDFromBytes", self.script)

    def test_engineering_values_are_bounded_by_source_and_unknowns_remain_explicit(self):
        groups = {group["key"]: group for group in self.report["obligations"]}
        self.assertIn("five seconds", groups["SHUTDOWN-EFFECTS"]["obligation"])
        self.assertIn("60 degrees C", groups["CELL-TEMPERATURE"]["obligation"])
        self.assertIn("less than 10 mm", groups["SENSOR-LOCATION"]["obligation"])
        self.assertIn("20 percent", groups["LITHIUM-COVERAGE"]["obligation"])
        self.assertIn("500 ohms per volt", groups["IMD-THRESHOLD"]["obligation"])
        self.assertIn("40 mm", groups["BUTTON-SIDES"]["obligation"])
        self.assertIn("24 mm", groups["BUTTON-COCKPIT"]["obligation"])
        self.assertTrue(groups["CELL-VOLTAGE-LIMIT"]["unresolved_decisions"])
        self.assertTrue(groups["IMD-APPROVAL"]["unresolved_decisions"])
        self.assertIn("600-V rule ceiling is not assumed", groups["IMD-THRESHOLD"]["unresolved_decisions"][0])

    def test_generated_script_matches_reviewed_template_and_executor_size_bound(self):
        compact = []
        for group in self.report["obligations"]:
            row = {key: group[key] for key in ("key", "identifier", "obligation", "model_keys", "verification_obligation", "unresolved_decisions")}
            row["source_clauses"] = [{key: source[key] for key in ("clause_id", "source_pages", "source_text_sha256", "source_review_status")} for source in group["source_clauses"]]
            compact.append(row)
        expected = (HERE / "shutdown-model-template.txt").read_text(encoding="utf-8").replace("@@OBLIGATIONS@@", builder.groovy(compact)).replace("@@PINNED_SHA@@", builder.PINNED_SHA)
        self.assertEqual(expected, self.script)
        self.assertLess(len(self.script), 100000)
        self.assertNotIn("@@", self.script)


if __name__ == "__main__":
    unittest.main()
