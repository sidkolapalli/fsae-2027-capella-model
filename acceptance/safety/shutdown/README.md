# EV.8 shutdown candidate safety slice

This additive draft covers EV.8 except the BSPD internals in EV.8.7, which belong to the driver-demand slice. The official final 2027 V1 PDF is pinned by SHA-256 `f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e`. Pages 105–110 were rendered and checked against every grouped obligation. Source paragraph hashes and pages are preserved in `shutdown-obligations.json`.

The local accounting retains 135 source extraction records: 122 prose records in 39 interpreted obligations and 13 diagram-label records requiring separate handling. The rotated labels mixed into EV.8.2.3:p003 are excluded from its manual-reset interpretation; EV.8.2.3:p004–p015 and EV.8.1.7:p002 are retained as figure labels without normative allocation. Shared rule inventory and ledger files are unchanged.

## Behavior and traceability

The draft creates seven distinct LA/PA branches for BMS monitoring, independent IMD monitoring, the hardwired high-side current path, protective-trip latching and local reset, isolation/precharge/discharge, shutdown motor-torque removal, and BMS/IMD warning indication. Twenty-four functions in each of SA/LA/PA provide concrete sensing, fault detection, independent trip propagation and safe-output behavior, with typed functional realizations and physical deployments. A dedicated GLV supply-observation leaf supplies the loss-of-power exchange. All 26 LA/PA exchange pairs carry native FunctionalExchangeRealizations and matching PortRealizations. OA includes emergency stopping, local reset, technical inspection, and leaf shutdown-trip/motor-inhibit interfaces. EPBS hardware/interface configuration items realize the PA equipment.

Native PhysicalPorts, PhysicalLinkEnds referencing their Parts, and PhysicalLinks represent the direct series shutdown-current path and parallel isolation/precharge coil branches. Three emergency buttons, GLVMS, TSMS, BOTS, inertia switching and a required-interlock interface are explicit equipment elements. A GLVMS branch also represents interruption of all GLV loads. This is a candidate circuit topology with explicit boundary interfaces and TBD hardware; it does not establish wiring, contact ratings or physical implementation correctness.

A native guarded state machine in the local-reset logical component represents Open, Energized, Discharging and Latched protective trip, plus initialization. Protective trips cannot transition directly to Energized. The reset transition returns to Open; activation remains a separate dependency. TSMS, button toggles, driver controls, remote requests and GLV power cycling cannot bypass a protective latch. Charger-connected monitoring remains available while TS is open. State invariants and verification constraints retain the immediate current/torque removal and the source five-second voltage-decay obligation.

Derived requirements use the separate SA module `FSAE2027:SD:derived-module`. The original source module retains its exact 3,197 source-clause identifiers. Typed `satisfy`, `verify` and `derive` relations connect actual functions/equipment, source obligations and native verification Constraints. Source coverage becomes `allocated` only within the model transaction after the relevant elements and traces exist. This means an assigned candidate design owner; it does not mean a complete or compliant implementation. Every verification status remains `not_verified`. The public draft metadata remains `draft_not_applied` until execution is independently evidenced.

## Apply and verify

1. Create the acceptance architecture, activate Requirements Viewpoint, and import the complete pinned source corpus.
2. Review and apply `30-model-shutdown.groovy` through the assistant's ordinary code-review/action path. It checks the project marker, source hashes, initial source status and absence of an earlier slice before any mutation. It changes no existing bootstrap partition and performs no automatic recovery edits.
3. Apply the driver-demand slice afterward; it connects its BSPD trip and motor-inhibit interfaces to this slice.
4. Normalize the shared bootstrap aggregates with the integration draft: a function with children cannot retain a leaf allocation, and its interface endpoints must reach real leaf functions. This is shared across the shutdown and driver-demand slices.
5. For an already-applied v1 shutdown slice, review and apply `35-repair-shutdown-semantics.groovy` once, followed by `36-repair-shutdown-realizations.groovy`. The first bounded transaction adds the GLV-observation leaf and two OA shutdown interfaces and recontains the existing dedicated GLV output ports without changing the 78 exchange IDs. The second adds native exchange/port realization chains. Each phase snapshots IDs before writing to avoid repeatedly searching the entire imported Requirements corpus. Existing requirement and verification statuses remain intact. Clean creation already includes both changes and refuses these repairs through their markers.
6. Run `40-verify-shutdown.groovy`, then save, close, reopen and run the same proof again. This proves native structure and retained references. It does not run electrical or vehicle tests.

The stable SA/LA/PA connection names are `FSAE2027:SD Accept independent shutdown trips` and `FSAE2027:SD Remove motor torque on shutdown`. Created model IDs use Java `UUID.nameUUIDFromBytes(UTF8('FSAE2027:SD:' + model_key))`; model keys appear in the draft and the returned structure report. Derived requirement identifiers are `FSAE2027:SD:obligation:<KEY>`.

Stable ID assignment requires the installed facade's `PersistentIds` support: changing an attached object's ID must also update the native XML resource's reference index, including command undo/redo. The drafts use `api.set` for IDs, and the verifier checks that the stored ID, native URI fragment and native lookup agree. Run the save/reopen proof after applying the changes; a successful in-memory lookup alone does not establish safe persistence.

Regenerate and check the source-bound artifacts with:

```text
python build_shutdown_slice.py --private-source <private-cache>/private-source-paragraphs.json
python -m unittest -v test_shutdown_slice.py
```

Exact cell chemistry/configuration and datasheet limits, measurement uncertainty, module/sensor isolation, protection/contact ratings, approved IMD part and official FAQ, discharge implementation, reset hardware/location, packaging, symbols and dependent rules remain TBD. Lithium sensing coverage, optional shared sensing and any interlock-separation exception remain conditional. Referenced T.3.3, T.9.1.2, T.9.3, T.9.4, EV.5.4, EV.6, EV.9 and activation/work-practice rules require integration review. No tests or compliance outcomes are inferred from these source imports or model allocations.
