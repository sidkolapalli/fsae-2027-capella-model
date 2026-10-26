# Model guide

This guide describes what is in the FSAE 2027 Capella model, how it is organized, and how to use it. It is for
systems engineers who are new to this model and possibly new to Capella. To install Capella and open the model,
follow [GETTING-STARTED.md](GETTING-STARTED.md). [CONTRIBUTING.md](../CONTRIBUTING.md) covers changes proposed back to this
repository, which are built by scripts. A team developing its own fork edits the model in Capella, as
[Using the model as a development platform](#using-the-model-as-a-development-platform) describes.

The model is a reference platform and a head start, not a finished car. It turns the rulebook into native
requirements, lays out a candidate architecture across all five Arcadia perspectives, keeps the major design
decisions open on purpose, and tracks what still needs verification. A Formula SAE team can adopt it as the
starting point of its own development work. Nothing in it guarantees a compliant or competitive vehicle: every
source record has `verification_status=not_verified`, and the eight design decisions are unresolved.

Counts in this guide were read from the model files. Where a number comes from a README, the README is linked.

## Contents

1. [Scope and baseline](#scope-and-baseline)
2. [Structure by perspective](#structure-by-perspective)
3. [Requirement modules](#requirement-modules)
4. [Requirement attributes](#requirement-attributes)
5. [Traceability](#traceability)
6. [Detailed safety slices](#detailed-safety-slices)
7. [Open decisions and tracking constraints](#open-decisions-and-tracking-constraints)
8. [Naming conventions](#naming-conventions)
9. [What the model does not establish](#what-the-model-does-not-establish)
10. [Using the model as a development platform](#using-the-model-as-a-development-platform)
11. [Using the model as a benchmark for agent skills](#using-the-model-as-a-benchmark-for-agent-skills)

## Scope and baseline

| Item | Value |
|---|---|
| Vehicle | Human-driven electric vehicle for Formula SAE Electric 2027 |
| Source document | Formula SAE Rules 2027, Version 1.0 (`FSAE-2027-V1`), 147 pages, pinned by SHA-256 in [`acceptance/rules/README.md`](../acceptance/rules/README.md) |
| Tool | Capella 7.0 with Requirements Viewpoint 0.14.0 |
| Status | Candidate architecture; `completion_status: in_progress` in [`model-schema.json`](../acceptance/model-schema.json) |

The source baseline holds every numbered paragraph of the rulebook (3,177) plus 20 unnumbered paragraphs from
tables and an inspection introduction: 3,197 source records in total. Records outside the baseline are kept, not
deleted:

- internal-combustion-only sections stay in the model with `applicability=not_applicable` and a reason;
- chassis material alternatives stay `conditional` until a chassis is chosen;
- shared paragraphs with internal-combustion distinctions stay `unresolved`;
- driverless content stays `conditional`. The schema lists a ninth decision, `DEC-DRIVERLESS`, with status
  `not_selected`; it is not created in the model.

The model stores rule identifiers, page numbers, headings and text hashes. It does not store rule text. Get the
rulebook from [fsaeonline.com](https://www.fsaeonline.com). Formula SAE is a trademark of SAE International, which
owns the rulebook copyright; this project is not affiliated with SAE International.

## Structure by perspective

The model has about 86,000 semantic elements. [`model-schema.json`](../acceptance/model-schema.json) lists 16
candidate domains and 11 candidate interfaces. The bootstrap creates the 15 vehicle domains (for example chassis and
crash protection, braking, driver demand, energy storage, shutdown, charging, thermal) and the 11 interfaces between
them; the 16th, competition inspection and work practices, becomes the `FSAE2027:process` activities in OA.

| Perspective | Top-level folders | Main elements |
|---|---|---|
| Operational Analysis (OA) | Operational Activities, Operational Capabilities, Interfaces, Data, Roles, Operational Entities | 72 operational activities, 5 entities, 15 operational capabilities, 47 interactions |
| System Analysis (SA) | four requirement modules, Requirements types, System Functions, Capabilities, Interfaces, Data, Structure, Missions, Operational Analysis realization, the decision and tracking constraints | 93 system functions, 5 system components (the vehicle and actors), 15 capabilities, 74 functional exchanges, 1 state machine, 3,283 requirements, 139 constraints |
| Logical Architecture (LA) | Logical Functions, Capabilities, Interfaces, Data, Structure, System Analysis realization | 93 logical functions, 48 logical components, 74 functional exchanges, 37 component exchanges, 15 capability realizations, 5 state machines, 24 constraints |
| Physical Architecture (PA) | Physical Functions, Capabilities, Interfaces, Data, Structure, Logical Architecture realization | 93 physical functions, 122 physical components (behavior and node), 74 functional exchanges, 37 component exchanges, 19 physical links, 43 deployment links, 15 capability realizations, 1 state machine, 3 constraints |
| EPBS | Capabilities, Structure, Physical Architecture realization | 61 configuration items, 104 physical artifact realizations to PA components |

Each perspective root also has a types folder. The schema sets the pattern for each perspective: one black-box
vehicle system in SA, subsystems as logical components in LA, behavior components deployed on node components in
PA, and configuration items without functions or exchanges in EPBS.

### Diagrams

The model has 12 diagrams. [DIAGRAMS.md](DIAGRAMS.md) lists them by perspective and shows each PNG export from
[`diagrams/`](diagrams).

## Requirement modules

Requirements live in four Requirements Viewpoint modules under System Analysis.

| Module identifier | Name | Requirements |
|---|---|---|
| `FSAE-2027-V1:source-module` | Formula SAE 2027 V1 source obligations | 3,197 |
| `FSAE2027:SD:derived-module` | EV.8 shutdown interpreted candidate obligations | 39 |
| `FSAE2027:DD:module` | Driver demand and BSPD derived candidate obligations | 15 |
| `FSAE2027:VP:derived-module` | Vehicle protection and mechanical braking candidate obligations | 32 |

The source module holds one requirement per source record. Its title is the rule identifier and heading; its text
is empty. Three source records (EV.3.3.1 to EV.3.3.3) carry a reviewed paraphrase; the rest have no text yet. The
three slice modules hold 86 derived obligations, written as paraphrases.

## Requirement attributes

Each numbered source requirement carries the 19 attributes below. Arrays and objects are stored as JSON text in
string attributes, because the native ReqIF attribute values are scalar. Sources: [`acceptance/README.md`](../acceptance/README.md),
[`acceptance/rules/README.md`](../acceptance/rules/README.md), [`model-schema.json`](../acceptance/model-schema.json),
[`build_inventory.py`](../acceptance/rules/build_inventory.py) and [`prepare_batches.py`](../acceptance/rules/prepare_batches.py).

| Attribute | Meaning | Values |
|---|---|---|
| `clause_id` | Stable identifier of the source record; also the requirement's ReqIF identifier | `FSAE-2027-V1:<rule id>:p<NNN>` |
| `source_document_id` | Source edition | `FSAE-2027-V1` |
| `source_sha256` | SHA-256 of the rulebook PDF | one hash for all records |
| `source_text_sha256` | SHA-256 of the paragraph text, so a reviewer can confirm the paragraph against the PDF | hash |
| `source_rule_id` | Rule number the paragraph belongs to | for example `GR.1.1` |
| `source_character_count` | Length of the paragraph text | integer |
| `source_pages` | PDF pages of the paragraph | JSON array, for example `[5]` |
| `source_spans` | Page coordinates of each text line | JSON array of `{pdf_page, x0, top, x1, bottom}` |
| `referenced_rule_ids` | Other rule numbers the paragraph cites | JSON array |
| `numeric_facts` | Every number in the paragraph, with unit if one follows; operators and conditions are not inferred | JSON array; each fact has `interpretation_status=source_token_only_needs_semantic_review` |
| `applicability` | Whether the paragraph applies to the human-driven electric baseline | `applicable`, `not_applicable`, `conditional`, `unresolved` |
| `applicability_reason` | Why the scope filter chose that value | short text, for example "Internal-combustion-only section; baseline is human-driven electric." |
| `applicability_evaluation_status` | How applicability was decided | `scope_filter_only_clause_conditions_need_review` (a rule-prefix filter, not a clause review) |
| `classification` | Kind of obligation, from the rule prefix | `design`, `competition`, `inspection`, `process`, `general` |
| `interpretation_status` | Whether the paragraph has a reviewed engineering interpretation | `needs_semantic_review`; `interpreted` for records inside the shutdown and driver-demand slices |
| `coverage_status` | Whether a design owner is assigned | `not_modeled`; `allocated` (candidate design owner assigned, not a complete implementation). `modeled` is reserved for a real reviewed mapping |
| `verification_status` | Whether verification evidence exists | `not_verified` on every record. `passed` requires recorded evidence and is never set automatically |
| `model_element_ids` | Design elements that satisfy the record | JSON array of model IDs; filled when the record is allocated |
| `verification_element_ids` | Verification constraints for the record | JSON array of model IDs |

In the current model, 179 source records are `allocated` (122 shutdown and 57 driver-demand paragraphs) and 3,018
are `not_modeled`.

Other attributes appear on some records:

- the 20 unnumbered records carry `source_section_id`, `source_object_id` and `source_object_type` (`table` or
  `unnumbered_introduction`) instead of a rule number, and no `applicability_evaluation_status`;
- the three paraphrased records carry `paraphrase_review_status` and `paraphrase_source_sha256`;
- the 409 vehicle-protection source records carry scoped `vehicle_protection_*` attributes
  (`vehicle_protection_coverage_status=allocated_candidate_design`, `vehicle_protection_verification_status=not_verified`
  and others) and keep their global `coverage_status=not_modeled`;
- slice records carry slice attributes such as `slice_id`, `source_clause_ids`, `model_owner`,
  `interpreted_obligation`, `conditions`, `unresolved_decisions`, `verification_method`,
  `evidence_status=not_available`, `compliance_status=not_claimed` (driver demand) and `design_status=unresolved`
  (vehicle protection). The 15 driver-demand obligations use two further `classification` values:
  `technical_design_and_verification` and `design_and_inspection_process`. Derived obligations also carry `coverage_status`: `allocated` for shutdown and driver
  demand, `allocated_candidate_design` for vehicle protection.

The model defines 46 attribute definitions in all.

## Traceability

Requirement links use the native Requirements Viewpoint relation types, as [`model-schema.json`](../acceptance/model-schema.json) defines them.

| Relation | Direction | Count |
|---|---|---|
| `satisfy` (CapellaOutgoingRelation) | design element to requirement | 13,149 |
| `verify` (CapellaIncomingRelation) | requirement to verification Constraint | 674 |
| `derive` (InternalRelation) | derived obligation to source requirement | 588 |

The 588 derive relations equal the number of source paragraphs inside the three slices. A `satisfy` link here means a candidate design allocation. A `verify` link points to a
verification obligation; it does not mean a test was run.

Elements are also connected between perspectives with native Capella realizations and allocations:

| Link | Count |
|---|---|
| Function realization (SA to OA, LA to SA, PA to LA) | 262 |
| Functional exchange realization | 195 |
| Port realization | 342 |
| Component realization | 87 |
| Component exchange realization | 23 |
| Capability realization | 45 |
| Physical artifact realization (EPBS to PA) | 104 |
| Function allocation to components | 310 |
| Component exchange to functional exchange allocation | 46 |
| Port allocation | 92 |
| Part deployment link (PA behavior to node) | 43 |

## Detailed safety slices

Three slices model selected safety rules in depth, from operational activities down to EPBS items. Together they
cover 588 of the 3,197 source records and add 86 derived obligations; 2,609 source records are outside them. The
slices do not overlap.

| Slice | Prefix | Source paragraphs | Obligations | SA diagram | README |
|---|---|---|---|---|---|
| Shutdown (EV.8, except BSPD internals) | `SD` | 122 | 39 | [SA shutdown protection detailed flows](diagrams/sa-shutdown-protection-detailed-flows.png) | [safety/shutdown](../acceptance/safety/shutdown/README.md) |
| Driver demand and BSPD | `DD` | 57 | 15 | [SA driver demand and BSPD detailed flows](diagrams/sa-driver-demand-and-bspd-detailed-flows.png) | [safety/driver-demand](../acceptance/safety/driver-demand/README.md) |
| Vehicle protection and mechanical braking | `VP` | 409 | 32 | [SA occupant protection and braking detailed flows](diagrams/sa-occupant-protection-and-braking-detailed-flows.png) | [safety/vehicle-protection](../acceptance/safety/vehicle-protection/README.md) |

**Shutdown (`SD`).** Seven logical/physical branches: BMS monitoring, independent IMD monitoring, the hardwired
shutdown current path, protective-trip latching and local reset, isolation/precharge/discharge, motor-torque removal
on shutdown, and BMS/IMD warning indication. It has 24 functions in each of SA, LA and PA, physical ports and links
for the series shutdown path (emergency buttons, GLVMS, TSMS, BOTS, inertia switch, interlocks), and a guarded state
machine for the protective latch. Hardware ratings, cell data, the IMD part and reset hardware are TBD.

**Driver demand and BSPD (`DD`).** Accelerator and brake sensor plausibility, brake/accelerator conflict, torque and
regeneration limits, and an independent nonprogrammable BSPD that feeds the shutdown slice's trip input. It adds 64
functions across OA, SA, LA and PA, 88 functional exchanges and seven configuration items, with five state machines
for the timing and latch behavior. Sensor calibration, thresholds that depend on the nominal voltage, and fault
recovery remain open. The README lists the source boundaries it keeps.

**Vehicle protection and mechanical braking (`VP`).** Chassis and crash structure, cockpit and driver restraint,
and mechanical braking with two hydraulic paths. It adds 17 leaf responsibilities in OA, SA, LA and PA and 14
logical/physical partitions. Construction alternatives (frame type, materials, attenuator, harness, pedals, hydraulic
split) stay open. Its BOTS behavior reuses the shutdown slice's contact.

## Open decisions and tracking constraints

The model has 166 constraints. They record what is open, what still needs verification, and which build steps ran.

### Open design decisions

Eight constraints in System Analysis hold the open design decisions. Each is named `FSAE2027:DEC-<NAME> UNRESOLVED`
and states that no numerical design value or compliance claim is assigned until the decision is reviewed.

| Decision | Question (verbatim from [`model-schema.json`](../acceptance/model-schema.json)) |
|---|---|
| `DEC-CHASSIS` | Tube frame monocoque or other allowed structure? |
| `DEC-CELLS` | Cell chemistry cell model series parallel configuration and exact ratings? |
| `DEC-DRIVE` | Motor inverter driveline topology and manufacturer specifications? |
| `DEC-BRAKES` | Brake circuit layout braking hardware and validated performance? |
| `DEC-CHARGER` | Charger equipment location charging limits and protection implementation? |
| `DEC-THERMAL` | Cooling strategy and worst-case thermal design? |
| `DEC-CONTROL` | Control hardware software architecture and independent safety implementation? |
| `DEC-PRESSURE` | Any pressurized pneumatic or hydraulic equipment triggering T.6? |

### Tracking constraints

The suffix of a constraint name says what it tracks.

| Suffix | Meaning | Where |
|---|---|---|
| `NOT_VERIFIED` / `NOT VERIFIED` | A verification obligation with no evidence yet | `FSAE2027:SD <topic> verification NOT_VERIFIED` (39), `FSAE2027:DD verify DD-01 NOT VERIFIED` to `DD-15` (15), `FSAE2027:VP <obligation> verification NOT_VERIFIED` (32) |
| `TBD` | Implementation detail not chosen yet | `FSAE2027:SD <topic> implementation TBD` and `FSAE2027:SD External safety dependencies TBD` (35), `FSAE2027:VP Material-dependent architecture alternatives TBD` (1) |
| `INVARIANT` | A property the design must keep in every state | three shutdown state invariants and the high-side current path (SD), independent brake circuits (VP) |
| `OPEN` | A known gap left for later work | `FSAE2027:VP Bounded slice dependencies and exclusions OPEN`, `FSAE2027:VP Shared bootstrap aggregate interfaces require leaf mapping OPEN` |
| `APPLIED` | A marker that a build or repair script ran; the scripts use it to refuse a second run | two each in SD and DD |

The remaining constraints are state-machine guards in SD and DD, two DD motor-inhibit arbitration constraints, and
`FSAE2027:DD decision unresolved calibration thresholds and recovery`.

## Naming conventions

- Every modeled element name starts with `FSAE2027:`.
- Slice elements add the slice prefix: `FSAE2027:SD ...`, `FSAE2027:DD ...`, `FSAE2027:VP ...`.
- Exchange names carry the perspective: for example `FSAE2027:LA <interface> transport`.
- Bootstrap operational processes are named `FSAE2027:process <name>`.
- Diagram names start with `FSAE 2027` and end with `(candidate)`.
- Source requirement identifiers: `FSAE-2027-V1:<rule id>:p<NNN>`, for example `FSAE-2027-V1:GR.1.1:p001`.
  Unnumbered records use their source object ID in place of the rule ID.
- Derived requirement identifiers: `FSAE2027:SD:obligation:<KEY>`, `FSAE2027:DD:DD-01` to `DD-15`,
  `FSAE2027:VP:obligation:<KEY>`.
- Decisions: `FSAE2027:DEC-<NAME> UNRESOLVED`.

## What the model does not establish

From [`acceptance/README.md`](../acceptance/README.md) and the slice READMEs:

- It does not establish complete rule coverage or a compliant physical car. Importing a requirement does not model
  it.
- Applicability comes from a scope filter. Each clause still needs review.
- No guessed dimensions, voltage limits, ratings or part specifications are set, and no rule text is stored.
- `allocated` means a candidate design owner is assigned. It does not mean a complete or correct implementation.
- No verification has been performed. No record has `verification_status=passed`.
- The 588-record slice scope is not global coverage. The 2,609 records outside the slices, source figures,
  conditional applicability, external documents (such as structural-equivalency templates and EV forms), hardware
  choices, operational scenarios and verification evidence still need work.
- The Groovy verifiers check model structure. They do not run electrical or vehicle tests.
- Native validation retains 517 warnings and zero errors (installed-workbench acceptance, 8 October 2026).

## Using the model as a development platform

A team can use the model the way a vehicle program uses its systems model: requirements from the rules,
architecture in every perspective, open decisions in view, and verification tracked to evidence. The steps below
work in order. Each step names the model elements it changes.

1. **Adopt the baseline.** Fork or copy the repository and open the project as [GETTING-STARTED.md](GETTING-STARTED.md) describes.
   Read the [scope](#scope-and-baseline) and confirm that a human-driven electric car is your baseline. In the
   source module `FSAE-2027-V1:source-module`, do not change the identifiers, the `source_*` attributes or the
   hashes; they tie each requirement to the rulebook. Steps 3 to 5 say which attributes you update.
2. **Resolve the `DEC-*` decisions in the order the model lists them.** For each decision, record the choice as described
   below, then update the domain it controls. Some records depend on a decision:
   - `DEC-CHASSIS`: the `conditional` chassis-material records (reason "Material/chassis construction alternative")
     and the VP construction alternatives;
   - `DEC-CELLS`: the shutdown slice's TBD cell data and the driver-demand thresholds that depend on nominal voltage;
   - `DEC-BRAKES`: the VP hydraulic circuit split (`Independent brake circuits and unresolved wheel split INVARIANT`);
   - `DEC-DRIVE`, `DEC-CHARGER`, `DEC-THERMAL`, `DEC-CONTROL`: the powertrain, charging, thermal and control
     components in LA and PA;
   - `DEC-PRESSURE`: the conditional rules it names.

   Write the choice and rationale in the constraint's summary and update its specification text, which still says
   "Status: unresolved". When the decision is approved, replace the ` UNRESOLVED` suffix (for example with
   ` RESOLVED`). Keep each constraint and its `FSAE2027:DEC-<NAME>` prefix. `20-verify-architecture.groovy` expects
   exactly eight and reports the ones whose names still end in ` UNRESOLVED`, with their summary as rationale.
3. **Update applicability and traces.** For each source requirement, review the clause against your design and set
   `applicability` and `applicability_reason`. The model has no reviewed value for `applicability_evaluation_status`
   yet; agree on one before you change it. For applicable technical obligations, add design elements in the right
   perspective, a `satisfy` relation from the element to the requirement, a `verify` relation to a verification
   Constraint, and the realizations between perspectives. Process, inspection and competition obligations trace to
   operational roles and activities (for example the `FSAE2027:process` activities in OA), not to invented hardware.
   Fill `model_element_ids` and `verification_element_ids`, and set `interpretation_status` and `coverage_status`.
   For a group of related paragraphs, follow the slice pattern: a derived obligation in its own module with a
   `derive` relation to each source paragraph.
4. **Replace TBDs.** Work through the `TBD` constraints and the open items listed in each slice README. Replace each
   with a chosen value or part and its source. Do not enter a number the rules or your design data do not support.
5. **Record verification evidence.** For each `NOT_VERIFIED` constraint, plan the method (driver-demand records
   already carry a `verification_method`), run the test, inspection or analysis on the car, and record the evidence.
   Source records have no evidence attribute: write the evidence reference in the specification of the matching
   `NOT_VERIFIED` constraint and replace its suffix, and on driver-demand records also update `evidence_status`.
   If you want a dedicated evidence attribute, agree on it as a team first. Only then set
   `verification_status=passed` on the requirement.
6. **Run validation** on the model root, as [GETTING-STARTED.md](GETTING-STARTED.md#7-run-validation) describes. Compare the result with the baseline
   of zero errors and 517 warnings; that baseline applies only to validation of the whole model root with the
   default rule categories. The Groovy verifiers in `acceptance/` need the Capella Agent Toolkit dev server;
   [CONTRIBUTING.md](../CONTRIBUTING.md) lists them. Save, close and reopen the model, then validate again.

   If you imported the project with copy-into-workspace, as GETTING-STARTED.md says, your edits are saved in the workspace
   copy, not in your repository. To commit them, close Capella, copy the workspace's `FSAE-2027-Electric-Arcadia`
   folder over `model/FSAE-2027-Electric-Arcadia/`, and regenerate `model/SHA256SUMS` with the command in
   [CONTRIBUTING.md](../CONTRIBUTING.md) step 3. Otherwise the `test` CI job fails its checksum check.

The completion gates in [`model-schema.json`](../acceptance/model-schema.json) describe when the model would be
complete. The model's `completion_status` is `in_progress`: several gates, such as reviewed applicability, resolved
decisions and justified validation warnings, are not met.

## Using the model as a benchmark for agent skills

The model is also the benchmark of the [Capella Agent Toolkit](https://github.com/sidkolapalli/capella-agent-toolkit).
The open decisions, TBD items and unmodeled records are realistic engineering tasks. Giving them to an AI agent shows
where its modeling skills hold and where they fail: tracing a requirement to the right element, decomposing a
function the Arcadia way, keeping realizations consistent across perspectives, and refusing to invent a value the
model marks as unresolved.

The task sets, harness and grader are in the toolkit's
[`test-models/evals`](https://github.com/sidkolapalli/capella-agent-toolkit/tree/develop/test-models/evals). To set it
up, see [Using it as a benchmark](../README.md#using-it-as-a-benchmark) in the README. The harness starts Capella with
`-Xms512m -Xmx3g -Xss4m` for this model. A change to the model changes the benchmark, so results stay tied to the
version tag they used (see [Versions](../README.md#versions)).
