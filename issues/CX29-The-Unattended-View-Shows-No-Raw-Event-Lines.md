# CX29 — The Unattended View Shows No Raw Event Lines

```json
{
  "$schema": "issue-1.json",
  "id": "CX29",
  "title": "The Unattended View Shows No Raw Event Lines",
  "state": "Backlog",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "the unattended view of a Codex task shows what the agent says and does, never a raw JSON event line: an unknown event dropped unless it names an error, read in the code beside the same finding for Claude Code"
}
```

**Why this size.** One class, the formatter, and its tests; no contract, since its output is read only by a person.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person reading an unattended Codex task in the window sees the agent's messages, its commands and its end; no line of raw JSON.

## Plan

- **What is wrong today, read in the code, not yet seen.** `CodexJsonFormatter.format` drops `thread.started` and `turn.started` and words an agent message, a finished command and the turn's end; every other event - `item.started`, an `item.completed` of another item type (reasoning, a file change, a to-do list), anything new - is returned as raw JSON.
- **The new rule.** Each event the pinned CLI writes is worded when the person can use it (a file change as `[edit] <path>`, a command started is not needed since its end is worded) or dropped; an unknown event is dropped unless it carries an error, which shows as `[error] <message>`.
- **What still shows, by design.** A line that says something went wrong - an error or a warning, a failed run, a refused request - is never dropped: it shows in words, with its message cut at 300 code points as today. A line that is not JSON at all still shows as it is, without control characters.
- **Tests.** The harm guarded is an error hidden from the person: the existing tests that an error, a failed run and a non-JSON line still show stay, and one per new rule shows an unknown event dropped while an unknown event that names an error still shows, each seen red first. The words of a worded event are not held verbatim by a test.

## Acceptance

- A recorded unattended run of the pinned CLI, with a command and a file change, rendered through the formatter, holds no line that begins with `{`.
- An error event of an unknown type still shows, and its test fails when the error rule is taken out.

## To be checked

- **The events 0.163.0 writes.** Which types and item types an unattended `codex exec --json` run writes, read from a run on a VM, values replaced, before the rule names them.
