# CX19 — The Adapter Reports Each Tool Call With Its Time

```json
{
  "$schema": "issue-1.json",
  "id": "CX19",
  "title": "The Adapter Reports Each Tool Call With Its Time",
  "state": "Backlog",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the adapter reads each tool call from its agent's output and hands Porelo its tool, kind, start, end, a shell call's command line, whether it ran in the background, and a sub-agent's name and the calls made under it, in Porelo's common entry",
  "openQuestions": 0
}
```

**Why this size.** one reader over the item events the formatter already reads, a type table and its tests; the stream holds no times, no background jobs and no sub-agents to join. It builds against `ToolEvents` of Porelo's agent API, contract version 5.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=167).

**What must be true.** For every tool call the agent makes in an unattended task, Porelo records one entry in its
common tool-call format: the task, the tool as the agent names it, its kind (shell, edit, read, search, web,
sub-agent, mcp or other), when it started and when it ended, for a shell call its command line, whether it ran in the
background, and for a sub-agent the name it was given and the calls made under it. Porelo joins those calls to the
issue whose id names the sub-agent. The adapter only maps its agent's own output; the format, the time a line without
its own time gets, the masking of a command line and where the entry is kept are Porelo's.

## Plan

- Source: `codex exec --json`, measured with Codex CLI 0.163.0 (CC57 and CC59 in porelo-claude-code). `item.started` is START,
  `item.completed` END, joined by `item.id`; `command_execution` carries `command`, `exit_code` and `status`.
  `CodexJsonFormatter` reads `command_execution` today.
- No line has a clock time; `at` stays empty and Porelo stamps the line when it arrives. Items overlap, so a start is
  joined to its end by `id` only.
- `CodexAgent` overrides `Agent#toolEvents(String line)`; `CodexToolEvents` maps one output line to its
  `ToolEvent(line, id, phase, tool, kind, command, background, jobId, at, name, parent)`. It keeps no state between
  lines: Porelo joins a start to its end by `id`, a background job's by `jobId`, and a call to its sub-agent by
  `parent`.
- Kinds: `command_execution` -> shell (`command` as given, with its `/bin/bash -lc` wrapper), `file_change` -> edit (paths only), `mcp_tool_call` -> mcp, `web_search` -> web, `collab_tool_call` -> sub-agent, any other item but a message or reasoning -> other.
- A shell call's `command` is its whole command line as the stream gives it, unmasked; Porelo masks it before it is
  stored.
- Sub-agents: the stream does not show a sub-agent's start, name or calls. Only a `collab_tool_call` item appears
  (`tool: wait`, the main agent waiting), mapped as kind sub-agent without `name`. Codex's session files hold the
  spawn with its name and times, but they are not lines of this stream.
- Background: Codex has no background job of its own; a command started with `&` is not seen.
- Red first: `CodexToolEventsTest` feeds recorded lines (two overlapping command items, a file change, a `collab_tool_call`, a failed command, and a line that is not JSON) and expects the events above; it fails while
  `toolEvents` still answers the API's default, nothing.
- Level: unit tests over recorded output only; the acceptance legs show the entries of a real run once Porelo writes
  them.

## Acceptance

- A recorded unattended run yields one entry per tool call with tool, kind, start, end and, for a shell call, its
  command line; a background job's start and end are joined by its id; a sub-agent carries its name.
- No entry holds a file's content or a tool's output; a command line reaches Porelo unmasked and is stored masked.
- A line the reader cannot read yields nothing and never stops the run.

## What makes it uncertain

- Whether a codex sub-agent still starts in a task: CC59 in porelo-claude-code saw one start there, ordered to;
  the adapter maps only what the stream shows of it.

## Decisions

- Only unattended tasks are measured. Attended (tmux) sessions write no JSON stream; whether their tool calls are
  recorded is decided later.
- A shell call keeps its whole command line, not only its program. Porelo masks it before it is stored; the adapter
  passes the line as the stream gives it.
- A task gives each issue a sub-agent of its own, named by the issue's id; an issue's time is its sub-agent's span.
- Features are built only for agents run in the task. The adapter does not read codex's session files; a codex
  task's time is shared evenly among its issues.

## To be checked

- The agent API with `name`, `parent` and `durationMs` on `ToolEvent`, installed locally to build against.
