This slice adds a candidate chassis, occupant protection and mechanical braking architecture to the marked `FSAE-2027-Electric-Arcadia` acceptance project. Its 32 authored obligations trace 409 exact source paragraphs from 143 selected rules in the pinned 2027 Formula SAE V1 book. It does not establish full vehicle or event-rule coverage, hardware acceptance or compliance.

The source SHA-256 is `f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e`. Each selected paragraph retains its rule ID, page numbers, source-text hash and source numeric facts in [obligations.json](obligations.json). The private rulebook paragraphs are used only to validate provenance; they are not included in this directory. Structural tables, the driver template, cockpit opening gauge and brake options were checked against the source PDF drawings/tables.

The model adds 17 leaf responsibilities in OA, SA, LA and PA, 14 logical/physical partitions, equipment boundaries and EPBS items, native deployment and adjacent function/component realizations. It removes the original allocations from the marked structure/cockpit/brakes/status aggregates when decomposing them and allocates the new leaves. The brake demand drives separate hydraulic A/B paths to the wheel-braking responsibility. Their wheel split and actual hardware remain unresolved. The BOTS behavior deploys on the shutdown slice's existing direct series contact and links to its shutdown cutout function. It does not create a second BOTS contact.

Run these native Groovy scripts once, in order, after the bootstrap, full source import and shutdown slice:

1. Run `30a-components.groovy`, `30b-functions.groovy` and `30c-exchanges.groovy` to create components/deployment/items, leaf responsibilities and typed interfaces.
2. Run `30d-unallocate-structure.groovy`, `30e-unallocate-cockpit.groovy`, `30f-unallocate-brakes.groovy` and `30g-unallocate-status.groovy` to remove the now-invalid original aggregate allocations with native deletion and view cleanup.
3. `30h-finish-structure.groovy` checks cleanup and creates the construction/verification dependency constraints and separate SA derived module.
4. `31-trace-vehicle-protection-a.groovy` creates the first 16 derived obligations and native `derive`, `satisfy` and `verify` links.
5. `32-trace-vehicle-protection-b.groovy` creates the remaining 16 obligations and links.
6. `35-realize-vehicle-protection.groovy` adds native cross-layer exchange/port realizations and component-exchange/function-port allocations. It is repeatable.
7. `40-verify-vehicle-protection.groovy` checks structure, endpoint directions, source hashes, typed relations and unresolved status. Run it again after saving, closing and reopening the model.

Each script is below the executor's 100,000-character input limit. A monolithic native creation attempt timed out at 30 seconds and rolled back; these bounded phases remove repeated whole-model ID/Part scans and limit allocation cleanup to one domain at a time. Parts for new components come from their immediate native owner. Existing shutdown hardware is resolved by a single PA-subtree scan; a missing or ambiguous instance is rejected. Deployment is owned by that exact location Part even when the Part is at the PA root. The old `30-model-vehicle-protection.groovy` fails immediately with the replacement sequence. Creation guards reject replay and another project/marker. Trace guards reject a missing/changed source paragraph, existing verification evidence or a previously allocated paragraph in this slice. Each execution is transactional; earlier successful batches remain visible if a later batch fails. The final verifier requires every phase to be complete.

The coordinated architecture integration repair must map any remaining shared bootstrap exchanges from newly decomposed aggregate ports to appropriate leaves or native delegation. This slice does not convert a generic brake-safety interface into BOTS semantics without review. Its verifier checks its own new leaf endpoints and allocations; whole-model native validation after the shared repair is a separate acceptance requirement.

All derived obligations remain `allocated_candidate_design`, `design_status=unresolved` and `verification_status=not_verified`. Source requirements receive scoped `vehicle_protection_*` ledger attributes; their global coverage/verification statuses and any other slice's attributes remain unchanged. Native satisfaction relations denote candidate design allocation here. A native verification relation points to a procedure/acceptance obligation and does not mean a test has passed.

The planned three-slice source delta is recorded in `expected-scope-delta.json`: shutdown 122 paragraphs, driver-demand 57 and protection 409, with no overlap. After every native batch passes, their candidate scope totals 588 of 3,197 source records; 2,609 remain outside these slices. They produce 86 derived obligations and select 213 rule IDs with paragraph records. The full inventory's 1,825 IDs also contains headings without direct paragraphs, so this is not a claim that 213 independently verified rules have been completed.

Construction alternatives remain explicit: tube frame/monocoque/hybrid, restricted steel/aluminum applications and composite proof, AIP material/attachment, standard/custom attenuator and its material-dependent loads/tests, harness type/posture, pedal material/manufacture, optional regenerative braking and hydraulic circuit split. The different AIP/IA weld inequalities and the source's distinction between pedal proof force and design criteria are preserved. The impact reference of 300 kg at 7.0 m/s is not assigned as the actual vehicle mass.

Dependent work remains open, including composite material tests and structural equivalence, other bulkhead/bracing/joint details, cockpit internal cross-section and thermal protection, harness mounting, fastener requirements, EV grounding/outboard-motor conditions and official event-site anthropometry/standard-IA specifications. Driver-demand owns APPS/BSE, brake-accelerator plausibility and BSPD; shutdown owns EV.8 and the shared BOTS contact. These boundaries are recorded in the model's dependency and construction-choice constraints.

Regenerate only from the exact pinned metadata/private source:

```powershell
python prepare_slice.py --private-source '<local-path>'
```

Generation verifies every selected source-text hash, exact rule ownership and the script-size limits. Secure Groovy parsing checks syntax; actual native execution, persistence and native validation are separate checks performed by the acceptance runner. No physical vehicle test has been performed by this slice.
