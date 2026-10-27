# CX03 — Forge Pull Requests

**Status:** Implementing; adapter part of the approved shared template change.
**Size:** S - one workflow condition and an executable regression guard, with the full build checks.

**What must be true.** A pull request from this repository's own branch on the host's forge is
checked as internal even when the forge supplies no author association. Contributions from other
repositories still require their authors' DCO sign-off; GitHub keeps its existing rule.

## Plan

- Carry the exact template from porelo-buildtools
  [BT36](https://github.com/porelo-ai/porelo-buildtools/blob/main/issues/README.md#bt36) into
  `.github/workflows/shared-rules.yml`: bind `PR_SERVER` from `github.server_url` and clear
  `--outside` for a non-GitHub server only inside the same-repository condition.
- Add `WorkflowPullRequestTest`, using the existing `WorkflowSteps` helper to run the actual
  workflow script with a stand-in release command. First observe the missing server binding
  and the forge's own branch fail against the unchanged workflow.
- Exercise the fork case with the forge condition deliberately moved outside the repository
  boundary, then restore it. Preserve GitHub's member, owner, own-bot and contributor behavior,
  including forks, and propagate a refused commit check.
- Run the workflow's host verification, native packages and shared checks with the pinned JDK;
  obtain a read-only review against the repository's skills. The Coordinator handles the push.

## Acceptance

- The event supplies the server and identity through environment bindings.
- A same-repository forge pull request without an association checks the exact commit range
  without `--outside`; a forge fork retains it.
- GitHub contributors and forks retain the DCO requirement, and members, owners and the own bot
  keep the existing internal treatment. A failed release check fails the workflow step.
- The first two regressions are red before the fix; a broadened forge exemption is red too.
- The complete applicable workflow and document checks pass; no live credential or rented
  machine is required. Nothing ships differently, so there is no changelog entry.

## Verification

- The executable guard runs ten cases. Against the unchanged workflow, two fail: the missing
  `PR_SERVER` binding and the own forge branch incorrectly passed with `--outside`. With the
  approved template change, all ten pass without skips.
- Moving the forge exemption outside the same-repository condition makes the forge fork case
  fail: the release command receives no `--outside` where it is required. The correct condition
  is restored after that measurement.
- Host verification passes with the digest-checked workflow JDK: 79 unit tests, four native
  linkage/protocol checks and four package/bill checks, with no skipped tests. Debian and RPM
  packages are built online; both publisher digests and the five shared repository checks pass.
  Every workflow loads with triggers and jobs. No VM, rented machine or live provider test runs.
- Independent read-only review of the workflow, executable guard and issue passes with no findings.
