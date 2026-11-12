# CX22 — The Agent's Own Telemetry Goes To Porelo

```json
{
  "$schema": "issue-1.json",
  "id": "CX22",
  "title": "The Agent's Own Telemetry Goes To Porelo",
  "state": "Idea",
  "size": "-",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "the adapter declares Codex CLI's own OpenTelemetry export pointed at Porelo's local receiver in the task, with every switch that would carry content - prompts, tool arguments and output, responses - set explicitly off"
}
```

**What must be true.** A task's Codex CLI sends its own telemetry - its events, metrics and, where it has them, traces -
over OTLP to the receiver Porelo runs for the task, and to nowhere else. Every switch that would put content into that
telemetry (the person's prompts, tool arguments and output, the agent's responses) is set off explicitly in the
definition, never left to a default, so a new version that changes a default changes nothing here.

The contract built against: Porelo runs a local OTLP receiver for each task and keeps what arrives in the task's
records under its run; the adapter only declares how its agent is pointed at it.

What is known today, to be confirmed at the pinned version:
- Codex configures it in `[otel]`, with separate exporters for logs, traces and metrics.
- `log_user_prompt = false` does not hide snippets of tool output, so what does is still to be found.
- `[analytics] enabled = false` keeps its own analytics off.

## To be checked

- Which setting keeps tool output out of the exported events, and whether one exists at the pinned version.
- How the `[otel]` exporters are set without a config file the agent can rewrite.
