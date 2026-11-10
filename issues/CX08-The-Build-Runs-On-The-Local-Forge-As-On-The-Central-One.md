# CX08 — The Build Runs On The Local Forge As On The Central One

```json
{
  "$schema": "issue-1.json",
  "id": "CX08",
  "title": "The Build Runs On The Local Forge As On The Central One",
  "state": "Review",
  "size": "S",
  "priority": "Medium",
  "rank": 3,
  "blockedBy": [],
  "covers": "the workflows run 1:1 on the local Forgejo and the central forge, before the move"
}
```

**Why this size.** `settings.xml`, the four workflows, the `pinned-jdk` action, the two workflow tests, `SnapshotTokenTest` and one
recorded decision, no legs and no contract another repository reads; about 2 hours of work once porelo-claude-code has set the shape, plus the runs on a local Forgejo.

**How it stands.** On a local Forgejo the Build runs green; on the central forge it waits for its push, and what only a host shows is named below.

**What must be true.** `porelo-codex`'s workflows run green on the local Forgejo and on the central forge on the same
commit, with no edit between them: the registry and the place it publishes to are variables of the setup. And a Build on a forge other than the central one whose `MAVEN_SNAPSHOTS_URL` is
empty fails at its start and says so, instead of falling back to Central's snapshots.

Before [CX04](README.md#cx04); the variables are those of porelo-host's `doc/local-forge.md`, *The local registry and publication settings*.

## Plan

Read in today's `.github/workflows/`, `.github/actions/pinned-jdk`, `settings.xml`, `pom.xml`, the workflow tests and
`doc/decisions.md`. The Build rents no machine yet (its legs are [CX06](README.md#cx06)), so this is the smallest of
the adapters' changes.

- **What ties the Build to GitHub today:**
  - `settings.xml` reads snapshots from Sonatype only, through the `standard` profile, active unless
    `-Dporelo.release`. From there come `porelo-bom`, `porelo-agent-api` and `porelo-wire` (`porelo.version`
    0.5.1-SNAPSHOT), and `porelo-parent`, `porelo-release` and `porelo-machines`; `porelo-acceptance-kit` sits in the
    `acceptance` profile, which no workflow starts until [CX06](README.md#cx06).
  - On a push to `main`, *Publish the packages* takes `vars.JF_URL` and `secrets.JF_ACCESS_TOKEN`, *The package is
    indexed, not merely stored* takes `vars.JF_URL`, and both stop on an empty `JF_URL`.
  - What needs only step 1's Maven values: `shared-rules.yml` already takes a pull request from the host's forge as
    from inside (`github.server_url`), `workflows.yml` checks the action pins and that every workflow loads, and
    `build.yml` and `update.yml` run `check-pin` for the `codex` and the `helper` archive, both assets of one release
    on `api.github.com`; nothing opens a pull request.
  - What guards today's shape: `WorkflowRulesTest` holds that the publish step alone receives a secret and that its
    `if` is exactly a push to `main`, `PublishStepsTest` runs *Not publishing*'s script, and `doc/decisions.md` says
    "The publish step alone receives a secret, the publish token."
- **Where Porelo's snapshots come from on the local forge:** `porelo`'s Build on the host's forge deploys them into its
  registry.
- **Steps:**
  1. `settings.xml` gains the `forge` profile and the `porelo-forge` server as porelo-message-sluice has them, and
     `standard` stays off where `MAVEN_SNAPSHOTS_URL` is set. Every Maven call passes `MAVEN_SNAPSHOTS_URL` and
     `PORELO_PACKAGES_READ_TOKEN` through the step's `env`, the `pinned-jdk` action's too, in all four workflows.
  2. `vars.PORELO_PUBLISH`, checked at the start to be `true` or `false`, switches Publish and its index check; where
     it is `false`, *Not publishing* runs and says why, and the packages stay attached to the run. *Only a snapshot is
     published* stays on every push to `main`.
  3. `WorkflowRulesTest` learns the second secret, `PORELO_PACKAGES_READ_TOKEN`, on the Maven steps, and the publish
     step's `if` with `vars.PORELO_PUBLISH`; `PublishStepsTest` runs *Not publishing* with `PORELO_PUBLISH=false`;
     the decision in `doc/decisions.md` names both secrets.
  4. `porelo-release check-providers` joins the Build's checks; it finds nothing today, and keeps
     [CX06](README.md#cx06)'s legs asking by kind.
  5. The `pinned-jdk` action, the first thing every job that runs Maven does, starts with *The forge names its
     snapshot repository*, as in porelo-claude-code: outside `https://github.com` an empty, white-space-only or unset
     `MAVEN_SNAPSHOTS_URL` stops the job and names the variable, since without it `settings.xml` falls back to
     Central's snapshots without a word; on the central forge it changes nothing. It is the action's first step and
     has no `if:`. `SnapshotTokenTest` comes along as that repository has it, self-contained, and holds this check
     and that no step names the publisher's token.
- **Red first:** today's `build.yml` on the host's Forgejo, which stops at the snapshot resolution or at Publish; and
  the changed `WorkflowRulesTest` against today's workflows;
  and `SnapshotTokenTest` on today's tree, which has no check in the action and the read-only token in no workflow.

## Acceptance

- Seen to fail first: today's `build.yml` on the local Forgejo.
- The same commit's Build green on the local Forgejo (nothing published) and on GitHub (published as today).
- A Build on the local forge with `MAVEN_SNAPSHOTS_URL` unset is seen to fail at its first step, naming the variable;
  on GitHub the Build is unchanged.

## Measured on a local Forgejo

Forgejo 17 and its runner 13.2.0, pinned as the host's forge pins them, with the host's job image `act-24.04` as root and
a JDK mounted as `JAVA_HOME_25_X64`; the registry held the snapshots this Build resolves, copied in from a Maven cache.

- **Red first:** `SnapshotTokenTest` and the changed `WorkflowRulesTest` on the tree before; the same forge stopped porelo-claude-code's
  Build at Maven's http blocker on a `MAVEN_SNAPSHOTS_URL` of `http://<host>.localhost`, at a denied read that root reads
  through, at the `rpm` the image lacks and at `actions/upload-artifact`, which refuses every server but GitHub's. This
  Build takes the same answers.
- **Beyond the plan:** the packages are uploaded only where `vars.PORELO_PUBLISH` is `true`; *The tools the packages are
  built and read with* installs only what is missing; and each `check-pin` step blanks `GITHUB_TOKEN`, since a forge's
  runner puts its own token there for every step and the check would hand it to GitHub.
- **Green:** the Build, nothing published and every snapshot read from the forge's registry.

- **The reading against `security-audit`, `clean-code`, `java-code-review` and `test-quality` (2026.906.3)** declined three
  findings: that the read-only token's place on each Maven step is not guarded, since a step without it fails on the
  forge with a `401` and gives no write token away; that *Whether this forge publishes* and the index step's condition
  are not guarded, since removing either is a change to the workflow read as such, and a missing variable stops the
  Build today; and that a workflow's `MAVEN_SNAPSHOTS_URL` binding is not guarded, since on a forge the action's first
  step stops a job without it, and on the central forge the fallback reads the same repository.

## What only a host shows

- The registry through the host's proxy and gate, with `MAVEN_SNAPSHOTS_URL` as the host sets it, and the runner's jobs in
  their own containers rather than in the runner's.
- `porelo`'s, `porelo-parent`'s and `porelo-buildtools`' snapshots deployed there by their own Builds, not copied.

## To be checked

- That this repository may read `vars.PORELO_PUBLISH`, `vars.MAVEN_SNAPSHOTS_URL` and `secrets.PORELO_PACKAGES_READ_TOKEN`
  on the central forge: the organization's access lists are separate, and only the operator can read them.
