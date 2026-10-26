# FSAE 2027 Capella model

A [Capella](https://eclipse.dev/capella/) 7.0 model of a Formula SAE 2027 electric car, built with the Arcadia
method from the public 2027 rulebook, and the scripts that built it. It is the benchmark model of the
[Capella Agent Toolkit](https://github.com/sidkolapalli/capella-agent-toolkit) experiments.

## The model

[`model/FSAE-2027-Electric-Arcadia`](model/FSAE-2027-Electric-Arcadia) is the Capella project. It holds:

- about 86,000 elements across the five Arcadia perspectives (operational analysis, system analysis, logical,
  physical, and end-product breakdown);
- every numbered paragraph of the Formula SAE Rules 2027 V1.0 as a native requirement, carrying its identifier,
  page, heading and a hash of its text, but not the text itself;
- three detailed safety slices (shutdown circuit, driver demand, vehicle protection) with 86 paraphrased
  obligations traced to the architecture.

It is a candidate architecture, not a compliant car: design decisions such as frame construction, cell arrangement
and traction hardware stay open on purpose. [`acceptance/README.md`](acceptance/README.md) says exactly what the
model does and does not establish.

To open it, install [Requirements Viewpoint 0.14.0](https://github.com/eclipse-capella/capella-requirements-vp/releases/tag/v0.14.0)
in Capella 7.0 and import `model/FSAE-2027-Electric-Arcadia` with **File → Import → Existing Projects into
Workspace**. Tick **Copy projects into workspace**. To change the model, see [CONTRIBUTING.md](CONTRIBUTING.md).

## Versions

Every Capella Agent Toolkit experiment used tag [`v1.0.0`](https://github.com/sidkolapalli/fsae-2027-capella-model/tree/v1.0.0),
where the project was stored as `model/FSAE-2027-Electric-Arcadia.zip` (SHA-256
`66e693abfdf821af96f241ee707f74efe44939c4d7b3d63ef830169b075e1ba3`). The files here are byte-identical to the
contents of that archive, which [`model/SHA256SUMS`](model/SHA256SUMS) records and CI checks; the `.capella` file's
SHA-256 is `2849ad9b229a025ea6b8958292971ee07d5ef8c3987202eb340e5f655fb00c2d`. A change to the model changes the
benchmark, so the release that ships it gets a new version tag.

## How it was built

The scripts in [`acceptance/`](acceptance) built the model in a disposable Capella project through the Capella
Agent Toolkit plug-in's dev server, in the order [`acceptance/README.md`](acceptance/README.md) gives;
[`acceptance/rules`](acceptance/rules) pins the rule
inventory and [`acceptance/safety`](acceptance/safety) the three slices.

## Using it as a benchmark

The task sets, harness and grader live in the toolkit's
[`test-models/evals`](https://github.com/sidkolapalli/capella-agent-toolkit/tree/develop/test-models/evals). Clone
this repository next to the toolkit, or point the harness at the project folder with `EVAL_MODEL`.

## Licensing

| What | Licence |
|---|---|
| The model in `model/` and the JSON data in `acceptance/` | [CC BY 4.0](LICENSE-CC-BY-4.0) |
| The scripts in `acceptance/` (`.groovy`, `.py`) | [EPL-2.0](LICENSE) |

To attribute the model, cite "FSAE 2027 Capella model, https://github.com/sidkolapalli/fsae-2027-capella-model".

The rule identifiers, page numbers and headings come from the Formula SAE Rules 2027, which are copyright SAE
International. They are included only to trace each requirement to the rulebook and are not covered by the
licences above; the rule text itself is not included, and the safety obligations are paraphrased. Get the rulebook
from [fsaeonline.com](https://www.fsaeonline.com). This project is not affiliated with or endorsed by SAE
International; Formula SAE is a trademark of SAE International.
