# Competition submissions layer

This layer lists what a team has to hand in for Formula SAE 2027: the SES, the IAD, the ESF, the design documents,
the cost report, the presentation and the general submission rules. For every data item it shows the rules that
ask for it, where its value comes from in the model, whether the model holds it, and which open decision blocks it.
The model stays a candidate architecture. A link says where a value should come from. It does not mean the item is
done, correct or compliant.

[`submissions.json`](submissions.json) holds the 7 submissions and 588 items as paraphrases and ids only. The
readiness pages are in [`../../docs/submissions`](../../docs/submissions) once the layer is built.

## What gets built

- One module in System Analysis, `FSAE2027:SUB:module`, next to the three safety slice modules.
- One folder per submission (`FSAE2027:SUB:SES`, `:IAD`, `:ESF`, `:DES`, `:COST`, `:PRES`, `:GEN`) and one section
  folder per item prefix (`FSAE2027:SUB:SES:TUB` for the `SES-TUB-nn` items).
- One requirement per item, `FSAE2027:SUB:item:<ID>`, with the item text, its section as chapter and these String
  attributes: `submission_id`, `submission_item_id`, `submission_rule_refs`, `submission_source_kind`,
  `submission_model_status`, `submission_model_anchor`, `submission_blocking_decisions`, `submission_status`,
  `submission_owner`, `submission_due`, `submission_value`, `submission_evidence_ref`, `submission_note`, plus the
  shared `slice_id`, `source_document_id` and `source_sha256`.
- Submission folders also carry `submission_governing_rules`, `submission_format`, `submission_penalty_group`,
  `submission_status`, `submission_due` (the official due date) and `submission_cancellation`.

Every unknown value is `TBD`. At build time every item has `submission_value=TBD` and `submission_status=not_started`,
and `submission_note` is an empty string when the verified data has no note. The two IC-only rows (GEN-PS1-11 and
GEN-PEN-09) are the exception: the car is electric, so they start as `not_applicable` (the optional `status` key in
`submissions.json`). Conditional items (aluminum, composite, monocoque, custom IA, digital APPS/BSE) start as
`not_started`; set them to `not_applicable` through the status CSV once DEC-CHASSIS or the IA route is chosen.

| Attribute | Allowed values |
|---|---|
| `submission_source_kind` | `model`, `cad`, `analysis`, `test`, `supplier`, `admin` |
| `submission_model_status` | `present`, `partial`, `absent` (a reviewed judgement, not computed; see below) |
| `submission_status` | `not_started`, `in_progress`, `ready_for_review`, `submitted`, `accepted`, `not_applicable` |
| `submission_penalty_group` | `Tech`, `Other`, `none` |
| `submission_due`, `submission_cancellation` | an ISO date such as `2027-01-15`, or `TBD` |

The Requirements Viewpoint API cannot create enumerations, so the verifier and `make_reports.py --check` enforce these.

One rule sets `submission_model_status` on every page:

- `present`: the candidate model already holds the content (an architecture element, state logic or a diagram), with
  no unresolved blocking decision and nothing marked TBD. It does not mean the submitted value is final.
- `partial`: a model element or obligation stands for the item but holds no team value yet. Items that only restate a
  rule value (for example the 300 kg and 7.0 m/s impact reference) are `partial`, not `present`.
- `absent`: the model has nothing for the item, or cannot hold it: photos, raw test data, receipts, facility and test
  records, declarations.

`submission_source_kind` says where the value comes from: `model` (this Eclipse Capella® model), `cad` (CAD and
drawings), `analysis` (calculations), `test` (physical tests), `supplier` (datasheets and purchase records), `admin`
(team administration).

The page overviews state the model state in plain words. In attribute terms: every source requirement still reads
`coverage_status=not_modeled` and `verification_status=not_verified`, traced or not, so trace links (not that
attribute) are the evidence for `partial`; the VP obligations read `coverage_status=allocated_candidate_design` and
`design_status=unresolved`; the Table PS-1/PS-2 rows read `applicability=unresolved`; the 2027 SES, ESO/ESA and
Cost Addendum templates are listed in [`../rules/source-manifest.json`](../rules/source-manifest.json) but not ingested,
and the 2027 ESF is still under `pending_sources` there.

## Link semantics

| Link | Meaning |
|---|---|
| `derive(item -> source paragraph)` | A rule asks for this item. A bare rule id links all its paragraphs, unless the item names some of them. Headings with no paragraphs of their own stay as text in `submission_rule_refs`. |
| `derive(item -> slice obligation)` | The item gives evidence for this `FSAE2027:VP:obligation:*` requirement. |
| `verify(item -> Constraint)` | The item is evidence that this verification obligation produces. |
| `satisfy(element -> item)` | This component, function, exchange, CI, actor, activity, state or guard/INVARIANT constraint holds or produces the value. |
| `derive(submission folder -> Table PS row)` | The Table PS-1/PS-2 row that lists the submission; IAD derives from F.8.8.1. |
| `satisfy(activity -> submission folder)` | "Prepare and document competition entry" produces every submission; "Inspect and present the vehicle" also produces the Cost Addendum. |

No link, or any number of links, says nothing about whether a submission is complete or compliant. `DEC-*` decisions
get no link; they are listed in `submission_blocking_decisions`, and an item is blocked while one of them still has a
constraint name ending in `UNRESOLVED`. The source paragraphs get no new attributes, and their `coverage_status` stays
as it was.

The verified anchors were prose. `prepare_submissions.py --freeze` turned them once into `{class, layer, name}`
entries with capellambse, using these rules:

- a `Part` is never an anchor; the component, CI or entity it types is;
- a name that exists in several layers links at SA, else PA, LA, OA, EPBS, unless the text names the class
  (for example `SystemFunction FSAE2027:DD ...`);
- Constraints that are `verify` targets get `verify`; guard and INVARIANT Constraints get `satisfy`; `DEC-*`
  Constraints only block; other Constraints (OPEN and TBD markers) stay text only;
- diagrams, PNG paths, `none ...` notes and wildcard or range text stay in `submission_model_anchor`.

## Run order

Run in a disposable copy of the acceptance project, after the bootstrap, the source import and the three safety
slices (see [`../README.md`](../README.md)). Each script is one transaction.

1. `30-create-submissions.groovy`: the module, 7 submission folders, 73 section folders, folder attributes and links.
   It refuses to run twice.
2. The 13 `31-import-<submission>-<n>.groovy` chunks, in any order. Each refuses to import its items twice. A chunk
   imports once per section folder it touches, so its run time grows with its sections as well as its items.
3. The 13 `32-trace-<submission>-<n>.groovy` chunks, in any order. Each needs all 588 items and rejects a missing or
   ambiguous anchor.
4. `40-verify-submissions.groovy`: ids, counts, attributes and vocabularies, exact link sets, source-paragraph hashes,
   the DEC constraints and the unchanged source statuses. Save, close and reopen the model, then run it again.
5. `python make_reports.py` writes the readiness pages. This script and `--freeze` need a Python with capellambse
   0.8.1 installed; everything else runs on plain Python.

## Updating status

The model is the source of truth. Edit the `submission_*` attributes in the Properties tab of the Requirements
Viewpoint, or batch them from a CSV with the columns `item_id,status,owner,due,value,evidence_ref`:

```text
python prepare_submissions.py --status status.csv
```

This checks the item ids, the status vocabulary and the dates, then writes `50-update-status.groovy`. Empty cells
leave a value unchanged. The script sets attributes on existing items only, never adds links, never changes
`submission_model_status`, and gives the same result when run twice. Official due and cancellation dates go on the
submission folders in the Properties tab.

1. Open your team's model in Capella with the dev server running, and run `50-update-status.groovy` through the dev
   server's `/execute`, as the other scripts are run (see [`../README.md`](../README.md)). It runs in one
   transaction; save the model afterwards (Ctrl+S) so the change reaches the `.capella` file.
2. Check the saved values: `python make_reports.py --check --model <your .aird>`.
3. Regenerate the pages: `python make_reports.py --model <your .aird>`. Without `--model` the script reads the model
   in this repository, not your working copy. `--out <folder>` writes the pages somewhere else.

`40-verify-submissions.groovy` is a build-time check and fails once statuses change.

When a decision is resolved or the design gains a value, review `submission_model_status` and add anchors with a new
trace chunk.

## Regenerate

```text
python prepare_submissions.py --private-source <private-cache>/private-source-paragraphs.json
python -m unittest -v test_submissions.py
```

`prepare` checks ids, vocabularies and the anchor rule, resolves every rule reference against
[`../rules/clause-ledger.json`](../rules/clause-ledger.json), prints the text-only heading refs and writes the chunks
from the templates (`*.template.groovy`) with `expected-manifest.json`. Each chunk stays under 90,000 characters and
60 items. With `--private-source` it also checks the source-paragraph hashes and rejects any run of 8 or more words
copied from the rulebook into `submissions.json` or the Markdown pages. The rulebook text is never stored here.

`40-verify-submissions.groovy` compares short SHA-256 digests per item; `expected-manifest.json` holds the full link
sets for finding a difference. To re-freeze the anchors after the model changes, run with capellambse (the reviewed
model status, notes, build status, overviews and governing rules already in `submissions.json` are kept):

```text
python prepare_submissions.py --freeze <verified-submissions.json>
```

Not built: native enumeration attributes, attributes on the source paragraphs, and merging the overlapping
administration items of SES, IAD and GEN.
