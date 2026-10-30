# Issues

What is open in this repository, grouped by state in the order Review, Rework, Implementing, Backlog,
Analyzed, Accepted, Idea, Estimated, Finished and Rejected; a group with nothing in it is left out.
Within a group, the order is the priority. The number is identity, not sequence.

## Review

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx05"></a>[CX05](CX05-Snapshot-Package.md) | M | — | every push to main publishes the snapshot package, so the ChatGPT sign-in can be tested from it; published as `0.1.0~snapshot.5` |
| <a id="cx03"></a>[CX03](CX03-Forge-Pull-Requests.md) | S | — | Carry the approved forge pull-request rule and guard its DCO boundary |

## Backlog

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx10"></a>[CX10](CX10-The-Sign-In-Says-How-It-Is-Answered.md) | XS | — | a person signing in to Codex CLI from a host is told to confirm the one-time code on OpenAI's device page |

## Analyzed

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="cx04"></a>[CX04](CX04-The-Repository-s-Agent-Works-In-A-Porelo-Task.md) | S | [CX08](#cx08) | the repository's agent moves into its own Porelo task, after a dry run on this machine | 0 |
| <a id="cx08"></a>[CX08](CX08-The-Build-Runs-On-The-Local-Forge-As-On-The-Central-One.md) | S | — | the workflows run 1:1 on the local Forgejo and the central forge, before the move | 0 |

## Idea

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx06"></a>[CX06](CX06-Rented-Acceptance-And-Releases.md) |  | — | rented acceptance legs, releases and CLI updates, as in the other adapters |
| <a id="cx09"></a>[CX09](CX09-A-Sign-In-Without-A-Browser-Is-Tested-On-Every-CLI-Update.md) |  | [CX06](#cx06) | the sign-in at a terminal without a browser, checked on every CLI update so a release that drops it fails its pin bump |

## Estimated

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx07"></a>[CX07](CX07-A-Repeated-Refused-Call-Ends-The-Wake-Scenario.md) | S | — | a real model repeating a refused tool call in a woken task ends the acceptance run early, the live case of the acceptance kit's loop watch |

## Finished

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx01"></a>CX01 — Agent Codex CLI | L | — | `porelo-agent-codex` ships Codex CLI as a packaged agent with its digest-verified CLI and code-mode host, brokering the ChatGPT sign-in (device-code login collected into the vault, renewed on the host) and the OpenAI API key with only a phantom token in the container; session, waiting and wake acceptance pass on snapshot 343 (finished 2026-10-27) |
| <a id="cx02"></a>CX02 — Build in a Porelo task | M | — | A pinned build-image declaration and equivalent project configuration build and verify native packages inside a real Porelo task; GitHub retains the package artifacts |
