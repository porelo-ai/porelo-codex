# CX24 — Codex Is Installed Root-Owned

```json
{
  "$schema": "issue-1.json",
  "id": "CX24",
  "title": "Codex Is Installed Root-Owned",
  "state": "Backlog",
  "size": "M",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the codex binary a task starts and its code-mode helper are installed by root and owned by root, so the agent in a task cannot replace what Porelo starts as Codex next time; their version stays pinned by the definition and moves only through the update job, and a self-update inside a task fails"
}
```

**Why this size.** M: the install moves from the agent's phase to the root phase of the image, which Porelo's agent
contract does not offer for a verified download today, so it takes one contract change agreed with Porelo; then the
definition, its tests and one check at the pin.

**Estimate.** M: 53 min, the measured median of M over all repositories (p80 114).

**What must be true.** Whatever the agent writes during a task, the next start of Codex runs the pinned binaries
Porelo built into the image.

- `codex` and `codex-code-mode-host` go to `/usr/local/bin/`, mode 0755, owned by `root:root`; nothing named `codex` or
  `codex-code-mode-host` stays under `/home/agent`, and Codex finds its helper beside its own binary as before.
- The version is pinned in `pom.xml` (`agent.cli.version`, `agent.cli.sha256`, `agent.helper.sha256`), each archive
  checked before it is unpacked; it moves only through the update job, outside any task.
- A self-update tried inside a task fails to write over either binary, by design.

## Plan

- Today both archives are `install.artifacts` entries fetched into a cache directory in the agent's home
  and unpacked by an `as_agent` step with `install -D` into the home's bin directory. Porelo renders every artifact after `USER agent`,
  so the agent owns all of it.
- Preferred: Porelo's agent contract lets an artifact be fetched as root, rendered with the same digest check before
  `USER agent`; the archives go to a root-only staging path and the unpacking moves to `as_root`, keeping its checks of
  each archive's one member. The pinned artifacts stay what `porelo agents --supply-chain` and `PinAgreementTest` read.
  Without that change the definition would write its own verified fetch in `as_root` and lose that record, so the
  contract change comes first.
- Tests, unit level: the definition's install puts both binaries under `/usr/local/bin`, owned by root, with no line
  that writes them as the agent (red first against today's definition); `PinAgreementTest` still holds the pin.
- At the pin, in a task: Codex starts from `/usr/local/bin/codex` and reaches its helper; a write to either as the
  agent fails.

## Acceptance

- In a built image, `stat` of both binaries shows `root`, and the agent's user cannot write them or their directory.
- An update tried in a task as the agent fails, and the next start runs the pinned version.

## To be checked

- The contract change for a root-owned artifact, agreed with Porelo's agent in the channel.
- How Codex finds `codex-code-mode-host` once both are outside the home, measured at the pin.
