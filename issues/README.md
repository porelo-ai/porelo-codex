# Issues

What is open in this repository, grouped by state in the order of [workflow.yml](workflow.yml); a group
with nothing in it is left out. The index is generated from the issues' heads and never edited by hand.
Within a group, the order is the priority. The number is identity, not sequence.

## Accepted

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx16"></a>[CX16](CX16-The-Tests-Follow-The-Pyramid.md) | XS | - | the suites by level with measured times; the refused telemetry host held in the unit phase, and the timeout test waits 100 ms |
| <a id="cx12"></a>[CX12](CX12-Mutation-Testing-With-PIT.md) | - | - | mutation testing by hand: a `pit` profile outside CI with no threshold, and a report sorting survivors into real gaps and harmless ones |

## Analyzed

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx14"></a>[CX14](CX14-The-Acceptance-Runs-On-Every-Pin-Change.md) | M | [CX08](README.md#cx08) | a pin change reaches `main` only after the acceptance suite passed on rented ubuntu and fedora machines against its packages, as in the other adapters; built after the move |

## Estimated

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="cx06"></a>[CX06](CX06-Rented-Acceptance-And-Releases.md) | XL | - | rented acceptance legs, releases and CLI updates, as in the other adapters | - |
| <a id="cx07"></a>[CX07](CX07-A-Repeated-Refused-Call-Ends-The-Wake-Scenario.md) | S | - | a real model repeating a refused tool call in a woken task ends the acceptance run early, the live case of the acceptance kit's loop watch | - |
| <a id="cx09"></a>[CX09](CX09-A-Sign-In-Without-A-Browser-Is-Tested-On-Every-CLI-Update.md) | M | [CX06](README.md#cx06) | the sign-in at a terminal without a browser, checked on every CLI update so a release that drops it fails its pin bump | 0 |
| <a id="cx13"></a>[CX13](CX13-Depend-On-The-Agent-Protocol-Porelo-Provides.md) | S | - | the agent contract named at install time | 0 |

## Backlog

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx04"></a>[CX04](CX04-The-Repository-s-Agent-Works-In-A-Porelo-Task.md) | S | [CX08](README.md#cx08) | the repository's agent moves into its own Porelo task, after a dry run on this machine |

## Implementing

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx20"></a>[CX20](CX20-The-Build-Takes-Its-Downloads-From-A-Cache.md) | XS | - | the runner's packages come from the Actions cache, keyed by their package manifest, so an outage of the package mirrors no longer fails a Build that already ran once; the CI fetches no CLI archive, and what must stay live is named |

## Review

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="cx18"></a>[CX18](CX18-The-Acceptance-Takes-A-Task-s-Container-Name-From-Porelo.md) | XS | - | the acceptance scenarios name a task by its own name in Porelo's commands and take its container's name from `porelo task status --json`, never built as `porelo-<project>-<task>` | - |
| <a id="cx19"></a>[CX19](CX19-The-Adapter-Reports-Each-Tool-Call-With-Its-Time.md) | S | - | the adapter reads each tool call from its agent's output and hands Porelo its tool, kind, start, end, a shell call's command line, whether it ran in the background, and a sub-agent's name and the calls made under it, in Porelo's common entry | 0 |
| <a id="cx15"></a>[CX15](CX15-Snapshots-Not-Published-On-The-Forge-Come-From-Upstream.md) | S | - | on a forge whose registry holds only its organization's packages, the other Porelo snapshots come from the anonymous repository `MAVEN_UPSTREAM_SNAPSHOTS_URL` names, the forge's token sent to the forge alone | - |
| <a id="cx11"></a>[CX11](CX11-Subagents-Only-As-Many-As-The-Task-Allows.md) | M | - | no subagent in a task unless the task allows it, then at most as many as it says; off (a hook on spawn_agent) and count measured to hold at the pin, depth does not | - |
| <a id="cx08"></a>[CX08](CX08-The-Build-Runs-On-The-Local-Forge-As-On-The-Central-One.md) | S | - | the workflows run 1:1 on the local Forgejo and the central forge, before the move | - |
| <a id="cx05"></a>[CX05](CX05-Snapshot-Package.md) | M | - | every push to main publishes the snapshot package, so the ChatGPT sign-in can be tested from it; published as `0.1.0~snapshot.5` | - |
| <a id="cx17"></a>[CX17](CX17-Third-Party-Notices-Ship-With-The-Package.md) | S | - | the license texts of what the executable compiles in from outside Porelo - SnakeYAML, JSpecify, the GraalVM runtime - in one THIRD-PARTY-NOTICES file, in the deb's doc directory and as `%license` in the rpm | - |
| <a id="cx03"></a>[CX03](CX03-Forge-Pull-Requests.md) | S | - | Carry the approved forge pull-request rule and guard its DCO boundary | - |

## Finished

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx10"></a>CX10 — The Sign-In Says How It Is Answered | XS | — | `codex.yaml` declares `login.flow: device-code`, so a person signing in to Codex CLI from a host is told to confirm the one-time code on OpenAI's device page; tested in sprint 6m: passed. |
| <a id="cx01"></a>CX01 — Agent Codex CLI | L | — | `porelo-agent-codex` ships Codex CLI as a packaged agent with its digest-verified CLI and code-mode host, brokering the ChatGPT sign-in (device-code login collected into the vault, renewed on the host) and the OpenAI API key with only a phantom token in the container; session, waiting and wake acceptance pass on snapshot 343 (finished 2026-10-27) |
| <a id="cx02"></a>CX02 — Build in a Porelo task | M | — | A pinned build-image declaration and equivalent project configuration build and verify native packages inside a real Porelo task; GitHub retains the package artifacts |
