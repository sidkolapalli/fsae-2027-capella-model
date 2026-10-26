# Contributing

Corrections and extensions to the model are welcome: a wrong trace, a rule read the wrong way, a missing
interface, a new detailed slice. Open an issue first for anything larger than a fix, so the change can be planned
against the completion gates in [`acceptance/README.md`](acceptance/README.md).

## Changing the model

The model in `model/` is built by the scripts in `acceptance/`; change the scripts, not only the model. Run them
through the [Capella Agent Toolkit](https://github.com/sidkolapalli/capella-agent-toolkit) dev server in a disposable
Capella 7.0 project with Requirements Viewpoint 0.14.0, then:

1. run the verifiers (`20`, `31`, `33`, the slice verifiers and `90` after save and reopen);
2. export native validation with `91` and compare it with the previous export;
3. zip the project as `model/FSAE-2027-Electric-Arcadia.zip` and update its SHA-256 in the README.

A new model archive changes the benchmark. Say so in the pull request: experiments run on the old archive stay
tied to its hash.

Never add rulebook text. Requirements carry the rule identifier, page, heading and a hash of the text; obligations
are paraphrased. The Python tests in `acceptance/` check the inventory and slices:

```bash
cd acceptance/rules && python -m unittest
cd ../safety/shutdown && python -m unittest
cd ../driver-demand && python -m unittest
```

## Branches and pull requests

The repository follows gitflow. `develop` is the default branch: branch `feature/<name>` or `fix/<name>` from it
and open the pull request against it. `main` holds releases, tagged `v<version>` when a `release/<version>` branch
merges. Both branches are protected: changes arrive through pull requests, the `test` check must pass, and force
pushes are refused. Commit messages follow Conventional Commits (`feat:`, `fix:`, `docs:`).

By contributing you agree that model and data changes are licensed CC BY 4.0 and script changes EPL-2.0, as
described in the [README](README.md#licensing).
