# CX07 — A Repeated Refused Call Ends The Wake Scenario

**Status:** Estimated; after the operator's 0.6 tests through 6i.
**Size:** S - one acceptance scenario and one bounded live run on the local VM, no adapter code.

**What must be true.** When a real Codex model repeats a refused tool call in a woken task, the
acceptance run stops as soon as the call repeats, naming what repeated, well before the scenario's
120-second bound, instead of spending model turns until the bound.

From porelo [B244](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b244), whose
acceptance-kit tests show the loop watch reading the agent's own record of its tool calls; the live
case with a real model is this repository's.

## Plan

- `src/acceptance/resources/org/fuin/porelo/agent/impl/codex/acceptance/wake-loop.feature`: the wake
  scenario's fixture and its API-key provider, started with `--clearance deny`; the woken message asks
  for `bash -c` with no argument, then `bash` alone, and to retry until one works.
- The scenario expects the kit's loop watch to end it with its loop message, and the run's elapsed time
  to stay well under 120 seconds.
- Red first, if it can be had: the same scenario against the acceptance kit before porelo [B244](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b244) runs to its
  bound, because Codex's compact view adds nothing to tmux's history.

## Acceptance

- On `codex@192.168.122.174`, with `OPEN_AI_API` and `PORELO_E2E_MODEL`, the scenario stops on the
  repeated refused call, its message names the repeated call, and it ends well before 120 seconds.
- The run makes at most 10 upstream model requests in all, with no retries and no hosted tools.
- The task and the project the scenario made are removed afterwards, after a failure too.

## To be checked

- Whether the kit before porelo [B244](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b244) can still be resolved for the red run, or the red is shown another way.
