# CX08 — The Build Runs On The Local Forge As On The Central One

**Status:** Analyzed; blocked by porelo [B306](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b306),
Porelo's snapshots in the forge's registry, and porelo-buildtools
[BT25](https://github.com/porelo-ai/porelo-buildtools/blob/main/issues/README.md#bt25) for `check-providers`.
**Size:** S - `settings.xml`, the four workflows, the `pinned-jdk` action, the two workflow tests and one recorded
decision, no legs and no contract another repository reads; about 2 hours of work once porelo-claude-code
[CC38](https://github.com/porelo-ai/porelo-claude-code/blob/main/issues/README.md#cc38) has set the shape, plus the runs
on the host's forge.

**What must be true.** `porelo-codex`'s workflows run green on the local Forgejo and on the central forge on the same
commit, with no edit between them: the registry and the place it publishes to are variables of the setup.

From porelo-project [PJ60](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj60), for the move's
wave 7b, before [CX04](README.md#cx04).

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
  registry (porelo [B306](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b306)).
- **Steps:**
  1. `settings.xml` gains the `forge` profile and the `porelo-forge` server as porelo-message-sluice has them, and
     `standard` stays off where `MAVEN_SNAPSHOTS_URL` is set. Every Maven call passes `MAVEN_SNAPSHOTS_URL` and
     `PORELO_PUBLISHER_TOKEN` through the step's `env`, the `pinned-jdk` action's too, in all four workflows.
  2. `vars.PORELO_PUBLISH`, checked at the start to be `true` or `false`, switches Publish and its index check; where
     it is `false`, *Not publishing* runs and says why, and the packages stay attached to the run. *Only a snapshot is
     published* stays on every push to `main`.
  3. `WorkflowRulesTest` learns the second secret, `PORELO_PUBLISHER_TOKEN`, on the Maven steps, and the publish
     step's `if` with `vars.PORELO_PUBLISH`; `PublishStepsTest` runs *Not publishing* with `PORELO_PUBLISH=false`;
     the decision in `doc/decisions.md` names both secrets.
  4. `porelo-release check-providers` joins the Build's checks; it finds nothing today, and keeps
     [CX06](README.md#cx06)'s legs asking by kind.
- **Red first:** today's `build.yml` on the host's Forgejo, which stops at the snapshot resolution or at Publish; and
  the changed `WorkflowRulesTest` against today's workflows.

## Acceptance

- Seen to fail first: today's `build.yml` on the local Forgejo.
- The same commit's Build green on the local Forgejo (nothing published) and on GitHub (published as today).

## To be checked

- That the host's Forgejo runner serves `actions/checkout` v7, `actions/upload-artifact` v7 and `actions/cache` v6,
  and that the `pinned-jdk` action works there.
- That the runner reaches GitHub where the checks still read it: `check-citations` fetches the other repositories'
  indexes from `raw.githubusercontent.com`, so a cited issue pushed only to the host's forge fails it, and
  `check-pin` asks `api.github.com` without a token, inside the limit of 60 requests an hour per address.
- That the runner has `python3` with `yaml` for `workflows.yml`'s load check.
