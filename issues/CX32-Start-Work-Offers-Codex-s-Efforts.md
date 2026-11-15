# CX32 — Start Work Offers Codex's Efforts

```json
{
  "$schema": "issue-1.json",
  "id": "CX32",
  "title": "Start Work Offers Codex's Efforts",
  "state": "Review",
  "size": "XS",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the definition declares the reasoning efforts Codex takes and how one is passed (`-c model_reasoning_effort={effort}`), so Start work offers them to pick instead of typing and an absent effort passes nothing"
}
```

**Why this size.** One section of the agent definition and a test that reads it; no code.

**Estimate.** XS: 9 min, the measured median of XS over all repositories.

**What must be true.** A person starting work with Codex picks the effort from the levels the pinned CLI accepts,
and no effort picked passes nothing.

## Why

Part of ST369, the Start work redesign. The operator wants to pick the model and the reasoning effort from what the
chosen agent offers, not type them. Porelo's contract (B795, handed over locally as porelo 5328c5df4) adds an optional
top-level `effort` section to an agent's definition: `effort: { arguments: [..., "{effort}"], values: [...] }`, the
values in the agent's own words and order. Today's definition reader skips an unknown top-level section, so the
declaration does not break an older Porelo.

## Plan

- Declare `effort` in the definition: `-c model_reasoning_effort={effort}`, with the levels the pinned Codex 0.163.0 takes, read from its own help on
  the pin where a VM slot allows, else from the laptop's version and said so.
- A test that reads the section from the definition itself (the API record does not hold it yet), seen red first.
- **Level:** unit tests of the definition.

## Acceptance

- The definition declares the effort levels and how they are passed, in the contract's shape, and today's reader
  still reads the definition.

## What was built

- **The definition** (`codex.yaml`) declares `effort` with `arguments: ["-c", "model_reasoning_effort={effort}"]` and `values: [none, low, medium, high, xhigh, max, ultra]`, in the
  contract's shape; an older Porelo's reader skips the section.
- **Where the levels come from:** the pinned 0.163.0's model catalog (`codex debug models`), read offline on the test VM: its 11 models take some of low to ultra, with low or medium as default; `none` added, which the acceptance suite already sets. A model offered a level it does not take is Porelo's question with the models section.
- **Test** `EffortDeclarationTest` reads the section from the definition itself: the arguments and the levels, in
  their order. Both seen red before the section stood; dropping the last level turned `offersTheLevelsTheCliTakes`
  red, restored identical.
- **The reading by another model** found no fault: the shape, the reader of porelo 5328c5df4, the substitution and
  the placement hold. It noted that `none` is not in the catalog's levels and that some models take only some of the levels; `none` stays, since the acceptance suite runs 0.163.0 with it set, and offering each model its own levels waits for the models section. Left: the test reads the YAML with a plain loader, so a duplicated `effort` key would pass it and fail the reader.
- **Level:** unit tests; suite 178 run, 0 failures, 0 skipped. Whether the pinned CLI accepts each level in a
  run is the operator's test.
