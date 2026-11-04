# CX11 — Subagents Only As Many As The Task Allows

**Status:** Review.
**Size:** M - a security-relevant hook and its trust, and `config.toml` written for every task; about 90
minutes: the M median of 47, raised toward its 75th percentile of 93 by the setup now writing a file for every
task, the scenario on the VM and three open points - whether the hook's trust can be written ahead, whether an
inline hook command runs, and how `max_depth` counts.

**What must be true.** Codex in a Porelo task starts no subagent unless the task allows it, and then no more
at once than the task says.

**This does not hold today.** Porelo takes a task's default of 0 as none and reads `codex.yaml`, which declares nothing, as
off, but this adapter switches nothing off, so Codex in such a task still starts a subagent, as measured below.

## Plan

- **The contract built against:** Porelo's agent interface carries one number, the most subagents an agent may
  run at the same time inside a task (not a total over the task), 0 (none) by default, and no depth; each
  agent definition declares what it can enforce of it - off only, or a count - and Porelo refuses a value the
  definition cannot enforce with exit code 64 instead of enforcing less. Where a CLI needs a depth to make the
  count hold, the adapter sets it itself.
- Measured headless at the pin 0.163.0 with its code-mode helper, on the VM with `gpt-6.1-sol`, counting
  one session file per agent thread: by default a subagent starts, and one it starts can start another.
  - **Concurrent count:** `-c agents.max_concurrent_threads_per_session=1` holds - of three asked at once,
    one ran and two were refused with "agent thread limit reached".
  - **Off:** a `PreToolUse` hook in `$CODEX_HOME/hooks.json` matching `collaborationspawn_agent` (the
    hook sees namespace and name joined; a matcher of `spawn_agent` alone never fires) and answering
    `permissionDecision: deny` holds - the call is refused, no second thread starts, and asked to find
    another way, the model started none. Codex runs a hook only once trusted, so the run passes
    `--dangerously-bypass-hook-trust`, or the hook's trust is written with it. A `SubagentStart` hook
    fires but does not stop the start.
  - **The feature switch is not off:** with `--disable multi_agent`, `--disable multi_agent_v2` or both,
    `features list` shows them off, yet `spawn_agent` is offered and a subagent starts. The session
    records `multi_agent_version: v2` and `multi_agent_mode: explicitRequestOnly`, which come with the
    model, not from the feature switch.
  - **Depth does not hold:** `-c agents.max_depth=1` and `=0` both let a subagent start another.
  - So this definition declares off and a concurrent count, not a depth.
- **`codex.yaml`** declares off and a concurrent count, not a depth.
- **`CodexContainerSetup.files`** today returns nothing when there is no broker endpoint; the subagent
  settings have to be written for every task, so the broker part becomes one section of a file that is
  always there.
- At 0 it writes `.codex/hooks.json` in the agent's home with a `PreToolUse` hook matching
  `collaborationspawn_agent` whose command prints the deny answer inline. Measured shape:
  `{"hooks":{"PreToolUse":[{"matcher":"collaborationspawn_agent","hooks":[{"type":"command","command":"<cmd>","timeout":10}]}]}}`,
  the command reading the call as JSON on standard input (`tool_name` among its keys) and printing
  `{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"<why>"}}`;
  the reason reaches the model - a `ContainerFile` carries no
  executable bit, so there is no script to run; above 0 it writes
  `agents.max_concurrent_threads_per_session` into `config.toml`.
- **The hook's trust:** written ahead into `config.toml` as Codex records it, if it can be; otherwise the
  `sandboxed` arguments gain `--dangerously-bypass-hook-trust`, which also runs any hook the workspace
  brings and then becomes a question for the operator.
- **Red first:** `CodexContainerSetupTest` - a context with the default and no broker yields the hook and
  its trust; today it yields no file at all.
- **On the VM:** a `session.feature` scenario asks for a subagent in a task with the default and sees one
  thread only.
- **A soft switch:** the agent has a shell and can start another copy of its CLI; this issue governs only
  the built-in subagents. A hard bound is Porelo's own, outside this repository.
- **Porelo's version:** `porelo.version` in `pom.xml` is already 0.5.1-SNAPSHOT, and the published snapshot's agent API
  carries the value (`SubagentLimit`, `subagents` in `SetupContext`).

## Acceptance

- Seen to fail first: a task with the default asks Codex to start a subagent, and it does.
- With the change, the same task starts none; a task allowing N starts at most N at once and refuses the
  next.

## Open questions

- None.

## Measured

On the VM at the pin 0.163.0 with `gpt-6.1-sol`, the adapter installed in the account, against a Porelo older than the
one on `main`, so `project remove` in the new scenario's last step was run as `project unfollow`:

- **Seen to fail first:** the new `session.feature` scenario against the adapter before the change: the agent
  answered `STARTED`.
- **With the change, headless:** the same scenario is green: the agent answers `MISSING`, and the container holds one
  session file. At `--subagents 5`, one run, without the hook, it holds two, which the scenario's count refuses, since
  what an agent says is not the proof. So the hook's trust can be
  written ahead, and a hook command given inline runs, through a shell. `--dangerously-bypass-hook-trust` is not
  passed, so nothing becomes a question for the operator. The form of the trust is read in 0.163.0's source,
  `codex-rs/hooks/src/engine/discovery.rs`: `[hooks.state."<file>:pre_tool_use:0:0"] trusted_hash`, the SHA-256 of the
  hook's normalized form.
- **Attended, at a terminal, one run, not kept as a scenario:** the screen shows "Blocked by hook / this task allows
  no subagents", and the agent says none started.
- **At a count of 1, headless, one run, not kept as a scenario:** of three asked at once, two were refused with "agent
  thread limit reached".

The unit test ties the hash to the hook the file holds and to the hash seen accepted; it cannot show that Codex
computes the same, which only the scenario does. Read by agy `gemini-4-argon-high`: both of these came from its
findings, then weaker.

A pin that changes the normalized form changes the hash, and Codex then does not run the hook, without an error. The
scenario sees that, but **no workflow of this repository runs the acceptance suite**, so a pin bump does not run it:
it has to be run by hand on the VM with a bump, until a workflow does.

The hook is also written for a task with neither endpoint nor token, as the plan's "every task" says. The agent
itself can write the settings file; as the plan says of its shell, this is a soft switch.

## To be checked

- Whether `agents.max_concurrent_threads_per_session` counts a subagent's own subagents too, since no depth can be
  set to stop them; and whether `max_depth` counts differently than read, or is ignored on the v2 path.
- The concurrent count attended, at a terminal.
