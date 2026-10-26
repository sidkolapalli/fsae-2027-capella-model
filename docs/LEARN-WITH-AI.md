# Learn the model with an AI agent

This page shows how to use an AI agent to learn the FSAE 2027 Capella model by asking it questions. It is for
Formula SAE students, including those who are new to Capella and to MBSE (model-based systems engineering).

## Why use an agent

The model has about 86,000 elements: 3,283 requirements, more than 300 functions, about 180 components and actors, and
12 diagrams. Clicking through the Project Explorer to learn all of that takes a long time. An agent can search
the model for you, read an element's properties and follow its links, then explain what it found in plain
language. You still check the answer in Capella yourself. This page shows how.

Two things to keep in mind from the start:

- The model is a candidate architecture. It is not reviewed or verified, and it does not make the car compliant.
- The model holds rule identifiers, page numbers and headings, never the rule text. Keep the official
  Formula SAE Rules 2027 open next to it. Get them from [fsaeonline.com](https://www.fsaeonline.com).

## What you need

| Item | Where to get it |
|---|---|
| Capella 7.0, Requirements Viewpoint 0.14.0 and this model, imported and open | [GETTING-STARTED.md](GETTING-STARTED.md), steps 1 to 5 |
| The Capella Agent Toolkit plug-in, installed in Capella | The toolkit [README quick start](https://github.com/sidkolapalli/capella-agent-toolkit#quick-start) |
| An AI agent that supports MCP, such as Claude Code, Codex, Cursor, GitHub Copilot or Gemini CLI | The toolkit [install guide](https://github.com/sidkolapalli/capella-agent-toolkit/blob/main/docs/INSTALL.md), one section per agent |
| An account for that agent. Most need a sign-in, and many need a subscription or an API key; some have free or student tiers | The agent's own website. Check this before you start |

The toolkit repository is private for now. The toolkit links on this page work once it is public. For now the
plug-in is built from source, which needs a Java 17 JDK, Maven and Python 3, as the quick start describes.

The plug-in runs a small server inside Capella. Your agent talks to that server with MCP (Model Context
Protocol), and the server reads the model that is open in Capella. Nothing leaves your computer except what your
agent sends to its own model provider.

## Connect your agent

0. Set a fixed token before you open Capella. The token is a password that Capella's server checks on every
   request. Follow the
   [Set a fixed MCP token](https://github.com/sidkolapalli/capella-agent-toolkit/blob/main/docs/INSTALL.md#set-a-fixed-mcp-token)
   section of the toolkit install guide, so the connection still works after you restart Capella. Then close
   Capella and your terminal, and reopen both: programs that are already running do not see the new token.
1. Open the model in Capella, as [GETTING-STARTED.md](GETTING-STARTED.md#5-import-the-project-and-open-it)
   describes. Wait until the `.aird` file has expanded into the model tree.
2. Start the server: **Window > Show View > Other > AI Dev Server > Start**. It listens on port 8765, on your
   computer only.
3. Connect your agent and install the toolkit's skills, following the section for your agent in
   [INSTALL.md](https://github.com/sidkolapalli/capella-agent-toolkit/blob/main/docs/INSTALL.md). A skill is a
   packaged set of instructions the agent loads when you run it by name.
4. Run the `capella-setup` skill to check the connection: `/capella-setup` in most agents, `$capella-setup` in
   Codex. It is done when the agent names the open project, `FSAE-2027-Electric-Arcadia`, and its layers.

If the check fails, the `capella-setup` skill lists the common causes. A 401 error, for example, means the token
changed because Capella restarted.

## Stay read-only while you learn

Asking questions only uses the toolkit's read tools. They never change the model:

| Tool | What it does |
|---|---|
| `model_summary` | Names the open project and its layers |
| `model_search` | Finds elements by part of a name, a requirement identifier or a type, optionally in one layer |
| `model_inspect` | Shows one element's properties, including a requirement's attributes |
| `model_relationships` | Shows what an element is linked to: traces, realizations, allocations |
| `model_validate` | Runs quick structure and method checks on an element or a layer |
| `native_validation` | Runs Capella's full validation on the whole model (slow on this model) |
| `capella_docs` | Reads Capella's own help and the toolkit's guides |
| `capella_capabilities` | Lists what the toolkit supports |

A change is different. The agent can only propose one with `propose_model_edit`. Capella then opens a review
dialog with the exact script, and nothing runs until you click **Apply changes**. If a review dialog appears while
you are only asking questions, click **Cancel**.

When you want to practise changes, do it on a copy of the model. Import the project with **Copy projects into
workspace**, as [GETTING-STARTED.md](GETTING-STARTED.md#5-import-the-project-and-open-it) step 4 says, so the
repository files stay unchanged. To start over, first delete the copy: right-click the project, select **Delete**
and tick **Delete project contents on disk**. Then import it again.

## Questions to ask

Each question below is a prompt you can paste as it is. Each one names something the agent can find with the read
tools: an element name, a requirement identifier, a module, a prefix or a layer. Ask a follow-up question whenever
an answer is unclear.

Some names you will see in the answers:

- Every element made for this model starts with `FSAE2027:`.
- A slice is a group of elements, across all the layers, that models one safety topic in detail. The three safety
  slices use the prefixes `SD` (shutdown), `DD` (driver demand and BSPD) and `VP` (vehicle protection and
  mechanical braking).
- Source requirements are named `FSAE-2027-V1:<rule id>:p<paragraph number>`, for example
  `FSAE-2027-V1:GR.1.1:p001`.
- The layers are OA, SA, LA, PA and EPBS.

### Tour of the model

- "Give me a short tour of the model open in Capella: which layers it has and what each one holds."
- "What does the FSAE2027: prefix on element names mean, and what do the SD, DD and VP prefixes stand for?"
- "Which top-level folders are under System Analysis, and what is each one for?"
- "Show me one element from each layer that belongs to the shutdown slice, and say how they connect."
- "I am on the brakes sub-team. Which elements in the model should I read first?"

### Each Arcadia layer

Arcadia looks at the same car from five angles, called layers:

- **Operational Analysis (OA):** what people need to do, before anyone picks a design.
- **System Analysis (SA):** what the car must do, seen as one black box.
- **Logical Architecture (LA):** how the car splits into subsystems, with no parts chosen.
- **Physical Architecture (PA):** what hardware does each job, and how it is connected.
- **EPBS (end-product breakdown structure):** what the team will build or buy and track as deliverables.

Operational Analysis:

- "List the operational entities in OA and say what each one represents."
- "Which operational activities does the driver perform? Group them by capability."
- "Find the FSAE2027:process activities in OA. What do they represent, and why are they in OA and not in PA?"

System Analysis:

- "Which system functions in SA belong to the shutdown slice? List them with a one-line explanation each."
- "Search SA for functions about charging. Which functions do they exchange data with?"

Logical Architecture:

- "List the logical components in LA and say what each subsystem is responsible for."
- "Which logical functions are allocated to FSAE2027:energy-storage Logical subsystem in LA?"
- "Which component exchanges connect FSAE2027:driver-demand Logical subsystem to other subsystems in LA?"

Physical Architecture:

- "What is the difference between behavior components and node components in PA? Give one example of each from
  this model."
- "Which physical links make up the series shutdown path in PA, and which equipment do they connect?"
- "Pick one physical function in the shutdown slice and follow its realization back to SA and OA."

EPBS:

- "List the configuration items in EPBS and say which PA components each one realizes."
- "Which configuration items belong to the driver demand and BSPD slice?"

### Requirements and traceability

- "Find FSAE-2027-V1:GR.1.1:p001 and explain each of its attributes."
- "Find the source requirements for rule EV.8.3. What are their applicability, coverage_status and
  verification_status?"
- "What do the values of coverage_status mean in this model: not_modeled, allocated and allocated_candidate_design?"
- "Find FSAE2027:SD:obligation:SERIES. Which design elements satisfy it, and which constraint verifies it?"
- "Which source requirements does FSAE2027:DD:DD-01 derive from? Give their rule identifiers and pages."
- "Explain the satisfy, verify and derive relations in this model, with one real example of each."
- "What is the difference between the source module and the three candidate obligation modules?"

### The three safety slices

Shutdown (`SD`, rule section EV.8):

- "Explain the shutdown slice: its seven branches and how a fault opens the shutdown circuit."
- "List the FSAE2027:SD constraints that end in implementation TBD. What does each one leave open?"
- "Which elements in the shutdown slice handle the protective latch and the manual reset?"

Driver demand and BSPD (`DD`):

- "Explain the driver demand and BSPD slice from the accelerator pedal to the motor torque."
- "What do the state machines in the DD slice describe? List their states."
- "How does the BSPD connect to the shutdown slice?"

Vehicle protection and mechanical braking (`VP`):

- "Explain the vehicle protection slice: chassis, cockpit, restraint and brakes."
- "Find the constraint about independent brake circuits. What does it require, and what is still open?"
- "Which construction alternatives does the VP slice leave open?"

### Open design decisions

- "List the eight FSAE2027:DEC constraints and explain each question in plain language."
- "Is FSAE2027:DEC-CELLS UNRESOLVED linked to any element? Which elements would a cell choice affect, judging by
  their names, and is that an inference or a model link?"
- "What would our team have to record in the model to resolve DEC-BRAKES?"
- "Why does the model leave these decisions open instead of choosing a design?"

### Validation warnings

- "Run the quick method checks on LA and explain each finding in plain language."
- "Run Capella's full validation and group the warnings by rule. Which rules give the most warnings?"
  Full validation of about 86,000 elements is slow. The recorded baseline is zero errors and 517 warnings
  ([GETTING-STARTED.md](GETTING-STARTED.md#7-run-validation)).
- "Pick one validation warning and show me the element it is about. Is it a real problem or a known gap?"

### Explain like I'm new to MBSE

- "What is MBSE, and how is a Capella model different from a set of drawings and spreadsheets?"
- "Explain the difference between a function and a component, using one example from this model."
- "What is a functional exchange, and how is it different from a component exchange? Show me one of each from the
  shutdown slice."
- "Use Capella's help to explain realization between layers, then show me one realization in this model."
- "Use Capella's help to explain state machines, then walk me through one in the DD slice."
- "Why does the model keep requirements separate from the architecture and link them with traces?"

## Check the agent's answers

The agent can be wrong. It can misread a link, miss an element or fill a gap with a guess. Check every answer
that matters against the model:

1. **Ask for names.** Ask the agent to cite the exact element names or requirement identifiers it used. If it
   does not, ask again.
2. **Find them in the Project Explorer.** Type part of the name into the filter box at the top of the Project
   Explorer. Select the element and read its **Properties** view.
3. **Check traces in Capella.** For a requirement's links, use the Requirements Allocation view or the Semantic
   Browser, as [GETTING-STARTED.md](GETTING-STARTED.md#follow-a-requirements-traces) shows. The Project Explorer
   hides relations.
4. **Open the diagram.** Find the diagram for that layer or slice in [DIAGRAMS.md](DIAGRAMS.md) and open it in
   Capella. Check that the boxes and lines match what the agent said.
5. **Check the rulebook.** For anything about a rule, look up the rule identifier and page in your copy of the
   official rulebook.

The screenshot below shows the Project Explorer with the requirement modules and the open `DEC-*` decisions under
System Analysis, and the shutdown protection diagram open on the right.

[![The Project Explorer with the System Analysis folder open, next to the SA shutdown protection detailed flows diagram](screenshots/capella-window-system.png)](screenshots/capella-window-system.png)

If the agent names an element you cannot find, treat its answer as wrong until you find the element.

## What the agent cannot tell you

- **The rule text.** The model holds rule identifiers, pages, headings and a hash of each paragraph, not the text.
  The agent can tell you where to look in the rulebook, not what the rule says. If it quotes a rule, check the
  quote against the rulebook.
- **Whether the car is compliant.** Every source requirement has `verification_status=not_verified`. A `satisfy`
  link is a candidate allocation, not proof, and zero validation errors does not mean compliance.
- **Design values.** The model sets no dimensions, ratings or part numbers, and the eight `DEC-*` decisions are
  open. A number the agent gives you for one of them does not come from the model.
- **Anything outside the model.** Your team's design, test results and the competition's rulings are not in it.

## Next: build on it

When you understand the model, your team can make it its own: resolve the open decisions, review the requirements,
replace the TBDs and record verification evidence. [MODEL-GUIDE.md](MODEL-GUIDE.md#using-the-model-as-a-development-platform)
gives the steps in order.
