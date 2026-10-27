# CX01 — Agent Codex CLI

**Status:** Implementing; attended ChatGPT device-code login, OAuth vault collection and signed-in readback pass; missing-authentication acceptance passes against published Porelo snapshot 323 without a type override; API-key setup and shell-task broker containment verified; ChatGPT brokered model turn, container containment and host renewal pass against snapshot 343; the four API-key scenarios (session continuation, model-picker waiting, idle and busy wake) pass against snapshot 343; the provider from porelo [P11](https://github.com/porelo-ai/porelo/blob/main/issues/providers/README.md#p11) is published.
**Size:** L - a new packaged agent, provider-specific broker acceptance and a separate ChatGPT sign-in/refresh integration.

**What must be true.** Codex CLI runs in a task as a packaged agent, signed in and brokered like the
supported ones, with its real credential never in the container.

## Why

**Upstream.** https://github.com/openai/codex, npm `@openai/codex`.

**Providers.** One vendor.

**Authentication.** An account sign-in or an API key.

**Can it be brokered?** Yes for the measured Responses-over-HTTP path: a container holding only a
dummy token receives a real model response through a host relay, and the CLI refreshes the real login
in the host keyring. The separate ChatGPT provider contract is published; the adapter declares
device-code file login and bounded credential extraction, with attended collection, a brokered
ChatGPT model turn and host renewal verified.

## Plan

- **Feasibility is measured**, including real sign-in, endpoint redirection, container token
  isolation, refresh and tool execution, as recorded below. The remaining work splits at the
  provider contract; size remains L for the complete adapter and its cross-repository dependency.
- **Build now:** take the single-module Maven scaffold from `porelo-omp`: `pom.xml`, `settings.xml`,
  the pinned wrapper, native and distribution profiles, Debian/RPM metadata, document checks and
  workflows. Main code depends only on the published `porelo-agent-api` and `porelo-wire`
  `0.5.1-SNAPSHOT` contracts, not their releases or implementation sources.
  Pin CLI 0.163.0 and both measured artifact digests in the filtered `agent/codex.yaml` definition.
- **Adapter code:** register `CodexAgent` as an Agent service under
  `org.fuin.porelo.agent.impl.codex`; implement only the JSON event/end interpretation and launch
  shaping that the published YAML schema cannot express. Keep all task state, including temporary
  helper aliases, under the task's explicit `CODEX_HOME`. Do not declare unmeasured terminal markers.
- **Tests first:** reproduce failed-turn JSON being mistaken for success before implementing the
  event/end interpreter; guard task-local profile placement and both pinned install artifacts.
  Retain applicable scaffold guards for package versions, native linkage, document tagging,
  workflow permissions and pin agreement. Mutate harmful behavior and observe the guard fail.
- **Provider-independent acceptance now:** definition discovery, native protocol behavior,
  installation metadata and scenarios supported by the published kit without a provider. Run the
  build on the host with the exact shared pinned GraalVM, then deploy only this adapter to the
  VM's own account and run the provider-independent acceptance suite from the host against it.
  Do not install a VM toolchain or rent machines from this host.
- **API-key stage first:** use the built OpenAI provider contract from porelo
  [P05](https://github.com/porelo-ai/porelo/blob/main/issues/providers/README.md#p05), with its
  verified provider-data handover where the installed snapshot needs it. Declare the OpenAI
  dialect and URL endpoint in `agent/codex.yaml`. API-key runs explicitly select OpenAI; the
  subscription login defaults to ChatGPT. Add `CodexContainerSetup` to
  write owner-only task-profile TOML from `SetupContext.endpoint()` and its phantom token, with
  no upstream URL, real credential, provider token-variable name or extra dialect path in Java.
- **Red first:** the default empty setup must fail the missing-routing-file guard. Reproduce
  endpoint/token injection and owner-permission failures before implementing the setup. Keep the
  existing failed-turn guard. In the real task, require a model reply through Porelo's broker
  and check the nonempty real-key reference against output, metadata, profile and exported rootfs
  before showing output. A temporary relay does not satisfy this stage.
- Use the provided key through SSH standard input into an encrypted test-owned vault, never a
  VM key file or command argument. Inspect existing state first and refuse an overwrite/reset.
  Recheck the least-cost compatible available model; target one minimal reply, zero CLI retries
  and a one-minute bound. Refuse `ab.chatgpt.com`, record whether it is queried, and clean up only
  run-owned tasks, provider/adapter overrides and vault state. Start only in the assigned VM slot.
- **ChatGPT remains separate:** use the published provider contract from porelo
  [P11](https://github.com/porelo-ai/porelo/blob/main/issues/providers/README.md#p11) for adapter
  sign-in and extraction, then verify refresh and complete attended first-run acceptance.
  An API-key success does not complete that part or permit a guessed credential extractor.
  Use the existing file collector and the accepted [login-only exposure](../doc/decisions.md#login-collection)
  instead of the measured encrypted keyring. First guard
  incomplete OAuth records, missing account routing, lost renewal metadata and secrets in
  parse failures; read access, refresh, account and expiry data from the measured login shape,
  using the published `Credential` attributes rather than another renewal implementation.
- The GitHub live-test credential binding follows the [build boundary](../doc/decisions.md#build-boundary);
  adding that future step must not expose the key to the provider-independent build or untrusted code.
- **Complete adapter acceptance:** extend `src/acceptance/resources/.../acceptance/` beyond
  `installed.feature`, following the shared kit used by `porelo-omp`: `session`, `waiting`,
  `terminal`, `wake`, `broker` and `credential`. Add provider-free cases first where their
  preconditions truly need none; run broker/credential cases with the OpenAI contract and add
  ChatGPT sign-in/refresh cases only with its provider contract. Do not copy another CLI's screen
  markers or turn a credential-dependent scenario into a nominal provider-free check.
- **Wake has priority:** measure the pinned CLI's attended ready, busy and at-rest screens in
  `/workspace` before declaring them in `agent/codex.yaml`. With a scenario-owned daemon, send a
  mailbox message to an idle task and require a computed model answer, not an echoed notification.
  Also send while the agent is busy, require delivery once it is at rest, and guard duplicate
  wakes and input while a dialog is open. Test the harmful false-idle match red first. This is
  Porelo's adapter wake path, not the machine-local channel watcher or a separate CLI process.
- **This run's scope:** host setup/native/package guards and the API-key real-broker containment
  test first. The remaining interactive session/waiting/terminal/wake and ChatGPT scenarios stay
  explicit pending work, not implicit passes or unnamed skips; each handover lists what ran.
- Record Codex-specific differences in `doc/decisions.md` and link the accepted risk in its owning
  provider issue rather than repeating it; add shipped behavior to the changelog. Diagnose and
  remove the previous measurement's default-profile helper directory before handing over.
- **Approved next live budget:** run the four API-key scenarios with `gpt-6-luna`
  and reasoning `none`, after an unpaid replay verifies the test-only admission guard. Preserve
  the fourteen spent requests; the approved cumulative ceiling is thirty, allowing sixteen
  further requests, zero CLI retries and a USD 0.25 working budget.
  Reject bodies over 128 KiB, approved in place of 32 KiB since the pinned CLI sends about 55,865 bytes with a fresh
  profile, and cap output at 1,024 tokens. A planning allowance of 64,000 input
  tokens per request at USD 0.125 per million, treating all input as cache writes, plus output
  at USD 0.50 per million gives USD 0.136192 for sixteen requests. This is an estimate, not a
  provider-enforced monetary limit; stop if usage invalidates the allowance or a failure repeats.
  Twenty-seven of the thirty are spent; a further paid API-key run needs a new approval.
  The next ChatGPT stage uses a fresh attended scratch login and published Porelo
  `0.5.1~snapshot.339` or later. Its ceiling is ten upstream Responses admissions in total and
  one host-side renewal attempt; each model turn has a 90-second bound, no retries and no hosted
  tools. It consumes subscription allowance; its marginal credit cost cannot be established
  without the account's billing terms. A visible turn can make several model requests, so the
  gate counts upstream admissions rather than completed turns. The API-key ledger stays separate.
- **Agreed host-renewal route:** task A starts with a brokered turn and a
  containment scan against the nonempty real access/refresh-token reference set. Stop task A,
  run `porelo vault renew chatgpt`, then start a new task B. The command marks the vault entry
  due without making a request; task B's first broker request performs the host-side renewal.
  Reserve at least one of the ten admissions for task B. Require its reply, one renewal line in
  `vault.log` and an advanced `expires_at` in `vault list`. An already running broker retains its
  token, so reusing task A does not test this
  route. Rotated-token storage before use is covered by the host's renewal tests; an additional
  live renewal is outside this run's scope.
- **Expiry contract:** `Credential.EXPIRES_AT` is epoch milliseconds, as supplied by
  `CodexCredentialExtractor`; host collection converts it to the ISO instant stored in the
  vault's `expires_at` setting. The host renewal reads and updates that setting. These two
  representations do not require an adapter change.
- **Containment evidence boundary:** retain the initial access/refresh-token references only in
  the supervising process's memory through the accepted login collector route. The public
  credential helper can supply the current access token on a pipe, including after renewal.
  A rotated refresh token is deliberately not readable through a public command. Task B can be
  scanned against the new access token and the retained initial references, but absence of the
  rotated refresh token is a host-code finding, not a live search result. Do not add a command
  that reveals hidden refresh grants for this test.
- **Request shape:** the broker forwards the Responses body without adding or stripping
  `max_output_tokens`; its acceptance by the subscription endpoint is not established. The
  ChatGPT admission guard preserves the CLI's body and does not inject that unverified field.
  The subscription run is bounded by admission count and turn time, separately from the
  API-key gate's output-token and monetary planning limits.
- **Decided loop-record capture:** porelo
  [B244](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b244) may need a real
  refused-tool record. The first task A prompt asks for `bash -c` without an argument, then
  `bash` alone, and both outputs. The approved allowance is ten upstream admissions across both
  tasks, including renewal in task B, with the same 90-second turn bound and no retries or hosted
  tools. Copy the newest `.codex/sessions/**/*.jsonl` into the handover only after a scan against
  nonempty phantom- and real-token reference sets finds neither. Stop on a repeated failure.
  No login or model request starts before the run timing and its VM slot are arranged.

## Acceptance

- It ships as its own binary and its own package, in this repository, discovered without
  Porelo changing.
- Nothing outside its own directory names it, enforced by the build.
- A task authenticates without anyone signing in inside the container.
- The real credential is never in the container, seen by searching the container for it.
- Session acceptance covers an unattended reply and continuing its recorded session without a
  second login; terminal acceptance reaches work in a fresh task without unanswered first-run dialogs.
- Waiting acceptance distinguishes actual work, input required and at rest from measured screens;
  a dialog or a failed turn is never classified as idle success.
- Wake acceptance delivers an unread mailbox message to an idle task and obtains its computed
  answer; a message sent while busy is handled after work, without duplicate wake input or a
  competing agent process. A missing or unsafe `at_rest` declaration must fail the relevant guard.
- Broker acceptance observes a request at Porelo's actual broker with only a phantom token in the
  task. Credential acceptance obtains a real model reply through that broker with the nonempty
  real-credential reference set absent from output, metadata, profile and exported rootfs.
- API-key and ChatGPT authentication/refresh results are reported separately; an API-key success
  does not satisfy the ChatGPT cases. Provider-free checks must not require a paid credential.

## To be checked

- Complete the final added-diff reading and arrange the attended login timing. Native adapter
  discovery, device-auth login preview and the shell-task command preview pass on the local VM
  without starting a login or container. No admitted request resets the cumulative limit for this
  run; the live subscription ledger remains separate from unpaid replay accounting.
- **Unpaid ChatGPT admission replay:** 23 host cases pass against local fake UNIX-socket brokers,
  with no sign-in or upstream request. They cover the ten-admission ceiling, renewal reservation,
  actual broker-phase binding, restart/concurrency accounting, the ninety-second armed window,
  preserved request bodies, refused tools and repeated failures. Runtime mutations make the
  spending, reservation, phase, ledger, body, clock and repeated-error guards fail. An overlong
  window, malformed reasoning block and missing broker-phase binding fail before their guards
  are added. Review also reproduces and removes inherited API-key output/usage constraints:
  absent subscription usage does not hide a successful reply or disable the ten-admission cap.
  The replay uses the published provider's `/backend-api/codex/responses` path; the inherited
  API-key path is seen failing before correction. The API-key admission ledger is unchanged.
- **Unpaid integration evidence:** nine host cases guard nonempty initial access/refresh
  references, protocol framing and cross-block leak detection; six guard run ownership and
  restoration of original override bytes and modes. Runtime mutations fail for missing references,
  dropped block overlap, replacement of initial references, partial frames, wrong run ownership,
  changed overrides and changed backups. A renewed access token in retained turn output, a
  stopped task without a container and a cleanup-command failure stranding overrides are
  reproduced before correction. Current access is collected before checking or retaining turn
  output; the task scan includes mounted directories and both log streams. An authenticated
  catalogue redirect is seen forwarding a fixture token before the probe refuses redirects; its
  original-authority positive control still selects a supported model. The unmodified native
  adapter forwards synthetic
  `ExtractCredential` values through the test-only collector; both references remain in the
  supervising process's RAM, neither reaches stdout/stderr, and interruption removes its sockets.
  A runtime parent with mode `0775` beneath a private `0700` ancestor is also reproduced and
  accepted without relaxing the socket's `0600` mode or its owner/peer checks.
  On the local VM, published Porelo `0.5.1~snapshot.339` serves ten fixture requests to a loopback
  stand-in through its actual broker. Task A's ninth-admission ceiling preserves task B's one,
  the eleventh is refused, and the host alone substitutes the fixture credential. A reproduced
  interrupt leaves the Core child's internal socket; the guard now removes only the recorded
  socket after reaping that child, and porelo
  [B302](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b302) records the
  confirmed host defect and its proposed signal-cleanup fix.
  The intermediate fixture omits the expected bearer prefix; the final launch supplies the
  provider's prefix and passes. Cleanup restores Porelo `0.5.1~snapshot.307`, original override
  hashes and modes, with no task, container, scratch vault or runtime socket left. Neither live
  ledger changes. No real login, real token or subscription/model endpoint is involved. This
  does not establish the attended controller's model, renewal or full task-level acceptance.

- **Attended first-turn refusal:** device-code login and OAuth collection complete, but the
  first model turn is stopped locally with zero upstream admissions. Cleanup restores snapshot
  307, the original adapter hash and all account overrides, leaving no task or container.
  The failed CLI record was not retained, so its exact body size is not measured. A credential-free
  replay of pinned CLI `0.163.0` emits 55,865 bytes with a fresh normal profile, exceeding the
  guard's inherited 32,768-byte cap. The same CLI through that cap is refused before admission;
  with the corrected 128 KiB cap it reaches a local stand-in unchanged and exits successfully.
  Neither observation is a real provider reply. The ten-admission, ninety-second, local-tool and
  renewal-reservation guards remain unchanged. Refusals retain only a fixed code and numeric
  size bounds, never request paths, headers or bodies. A failed turn now retains private CLI
  diagnostics only after current real-token and phantom-token scans pass. The exact failed
  zero-admission ledger must be preserved before the approved repeat; any used allowance or
  changed ledger is refused. A new attended login is needed because its disposable vault is gone.

- **Untyped subscription answers are withheld by the broker:** an attended walk through Porelo's own
  unattended start, on account-local Porelo `0.5.1~snapshot.341` with adapter SHA-256
  `d0ed3445d62f6a597bfcec230deb024a703a974d5174db743c6ca3225821adca`, signs in and stores
  `chatgpt` as `oauth`. Task A holds only a phantom token. The task's `vault.log` records 30
  `POST /backend-api/codex/responses -> 200 from the provider`, each followed by `answer withheld:
  no content-type`, and the CLI receives 502 each time. No answer reaches the container, and the
  repeated test's single 502 has the same cause. The same endpoint's 401 carries `application/json`.
  The container scan, reply, renewal and the porelo
  [B244](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b244) session
  record did not run. The cleanup readback finds no task, container, image or scratch vault left.
- **Retries multiply a withheld answer:** offline, the pinned CLI with the adapter's profile and
  default retries sends 30 requests for a 502, 6 for a 403 or 422 and 1 for a 400, against a local
  stand-in; none starts a sign-in. The 30 admissions above exceed the earlier ten-admission
  allowance; the walk now stops its task at the broker's first withheld or error line. A broker
  correction that reads an untyped body opening as JSON or server-sent events, and answers a
  withheld one with a non-retried status, belongs to porelo; the next live run waits for it.
- **Brokered ChatGPT turn and host renewal pass** on account-local Porelo `0.5.1~snapshot.343`,
  which carries porelo [B303](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b303),
  with the same adapter SHA-256 and a fresh attended login. Task A's two admissions are both
  `-> 200 from the provider` with no withheld line, and Codex replies in 38 s; `bash -c` exits 2 and
  `bash` exits 0. No real access token is found in the exported root filesystem or in the
  task's mounts, and the profile holds only the phantom token, with no `auth.json`. After
  `porelo vault renew chatgpt`, task B's broker logs exactly one host renewal, then one admission
  returns 200, and `expires_at` advances. The scanned session record holds the refused
  `bash -c` call for porelo
  [B244](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b244). The walk used
  five of its ten admissions: an earlier attempt stopped after two, because its scan could not
  archive a single-file mount. Cleanup leaves no task, container, new image or scratch vault.
- **The operator's own walk passes** on 2026-10-27 at 18:24Z, with the same snapshot, adapter and walk
  (`run-20261027T182403Z`): task A two admissions, both 200, with the answer after 44.3 s; task B one, 200,
  after 17.4 s; no real token in either container; renewal one broker line, `expires_at` from 18:24:30Z
  to 18:25:38Z; nothing left.

- **Approved live-run preflight:** on `1b41511`, the model-picker fixture reaches the real
  composer, opens `/model` and is correctly reported as waiting. The published acceptance kit
  then refuses the fixture's `Escape` key name; its supported name is `Esc`. This is corrected
  in the fixture, with the failed selected case retested before the three unrun cases. The
  test-only guard is observed on the task's actual broker socket; inspected limits are 2 GiB,
  one CPU and 2,048 pids. Fifteen unpaid guard replays pass, with five spend-bound mutations
  seen red; a stale local admission snapshot is refused against a durable cumulative ledger.
  A separate unpaid cleanup replay refuses a different run's ownership before any action.
  Both additional guards were seen red first. No new paid request occurs: cumulative admissions remain fourteen of thirty.
  Independent cleanup restores snapshot 307 and matching override backups, with no task,
  container or attributed scenario vault left.
- **The four API-key scenarios pass** on `c6356be`, whose tree is `634df1a`'s, against account-local Porelo
  `0.5.1~snapshot.343`, adapter SHA-256 `d0ed3445d62f6a597bfcec230deb024a703a974d5174db743c6ca3225821adca` and
  acceptance kit `0.5.1-20261027.160227-39`, with `gpt-6-luna`, reasoning `none`, zero CLI retries and the test-only
  gate on each task's actual broker socket. The model picker spends no admission, session continuation across a stop
  and restart two, idle wake five and busy wake six, the last of them admitted while busy wake's task was being
  removed after the scenario had passed; it recorded no status and is counted as spent: twenty-seven of thirty. The
  twelve recorded requests report 123,603 input tokens, 73,268 of them cached, and 728 output tokens; none exceeds
  11,851 input. Cleanup restores the account's CLI by hash, with no task, container, scenario vault or new image left.
- **Porelo's waiting reading trails the screen by up to one poll.** With a fixture key and no gate, so that no model
  request can be made, `Esc` closes the picker every time; a capture shows the picker on screen while
  `porelo task status` still reads not waiting, and the reverse after `Esc`. The kit's "not waiting" step judges its
  first poll, so the earlier fixture failed in two of three runs, after zero admissions. The fixture now waits up to
  30 seconds for `porelo task status` to read not waiting before the kit's step must hold it for eight; three runs
  pass. The kit's step itself belongs to porelo.
- **The API-key gate's unpaid replay:** 26 host cases and one cleanup-ownership case pass. Every guard added for this
  run was seen red by mutation: the 128 KiB cap, both socket option forms under the account's runtime directory only,
  a broker started after leading options refused, a missing or linked ledger refusing the run instead of restarting
  from a seed, a persistent stop holding at equal counts, a non-object `reasoning` and deeply nested JSON refused with
  a stop, and the inherited zero-admission size refusal released exactly once. A refusal keeps only a code and sizes.
- **Published-snapshot recheck:** source `9ad9973` runs the unchanged missing-authentication
  scenario in the VM's own account against published Porelo `0.5.1~snapshot.318`, with package
  SHA-256 `5cbcd36f541eb0d078f84cc84ab9c900374681d79ea233c51ecb6af0b5d31359`
  verified against its published index. The native adapter has the previously verified SHA-256
  `d04907c57d0070b0c193dc9b66b5ba24cb068ce1da73e442f0a4b51c3103a375`.
  The kit creates and unlocks its own empty vault and removes `chatgpt` before starting the task.
  The CLI still reports an `api-key` stored for `codex`, rather than missing authentication;
  the unchanged assertion fails. No credential-type override, real credential, sign-in or model
  request is used. All 69 unit tests pass; this one acceptance scenario fails. The other nine are
  filtered out: "Porelo discovers the separately installed adapter", "A terminal lists the adapter
  by its label", "Previewing a login does not start authentication", "The deliberately refused
  telemetry host is visible", "A script is not asked an interactive launch question", "An unattended
  task remembers across a stop and restart", "The model picker requires input and closing it
  restores readiness", "A message to an idle task obtains a computed answer" and "A message
  arriving during work is handled after the turn". The nine are not acceptance passes. The
  controller measures 24.3 seconds from installing the overrides through restoration, excluding
  artifact transfer. It restores every temporary override and verifies its hash; readback sees
  the original snapshot 307, no tasks, no containers, only the default project and no scenario
  fixture. Evidence is retained in the source commit's handover directory under
  `cx01-missing-auth-318`.
- **Corrected-snapshot recheck:** the unchanged scenario passes against published Porelo
  `0.5.1~snapshot.323`, package SHA-256
  `9c6c3f4bef5df3b7d464235ea6eb61c4ee3eae89402c138c243f96d91ee54a75`, verified
  against its published index. Source, native adapter and procedure are the same as the 318
  measurement. The correction from porelo [B255](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b255)
  is verified: the empty vault is no longer classified as a stored API key, the unchanged
  missing-authentication assertion passes and the refusal names `chatgpt`. All 69 unit tests pass;
  one acceptance scenario passes and the same nine named above are filtered out. Restoration and
  independent cleanup readback pass again, with 37.8 seconds measured by the controller on the
  same basis. Evidence is retained separately under
  `cx01-missing-auth-323`. This removes the upstream blocker; it is not complete provider-free or
  live acceptance. The [attended login walk](../doc/index.md#attended-chatgpt-login-acceptance)
  has been checked through scratch-vault creation, preview and the real device-code page on
  snapshot 323, without browser approval or a model request. Two Ctrl+C attempts did not cancel
  the login; stopping only its own container with `podman stop --time 5` returned control with
  exit 137, removed the container and left an empty vault with Codex signed out. The scratch
  vault was locked and removed. The cancellation finding is handed to the owning Porelo agent;
  the account-local verified binaries were used for the attended walk. Complete brokered
  ChatGPT model/renewal acceptance and the four credential scenarios remain pending.
- **Transient failure diagnosis:** the first attended walk reached the device-code page,
  but the CLI exited with `device auth failed with status 503 Service Unavailable` before
  browser approval completed. Porelo reported exit 1 and no credential; the run's container
  exited 1 and was removed, confirmed by Podman's events and an absent-container log lookup.
  Its response headers and body were not retained, so the original 503's exact origin is unknown.
  An unauthenticated POST with invalid dummy values to
  `https://auth.openai.com/api/accounts/deviceauth/token`, from the same login image on the
  host network, returned HTTP 530 with `cf_route_error` from Cloudflare and no proxy variables.
  Subsequent direct-VM and same-image curl probes returned HTTP 404; the latter's application
  error was `deviceauth_not_found`. The direct connection verified a TLS 1.3 certificate for
  `auth.openai.com`. This suggests a transient provider-facing routing failure, rather than a
  Porelo egress refusal; it does not prove the original 503's origin or a successful retry.
  The pinned CLI retries poll responses 403 and 404 but treats 503 as terminal. No second login
  or model request was made during diagnosis. Disposable probes were removed; the attended
  scratch vault was reused for a fresh login code without reinitializing the vault.
- **Attended login passes on retry:** terminal readback confirms that the device-code login
  completed and Porelo stored `chatgpt` as `oauth`. The vault list includes `client_id`,
  `expires_at` and `token_url = https://auth.openai.com/oauth/token`. The account-local adapter
  is discovered with `credential: chatgpt` and `signed_in: true`, with the vault open. This is
  attended terminal evidence, not an independently repeated browser approval. No token or account
  identifier is retained in the issue. Cleanup readback confirms the scratch vault is absent,
  no login container/process or temporary collection directory remains, and all eight recorded
  account overrides are restored with matching backup hashes. Independent commands report the
  original Porelo snapshot 307, no tasks and only the default project. Successful collection
  does not establish a ChatGPT brokered model response, renewal or the four live scenarios.
- **Published contract available from porelo [B242](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b242):** the definition now uses
  `session_meta/payload.id` for attended files, distinct from unattended `thread.started/thread_id`,
  and `item.completed` with `item.type: agent_message` for `item.text`. Four adapter extraction
  guards fail on the previous definition. The complete Porelo continuation path still needs VM
  acceptance; this replay is not an end-to-end pass. Packages require Porelo 0.5.1~snapshot.317.
- Exact implementation commit `48d748244c9e032055711dda8b6cad373bfb70d1` passes the complete
  provider-independent workflow in a real Porelo task on Core `bf0a07f1`, local snapshot 933,
  using the published agent API snapshot `0.5.1-20261022.121950-17`: 46 unit, three native,
  four package and five document tests, all five shared checks, both publisher pins and the
  actual scheduled artifact-check steps. Six provider-free VM acceptance scenarios pass.
  Four credential scenarios are excluded: "An unattended task remembers across a stop and
  restart", "A message to an idle task obtains a computed answer", "A message arriving during
  work is handled after the turn", and "The model picker requires input and closing it restores
  readiness". The whole run takes 593.9 seconds; no model request or sign-in occurs. Independent
  readback confirms no task, container, image or private run/wrapper remains, only the default
  project, with the original adapter restored. Host and VM artifact hashes agree. Sanitized
  logs and artifacts live in the exact commit's handover directory.
- The changed definition, package compatibility guards and tests were read against `graal`
  2026.910.3, and `java-code-review`, `test-quality`, `security-audit`, `concurrency-review`,
  `clean-code` and `solid-principles` 2026.906.3. Extraction uses the published API rather than
  duplicating its parser; the slice adds no credential storage or concurrent state. The native
  and package checks verify the resulting executable and packages, not configuration alone.
- **ChatGPT collection route confirmed:** reading `AgentLogin.run` and `watchFor` finds `podman.copyOut`
  into host temporary directories before extraction; independently, the published
  `CredentialExtractor` and `ExtractCredential` protocol accept only `configDirectory`. The
  measured Codex login uses an encrypted Secret Service rather than `auth.json`. The owning
  repository confirms that no encrypted/in-memory collection route exists and directs the
  adapter to the standard file route: login writes `auth.json`, it is copied to an owner-only
  host temporary directory, read into the encrypted vault and deleted. Both the container's
  writable layer and the host copy may be on disk or swapped. Removal does not establish
  secure erasure. The login-only exposure is accepted in the
  [collection decision](../doc/decisions.md#login-collection); ordinary tasks remain phantom-only.
  Hardening in the owning repository is not a prerequisite and does not establish encrypted
  transient storage. The adapter can implement the existing route without a new interface.
- ChatGPT adapter code now declares device-code file login and preserves access, refresh,
  account and expiry metadata through `CodexCredentialExtractor`. The pinned CLI archive's
  publisher digest is verified before reading its public client ID, token endpoint and login
  options. Twenty extraction cases and the login/provider wiring guard compile; the unchanged
  adapter fails nineteen extraction cases and the default-provider guard. Two further token
  control-character cases fail before their guard. The missing-login and parser-cause guards
  also fail under deliberate compiling mutations. All use synthetic tokens, never a real login.
  The complete host gate passes 69 unit, four native and four package checks with no skipped
  tests; the native protocol check reads the actual extraction result and all five renewal/account
  attributes, and fails against the retained pre-extractor native binary before passing against
  this implementation. Five document tests and all five shared checks pass. On exact implementation
  `1141b3bc483fe72fe651b8f110dd139bf39b1e95`, the VM task passes 69 unit tests, four native checks,
  four package checks, five document tests, all shared and pin checks and the scheduled artifact-check
  steps, with no skipped tests in those gates. Provider-free acceptance passes five scenarios and
  fails the missing-authentication scenario; the four credential scenarios listed above remain
  excluded. With a newly created empty vault, Core's credential selection defaults an absent entry
  to `api-key` and refuses that kind for ChatGPT before reporting the missing credential. The test
  is not broadened to accept the misleading stored-key refusal. The owning repository confirms
  the defect as porelo [B255](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b255);
  its correction passes the published-snapshot recheck above. An explicit `--credential-type=oauth` would hide the defect
  by testing a different start and is not an interim workaround. The complete controller exits non-zero
  after 386.4 seconds. Independent cleanup finds no tasks, containers or images, only the default
  project, no temporary wrappers/provider definition or run/build/upstream directories, and the
  original adapter restored. ChatGPT brokered model and renewal acceptance remain pending. No real sign-in or model request
  occurs in these checks. The changed scope is read against `graal` 2026.910.3
  and the six Java review skills 2026.906.3; token-control input was fixed only after its test failed.
- The complete startup and first-run dialogs in the packaged task environment; a noninteractive
  model turn does not measure those dialogs.
- The unattended Porelo launch path. The real-key broker
  measurement below invokes the CLI inside a shell task; it is not unattended-launch coverage.
- The actual Porelo attended/unattended launcher, complete session, waiting, terminal and
  computed-answer wake acceptance, and standing instruction delivery. The fresh native-generated
  profile reaches the composer, but the observed
  composer and selection markers do not establish end-to-end mailbox delivery.

## Live wake acceptance

- Source `933cd10957f4af833487255570c677b2fab5d209` passes the complete applicable VM task
  Build: 42 unit tests, three native checks, four package checks, five document tests, all five
  shared checks and both publisher pin checks, with no skipped tests in these steps. The native
  artifact has SHA-256 `7a3e38117b4feebe0b9ced77b669508db4a60375edd4ddb335e34913b353db05`.
  Cross-repository citations use the owning repository's verified local issue index; the new
  index anchor and shared tooling must be published before handover for a push.
- Six provider-free scenarios pass against the VM-built adapter on `69eba7d` and `4c878ce`.
  Four credential scenarios are
  excluded from that run: unattended session continuation, model-picker waiting, idle wake and
  busy wake. Exclusion is not a pass of any of those scenarios.
- The idle credential scenario reaches the real attended composer without a startup question.
  The scenario's daemon delivers the mailbox notification. Nine Responses requests return HTTP
  200 through the actual broker, but `gpt-5-nano` repeatedly supplies unsupported `cmd_args` to
  the shell tool. Two calls return the same `bash: -c: option requires an argument` failure;
  subsequent bare shells return no output. No computed answer appears before the 120-second
  controller bound. HTTP success does not establish tool or wake acceptance.
- The nine requests report 74,067 input tokens, including 59,776 cached tokens, and 1,000 output
  tokens with no reasoning tokens. The inherited admission count remains nine of sixteen;
  another model must not reset it. Busy wake, session continuation and model-picker acceptance
  have not run. An earlier fixture failure consumed zero requests because duplicate image
  mappings prevented task creation; the exact existing mapping is now configured instead.
- An unpaid UNIX-socket replay reproduces the command-error loop and fails before the guard is
  added. The corrected test-only guard stops before another upstream request after two distinct
  tool calls return the same error, without counting a resent call twice or treating silent
  success as failure. All six replay tests pass. This guard is not the channel watcher and does
  not substitute for shared acceptance-kit loop detection.
- The outer build controller times out waiting for its host acknowledgement after the Build
  steps pass. Its cleanup refuses changed overrides rather than overwriting them. Exact backup
  hashes permit explicit restoration: the original adapter is restored, temporary wrappers are
  removed, no tasks or test daemon remain, only the default project remains, and owned images
  and run directories are absent. The interrupted scenario's remaining encrypted vault is
  attributed by account, kit layout and recorded run window, locked, then its exact two files
  and empty directory are removed. No process environment or credential content is inspected.
- The bounded `gpt-6-luna` attempt ran after payload approval, against verified Core snapshot
  928, with reasoning `none`, zero retries, no web search and no repository sources. Five more
  Responses requests returned HTTP 200 through the actual broker: 53,343 input tokens, including
  33,006 cached, and 430 output tokens, none reasoning. Admission remains fourteen of sixteen;
  a further attempt must not reset that budget.
- The daemon delivered the wake and the real assistant's `response_item/message`, with role
  `assistant` and phase `final_answer`, said "Sent the direct reply: 5555." The CLI returned to
  its composer. This is a computed model answer, but the strict driver still exited 1: it
  inspected only `event_msg/agent_message` and required digits-only output. Its failed predicate
  is retained, not rewritten into a pass. Return delivery, busy wake, session continuation and
  selection acceptance remain unproven. Cleanup restored the original adapter and left no
  owned task, daemon, wrapper, vault or run directory. No further paid request is authorized
  by the definition-only replay.

## Build tooling gap

The bridge is removed. Shared `check-pin --pin codex`, `check-pin --pin helper` and two
`add-fetched-cli` calls using those pins and `--pom pom.xml` replace it. Recording uses a Maven
plugin dependency, never an adapter/test dependency. The verified `porelo-buildtools` handover
`a3af091` supplies the JAR and flattened POM; its manifest SHA-256 begins
`5e879c593fc382fa`. Both named pin checks receive `--definition target/classes/agent/codex.yaml`;
they require no legacy digest/upstream metadata. `FetchedArtifactsCheck` reads the actual generated bill and requires exactly
both fetched executables and their version, digest, URL and provenance. Disabling recording made
the guard fail with zero fetched components; restoring recording made it pass. A gathered build
of this change needs the updated shared-tooling snapshot published first.
Both named publisher checks pass against the actual upstream after declaring `rust-v` as the
pin's tag prefix: the version, download URL and each independent publisher digest agree.

## API-key setup verification

- `CodexContainerSetup` consumes only the published `SetupContext` contract and returns owner-only
  `/home/agent/.codex/config.toml`. The custom provider uses the supplied endpoint unchanged and
  its phantom bearer, with Responses, OpenAI login disabled and WebSockets disabled. There is no
  upstream URL, provider environment-variable name or real credential in the setup.
- An empty endpoint/token pair produces no credential file for an unbrokered shell. Incomplete
  pairs, unsafe bearer characters, non-HTTP URLs, user information, query/fragment fields and
  unsafe endpoint characters are refused without echoing the input or chaining parser diagnostics.
- The missing-file and fail-closed guards failed against the original empty setup. An intentional
  owner-only mutation failed the credential-permission guard; the old native executable failed
  the new URL-broker declaration guard. Both are restored and green in the complete build.
- Host `-Pnative,dist verify` passes 36 unit tests, three native checks and four package checks,
  none skipped. Both shared recorders populate the actual bill; native protocol output includes
  the OpenAI default, Responses dialect, URL endpoint and `ab.chatgpt.com` refusal. This is host
  verification, not a real-key broker acceptance or a VM workflow handover.

## Provider-independent verification

- The updated full Build on `cb30109635cd755783322362f37fa2f9a231a236` passes inside a real
  Porelo task: 36 unit tests, three native checks, four package checks, five document tests,
  all five shared checks and both actual publisher pin checks, none skipped. The separate host
  acceptance run passes both provider-free scenarios against the VM-built native adapter,
  SHA-256 `20c6ac52d3edba7e1c3dc536ceb8b800c3a2275d8acfefb36bdda72e88ef2074`.
  No provider key is read and no model is called. The guarded runner and independent read-back
  confirm no tasks, only the default project, no images and no run directory; the original
  adapter is restored with its original hash. Evidence is retained in that commit's handover.
- The shared pinned GraalVM builds the native adapter with `-march=x86-64`; 32 unit tests,
  three native linkage/protocol checks and three package-version checks pass, with none skipped.
  Both publisher version/URL/digest checks and all five shared document/workflow checks pass.
- The native executable is discovered from the VM account's own adapter directory by
  `porelo 0.5.1~snapshot.307`. Both published-kit acceptance scenarios pass: script discovery
  and attended-terminal listing. These scenarios use no project, vault, credential or paid model.
- Deliberate mutations make guards fail for incorrect completion/command behavior, absent profile
  placement, a drifting helper digest, a wrong recorded bill digest, terminal escape injection,
  an unmarked package and a workflow skipping tests. The mutations are restored before full runs.
- The Claude Code, OMP and Pi implementations were compared read-only. The single-module layout,
  `YamlAgent` entry point, stateless formatter, service descriptor, null-marked main package and
  shared Maven profiles follow that structure. Codex-specific differences are indexed in
  `doc/decisions.md`; no other adapter or Porelo implementation is changed.
- Code review used `graal` 2026.910.3 and `java-code-review`, `test-quality`, `security-audit`,
  `concurrency-review`, `clean-code` and `solid-principles` 2026.906.3. Mutable per-task state and
  credentials are absent from the adapter; the native protocol check measures resource inclusion.
  Credential containment and the complete first-run dialogs are not claimed by this part.

## Attended boundary measurement

- The pinned CLI is measured in a real terminal in `/workspace` in the disposable task. A fresh
  profile asks for folder trust despite the sandbox bypass. After confirming the reviewed task
  workspace, its own configuration records `[projects."/workspace"]` with `trust_level = "trusted"`.
  The setup now writes that trust entry only for the validated workspace from `SetupContext`.
  Missing trust, unsafe quoting and invalid paths each fail against the original implementation.
- The idle composer shows `? for shortcuts`. The same footer remains during a request, beside
  `esc to interrupt`; a footer-only at-rest declaration fails against that actual busy screen.
  Restoring the busy exclusion protects the wake boundary. Reference screens retain the terminal's
  last twenty lines, matching the region read by Porelo, without pinning a model or CLI version.
- Folder trust ends `enter continue · esc quit`; the measured model picker ends
  `enter select · esc back` and removes the composer footer. Only these observed selections are
  declared as waiting. They do not establish every tool question or approval dialog.
- The busy capture uses a bounded localhost-only HTTP fixture with no real key and no upstream
  forwarding. It replays an already measured provider refusal after holding the request open;
  it is not a successful model turn or a substitute for computed-answer wake acceptance.
- The full VM workflow on `9f8dc17` passes 42 unit tests, three native checks, four package
  checks, five document tests, all five shared checks and both actual publisher pin checks.
  Both existing provider-free acceptance scenarios pass against its VM-built adapter, with
  no skipped tests. The actual native ContainerSetup protocol generates a parseable owner-only
  TOML profile with exactly `/workspace` trusted; the fresh CLI reaches its composer without
  a trust or login question. That profile uses only a dummy token and a loopback fixture.
- A real session file is nested below `.codex/sessions/YYYY/MM/DD/` and starts with
  `session_meta`, whose `payload.id` is the resumable UUID. The filename stem is refused as
  an ID. With `--no-daemon`, resuming the UUID restores the transcript and composer after the
  first CLI releases its lock, without a model call. Without that flag the standalone CLI
  refuses to resume because no complete local package is installed. Both command guards fail
  before the launch-prefix correction. Public-contract agreement is needed for extraction;
  this manual UUID check does not establish Porelo's automatic attended continuation.
- `terminal.feature` adds provider-free refusals. Credential-gated `session.feature`,
  `waiting.feature` and `wake.feature` cover unattended continuation, the measured model picker
  and computed-answer idle/busy delivery. These scenarios are not claimed as live passes until
  they run with an explicit model and credential; binding-only dry runs are reported separately.
- The binding-only dry run resolves all ten scenarios through the published kit, with no custom
  glue. It executes no steps and establishes neither a real session continuation nor a wake.
- The first idle-wake live attempt stops before task creation with duplicate `image` keys:
  the published kit appends text after its own base-image mapping rather than merging mappings.
  No Responses request is made. The corrected fixtures validate ownership and the exact initial
  image line, replace that line with the pinned digest and extend the existing mapping, then
  commit and re-follow it through the public CLI. The zero-request failure is retained separately
  from the approved one-time retry; it is not a model or wake result.

## API-key live measurement

- The packaged setup is verified on source `6b08c57` with the Core `7b2f639c` handover,
  `porelo 0.5.1~snapshot.922+local.20261021T191217`, in the VM's own account. A guarded shell
  task uses 2 GiB, one CPU and 2,048 PIDs, checked in the actual container metadata. The Core
  binary and hooks are temporary account-local files; nothing machine-wide is installed.
- The encrypted scenario-owned vault receives the real key through SSH standard input, with
  no key argument, environment value or copied VM key file. The authenticated model list returns
  HTTP 200 and contains `gpt-5-nano`. The CLI invocation disables web search, uses minimal
  reasoning, sets both retry limits to zero and is bounded at 60 seconds. Minimal reasoning with
  the CLI's default web-search tool was refused once with HTTP 400; that run stopped immediately.
- The task's generated user-profile TOML parses, carries a minted phantom bearer and is mode
  `600` owned by `agent`. One corrected CLI turn exits 0 with exactly `PORELO_BROKER_API_OK` and
  `turn.completed`; Porelo's actual vault broker records exactly one
  `POST /v1/responses -> 200 from the provider`. No temporary relay stands in for this broker.
- The nonempty real-key reference is absent from CLI/helper output, container metadata, the
  profile/workspace archive (3,809,280 bytes) and exported rootfs (577,183,744 bytes), compared
  in memory before logs are retained. The explicit negative lookup of `ab.chatgpt.com` exits 2.
- CLI usage reports 7,124 input tokens, no cached input and 26 output tokens. At the checked
  standard rates, USD 0.05 input and USD 0.40 output per million, the estimate is USD 0.0003666,
  not an invoice measurement. Codex warns that it uses fallback model metadata for this model;
  the successful bounded turn does not establish every feature with it.
- Cleanup and an independent read-back confirm no tasks, only the default project, no new
  task/base images and no run directory. The original adapter's hash matches its backup;
  account overrides are restored and only the test vault's scoped unlock is forgotten. Sanitized
  evidence and the runner are retained under the source commit's handover directory, `cx01-api-3`.
- This measures the packaged setup and real-broker containment in a shell task. It does not
  establish Porelo's unattended launcher, complete first-run dialogs, session/waiting/terminal/wake
  acceptance or ChatGPT authentication/refresh. The earlier relay-only transport measurement
  follows separately for that narrower finding.

- The VM reports `porelo 0.5.1~snapshot.307`; the independently downloaded Codex CLI and code-mode
  host are version 0.163.0 and match the two pinned publisher digests. The authenticated model list
  returns HTTP 200 and includes `gpt-5-nano`, the lowest-priced listed general text model checked
  for this probe: standard input USD 0.05 and output USD 0.40 per million tokens.
- A disposable host-side relay reads the real API key through SSH standard input and sends it
  only to `https://api.openai.com/v1/responses`. The CLI receives an isolated environment, an explicit
  disposable `CODEX_HOME` and a dummy token. No login is performed or real key stored in that profile. The relay
  permits one Responses request, disables tools and storage, caps output at 256 tokens and selects
  minimal reasoning; both CLI retry limits are zero.
- Exactly one model request returns HTTP 200. The CLI emits an `agent_message` whose complete text
  is `PORELO_API_OK`, followed by one `turn.completed`, and exits 0. Upstream usage reports 4,201
  input tokens and 23 output tokens, 4,224 total. Treating all input as uncached gives an upper
  standard-rate token-cost estimate of USD 0.00021925, not a measured invoice amount.
- The nonempty real key is absent from the CLI request, output and disposable profile, checked
  in memory before any client output is shown. The key is neither copied to a VM file nor placed
  in a command-line argument. The disposable profile and this run's downloads and scripts are
  removed; the existing account-local adapter is retained. No project, vault or shared VM install
  is changed, and the channel watcher stays stopped.
- This is a real API-key Codex CLI transport smoke test using a temporary host relay, not a test
  of the packaged Porelo broker or container containment. It neither substitutes for the ChatGPT
  OAuth provider contract nor clears the provider-integration dependency.

## Measurement

- On the Ubuntu VM in the isolated `codex` account, the standalone Linux x86-64 musl binary
  reports `codex-cli 0.163.0`. Its archive matches the published SHA-256
  `beb339e8ecd9ba057a6bb2dc81ad8ca474812edfbf0fc5056ca818797648dde4`.
- `login --help` offers `--device-auth`. The device-code flow reaches its browser URL and one-time
  code prompt in a detached terminal. Sign-in completes successfully, and a separate invocation
  reports `Logged in using ChatGPT`.
- The credential store is explicitly `keyring`, using an isolated Secret Service and a randomly
  generated unlock password passed through standard input. The profile directory is owner-only
  runtime `tmpfs`; the VM has active swap, so a plaintext `auth.json` there would not establish
  encrypted credential storage. The actual stored JSON has `OPENAI_API_KEY`, `auth_mode`,
  `last_refresh` and `tokens`; the token object has `access_token`, `account_id`, `id_token` and
  `refresh_token`. Only field names were observed in output.
- A normal model turn returns `CX01_OK`. Reasoning effort `minimal` is refused for the selected
  model; `low` succeeds. The standalone archive reports that `codex-code-mode-host` is missing,
  even on a successful text-only turn; tool execution is not established by this measurement.
- A second client uses a fresh profile, an environment without keyring access and only a dummy
  bearer token. Its custom Responses provider points at a loopback relay with
  `requires_openai_auth=false`, `supports_websockets=false` and retry limits zero. The relay alone
  reads the real login into memory, inserts the access-token bearer and `ChatGPT-Account-Id`,
  and forwards to `https://chatgpt.com/backend-api/codex/responses`.
- The relay observes one `POST /backend-api/codex/responses`, HTTP 200. The client receives
  `CX01_BROKER_OK` and exits zero. An in-memory comparison against the real token values finds
  none in the client profile or output.
- The same request succeeds from a rootless Podman container in `/workspace`, using Alpine
  `3.22.6@sha256:3e9b4b680bfc9fb5269227cffbd6d42be39fbf7c0b908123913864aa4447e764`,
  a read-only root filesystem, dropped capabilities and only the CLI and fresh profile mounted.
  Its provider uses a dummy token and the host loopback relay through slirp4netns. The real access,
  ID and refresh token values are absent from the exported root filesystem, mounted profile,
  output and container metadata, checked in memory against the nonempty token reference set.
  The container is removed and its absence is checked after the run. This is a transport and
  credential-isolation measurement, not the adapter's Porelo acceptance suite.
- Negative authentication uses an invalid bearer at the upstream. One response is HTTP 401;
  the client exits 1 and reports failure. Retry limits are zero.
- A refresh measurement marks only this run's encrypted login record stale. The CLI sends
  `client_id`, `grant_type=refresh_token` and `refresh_token` through an observing loopback endpoint
  to `https://auth.openai.com/oauth/token`. HTTP 200 returns replacement tokens, including a changed
  refresh token; the encrypted keyring retains them. A following real turn returns
  `CX01_REFRESH_OK`, exit 0. Replacement tokens are retained in the encrypted store before the
  response is delivered to the CLI, so an interrupted probe cannot lose a rotated token.
- A CONNECT-only observing proxy measures `chatgpt.com` and `ab.chatgpt.com` during a normal
  authenticated startup and tool turn. It does not decrypt TLS or establish the complete set of
  hosts used by every CLI feature. The custom dummy-provider client reaches the relay's Responses
  route without a separate ChatGPT session-validation request.
- Tool execution succeeds after installing the separate `codex-code-mode-host` artifact of the
  same version, verified against SHA-256
  `314263128ba6592e7e807c265deeaf928a692b72dcbfeb5e0b20c9423b703529`.
  The CLI runs `printf CX01_TOOL_OK`, observes that output with command exit 0 and returns
  `CX01_TOOL_DONE`; the missing-helper error is absent. The adapter's installer must provide both
  artifacts, and their two digests, rather than the CLI archive alone.
- Cleanup removes the probe login and terminates its private terminal, D-Bus and keyring processes.
  The exact probe directories, encrypted keyring files, binaries and the run's pulled image are
  removed. A later inspection found a helper-alias directory beneath the default `~/.codex`,
  left by the CLI-help invocation without an explicit profile. A no-credential `strace` of CLI
  help with `CODEX_HOME` set observes all helper aliases beneath that selected profile. The exact
  old directory and its four entries are removed and their absence checked. The task image sets
  `CODEX_HOME` explicitly for every invocation, including help.
