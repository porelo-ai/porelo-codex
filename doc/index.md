# Codex CLI adapter

The adapter is discovered through Porelo's published agent protocol. Its package supplies the
adapter executable and bill of materials; a task image downloads the pinned Codex CLI and code-mode
host archives and verifies each digest before extraction.

The adapter builds and is discovered by Porelo. Its OpenAI API-key setup points Codex at the task's
broker using only a phantom token. A real model reply and credential containment are verified in
a shell task; unattended launch and interactive session/wake acceptance remain pending. ChatGPT
is the default provider. The adapter declares device-code login and collects its OAuth record
for host-side vault storage, account routing and renewal; the complete attended login still
needs acceptance. API keys use the explicit OpenAI provider. The adapter requires Porelo 0.5.1~snapshot.317 or
newer to read attended session metadata and unattended final replies through the published selectors.

`porelo vault login codex --dry-run` previews login without starting it. A real
`porelo vault login codex` asks for the browser/device-code step. Its temporary plaintext-file
exposure is limited to the [accepted login collection route](decisions.md#login-collection), not
ordinary tasks. Store API keys under `openai` and select `--provider openai` for API-key tasks.

Build with GraalVM and `./mvnw -U -B -s settings.xml verify`. The native executable and Debian/RPM
packages are built by `./mvnw -U -B -s settings.xml -Pnative,dist verify`. Main dependencies are the
latest published Porelo agent API and wire snapshot selected by the imported BOM; no Porelo
implementation checkout is required.

## Building in a task

The root `porelo.yml` drafts the build image: pinned Ubuntu and verified GraalVM and shfmt
downloads, with native-linkage and Debian/RPM packaging tools. It contains tools only; egress,
resource limits, agent selection and credentials belong to the project running the task.

Porelo does not read this repository-image declaration yet. A task-build measurement must derive
an equivalent disposable `project.yml` from its exact image block, outside the repository, and
run the applicable Build commands in that task. This demonstrates the declared toolchain, not
automatic repository-image discovery. GitHub checkout/cache/upload actions and `jdk --github`
are runner-specific: use a commit-scoped handover checkout, the image's pinned JDK and local
package inspection instead. No Maven verification or shared check is exempted.

Task profiles use `CODEX_HOME=/home/agent/.codex`. This includes Codex's temporary helper aliases,
so help and startup paths stay in the same task profile. Provider integration and any credential
store selection remain separate from this task-local path.

See [Decisions](decisions.md) for the adapter's differences from the scaffold.
