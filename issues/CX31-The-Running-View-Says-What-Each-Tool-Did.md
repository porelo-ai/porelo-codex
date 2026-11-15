# CX31 — The Running View Says What Each Tool Did

```json
{
  "$schema": "issue-1.json",
  "id": "CX31",
  "title": "The Running View Says What Each Tool Did",
  "state": "Review",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the running view of an unattended Codex task says every tool Codex used - a file change by its paths, an MCP call by its server and tool, a web search by its query - besides a command, whose line is cut to its first line, with its failure"
}
```

**Why this size.** One class, `CodexJsonFormatter`, and its tests, plus one recorded run with a file change, an MCP call and a web search through the coordinator's VM slot.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person reading an unattended Codex task sees every tool Codex used, each on one line with what it was used on and whether it failed.

## Why

Part of ST369, *Findings of the second live test on a rented host* (High). In the operator's live test (porelo
snapshot.553, porelo-agent-claude snapshot.186, the build with the raw-lines fix) the running view of an unattended Claude Code task
showed almost nothing of the work: its lines were only `[Bash]`, `> [tool_result]`, `[thinking]` and once `[git] push`,
over and over. The operator asked whether it is normal to see nothing of the tools. It is not: a reader must see what
each tool did, in short.

Checked in this repository for the same gap: `CodexJsonFormatter` words a command as `[command] <command> (exit N)`, so
commands are seen, but a `file_change`, an `mcp_tool_call`, a `web_search` and a todo list are left out since
[CX29](README.md#cx29), and a command's text keeps all its lines. Takes over [CX30](README.md#cx30)'s point of a file
change said as `[edit] <path>` and the recording it needs.

## Plan

- **A file change** as `[edit] <path>` (more paths counted), from a recorded run's fields.
- **An MCP call** as `[mcp] <server>/<tool>`, **a web search** as `[search] <query>`, a todo list by its number of items.
- **A command** with its first line only, cut, and its failure: a failed status or a non-zero exit, with the error's
  first line when there is one (the failed command's reason from [CX30](README.md#cx30)).
- **Tests:** each rule seen red first; no test holds a worded line verbatim.
- **Level:** unit tests of the formatter; the recording through the coordinator's VM slot.

## Acceptance

- Rendered through the formatter, a recorded run with a file change, an MCP call or a web search shows each on one line
  with what it was used on.
- A command shows only its first line, and a failed one says so.

## What was built

- `CodexJsonFormatter` words a completed tool item on one line with what it was used on, folded and cut, never its
  output or what it wrote: `[edit] <first path>` with `and N more` for the other paths, `[mcp] <server>/<tool>`
  without its arguments, `[search] <query>`, `[todo] <n> items`, and `[command] <first line>` with `…` when the
  command has more lines and `(exit N)` when it has an exit code (no more `(exit null)`).
- A tool item that failed - a status of failure, or a command's non-zero exit - says `failed`, with the first line of
  its error, or for a command the first line of its output that names an error (error, fatal, fail, not found, no
  such, denied, cannot, refused, exception, abort, invalid, unknown) and else its last line.
- The error rule is unchanged: an error item, and an error or warning in any other event or in a started or updated
  item, is shown as `[error]` or `[warning]` as before.
- The shape built against is `changes: [{path, kind}]` and `status` for a file change, `server`, `tool`, `status`
  and `error.message` for an MCP call, `query` for a web search and `items` for a todo list. Of these, only a file
  change is measured on a recorded run: `src/test/resources/tool-events/edit.jsonl`, recorded for the tool events,
  holds one, and `failed.jsonl` beside it a command that exited 127; both are rendered in the tests. An MCP call, a
  web search and a todo list are not measured on a recorded run and are shown by single lines.
- Tests, each seen red before the code and again when its rule was broken on purpose (the failure mark, the non-zero
  exit, the first line, the error's line, the count of paths, the MCP case, the fold of a query, the count of items):
  `aRecordedCommandShowsOnlyItsFirstLine`, `aRecordedFailedCommandSaysSoWithItsError`,
  `aRecordedFileChangeShowsItsPath`, `aFileChangeShowsItsFirstPathAndCountsTheOthers`,
  `anMcpCallShowsItsServerAndTool`, `aWebSearchShowsItsQuery`, `aTodoListShowsItsNumberOfItems`,
  `aCommandShowsItsFirstLineAndItsFailure` and `aToolsFieldCannotOpenALineOfItsOwn`.
- Open: the plan's recording of a run with a file change, an MCP call and a web search through the coordinator's VM
  slot is not done here.
