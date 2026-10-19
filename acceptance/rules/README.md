# Pinned Formula SAE source inventory

The governing baseline is the official final **Formula SAE Rules 2027, Version 1.0**, dated 1 September 2026 and published 15 September 2026. The [official publication announcement](https://www.fsaeonline.com/cdsweb/app/NewsItem.aspx?NewsItemID=219f84d4-72a6-4672-9125-7f69d790ae3a) confirms publication and the Electric event dates. The [main PDF](https://www.fsaeonline.com/cdsweb/gen/DownloadDocument.aspx?DocumentID=da79bcb4-0935-4f7b-83d7-0dbb8ce68d38) has 147 pages and SHA-256 `f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e`.

The modeling baseline is a human-driven electric vehicle for Formula SAE Electric 2027 at Michigan International Speedway. IC-only sections are retained with a scope exclusion. Material alternatives remain conditional until a chassis design is chosen; shared paragraphs containing IC distinctions remain unresolved. These initial scope filters require clause-level review before they become engineering decisions. Driverless resources remain conditional outside the human-driven baseline.

## Inventory and status

`source-manifest.json` pins source identity and records supporting documents and unresolved publication/applicability. `rule-inventory.json` contains all 1,825 numbered rule/heading IDs, including 1,398 leaf IDs, with page and paragraph references. `normalized-clauses.json` contains 3,177 numbered paragraphs/bullets plus 20 unnumbered paragraphs from Tables PS-1/PS-2 and the inspection introduction. The three unnumbered source objects retain their own identifiers; their `source_rule_id` is null.

The PDF actually omits the parent heading D.8.1 before D.8.1.1–D.8.1.7 on page 136. The inventory records this source omission without inventing a rule ID. No duplicate rule anchors or unassigned body text lines remain. Text extraction does not establish that figure geometry, equations or table cells have been interpreted correctly. Visual and semantic review are explicitly incomplete.

`clause-ledger.json` is the persistent clause workflow ledger. Preparing or importing a source requirement never changes its `coverage_status` from `not_modeled`, or its `verification_status` from `not_verified`. Model and verification IDs must be backed by native persisted elements and typed requirement relations before coverage is assessed. Competition and submission obligations need process evidence as well as a vehicle model. No artifact here claims a complete design or regulatory compliance.

## Rebuild the inventory

Keep the downloaded PDF and complete extracted source paragraphs in a private cache outside the repository. Run the extraction with Python and `pdfplumber`:

```text
python build_inventory.py --pdf <private-cache>/FSAE_Rules_2027_V1.pdf --catalog <private-cache>/catalog.json --private-cache <private-cache>
```

The extractor rejects an unexpected PDF checksum or page count. Catalog metadata is data extracted from the official resource catalog, not executable page scripts. Numbered anchors use margin position and font information so inline cross-references do not become invented rules. Source paragraph hashes and page coordinates allow a reviewer to return to the PDF. Numeric values are source tokens pending interpretation; the parser does not infer operators, conditions or unit conversions.

## Prepare grounded assistant batches

```text
python prepare_batches.py --private-source <private-cache>/private-source-paragraphs.json
python -m unittest -v test_rule_batches.py
```

The default private output is the workspace parent `.build-tools/fsae-batches`. A full run produces 64 batches with at most 50 clauses and 16,000 source characters per batch. `--rule-prefix EV.` limits a run to a rules branch without dropping other rows from the persistent ledger. The tool checks every source paragraph hash, exact clause membership and any reviewed seed hash before writing drafts.

Each batch contains a `.prompt.txt` with pinned source paragraphs for assistant grounding, a `.records.json` payload, and a `.groovy` provenance import draft. Complete source text is never written inside this repository. Three source-checked paraphrases in `reviewed-seeds.json` cover the power, voltage and low-speed regeneration limits at EV.3.3.1–EV.3.3.3. Other requirement text remains null pending review. These seeds still require design and verification review.

Activate the native Requirements viewpoint using the assistant UI before applying any script. The reviewed acceptance bootstrap must create the source module with identifier `FSAE-2027-V1:source-module`. Batch scripts find that module and import missing clauses using `api.requirements().importRequirements(...)` inside one `api.batch(...)`. They skip existing identifiers so reruns preserve existing text, applicability decisions and coverage. Source pages, references and numeric facts are JSON String attributes because the native ReqIF API accepts scalar typed attribute values.

Send a bounded prompt through the assistant, review its proposed code and unresolved decisions, then apply exactly the reviewed draft. Specification imports use `SpecImporter.importSpec(...)` to generate a draft through `ScriptRouter` privacy handling; `applyReviewed(...)` requires an explicit code-review callback before the normal action controls run. OA, SA, LA, PA and EPBS are available; EPBS supports configuration items and physical artifact realization, without functions or exchanges. Native `satisfy`, `verify` and `derive` relations carry evidence separately from source import status.

## Remaining source work

The [official resource catalog](https://www.fsaeonline.com/cdsweb/gen/DocumentResources.aspx), [submission deadlines](https://www.fsaeonline.com/Deadlines.aspx?seriesCode=FSAE) and [official announcements](https://www.fsaeonline.com/cdsweb/app/news.aspx) can change during the season. Published 2027 structural-equivalency templates have not been ingested into this inventory. Applicable 2027 EV forms, energy-meter instructions, handbooks, supplements and later official updates must be pinned and reviewed before any completeness claim. Prior-season resources are explicitly unbaselined. The main rulebook inventory alone cannot prove a complete competition-ready vehicle.
