# CX01 — Agent Codex CLI

**Status:** Implementing; this publication contains only the provider-independent scaffold.
**Size:** L - a new agent repository with its packaging and an acceptance leg, after a measurement of whether it can be brokered; it needs a provider `porelo` does not have yet.

**What must be true.** Codex CLI runs in a task as a packaged agent, signed in and brokered like the
supported ones, with its real credential never in the container.

## Why

**Upstream.** https://github.com/openai/codex, npm `@openai/codex`.

**Providers.** One vendor.

**Authentication.** An account sign-in or an API key.

**Can it be brokered?** The feasibility measurement is complete; provider integration and its
measurement notes remain separate from this provider-independent publication.

## Plan

- **Provider-independent scaffold:** `CodexAgent`, `CodexJsonFormatter` and `agent/codex.yaml`
  provide protocol discovery, command assembly and structured turn outcomes through the published
  agent API and wire snapshots. The definition pins and verifies both required CLI artifacts.
- **Build and packaging:** `pom.xml`, the Maven wrapper, native/distribution profiles, package
  metadata and workflows follow `porelo-omp`. Shared named pin checks and fetched-component
  recording replace the private tooling bridge; no build tool becomes an adapter dependency.
- **Red first:** failed-turn, command-boundary, artifact-digest, native-linkage and workflow guards
  protect the package. Provider-free discovery acceptance runs against the VM-built executable.
- **Integration remains local:** provider routing, credential extraction, workspace trust,
  terminal markers and live/session/wake fixtures are not part of this publication. ChatGPT
  integration uses the provider contract in porelo-project
  [PJ54](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj54).

## Acceptance

- It ships as its own binary and its own package, in this repository, discovered without
  Porelo changing.
- Nothing outside its own directory names it, enforced by the build.
- A task authenticates without anyone signing in inside the container.
- The real credential is never in the container, seen by searching the container for it.

## To be checked

- The full provider-independent Build on the exact publication commit inside a VM task.
- The published shared-tooling snapshot with named multi-artifact pin support, before pushing.
- Provider integration and its complete credential, terminal, session and wake acceptance.
