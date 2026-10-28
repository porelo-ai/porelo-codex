# CX05 — Snapshot Package

**Status:** Implementing.
**Size:** M - one workflow change with its helper, three guards run against stubs, and a decision changed.

**What must be true.** Every push to main publishes `porelo-agent-codex` to the snapshot channel, so a
machine installs it with `apt` or `dnf` like the other agents, and an operator test of the ChatGPT
sign-in can start from the published package.

## Plan

- `.github/workflows/build.yml`: after the packages are built, a push to main refuses a release
  version, uploads the `.deb`, the `.rpm` and the bill to the snapshot channel through
  `.github/scripts/artifactory.sh` (byte for byte the adapters' copy), and believes the upload only
  once the snapshot index lists the exact `.deb`. Every other run says that it publishes nothing.
- `PublishStepsTest` runs those steps against a stub `curl`; `PublishTokenTest` holds that the token
  reaches `curl` only on standard input; `WorkflowRulesTest` allows exactly one secret, the publish
  token, in the publish step. Each went red before the steps existed and again when its guarded
  behavior was broken on purpose.
- `doc/decisions.md`, *Build boundary*: CI publishes snapshots, but runs no rented acceptance leg,
  no release and no automated update; those follow in
  [CX06](README.md#cx06).

## Acceptance

- The Build of the pushed commit passes every job, and its publish step names the uploaded files.
- The snapshot index lists `porelo-agent-codex` with the run's version, read after the Build.

## To be checked

- After the push, read the snapshot index for the version and post it in the channel.
