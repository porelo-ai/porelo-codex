# Decisions

| Subject | What holds | Detail |
|---|---|---|
| Provider boundary | ChatGPT is the sign-in default; API-key routing remains explicitly OpenAI | [Provider boundary](#provider-boundary) |
| Login collection exposure | Temporary plaintext files are accepted only while transferring a sign-in to the vault | [Login collection](#login-collection) |
| Two upstream artifacts | The task image verifies and installs both the CLI and code-mode host | [Installation](#installation) |
| Command grammar | Java assembles exec and exec resume commands | [Commands](#commands) |
| Conversation records | Published selectors distinguish run output from session metadata | [Conversation records](#conversation-records) |
| Task-local profile | The image sets CODEX_HOME explicitly | [Profile](#profile) |
| Terminal state | Measured composer, busy and selection boundaries gate wake input | [Terminal state](#terminal-state) |
| Artifact checks | Shared named-pin calls check and record both fetched components | [Artifact checks](#artifact-checks) |
| Build boundary | CI verifies packages without publishing or live credentials | [Build boundary](#build-boundary) |

## Provider boundary

The ChatGPT provider decision and accepted exposure belong to porelo-project's
[provider issue](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj54).
The adapter defaults to ChatGPT and speaks the Responses dialect through a URL broker. API keys
use the explicit OpenAI provider. Task setup writes only the supplied endpoint and phantom token
into owner-only user-profile TOML. Login collection is separate: a device-code login writes
Codex's `auth.json`, and the extractor returns its access token, account ID and renewal metadata
through the published credential contract. Porelo owns vault storage and renewal, not the adapter.
The API-key acceptance must use Porelo's broker; a standalone relay does not establish it.

## Login collection

Sign-in uses the standard throwaway login container and file collector. The CLI writes
`auth.json`, Porelo copies the configuration into an owner-only host temporary directory,
extracts the credential into the encrypted vault and deletes the copy. The container's writable
layer and the host copy can reach disk or swap; deletion does not establish secure erasure.

This login-only plaintext exposure is an accepted risk so the existing collection contract can
be used without a separate keyring or handoff protocol. It does not permit real credentials in
ordinary tasks, logs or build artifacts: tasks retain only phantom tokens. Runtime-backed
collection can narrow the exposure later, but only a route without plaintext filesystem copies
would remove it. The acceptance must be reconsidered if the collector retains copies beyond
login, permits other users to read them, or a safer supported route becomes available.

Login explicitly selects file storage and device authentication; it does not depend on a host
keyring, browser or loopback callback reaching the container. The extractor accepts only a
ChatGPT OAuth record, not an API key. It retains `account_id`, refresh token, access-token expiry
in epoch milliseconds, and the pinned CLI's token endpoint and public client ID. It does not
retain the unused ID token. Reading JWT expiry is metadata extraction, not signature validation
or proof of authentication; the provider validates the access token.

The credential file is a regular non-symlink, bounded to one mebibyte before parsing. Missing
files report no sign-in; existing malformed records fail. Errors never echo credential-bearing
JSON or nested parser/decoder diagnostics. The extractor follows the existing adapter pattern
of a small `CredentialExtractor` override because the nested token fields and JWT expiry cannot
be expressed by the shared YAML reader. Its token endpoint and public client ID were read in
the digest-verified pinned CLI; a pin change requires rechecking that sign-in and renewal identity.
Live device-code collection and actual broker renewal still require attended acceptance.

## Conversation records

Unattended output names the conversation in `thread.started/thread_id`. Attended transcripts
live below `.codex/sessions`, with dated directories and a timestamp in each rollout name;
their `session_meta/payload.id` is the ID that resume takes, not the filename stem. The
definition declares these separately through the published `file_record` and `file_key`
selectors. It selects the last `item.completed` whose `item.type` is `agent_message` and reads
`item.text`, so command output and unfinished messages cannot replace the final reply.

The Debian and RPM packages require Porelo 0.5.1~snapshot.317 or newer, the first published
host with these selectors. Definition-level replay establishes extraction, not a successful
live conversation continuation; unattended and attended end-to-end acceptance remain separate.

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
The shared launch prefix includes `--no-daemon`: the standalone distribution otherwise refuses
interactive resume for lack of a complete local package. It runs in the task's own terminal,
without a second shared server. The same global flag is accepted before `exec resume`.

## Artifact checks

The shared pin command checks `codex` and `helper` separately. The fetched-CLI recorder selects
each artifact by its named pin and records `codex` and `codex-code-mode-host` in the bill of
materials. It is a Maven plugin dependency only, never a dependency of the adapter or its tests.
The package checks read the actual generated bill and require the complete fetched component
set, its built versions, checked digests, download URLs and fetched-at-image-build provenance.

## Profile

The image sets `CODEX_HOME=/home/agent/.codex` for interactive and unattended invocations. Codex
puts its temporary helper aliases beneath the selected profile too; it does not need a second
host directory. A CLI invocation without that variable uses the invoking account's default profile.

The custom `broker` provider uses the supplied base URL unchanged, Responses over HTTP, and the
task token as its explicit bearer. OpenAI login and WebSockets are disabled for that route. A
missing endpoint/token pair or unsafe URL or bearer input is refused without echoing it. A shell
with neither receives no credential file. Only the disposable phantom token belongs in this
file; the real credential remains the broker's responsibility.

The brokered setup trusts only the absolute normalized workspace supplied by the task context.
Codex otherwise asks for folder trust even with its permission bypass. Trust enables that
workspace's project settings and hooks, so this is scoped to the task's selected repository,
never the filesystem root. Quotes and backslashes are escaped in the TOML key; invalid or
ambiguous paths are refused without echoing their contents.

## Terminal state

The composer footer `? for shortcuts` establishes startup readiness, not idleness: it remains
visible during work. Wake input additionally requires the composer prompt and excludes the
measured busy footer `esc to interrupt` and selection/trust dialog footers. The reference screens
are the terminal's actual last twenty lines; a bounded loopback-only HTTP fixture holds a request
open to capture work without calling a paid service. It does not establish a successful model turn.

Waiting rules cover the measured folder-trust and model-selection footers. They do not establish
every tool question or approval dialog. Computed-answer acceptance for idle and busy mailbox
delivery remains required; these screen guards alone do not establish end-to-end wake delivery.

## Build boundary

The Java and resource layout follows the existing adapters: a small `YamlAgent` subclass, a
stateless JSON formatter and container setup, service registration and a null-marked main package.
The setup consumes only the published agent contract; provider upstreams, header substitution
and credential renewal remain outside the adapter. The credential extractor reads only the
CLI-specific login representation.

Unlike the complete adapters, CI only checks the existing pins and builds packages. It neither
publishes an incomplete adapter nor proposes CLI upgrades without a live-provider gate. Shared
release tooling remains the source of pin metadata and bill recording. Provider acceptance,
publishing and automated updates require the provider contract and its acceptance coverage.

The credential name reserved for GitHub live tests is `OPEN_AI_API`: a live-test step receives it
as an environment variable from the GitHub Actions secret of the same name. The binding belongs
only on that step, never at workflow or job scope, and is unavailable to untrusted pull-request
code. The current provider-independent workflow has no live-test step and requests no secret.

The published acceptance kit creates its own base-image mapping before appending extra YAML.
The live fixtures therefore replace exactly its known initial base-image line in the owned Git
fixture, append the system-layer settings beneath that one mapping and re-follow the committed
configuration through the public CLI. A missing ownership marker or an unexpected initial base
fails before editing. This keeps the image pinned without adding custom glue or a second mapping.
