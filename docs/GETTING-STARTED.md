# Getting started

This guide takes you from nothing to exploring the FSAE 2027 Capella model. It assumes you know systems
engineering but may be new to Capella. For how the model is organized, see [MODEL-GUIDE.md](MODEL-GUIDE.md).

## What this model is, and what it is not

The model is a reference platform for a Formula SAE electric car. It shows what a vehicle development model can
look like when the rules are turned into requirements, the architecture covers every Arcadia perspective, design
decisions are written down, and verification is tracked. A team can use it as a guideline and a head start.

It is not a finished or compliant car:

- the architecture is a candidate, not a reviewed design;
- every source requirement has `verification_status=not_verified`;
- eight design decisions (`DEC-*`) are open on purpose, for your team to make;
- many records are still marked as needing review or as not modeled.

The open decisions and unfinished items are also realistic tasks. The
[Capella Agent Toolkit](https://github.com/sidkolapalli/capella-agent-toolkit) uses them to find gaps in AI agent
skills.

The model carries rule identifiers, page numbers and headings from the Formula SAE Rules 2027, but not the rule
text. Get the rulebook from [fsaeonline.com](https://www.fsaeonline.com). This project is not affiliated with or
endorsed by SAE International; Formula SAE is a trademark of SAE International.

## 1. What you need

| Item | Details |
|---|---|
| Capella 7.0.x | The model was built with Capella 7.0. Use the latest 7.0.x release, [Capella 7.0.1](https://github.com/eclipse-capella/capella/releases/tag/v7.0.1). |
| Requirements Viewpoint 0.14.0 | The add-on that holds the requirements. [Release page](https://github.com/eclipse-capella/capella-requirements-vp/releases/tag/v0.14.0); it is compatible with Capella 7.x. |
| Computer | The official Capella guide recommends a 64-bit Windows, Linux or macOS computer, a 2 GHz processor, 4 GB RAM and 15 GB of free disk space. It also notes that Linux and macOS are less field-tested than Windows. |
| Memory for Capella | Capella 7.0 starts with a maximum Java heap of 3000 MB (`-Xmx3000m` in `capella.ini`). The model has about 86,000 elements. The toolkit's evaluation harness opens it with `-Xmx3g`, about the same size as the default. |
| Disk for the model | About 26 MB for the project files. |
| Git (optional) | To clone the repository. You can download a ZIP instead. |

Do not open the model with a newer Capella line (7.1 or later) unless you mean to migrate it. The official Capella
guide says a project opened in a newer version must be migrated, and a migrated project cannot be opened with the
older version again.

## 2. Install Capella

The steps below follow the official guide,
[How to install Capella and Addons](https://github.com/eclipse-capella/capella/blob/v7.0.1/doc/plugins/org.polarsys.capella.ui.doc/html/Installation%20Guide/How%20to%20install%20Capella%20and%20Addons.mediawiki).

1. Open the [Capella 7.0.1 release](https://github.com/eclipse-capella/capella/releases/tag/v7.0.1) and download
   the product for your system (Windows x86_64, Linux x86_64 or aarch64, macOS x86_64 or aarch64).
2. On Windows or Linux, extract the archive into a folder. Installation is complete once it is extracted. The
   folder contains `capella` (the workbench and its executable) and `samples`.
   - On Windows, keep the path to the `capella` folder short. The official limit is 115 characters, and a space
     counts as three.
3. On macOS, extract the archive, drag Capella into Applications, then run this in Terminal:

   ```bash
   xattr -d com.apple.quarantine /Applications/Capella.app
   ```

4. Start Capella and choose a new, empty workspace folder.

Capella 6.0 and later include their own Java runtime. You do not need to install Java.

On macOS, the installation folder is inside the application bundle: `capella.ini` and the `dropins` folder are in
`Capella.app/Contents/Eclipse`. In Finder, right-click `Capella.app` and choose **Show Package Contents** to see them.

### Memory settings

The heap settings are in `capella.ini` in the Capella installation folder, in the `-vmargs` section. Capella 7.0.1
ships with `-Xms1000m` and `-Xmx3000m`. The official
[Performance Recommendations](https://github.com/eclipse-capella/capella/blob/v7.0.1/doc/plugins/org.polarsys.capella.th.doc/html/Performance%20Recommandations/Performance%20Recommandations.mediawiki)
say you can adapt these values to your machine and model size by editing `capella.ini`.

- Check that your `capella.ini` has `-Xmx3000m` (or `-Xmx3g`) or more. Do not set it lower.
- If Capella reports an out-of-memory error, close Capella, raise `-Xmx` (for example to `-Xmx4g`), and restart.
  Keep the value below the free memory of your computer.

## 3. Install Requirements Viewpoint 0.14.0

The model stores its requirements with the Requirements Viewpoint. Without it, the model does not open correctly.
The [release page](https://github.com/eclipse-capella/capella-requirements-vp/releases/tag/v0.14.0) offers two
packages. Use one of them.

### Option A: dropins (offline)

1. Download `Requirements-dropins-0.14.0.202407170938.zip` from the
   [dropins folder](https://download.eclipse.org/capella/addons/requirements/dropins/releases/0.14.0).
2. Close Capella.
3. Extract the archive into the `dropins` folder of your Capella installation. The archive already contains a
   top folder named `Requirements`. The result must be `dropins/Requirements/eclipse/plugins` and
   `dropins/Requirements/eclipse/features`. Some extract tools, such as Windows **Extract All**, add an extra folder
   named after the archive. If yours did, move `Requirements` up so the structure matches.
4. Start Capella once with the `-clean` command-line parameter, so it picks up the new add-on. Either run
   `capella.exe -clean` (Windows) or `./capella -clean` (Linux) from a terminal in the `capella` folder, or add
   `-clean` as the first line of `capella.ini` and start Capella as usual. If you edited `capella.ini`, delete that
   line after the first start.

### Option B: update site (online)

1. In Capella, open **Help > Install New Software**.
2. First add the three dependency sites the release page lists, each with **Add** and its URL:
   - Coevolution: `https://download.eclipse.org/diffmerge/releases/0.15.0/edm-coevolution-site`
   - RMF: `https://download.eclipse.org/rmf/updates/releases/0.13.0`
   - Sphinx: `https://download.eclipse.org/sphinx/releases/0.9.x`
3. Add the Requirements Viewpoint site:
   `https://download.eclipse.org/capella/addons/requirements/updates/releases/0.14.0`
4. Select the Requirements Viewpoint features, click **Next**, accept the license and click **Finish**.
5. Restart Capella when prompted.

### Check the installation

1. Open **Help > About Capella > Installation Details**. The Requirements Viewpoint features should be listed.
2. Open **Window > Show View > Other > General > Error Log** and check that there are no errors about the add-on.

You do not need to activate the viewpoint in this model. It is already referenced.

## 4. Get the model

### Pick a version

| You want to | Use |
|---|---|
| Explore the model or start from it | The latest release on the [releases page](https://github.com/sidkolapalli/fsae-2027-capella-model/releases). Later releases lay out the diagrams; the semantic `.capella` file is the same as in v1.1.0. |
| Reproduce benchmark results recorded against the original version | `v1.0.0` (stored as `model/FSAE-2027-Electric-Arcadia.zip`) or `v1.1.0` (the same model as plain files). |

### Download it

With Git:

```bash
git clone https://github.com/sidkolapalli/fsae-2027-capella-model.git
cd fsae-2027-capella-model
git checkout <tag>   # for example the latest tag on the releases page, or v1.1.0
```

The clone starts on the default branch, `develop`, which may be ahead of the latest release. Check out a tag to
get exactly that release.

Without Git: on GitHub, use **Code > Download ZIP**, or download the source archive of a release from the
releases page. Extract it.

For v1.1.0 and later, check the files from the repository root (Git Bash or Linux):

```bash
cd model && sha256sum -c SHA256SUMS
```

On macOS, use `cd model && shasum -a 256 -c SHA256SUMS`. In PowerShell on Windows, run this from the `model`
folder:

```powershell
Get-Content SHA256SUMS | ForEach-Object { $h, $f = $_ -split '  ', 2; if ((Get-FileHash $f -Algorithm SHA256).Hash -eq $h) { "$f OK" } else { "$f FAILED" } }
```

Every line should end in `OK`. The model files are stored byte for byte, so line endings do not change between
operating systems. For v1.0.0, run `sha256sum model/FSAE-2027-Electric-Arcadia.zip` (on macOS
`shasum -a 256 model/FSAE-2027-Electric-Arcadia.zip`, in PowerShell
`Get-FileHash model/FSAE-2027-Electric-Arcadia.zip`) and compare the result with the SHA-256 in the
[README](../README.md#versions).

## 5. Import the project and open it

The steps follow the official guide
[How to import an existing Capella project](https://github.com/eclipse-capella/capella/blob/v7.0.1/doc/plugins/org.polarsys.capella.ui.doc/html/First%20steps%20with%20Capella/3.3.%20How%20to%20import%20an%20existing%20Capella%20Project.mediawiki).

1. In Capella, choose **File > Import**.
2. Expand **General**, choose **Existing Projects into Workspace** and click **Next**.
3. For v1.1.0 and later, choose **Select root directory** and browse to
   `model/FSAE-2027-Electric-Arcadia` in the repository.
   For v1.0.0, choose **Select archive file** and select `model/FSAE-2027-Electric-Arcadia.zip`.
4. With **Select root directory**, tick **Copy projects into workspace**. Your edits then go to the copy in the
   workspace, and the repository files stay unchanged. An archive import always extracts the project into the
   workspace, so there is nothing to tick.
5. Click **Finish**.
6. In the Project Explorer, expand `FSAE-2027-Electric-Arcadia` and double-click
   `FSAE-2027-Electric-Arcadia.aird`.

Opening takes a while because of the model size. When it is open, the `.aird` file expands into the model tree.

## 6. A first tour

### Where each perspective lives

The Project Explorer shows the model in the five Arcadia perspectives:

| Perspective | What you find there |
|---|---|
| Operational Analysis | The vehicle and its operational actors (driver, team personnel, competition officials, emergency responders), their activities and capabilities |
| System Analysis | The vehicle as a black box, its functions and actors, all requirement modules, and the open decisions |
| Logical Architecture | Logical subsystems and logical functions |
| Physical Architecture | Physical equipment, physical functions and their deployment |
| EPBS Architecture | Configuration items of the end-product breakdown |

Architecture elements created for this model (actors, functions, components, exchanges, constraints) have names
starting with `FSAE2027:`. Requirement modules and diagrams have descriptive names, and source requirements are
identified by `FSAE-2027-V1:<rule id>:p<paragraph number>`. [MODEL-GUIDE.md](MODEL-GUIDE.md) gives the full
structure and naming rules.

### Open the diagrams

The model has 12 diagrams. Each ends in `(candidate)`. A diagram sits under the element it represents; the
Project Explorer also has a folder that lists all diagrams by category. Double-click a diagram to open it.

| Perspective | Diagrams |
|---|---|
| Operational Analysis | `FSAE 2027 OA operational context (candidate)`, `FSAE 2027 OA activities and interactions (candidate)` |
| System Analysis | `FSAE 2027 SA vehicle and external actors (candidate)`, `FSAE 2027 SA vehicle functions and flows (candidate)`, `FSAE 2027 SA shutdown protection detailed flows (candidate)`, `FSAE 2027 SA driver demand and BSPD detailed flows (candidate)`, `FSAE 2027 SA occupant protection and braking detailed flows (candidate)` |
| Logical Architecture | `FSAE 2027 LA logical subsystems (candidate)`, `FSAE 2027 LA logical functions and flows (candidate)` |
| Physical Architecture | `FSAE 2027 PA equipment and deployed behavior (candidate)`, `FSAE 2027 PA physical functions and flows (candidate)` |
| EPBS Architecture | `FSAE 2027 EPBS subsystem configuration items (candidate)` |

You can see all 12 without Capella in [DIAGRAMS.md](DIAGRAMS.md).

### Find a rule requirement and its attributes

1. Expand **System Analysis**. The requirement modules are there:
   - `Formula SAE 2027 V1 source obligations`: 3,197 source records, one per rulebook paragraph or source object;
   - `EV.8 shutdown interpreted candidate obligations` (39), `Driver demand and BSPD derived candidate obligations`
     (15) and `Vehicle protection and mechanical braking candidate obligations` (32): the 86 paraphrased
     obligations of the three safety slices.
2. Expand the source module. Source requirement identifiers follow the pattern
   `FSAE-2027-V1:<rule id>:p<paragraph number>`, for example `FSAE-2027-V1:GR.1.1:p001`.
3. Find the requirement for `GR.1.1`. The tree label of a requirement is set by the Requirements Viewpoint
   preference (**Window > Preferences > Capella > Requirements**). Source records have no name, and all but three have no text, so by
   default the label shows their chapter name, the rule number, not the identifier. The module lists nine entries
   labelled `GR.1.1`, one per paragraph. `p001` is the first of them and the first item in the module.
4. Select it and open the **Properties** view. Check that its identifier is `FSAE-2027-V1:GR.1.1:p001`. Its long
   name is the rule number and heading. Its text is empty on purpose: the rulebook text is not in the model.
5. Look at its 19 attributes. Right-click the requirement and choose **Send to Mass Visualization** (or
   **Send to Mass Editing** to change values). A table opens with the requirement's attributes. The most useful
   ones are:

   | Attribute | What it tells you | Value for GR.1.1 p001 |
   |---|---|---|
   | `source_rule_id`, `source_pages` | Where the paragraph is in the rulebook | `GR.1.1`, `[5]` |
   | `source_text_sha256` | A hash of the paragraph text, to check against your own copy | a SHA-256 value |
   | `applicability`, `applicability_evaluation_status` | Whether it applies to this car, and how far that was checked | `applicable`, `scope_filter_only_clause_conditions_need_review` |
   | `interpretation_status` | Whether someone has read and interpreted it | `needs_semantic_review` |
   | `coverage_status` | Whether the architecture covers it | `not_modeled` |
   | `verification_status` | Whether it is verified | `not_verified` |

   The other attributes record the source document, its hash, the clause identifier, page coordinates,
   referenced rules, numeric facts and linked model elements.

### See the open decisions

Under **System Analysis** there are eight constraints named `FSAE2027:DEC-<topic> UNRESOLVED`:
`DEC-CHASSIS`, `DEC-CELLS`, `DEC-DRIVE`, `DEC-BRAKES`, `DEC-CHARGER`, `DEC-THERMAL`, `DEC-CONTROL` and
`DEC-PRESSURE`. They cover choices such as frame construction, cell arrangement, drive hardware, brake layout,
charging, cooling, control architecture and pressurized equipment. The model sets no dimensions, ratings or part
numbers for them. They are your team's decisions to make.

The safety slices also hold tracking constraints whose names say what is still open, for example
`FSAE2027:SD <topic> verification NOT_VERIFIED`.

### Follow a requirement's traces

The model uses three relation types:

| Relation | From | To |
|---|---|---|
| `satisfy` | A design element | The requirement it satisfies |
| `verify` | A requirement | The constraint that tracks its verification |
| `derive` | A derived obligation | The source requirement it comes from |

To follow them:

1. Expand `EV.8 shutdown interpreted candidate obligations`, then its folder `Shutdown safety obligations`. These
   obligations have text, so the tree shows the start of the text. Select the requirement whose label starts
   with "Connect the BMS, IMD, BSPD interface...". In the **Properties** view, check that its identifier is
   `FSAE2027:SD:obligation:SERIES` (long name `FSAE2027:SD SERIES`).
2. In the **Properties** view, open the Requirements Allocation view. It lists the incoming and outgoing relations.
   This obligation has 21 incoming `satisfy` relations from design elements.
3. In the **Semantic Browser**, Requirements Viewpoint elements are hidden by default. Click the toolbar toggle that
   shows them. The browser then lists queries such as *Allocating model elements and representations* and
   *Referenced Requirements via outgoing Internal Relations*.

Relations are technical elements and the Project Explorer hides them by default. Use the Properties view or the
Semantic Browser rather than the tree.

## 7. Run validation

Capella has a built-in validator
([Model Validation](https://github.com/eclipse-capella/capella/blob/v7.0.1/doc/plugins/org.polarsys.capella.ui.doc/html/12.%20Model%20Analysis/12.1.%20Model%20Validation.mediawiki)).

1. In the Project Explorer, right-click the root of the model and run the validation command from the context
   menu. You can also run it on any element to validate only that part.
2. Wait. On about 86,000 elements this takes some time.
3. Read the results in the **Information View**. A tooltip explains each broken rule. To keep the results, use the
   **Save** button in the Information View toolbar to export them as HTML.

What to expect: the recorded run of the complete installed validator ([acceptance/README.md](../acceptance/README.md))
gave **zero errors and 517 warnings**. You get different numbers if you validate only part of the model or change
the active rule categories (**Window > Preferences > Model Validation > Constraints > Capella**). The warnings are
kept in the record, not resolved. Zero errors means the model is well formed; it does not mean the car is compliant
or the design is verified.

## 8. Use it with an AI agent

The [Capella Agent Toolkit](https://github.com/sidkolapalli/capella-agent-toolkit) is a Capella 7 plug-in with an
MCP server. Any MCP client, such as Claude Code or Codex, can then read and change the model open in Capella, and a
person reviews every edit in Capella before it runs. The toolkit also has agent skills and the evaluation harness
that uses this model. To learn the model by asking an agent questions, see [LEARN-WITH-AI.md](LEARN-WITH-AI.md).
To set up the harness, see
[Using it as a benchmark](../README.md#using-it-as-a-benchmark) in the README. For `v1.0.0`, unzip `model/FSAE-2027-Electric-Arcadia.zip` and
point `EVAL_MODEL` at the extracted project folder. Install and setup steps are in the toolkit's README.

## 9. Troubleshooting

| Problem | Likely cause | What to do |
|---|---|---|
| The model does not open, reports missing extensions, or the requirement modules are missing | Requirements Viewpoint 0.14.0 is not installed | Install it (step 3) and restart. Check **Help > About Capella > Installation Details**. Do not run model detachment: it permanently deletes the requirement data. |
| The viewpoint is installed but requirements tools are missing | The viewpoint did not load | Check the **Error Log** view. For a dropins install, check the folder structure and start once with `-clean`. |
| Capella says the project must be migrated | You opened it with a newer Capella line | Cancel and use Capella 7.0.x. A migrated project cannot be opened with 7.0 again. |
| Out-of-memory error, or Capella freezes while opening | The Java heap is too small | Check `-Xmx` in `capella.ini` (default `-Xmx3000m`) and raise it if your computer has the memory (step 2). |
| Some files cannot be opened, or editors fail, on Windows | The Capella installation path is too long | Reinstall Capella in a short path (under 115 characters to the `capella` folder). |
| Capella does not start on macOS | macOS quarantine | Run `xattr -d com.apple.quarantine /Applications/Capella.app`. |
| Your edits show up as changes in the Git repository | The project was imported without **Copy projects into workspace** | Remove the project from the workspace (do not delete the files) and import it again with the copy option. To restore the repository files, use `git checkout -- model`. |
| The model files are read-only or Capella cannot save | The files are in a read-only location, such as a protected folder | Import with **Copy projects into workspace** into a workspace you can write to. |
| `sha256sum -c` reports `FAILED` | The model files changed after download or checkout | Download again or reset with `git checkout -- model`. |
| A diagram looks different from [DIAGRAMS.md](DIAGRAMS.md) after you edit the model | Capella refreshed the diagram, or an automatic layout moved elements | This is expected after edits. **Arrange All** in the diagram toolbar gives a new automatic layout, not the published one. Closing without saving discards every unsaved change, including your model edits. To get the published layout back, import the project again from the repository (if you imported without the copy option, first restore the files, see the row on edits showing up in Git). |

## Next steps

- Read [MODEL-GUIDE.md](MODEL-GUIDE.md) for the structure, naming and slices.
- Read [acceptance/README.md](../acceptance/README.md) for exactly what the model does and does not establish.
- To propose changes, see [CONTRIBUTING.md](../CONTRIBUTING.md).
