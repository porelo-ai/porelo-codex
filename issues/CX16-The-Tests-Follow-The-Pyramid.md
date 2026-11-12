# CX16 — The Tests Follow The Pyramid

```json
{
  "$schema": "issue-1.json",
  "id": "CX16",
  "title": "The Tests Follow The Pyramid",
  "state": "Estimated",
  "size": "XS",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "the suites by level with measured times; the refused telemetry host held in the unit phase, and the timeout test waits 100 ms"
}
```

**Why this size.** two test classes, no contract.

**What must be true.** Every flow this adapter owns is proven by unit tests that run in seconds, and no test waits
longer than the thing it checks needs.

## The suites by level

Measured with a clean `./mvnw -o -B -s settings.xml clean test` on the host under a load of about 40 on 16 cores:
17 s in all, compilation included, 101 tests green and none skipped. The Build on GitHub took 2.7 min (run
42477655053). A class that starts a process counts as integration as a whole.

| Level | Tests | Run time | Share by count | What it holds |
|---|---|---|---|---|
| Unit | 74 | 0.9 s | 62 % | `CodexCredentialExtractorTest` 22, `CodexContainerSetupTest` 12, `CodexAgentTest` 10, `CodexJsonFormatterTest` 4, `CodexSessionTest` 4, `CodexScreenTest` 3, and the document and pin rules 19 |
| Integration | 27 + 8 | 2.6 s, plus the checks inside the 2.7 min Build job | 29 % | the workflow-step tests 25 (bash with stand-in tools), `WorkflowStepsTest` 2, and the `dist` checks of the built packages and the native executable (`NativeLinkageCheck` 2, `NativeProtocolCheck` 2, `PackageVersionCheck` 3, `FetchedArtifactsCheck` 1) |
| End-to-end | 11 scenarios | not run by any workflow; run attended on a local VM | 9 % | `terminal` 4, `session` 2, `wake` 2, `installed` 2, `waiting` 1 |

The unit share is below the pyramid's 70-80 % because the Build's own workflow steps are bash and are proven by
running them with stand-in tools, which is their lowest level. No workflow runs the end-to-end scenarios yet; the
rented legs that would are [CX06](README.md#cx06) and [CX14](README.md#cx14), and this issue adds none.

## Flows proven only above the unit level

- **The workflow steps** (publish steps and tokens, snapshot tokens, the update pull request): bash run with
  stand-in tools. They stay integration: the step is the real piece, and its tests take seconds.
- **The native executable's protocol** (`NativeProtocolCheck`): that the built executable answers `describe` and
  keeps a sign-in's renewal attributes is checked against the native build only, since only the native image shows
  what its reflection configuration kept. The same reading in Java is unit-tested in `CodexCredentialExtractorTest`.
- **The refused telemetry host**: that the definition refuses `ab.chatgpt.com` is held only by
  `NativeProtocolCheck` against the native build, and seen by a person only in *The deliberately refused telemetry
  host is visible*. No unit test reads it, so a change that drops it fails only in the `dist` build.
- **End-to-end only:** no other flow of this adapter. Every other scenario's adapter fact - the composer's screen at
  rest and in the model picker, the recorded session, the subagent switch, the login's arguments - has a unit test
  over the recorded screen or file. What the scenarios add is that the pinned Codex CLI honors those
  facts, which only the real CLI can show.

## Waits, sleeps and the network

- `WorkflowStepsTest` runs `sleep 20` under a limit of 2 s and so waits those two seconds each run; a limit of
  100 ms shows the same kill.
- No test reaches the network: every download in a workflow step is a stand-in, and no test opens a socket or a
  URL (searched for `HttpClient`, `openConnection`, `openStream`, `new URL(`, `InetAddress` and `ServerSocket`).

## Plan

- **The refused host is held in the unit phase** (XS): `CodexAgentTest` holds that the definition refuses exactly
  `ab.chatgpt.com`, seen red first with the entry taken out. `NativeProtocolCheck` keeps its reading of the native
  executable.
- **The timeout test waits 100 ms** (XS): `WorkflowStepsTest` with a limit of 100 ms, seen red first by a limit
  the sleep does not reach.

## Acceptance

- The unit suite stays under a minute, and no flow of the adapter is reached only from an integration or an
  end-to-end run; it fails when the refused host leaves the definition.
- `WorkflowStepsTest` takes under 0.5 s.

## Open questions

- None.
