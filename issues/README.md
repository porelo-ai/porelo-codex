# Issues

What is open in this repository, grouped by state in the order of [workflow.yml](workflow.yml); a group
with nothing in it is left out. The index is generated from the issues' heads and never edited by hand.
Within a group, the order is the priority. The number is identity, not sequence.

## Idea

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx22"></a>[CX22](CX22-The-Agent-s-Own-Telemetry-Goes-To-Porelo.md) | - | - | the adapter declares Codex CLI's own OpenTelemetry export pointed at Porelo's local receiver in the task, with every switch that would carry content - prompts, tool arguments and output, responses - set explicitly off |

## Accepted

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx12"></a>[CX12](CX12-Mutation-Testing-With-PIT.md) | - | - | mutation testing by hand: a `pit` profile outside CI with no threshold, and a report sorting survivors into real gaps and harmless ones |

## Analyzed

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx14"></a>[CX14](CX14-The-Acceptance-Runs-On-Every-Pin-Change.md) | M | [CX08](README.md#cx08) | a pin change reaches `main` only after the acceptance suite passed on rented ubuntu and fedora machines against its packages, as in the other adapters; built after the move |

## Estimated

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="cx16"></a>[CX16](CX16-The-Tests-Follow-The-Pyramid.md) | XS | - | the suites by level with measured times; the refused telemetry host held in the unit phase, and the timeout test waits 100 ms | - |
| <a id="cx13"></a>[CX13](CX13-Depend-On-The-Agent-Protocol-Porelo-Provides.md) | S | - | the agent contract named at install time | 0 |
| <a id="cx06"></a>[CX06](CX06-Rented-Acceptance-And-Releases.md) | XL | - | rented acceptance legs, releases and CLI updates, as in the other adapters | - |
| <a id="cx07"></a>[CX07](CX07-A-Repeated-Refused-Call-Ends-The-Wake-Scenario.md) | S | - | a real model repeating a refused tool call in a woken task ends the acceptance run early, the live case of the acceptance kit's loop watch | - |
| <a id="cx09"></a>[CX09](CX09-A-Sign-In-Without-A-Browser-Is-Tested-On-Every-CLI-Update.md) | M | [CX06](README.md#cx06) | the sign-in at a terminal without a browser, checked on every CLI update so a release that drops it fails its pin bump | 0 |

## Backlog

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx04"></a>[CX04](CX04-The-Repository-s-Agent-Works-In-A-Porelo-Task.md) | S | [CX08](README.md#cx08) | the repository's agent moves into its own Porelo task, after a dry run on this machine |

## Review

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="cx19"></a>[CX19](CX19-The-Adapter-Reports-Each-Tool-Call-With-Its-Time.md) | S | - | the adapter reads each tool call from its agent's output and hands Porelo its tool, kind, start, end, a shell call's command line, whether it ran in the background, and a sub-agent's name and the calls made under it, in Porelo's common entry | 0 |
| <a id="cx21"></a>[CX21](CX21-An-Attended-Session-Takes-An-Opening-Text.md) | XS | - | an attended session of Codex CLI opens on the person's opening text as its first turn and then waits for the person, the form (positional) declared in the agent definition and measured in a task | - |
| <a id="cx24"></a>[CX24](CX24-Codex-Is-Installed-Root-Owned.md) | M | - | the codex binary a task starts and its code-mode helper are installed by root and owned by root, so the agent in a task cannot replace what Porelo starts as Codex next time; their version stays pinned by the definition and moves only through the update job, and a self-update inside a task fails | - |
| <a id="cx08"></a>[CX08](CX08-The-Build-Runs-On-The-Local-Forge-As-On-The-Central-One.md) | S | - | the workflows run 1:1 on the local Forgejo and the central forge, before the move | - |

## Finished

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cx11"></a>CX11 — Subagents Only As Many As The Task Allows | M | — | Codex CLI in a task starts no subagent unless the task allows it: at 0 a trusted `PreToolUse` hook denies `spawn_agent`, above 0 the concurrent agent threads are capped at the task's number, and skills are left out of its instructions. |
| <a id="cx20"></a>CX20 — The Build Takes Its Downloads From A Cache | XS | — | A Build, an update run and the workflow check take the runner's packages from the Actions cache, each `.deb` checked against the hash its signed apt metadata names before it is installed; an entry of an old package list goes by itself. |
| <a id="cx17"></a>CX17 — Third-Party Notices Ship With The Package | S | — | `porelo-agent-codex` carries the license texts of every component compiled into its executable from outside Porelo - SnakeYAML, JSpecify and the GraalVM runtime - in one `THIRD-PARTY-NOTICES` file, named in `copyright` and marked `%license` in the RPM, and `PackageNoticesCheck` refuses a package without it. |
| <a id="cx18"></a>CX18 — The Acceptance Takes A Task's Container Name From Porelo | XS | — | The acceptance scenarios name a task by its own name and take its container's name from `porelo task status --json`, so they hold however Porelo spells a container. |
| <a id="cx23"></a>CX23 — The Issue Heads Are Checked Every Day | XS | — | The daily shared-rules run checks every issue head and index with `issues check issues`, so a head that breaks the rules turns it red, not only the next push. |
| <a id="cx03"></a>CX03 — Forge Pull Requests | S | — | A pull request from this repository's own branch on the host's forge is checked as internal without an author association, while a fork there and every outside contribution on GitHub still need their authors' DCO sign-off. |
| <a id="cx05"></a>CX05 — Snapshot Package | M | — | Every push to main publishes `porelo-agent-codex` to the snapshot channel, believed only once the snapshot index lists the exact package, so a machine installs it with `apt` or `dnf` like the other agents. |
| <a id="cx15"></a>CX15 — Snapshots Not Published On The Forge Come From Upstream | S | — | A Build on a forge whose registry holds only its organization's packages takes the other Porelo snapshots from the anonymous repository `MAVEN_UPSTREAM_SNAPSHOTS_URL` names, and the forge's token goes to the forge's registry alone. |
| <a id="cx10"></a>CX10 — The Sign-In Says How It Is Answered | XS | — | `codex.yaml` declares `login.flow: device-code`, so a person signing in to Codex CLI from a host is told to confirm the one-time code on OpenAI's device page; tested in sprint 6m: passed. |
| <a id="cx01"></a>CX01 — Agent Codex CLI | L | — | `porelo-agent-codex` ships Codex CLI as a packaged agent with its digest-verified CLI and code-mode host, brokering the ChatGPT sign-in (device-code login collected into the vault, renewed on the host) and the OpenAI API key with only a phantom token in the container; session, waiting and wake acceptance pass on snapshot 343 (finished 2026-10-27) |
| <a id="cx02"></a>CX02 — Build in a Porelo task | M | — | A pinned build-image declaration and equivalent project configuration build and verify native packages inside a real Porelo task; GitHub retains the package artifacts |
