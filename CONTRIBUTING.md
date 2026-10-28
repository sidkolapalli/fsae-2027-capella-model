# Contributing

Corrections and extensions to the model are welcome: a wrong trace, a rule read the wrong way, a missing
interface, a new detailed slice. Open an issue first for anything larger than a fix, so the change can be planned
against the completion gates in [`acceptance/model-schema.json`](acceptance/model-schema.json). Everyone taking part
follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## Changing the model

The model in `model/` is built by the scripts in `acceptance/`; change the scripts, not only the model. Run them
through the [Agent Toolkit for Eclipse Capella®](https://github.com/sidkolapalli/agent-toolkit-for-capella) dev
server in a disposable Capella 7.0 project with Requirements Viewpoint 0.14.0, with the prerequisites and in the
order that [`acceptance/README.md`](acceptance/README.md) gives, then:

1. run the verifiers (`20`, `31`, `33`, the slice verifiers `safety/shutdown/40-verify-shutdown.groovy`,
   `safety/driver-demand/40-verify-driver-demand.groovy` and `safety/vehicle-protection/40-verify-vehicle-protection.groovy`,
   and `90` after save and reopen);
2. export native validation with `91` and compare it with the previous export;
3. close Capella, replace `model/FSAE-2027-Electric-Arcadia/` with the saved `FSAE-2027-Electric-Arcadia` folder
   from the Capella workspace, and update the checksums:

   ```bash
   cd model && find FSAE-2027-Electric-Arcadia -type f | sort | xargs sha256sum --text > SHA256SUMS
   ```

4. update the `.capella` SHA-256 and the version statement in the README's [Versions](README.md#versions) section.

A model change changes the benchmark. Say so in the pull request: the release that ships it gets a new version
tag, and benchmark results stay tied to the tag they used. The model files are stored byte for byte (see
`.gitattributes`), so their line endings never change between operating systems.

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
