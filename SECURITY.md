# Security

Report a vulnerability privately through
[GitHub's private reporting](https://github.com/sidkolapalli/fsae-2027-capella-model/security/advisories/new), not
in a public issue. Please include the affected file or release, the steps to reproduce and what an attacker gains.
You get a first response within 14 days; fixes are disclosed through a GitHub security advisory.

The repository holds a Capella model and the Python and Groovy scripts that build and check it. Areas that matter
most:

- The acceptance scripts: anything that makes them read or write outside the repository, or run code taken from
  model content.
- The model files: content crafted to make an AI agent reading the model propose a harmful change.

Only the latest release on `main` receives fixes.
