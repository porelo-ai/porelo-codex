# CX30 — The Unattended View's Remaining Gaps

```json
{
  "$schema": "issue-1.json",
  "id": "CX30",
  "title": "The Unattended View's Remaining Gaps",
  "state": "Backlog",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "what [CX29](README.md#cx29) left open in the unattended view: a failed command's error text, a file change said as [edit] <path>, the item types of a run with a file change, and line breaks in worded lines"
}
```

**Why this size.** One class, `CodexJsonFormatter`, and its tests, plus one recorded run with a file change on the
test VM.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** Nothing [CX29](README.md#cx29) left open is lost: each point below is fixed, or shown by a test or a recording
not to happen.

## Why

Kept from [CX29](README.md#cx29) (*The Unattended View Shows No Raw Event Lines*, Finished 2026-11-15) when it was deleted: the points
the reading by another model raised and [CX29](README.md#cx29) noted but did not fix, and the part of its point to be checked that its
recording did not answer.

## Plan

- **A failed command's reason.** A command item that ends `failed` with an `error` shows only `[command] ... (exit
  null)`; its error text is lost. Show it on the same line.
- **A file change.** [CX29](README.md#cx29)'s plan said `[edit] <path>` for a `file_change` item; it is left out today. The 0.163.0
  recording held no file change, so its fields are to be read from a run with one on the test VM before it is worded.
- **The item types of 0.163.0.** Beyond `agent_message`, `command_execution` and `error`, which item types an unattended
  `codex exec --json` writes (reasoning, todo list, MCP call, web search), read from that run.
- **Line breaks.** An agent message, a command's line and the turn's `-- error:` keep their line breaks, so they can
  hold a line that reads like `[error] ...`. Decide with the coordinator whether a person's view folds them.
- **Level:** unit tests of the formatter; the recording through the coordinator's VM slot.

## Acceptance

- Each point above is fixed with a test seen red first, or recorded as not happening with its evidence.
