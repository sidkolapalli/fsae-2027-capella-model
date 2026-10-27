# Competition submissions

One page per Formula SAE 2027 submission, generated from the model by `acceptance/submissions/make_reports.py`. Readiness counts submitted and accepted items and leaves out not_applicable ones. An item is blocked while a decision it needs still has a constraint name ending in UNRESOLVED (⛔).

The ESO/ESA form (Electrical System Officer and Advisor details, PDF, Tech group) is a separate upload with no checklist of its own. Like the SES and ESF, missing it is grounds for removing the entry. It is tracked as GEN-PS1-02 on the [general page](general.md).

How to read the tables:

- **Model**: `present` means the candidate model already holds the content (an architecture element, state logic or a diagram) with nothing open; it does not mean the submitted value is final. `partial` means a model element or obligation stands for the item but holds no team value yet; items that only restate a rule value are `partial` too. `absent` means the model has nothing for the item, or cannot hold it (photos, raw test data, receipts, records).
- **Source**: where the value comes from: `model` (this Capella model), `cad` (CAD and drawings), `analysis` (calculations), `test` (physical tests), `supplier` (datasheets and purchase records), `admin` (team administration).
- **Status**: the team's progress. Conditional rows (aluminum, composite, monocoque, custom IA, digital APPS/BSE) count until you mark them `not_applicable` through the status CSV, once DEC-CHASSIS or the IA route is chosen ([how to update status](../../acceptance/submissions/README.md#updating-status)).
- **Blocking**: ⛔ marks a decision whose constraint name still ends in UNRESOLVED. [Open design decisions](../MODEL-GUIDE.md#open-design-decisions) lists them, and step 2 of [Using the model as a development platform](../MODEL-GUIDE.md#using-the-model-as-a-development-platform) says how to record and close one.
- **Note** says why an item is partial or absent, which rows are conditional and what is still missing. **Evidence** is the drawing, file, photo or test id the team records.

| Submission | Items | Present | Partial | Absent | Blocked | Readiness |
|---|---|---|---|---|---|---|
| [Structural Equivalency Spreadsheet (SES) and structural documentation](ses.md) | 141 | 0 | 56 | 85 | 137 | 0 % (0 of 141) |
| [Impact Attenuator Data (IAD) report - section of the Structural Equivalency Spreadsheet (SES)](iad.md) | 84 | 0 | 61 | 23 | 26 | 0 % (0 of 84) |
| [Electrical System Form (ESF)](esf.md) | 150 | 6 | 61 | 83 | 116 | 0 % (0 of 150) |
| [Design Event documents (Design Briefing, Design Spec Sheet, Vehicle Drawings) - S.4.2 to S.4.5, with S.4.6 to S.4.9 event-day evidence](design.md) | 79 | 3 | 32 | 44 | 40 | 0 % (0 of 79) |
| [Cost Report (Cost and Manufacturing Event, with its Cost Addendum and Cost Scenario companions)](cost.md) | 45 | 0 | 6 | 39 | 12 | 0 % (0 of 45) |
| [Presentation Event (Business Presentation) pre-event submission, Table PS-1 row "Presentation (if required)", S.2.4](presentation.md) | 37 | 0 | 6 | 31 | 6 | 0 % (0 of 37) |
| [General submission rules (PS.1-PS.3 and cross-cutting admin rules): location, format, deadlines, penalties](general.md) | 52 | 0 | 2 | 50 | 1 | 0 % (0 of 50) |
