# Decisions

| Subject | What holds | Detail |
|---|---|---|
| Provider boundary | The adapter omits provider integration until its contract is published | [Provider boundary](#provider-boundary) |
| Two upstream artifacts | The task image verifies and installs both the CLI and code-mode host | [Installation](#installation) |
| Command grammar | Java assembles exec and exec resume commands | [Commands](#commands) |
| Task-local profile | The image sets CODEX_HOME explicitly | [Profile](#profile) |
| Artifact checks | Shared named pin checks and recorders cover both fetched components | [Artifact checks](#artifact-checks) |
| Build boundary | CI verifies packages without publishing or live credentials | [Build boundary](#build-boundary) |

## Provider boundary

The ChatGPT provider decision and accepted exposure belong to porelo-project's
[provider issue](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj54).
The adapter declares no provider and implements no credential import until that contract exists.
This permits the definition, protocol, packages and provider-independent checks to be built without
inventing a provider identity or a refresh contract.

## Installation

Unlike the single executable installed by the scaffold, Codex requires its CLI and code-mode host
beside each other. Both archives share a version and have independent publisher digests. The
definition declares both downloads; extraction checks the complete archive member list before
installing either executable. Both fetched components belong in the package's bill of materials.

## Commands

Codex uses the `exec` subcommand for unattended work and `exec resume` to continue a thread. The
generic flag assembler cannot place those subcommands and positional arguments correctly, so the
adapter assembles them in Java. A positional delimiter keeps prompt text from becoming an option.
The CLI has no turn-limit flag, so a requested limit is refused rather than silently ignored.

## Artifact checks

The shared `check-pin` command checks `codex` and `helper` separately against the definition.
Two `add-fetched-cli` executions record their fetched-component provenance in the bill of
materials through an exec-plugin dependency. No private bridge or build-tool dependency ships
in the adapter. A package guard checks the actual bill against both declared executable artifacts.

## Profile

The image sets `CODEX_HOME=/home/agent/.codex` for interactive and unattended invocations. Codex
puts its temporary helper aliases beneath the selected profile too; it does not need a second
host directory. A CLI invocation without that variable uses the invoking account's default profile.

## Build boundary

The Java and resource layout follows the existing adapters: a small `YamlAgent` subclass, a
stateless JSON formatter, service registration and a null-marked main package. No provider-specific
setup or credential extractor is added before a published contract requires it.

Unlike the complete adapters, CI only checks the existing pins and builds packages. It neither
publishes an incomplete adapter nor proposes CLI upgrades without a live-provider gate. Shared
release tooling remains the source of pin metadata and bill recording. Provider acceptance,
publishing and automated updates require the provider contract and its acceptance coverage.
