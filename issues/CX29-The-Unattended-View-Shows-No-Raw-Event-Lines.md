# CX29 — The Unattended View Shows No Raw Event Lines

```json
{
  "$schema": "issue-1.json",
  "id": "CX29",
  "title": "The Unattended View Shows No Raw Event Lines",
  "state": "Review",
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

## What was built

- `CodexJsonFormatter` keeps its words for an agent message, a finished command and the turn's end; every other event,
  `item.started` and items it does not word among them, is left out instead of shown as it came.
- An event or an item of any phase that reports an error or a warning is always shown, as `[error]` or `[warning]` with
  its message on one line: by its type (error, fail, fatal, exception, warn), its level, a status of failure (failed,
  error, killed, rejected, denied, aborted, in any case), `is_error` or `isError`, or any error it carries; a lifecycle
  event that carries one too. A line break, a Unicode separator or an invisible format mark in it is folded away.
- Fixture: an unattended run recorded with the pinned 0.163.0 in a task on a VM, which holds the `error` item 0.163.0
  writes for a model it does not know, `src/test/resources/streams/unattended-background-run-0.163.0.jsonl`.
- Tests in `CodexJsonFormatterTest`, each seen red first; the error rule, the status rule and the fold each broken on
  purpose and seen red.
- The reading by another model found errors still dropped - in `turn.started`, in an `item.started` or `item.updated`,
  by a status or flag of another spelling, by a type such as `stream_failure` - and a line break in an error; all fixed.
  Noted, not fixed: an agent message, a command's line and the turn's error keep their line breaks, a failed command
  shows its exit but not its error text, and a file change is not worded as `[edit] <path>` (no file change was
  recorded here) - all as before.
