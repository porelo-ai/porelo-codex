# CX14 — The Acceptance Runs On Every Pin Change

**Status:** Analyzed. Asked for by the operator on 2026-11-08: plan it now and build it after the move.
**Size:** M - a new job copied from the other adapters, one repository secret, and one green run on GitHub. About 60
minutes, the M median.

**What must be true.** A change to `agent.cli.version` (Codex CLI's pin) is not on `main` until the acceptance suite has passed against the
packages built from that change, on a rented ubuntu machine and a rented fedora machine, as in `porelo-claude-code`,
`porelo-pi` and `porelo-omp`.

**This does not hold today.** No workflow here runs the acceptance suite. `build.yml` builds and publishes the packages,
and `update.yml` checks only the publishers' digests against the pin. [CX11](README.md#cx11)'s hook trust is a SHA-256 of the hook's
normalized form, so a pin that changes that form switches the hook off without an error, and only the suite on the VM,
run by hand, sees it ([CX11](README.md#cx11), *Measured*).

## Plan

- **`build.yml` gains the `acceptance` job of `porelo-omp`'s Build.** It runs one leg per operating system, matrix
  `os: [ubuntu, fedora]` with `max-parallel: 1`, and needs the `build` job.
  - It runs on a push to `main` (a pin change, made by hand here), on a tag and on a dispatch, on GitHub only.
  - Each leg takes the packages from the `packages` artifact the `build` job already uploads where
    `vars.PORELO_PUBLISH` is `true`.
  - Each leg runs `porelo-machines acceptance --os <os> --kind amd64 --cucumber .` with `PORELO_CI_LEG: acc-<os>`, and
    ends with *Delete what this run created* (`sweep --mine`).
- **Legs proven once:** the `proof` job, with `porelo-release proven-legs`, as in the other adapters' Builds: given
  the commit's tree and the legs' job names, it looks for an earlier run whose legs passed on that same tree, and the
  legs are skipped when it finds one. A push
  that does not change the shipped tree takes legs that already proved it, so only a change to the tree, a pin change
  among them, rents.
- **The credential scenarios:** `session.feature` and `waiting.feature` are `@credential` and read `OPEN_AI_API`.
  - The leg passes `secrets.OPEN_AI_API`.
  - A step before the legs refuses a run where the secret is empty, as omp's *The credential scenarios can run* does,
    so a missing secret fails loud and never skips them quietly.
- **`build.yml`'s `paths-ignore`** keeps `.github/**`, so changing the workflow alone runs nothing. A pin change edits
  `pom.xml`, which is not ignored.
- **Red first:** the first run is at the pin as it stands with [CX11](README.md#cx11)'s trust hash deliberately wrong, and the
  subagent scenario must fail on a leg. Then the hash is put back.
- **Not in this issue:** an update job that proposes a new Codex CLI version behind the legs, and releases, stay with
  [CX06](README.md#cx06).

## Acceptance

- A push to `main` that changes `agent.cli.version` runs both legs against that push's packages, and the Build is
  green only if both pass.
- A Build with the hook's trust hash broken goes red on the subagent scenario, seen once on GitHub.
- A Build without `secrets.OPEN_AI_API` stops before it rents.

## Open questions

- None. The key is the organization's secret `OPEN_AI_API`, already allowed for `porelo-codex`.

## To be checked

- Whether the `build` job's `packages` upload already holds everything a leg installs (the agent package and its
  code-mode helper).
- How long one leg takes with the code-mode helper, measured on the first run; the other adapters' legs take 10 to 13
  minutes.
