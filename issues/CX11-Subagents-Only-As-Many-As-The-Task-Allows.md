# CX11 — Subagents Only As Many As The Task Allows

**Status:** Accepted (operator, 2026-10-31).

**What must be true.** Codex in a Porelo task starts no subagent unless the task allows it, and then no
more at once and no deeper than the task says.

## Plan

- **The contract built against:** Porelo's agent interface carries the number of subagents an agent may
  start inside a task, 0 (none) by default; each agent definition declares what it can enforce - off, a
  concurrent count, a depth - and Porelo refuses a value the definition cannot enforce instead of
  enforcing less.
- Measured headless at the pin 0.163.0 with its code-mode helper, on the VM with `gpt-6.1-sol`, counting
  one session file per agent thread: by default a subagent starts, and one it starts can start another.
  - **Concurrent count:** `-c agents.max_concurrent_threads_per_session=1` holds - of three asked at once,
    one ran and two were refused with "agent thread limit reached".
  - **Off does not hold:** with `--disable multi_agent`, `--disable multi_agent_v2` or both, `features list`
    shows them off, yet `spawn_agent` is offered and a subagent starts. The session records
    `multi_agent_version: v2` and `multi_agent_mode: explicitRequestOnly`, which come with the model,
    not from the feature switch.
  - **Depth does not hold:** `-c agents.max_depth=1` and `=0` both let a subagent start another.
  - So today this definition can declare a concurrent count of 1 or more, but neither off nor a depth.

## Acceptance

- Seen to fail first: a task with the default asks Codex to start a subagent, and it does.
- With the change, the same task starts none; a task allowing N starts at most N at once and refuses the
  next.

## To be checked

- A way to switch subagents off that holds: a hook denying `spawn_agent`, or a setting that overrides the
  model's multi-agent mode; until one is found, a task with the default 0 cannot run Codex.
- Whether `max_depth` counts differently than read, or is ignored on the v2 path.
- The concurrent count attended, at a terminal.
