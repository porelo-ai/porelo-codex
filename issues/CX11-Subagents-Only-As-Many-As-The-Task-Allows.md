# CX11 — Subagents Only As Many As The Task Allows

**Status:** Accepted (operator, 2026-10-31).

**What must be true.** Codex in a Porelo task starts no subagent unless the task allows it, and then no
more at once and no deeper than the task says.

## Plan

- **The contract built against:** Porelo's agent interface carries the number of subagents an agent may
  start inside a task, 0 (none) by default; each agent definition declares what it can enforce - off, a
  concurrent count, a depth - and Porelo refuses a value the definition cannot enforce instead of
  enforcing less.
- Read in 0.164.0, not yet at the pin: subagents are the feature `multi_agent`, stable and on by default,
  switched off with `--disable multi_agent`; the limits are `agents.max_concurrent_threads_per_session`
  and `agents.max_depth`, set with `-c`.

## Acceptance

- Seen to fail first: a task with the default asks Codex to start a subagent, and it does.
- With the change, the same task starts none; a task allowing N starts at most N at once and refuses the
  next.

## To be checked

- On the VM, at the pinned version: that `--disable multi_agent` removes the subagent tools, attended and
  headless, and that both limits hold.
