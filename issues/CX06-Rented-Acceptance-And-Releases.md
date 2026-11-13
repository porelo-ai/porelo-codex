# CX06 — Rented Acceptance and Releases

```json
{
  "$schema": "issue-1.json",
  "id": "CX06",
  "title": "Rented Acceptance and Releases",
  "state": "Estimated",
  "size": "XL",
  "priority": "Low",
  "rank": 1,
  "blockedBy": [],
  "covers": "rented acceptance legs, releases and CLI updates, as in the other adapters"
}
```

**Why this size.** Read against `porelo-claude-code`'s and `porelo-pi`'s workflows, three separate pieces are missing
here, each a workflow job with its guards: the rented legs with their `proof` job (already planned as
[CX14](README.md#cx14), M), the tag release (M) and the update job that follows Codex CLI (M). Together they are
several days of work on one repository with three separate green runs to see, so this is too big to build as one.
It splits into [CX14](README.md#cx14) and two new issues, below.

**Estimate.** XL: not estimated. It splits into three issues of M: 3 x 53 min, the measured median of M over all
repositories (n=113).

**What must be true.** Like the other adapters, a push to main and a release tag run the
acceptance on rented ubuntu and fedora machines against the published package, a tag publishes a
release only after that acceptance passed, and the update job proposes new Codex CLI versions
behind the same gate.

## What codex lacks, read in the workflows

Compared with `porelo-claude-code` and `porelo-pi` (and `porelo-omp`, which has the same jobs):

- **`build.yml` has one job, `build`.** The others have four: `build`, `proof`, `acceptance` and `release`.
  - No `proof` job (`porelo-release proven-legs`, which takes legs an earlier run proved on the same tree) and no
    `acceptance` matrix (`porelo-machines acceptance --os ubuntu|fedora --kind amd64 --cucumber .`, then
    `sweep --mine`). This is [CX14](README.md#cx14).
  - No `tags: ['v*']` trigger, and `MAVEN_ARGS` is a plain `-U`, not the `-P!standard,!forge,!upstream` a tag
    builds with, from releases only.
  - The step *Only a snapshot is published* refuses any version but a snapshot; there is no *Which channel* step
    (tag `v<version>` equal to the pom's version, neither it nor `porelo.version` a snapshot), no
    *A release depends only on releases* (`check-releases` on the effective pom), and the publish and index steps
    write `snapshots` fixed, with no check that a release is not published already.
  - No `release` job that `needs: [build, acceptance]`, downloads the `packages` artifact and publishes to the
    `releases` channel only after both legs passed.
- **`update.yml` only checks the two digests weekly.** The others' *Agent version update* follows upstream:
  `upstream-version` with the pom's `porelo.release.min-age`, `update <version>` to write the pin, unit suite,
  `check-pin`, native packages, `compare-bills` against the published bill, a stop on a major move, the tree
  artifact `porelo-verified-tree-<tree>` the Build's `proof` reads, the key check, two acceptance legs against
  the candidate packages with their sweeps, a pull request on `update/<agent>-<version>`, a merge behind
  `vars.PORELO_UPDATE_AUTO_MERGE` and `secrets.PORELO_UPDATE_TOKEN`, and a red *Stop* step. The job takes
  `contents: write` and `pull-requests: write`.
- **The pom lacks the follow settings.** It has the named pins `porelo.release.pin.codex.*` and
  `porelo.release.pin.helper.*` (both on `agent.cli.version`, read from GitHub's releases of `openai/codex`, tag
  prefix `rust-v`), but no `porelo.release.agent` label and no `porelo.release.min-age`. Porelo's release tool
  already takes `--pin NAME` in `upstream-version` and `update`, and knows `github-releases` and `github-asset`.
- **The tests forbid all of it today.** `WorkflowRulesTest` holds exactly four workflows, no
  `machines.Main acceptance`, no `contents: write` or `pull-requests: write`, and `JF_ACCESS_TOKEN` as the only
  secret besides the read token. `PublishStepsTest` runs *Only a snapshot is published* and *Publish the packages*
  with `snapshots` fixed. Each change below turns one of these red first.
- **`doc/decisions.md`, *Build boundary*,** says CI uses no live credentials or rented machines and there is no tag
  release yet; it changes with each piece.
- **Outside this issue's scope**, also missing: the manual workflows `artifactory-smoke.yml` (is the publish token
  able to write) and `delete-runs.yml` (old run history). Each is a copy, XS, and would be an issue of its own if
  wanted.

## Plan

Built as three issues, in this order:

1. **[CX14](README.md#cx14), the acceptance legs in the Build (M, exists).** The `proof` and `acceptance` jobs as in
   the other adapters, `secrets.OPEN_AI_API` checked before anything is rented. Its `proven-legs --jobs` list
   gains the update job's name once piece 3 exists. A leg gets the run's packages from the `packages` artifact,
   which `build` uploads only where `vars.PORELO_PUBLISH` is `true`; that is enough, since the legs and the
   `release` job run only on GitHub, where it is `true`, and a forge skips them.
2. **Releases From A Tag (M, new, blocked by [CX14](README.md#cx14)).** `build.yml`: the `v*` tag trigger, the tag's
   `MAVEN_ARGS`, *Which channel* in place of *Only a snapshot is published*, *A release depends only on releases*,
   the publish and index steps on `${CHANNEL}` with the refusal of a release already published, and the `release`
   job after `acceptance`. Red first: `PublishStepsTest` cases for a tag that is not `v<version>`, a snapshot
   version on a tag, a snapshot `porelo.version` on a tag, and a release already in the index; the snapshot
   cases keep passing. `doc/decisions.md` *Build boundary* and a short release how-to in the documentation follow.
   Unit level; one tag run on GitHub is the acceptance, and it needs a released Porelo.
3. **The Update Job Proposes New Codex CLI Versions (M, new, blocked by [CX14](README.md#cx14)).** `update.yml` rewritten after
   `porelo-claude-code`'s *Follow Claude Code*, with `upstream-version --pin codex`, then `update --pin codex` and
   `update --pin helper` for the same version, both `check-pin`s, the legs with `OPEN_AI_API` and
   `PORELO_CI_LEG: upd-<os>`, and the pull request and merge as there. The pom gains `porelo.release.agent`
   (`Codex CLI`) and `porelo.release.min-age` (`3d`). `WorkflowRulesTest` changes so that only the update job
   may write, and only the Build's legs and the update's legs read `OPEN_AI_API` and the machine secrets; that
   test goes red first. A dispatch with an explicit version is the acceptance, and a pull request opened by it.
   [CX09](README.md#cx09)'s sign-in check is a step of this job, so [CX09](README.md#cx09) waits on this piece, not on all of [CX06](README.md#cx06).

## Acceptance

- [CX14](README.md#cx14)'s acceptance holds.
- A tag `v<version>` on GitHub runs both legs against the run's own packages and publishes to `releases` only after
  them; a tag that does not match the version, or a release already published, stops before any upload.
- A dispatched update to a newer Codex CLI version opens a pull request that moves `agent.cli.version` and both
  digests, after both legs passed against the candidate packages; a failed leg leaves the request open, says why,
  and turns the run red.

## What makes it uncertain

- Two pins share one version property. Whether `update --pin helper` after `update --pin codex` rewrites only the
  helper's digest is not yet run; if the release tool needs a change, piece 3 waits on porelo-buildtools and grows
  to L.
- Codex CLI releases often (several a week). With a 3-day minimum age the job proposes at most once a week, but each
  run rents two machines for up to an hour.
- The first release needs a released Porelo (`porelo.version` not a snapshot); until then piece 2 can only be seen
  refusing a tag.

## To be checked

- Whether `update --pin codex <v>` followed by `update --pin helper <v>` writes the version once and both digests,
  with a `--dry-run` against the current pom.
- Which components of the bill move with a CLI update, for `compare-bills --expect-moved`.
- The update job's time limit, from the legs' times measured in [CX14](README.md#cx14)'s first run.
