# FSAE 2027 acceptance project

The scripts that built the benchmark model in [`../model`](../model) from the public Formula SAE 2027
rulebook. Run them in a disposable Capella project through the dev server's `/execute`, in numeric order:

| Scripts | Step |
|---|---|
| `00`–`13` | Create the project, build the candidate architecture, repair traces, normalize and realize interfaces |
| `20`–`21` | Verify the architecture and export the source inventory |
| `30`–`34` | Overview and safety diagrams, their checks and layout |
| `90`–`91` | Verify persistence after save and reopen; export native validation |

[`rules/`](rules) holds the pinned rule inventory and its import batches; [`safety/`](safety) holds the three
detailed safety slices, each with its own README. The rest of this page says what the model does and does not
establish.

## What the model establishes

The acceptance project uses the official 2027 Formula SAE V1 source inventory in `rules/`. It retains numbered clauses and unnumbered source objects, including clauses that do not apply to the chosen driver-operated electric baseline. A structural bootstrap and a requirement import are preparation steps. They do not establish complete rule coverage or a compliant physical car.

Native requirements use [Capella Requirements Viewpoint 0.14.0 for Capella 7](https://github.com/eclipse-capella/capella-requirements-vp/releases/tag/v0.14.0). A module is an architecture extension. Source records have stable ReqIF identifiers and typed metadata. Selected candidate obligations have native satisfaction, verification and derivation relations; importing the remaining records does not establish those mappings. Activation must complete before a semantic script runs. `api.requirements().importRequirements(parent, records)` imports explicit ledger records atomically; it never retrieves or interprets a book itself.

`model-schema.json` defines the candidate architecture and completion gates. Candidate source-section associations identify review destinations, not satisfaction claims. The builder creates reusable subsystem/function locations and unresolved decision constraints. It sets no guessed dimensions, voltage limits, ratings, part specifications or source quotations. Decisions such as frame construction, cell arrangement, traction hardware and controls remain visible until the design and source conditions resolve them.

The source Requirement for every clause must carry its exact `clause_id`, source edition/hash/page provenance, applicability decision, interpretation state, coverage state and verification state. Unnumbered objects keep their source object ID; they do not acquire fictitious rule IDs. Arrays and structured source facts are JSON StringValueAttributes. Rule IDs belong in strings, not ReqIFForeignID, whose native type is an integer.

For applicable technical obligations, model coverage requires a reviewed interpretation, responsible design elements, a native model-to-requirement satisfaction relation, a requirement-to-verification Constraint relation and suitable cross-perspective realizations. Administrative, inspection and competition obligations require responsible operational roles/procedures and evidence obligations. They must not disappear because they lack a vehicle component. Source-only filtering of an IC paragraph or a conditional rule remains subject to clause-level review.

The completeness report must compare the complete input ledger against persisted native identifiers, reject duplicate or missing clauses, list unresolved interpretations and design choices, and check relationship endpoints after save/reopen. It must retain all warnings, failed checks, unresolved external documents and figure/table reviews. `coverage_status=modeled` requires a real reviewed mapping. `verification_status=passed` requires recorded evidence; neither status follows from creating an EObject or linking a generic subsystem.

Detailed slices live in `safety/shutdown`, `safety/driver-demand` and `safety/vehicle-protection`. Their created combined scope is 588 distinct source paragraph records and 86 candidate derived obligations; 2,609 source records remain outside these slices. Candidate interpretation and native satisfaction traces do not close physical verification or semantic review. Each slice README specifies bounded creation, migration and verification order.

After decomposing domain functions, run `12-normalize-interface-leaves.groovy` to move preserved interface endpoints to allocated leaves, then `13-realize-interface-transports.groovy` for native transport/port allocations. The boundary responsibilities remain unreviewed candidates. Re-run whole-model native validation and slice verifiers, save, reopen, reject load errors and repeat the exact source-identifier/hash audit before distributing a model.

The installed-workbench save/reopen acceptance passed on 8 October 2026: all 3,197 source identifiers and document/paragraph hashes matched; 86,658 semantic persistent IDs were unique and resolved through their native resource; no semantic load errors or unresolved proxies were found. Nine overview diagrams and three detailed safety diagrams retained their semantic bindings, with all 63 selected SA safety exchanges visible. The complete installed validator export retains 517 warnings and zero errors. Eight bootstrap decisions remain unresolved. Every source record retains `verification_status=not_verified`.

The global source ledger reports 179 allocated candidate records and 3,018 not modeled. Vehicle protection's additional 409 records carry separate bounded-scope metadata; the combined 588-record slice scope must not be presented as globally completed coverage. The 2,609 records outside the detailed slices, source figures, conditional applicability, external documents, hardware choices, operational scenarios and verification evidence still require work.

Native traversal in these acceptance scripts uses explicit `hasNext()/next()` loops. Groovy collection extensions can inspect EMF tree iterators' internal traversal stack instead of model descendants, producing empty or incomplete checks. `90-verify-persistence.groovy` rejects a traversal below 86,000 IDs, and `33-verify-safety-diagrams.groovy` requires all 63 persisted slice exchanges and their visible native views.
