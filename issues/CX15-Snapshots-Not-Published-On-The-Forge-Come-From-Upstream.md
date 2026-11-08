# CX15 — Snapshots Not Published On The Forge Come From Upstream

**Status:** Review.
**Size:** S - the change `porelo-claude-code`, `porelo-pi` and `porelo-omp` carry since 2026-11-08, carried over.

**What must be true.** On a forge whose registry holds only its organization's packages, the other Porelo snapshots
come from the anonymous repository `MAVEN_UPSTREAM_SNAPSHOTS_URL` names, and the forge's token goes to the forge's
registry alone.

## Plan

- `settings.xml` gains the `upstream` profile, activated by `MAVEN_UPSTREAM_SNAPSHOTS_URL`, its repository id
  `porelo-upstream-snapshots` without a `<server>`.
- Every workflow passes `MAVEN_UPSTREAM_SNAPSHOTS_URL: ${{ vars.MAVEN_UPSTREAM_SNAPSHOTS_URL }}` beside
  `MAVEN_SNAPSHOTS_URL`, and the `pinned-jdk` action's comment names both.
- `UpstreamSnapshotsTest`, the other adapters' copy, guards the token.
- Nothing builds a tag here, so the test reads every workflow as YAML and holds that none runs on a tag: no tag
  filter, no `release` or `create` event, and no `push` without a branch filter, which GitHub and Forgejo run on
  every tag. The tag guard of the other adapters comes with [CX06](README.md#cx06).

## Acceptance

- `UpstreamSnapshotsTest` passes, and goes red when the upstream repository takes the forge's id, when the profile's id
  changes, when its URL names the forge's, and when a tag would read snapshots.
- From an empty local repository, with `MAVEN_SNAPSHOTS_URL` naming a repository without Porelo's snapshots, the build
  resolves with `MAVEN_UPSTREAM_SNAPSHOTS_URL` set and fails without it.

## Measured

- The guards, each broken on purpose and restored: red in every case, among them `build.yml` with its branch filter
  removed, a new `.yaml` workflow, and each kind of tag trigger in the fixtures.
- Read by agy `gemini-4-argon-high`: its two findings, that the first guard read `build.yml` alone, with a pattern
  that missed an unfiltered `push`, a quoted key and a release, and that its failure paths had no fixtures, are
  built in.
- `dependency:resolve` from an empty local repository, `MAVEN_SNAPSHOTS_URL` naming Maven Central: with
  `MAVEN_UPSTREAM_SNAPSHOTS_URL` naming Central's snapshots it passed, 11 files from `porelo-upstream-snapshots`;
  without it, and with `-P!standard,!forge,!upstream`, it failed.

## To be checked

- The forge's Build green after the move, with the organization's `MAVEN_UPSTREAM_SNAPSHOTS_URL`.
