# FSAE 2027 Capella model

A [Capella](https://eclipse.dev/capella/) 7.0 model of a Formula SAE 2027 electric car, built with the Arcadia
method from the public 2027 rulebook, and the scripts that built it. It is the benchmark model of the
[Capella Agent Toolkit](https://github.com/sidkolapalli/capella-agent-toolkit) experiments.

## The model

[`model/FSAE-2027-Electric-Arcadia.zip`](model) (SHA-256
`66e693abfdf821af96f241ee707f74efe44939c4d7b3d63ef830169b075e1ba3`) holds:

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
in Capella 7.0, unzip the archive, and import the project with **File → Import → Existing Projects into
Workspace**.

## How it was built

The scripts in [`acceptance/`](acceptance) built the model in a disposable Capella project through the Capella
Agent Toolkit plug-in's dev server, in numeric order; [`acceptance/rules`](acceptance/rules) pins the rule
inventory and [`acceptance/safety`](acceptance/safety) the three slices.

## Using it as a benchmark

The task sets, harness and grader live in the toolkit's
[`test-models/evals`](https://github.com/sidkolapalli/capella-agent-toolkit/tree/main/test-models/evals). Clone
this repository next to the toolkit, or point the harness at the archive with `EVAL_MODEL_ZIP`.

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
