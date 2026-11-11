# CX19 — The Adapter Reports Each Tool Call With Its Time

```json
{
  "$schema": "issue-1.json",
  "id": "CX19",
  "title": "The Adapter Reports Each Tool Call With Its Time",
  "state": "Analyzed",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the adapter reads each tool call from its agent's output and hands Porelo its tool, kind, start, end, the program a shell call started and whether it ran in the background, in Porelo's common entry",
  "openQuestions": 2
}
```

**Why this size.** one reader over the item events the formatter already reads, a type table and its tests. It builds against `ToolEvents`, which Porelo's agent API adds in its contract version 5;
nothing of that format is decided here.

**What must be true.** For every tool call the agent makes in a task, Porelo records one entry in its common
tool-call format: the task, the tool as the agent names it, its kind (shell, edit, read, search, web, sub-agent, mcp
or other), when it started and when it ended, for a shell call the program it started, and whether it ran in the
background; a background job's start and end are joined by its id. The adapter only maps its agent's own output into
that entry; the format, the time a line without its own time gets, and where the entry is kept are Porelo's.

## Plan

- Source: `codex exec --json`. `item.started` and `item.completed`, joined by the item's `id`; `CodexJsonFormatter` reads `command_execution` today.
- `CodexAgent` overrides `Agent#toolEvents(String line)`, which the agent API's contract version 5 adds and AgentServer
  serves as `ToolEvents`; `CodexToolEvents` maps one output line to its tool events (`id`, START or END, `tool`, `kind`,
  `program`, `background`, `jobId`, `at` where the line has a time). It keeps no state between lines: Porelo joins a
  start to its end by `id`, and a background job's by `jobId`.
- Kinds: `command_execution` -> shell (program from `command`), `file_change` -> edit, `mcp_tool_call` -> mcp, `web_search` -> web, any other item but a message or reasoning -> other.
- The program of a shell call is the first word of its command line, after `env`, assignments and `sudo`; never the
  whole line, which can carry a secret.
- Background: Whether the pinned Codex reports a command that keeps running after its item completed is measured before the build; until then a call is foreground.
- Red first: `CodexToolEventsTest` feeds a recorded run's lines (a shell call, an edit, an unknown tool, a failed call,
  and a line that is not JSON) and expects one start and one end per call, the kinds above and the program alone; it
  fails while `toolEvents` still answers the API's default, nothing.
- Level: unit tests over recorded output only; the acceptance legs show the entries of a real run once Porelo writes
  them.

## Acceptance

- A recorded unattended run yields one entry per tool call with tool, kind, start, end and, for a shell call, the
  program; a background job's start and end are one entry.
- No entry holds a command line beyond its program, a file's content or a tool's output.
- A line the reader cannot read yields nothing and never stops the run.

## Open questions

- Attended tasks: their agent runs at a terminal and writes no JSON stream. Should their tool calls be recorded too?
  That takes the agent's session files or hooks, a larger change for each adapter.
- The program of a shell call: is the first word enough, or should the entry keep the whole command line, which can
  carry a secret?

## To be checked

- Porelo's agent API with contract version 5 and `Agent#toolEvents`, installed locally to build against.
- Whether the pinned Codex reports a background or still-running command, and how its end is told.
- Whether `item.*` events carry a time of their own.
