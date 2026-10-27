# Issues

What is open in this repository, grouped by state in the order Review, Rework, Implementing, Backlog,
Analyzed, Accepted, Idea, Estimated, Finished and Rejected; a group with nothing in it is left out.
Within a group, the order is the priority. The number is identity, not sequence.

## Implementing

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx03"></a>[CX03](CX03-Forge-Pull-Requests.md) | S | — | Carry the approved forge pull-request rule and guard its DCO boundary |

## Idea

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx04"></a>[CX04](CX04-The-Repository-s-Agent-Works-In-A-Porelo-Task.md) |  | [PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59) | the repository's agent moves into its own Porelo task, wave 7b of the move |

## Finished

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx01"></a>CX01 — Agent Codex CLI | L | — | `porelo-agent-codex` ships Codex CLI as a packaged agent with its digest-verified CLI and code-mode host, brokering the ChatGPT sign-in (device-code login collected into the vault, renewed on the host) and the OpenAI API key with only a phantom token in the container; session, waiting and wake acceptance pass on snapshot 343 (finished 2026-10-27) |
| <a id="cx02"></a>CX02 — Build in a Porelo task | M | — | A pinned build-image declaration and equivalent project configuration build and verify native packages inside a real Porelo task; GitHub retains the package artifacts |
