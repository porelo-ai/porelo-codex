# Codex CLI adapter

The adapter is discovered through Porelo's published agent protocol. Its package supplies the
adapter executable and bill of materials; a task image downloads the pinned Codex CLI and code-mode
host archives and verifies each digest before extraction.

The provider-independent adapter builds and is discovered by Porelo. It does not declare a provider, import
credentials or claim that a task can authenticate. ChatGPT provider integration depends on the
published contract tracked in porelo-project's [issue index](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj54).

Build with GraalVM and `./mvnw -U -B -s settings.xml verify`. The native executable and Debian/RPM
packages are built by `./mvnw -U -B -s settings.xml -Pnative,dist verify`. Main dependencies are the
latest published Porelo agent API and wire snapshot selected by the imported BOM; no Porelo
implementation checkout is required.

Task profiles use `CODEX_HOME=/home/agent/.codex`. This includes Codex's temporary helper aliases,
so help and startup paths stay in the same task profile. Provider integration and any credential
store selection remain separate from this task-local path.

See [Decisions](decisions.md) for the adapter's differences from the scaffold.
