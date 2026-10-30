# Changelog

## [Unreleased]

### Added

- The definition names how its sign-in is answered, `login.flow: device-code`, so a person signing
  in on a host over SSH can be told to confirm the one-time code on OpenAI's page; Porelo 0.5.0,
  which this builds against, ignores the key, and a later Porelo reads it.
- A provider-independent Codex CLI adapter with task-local profiles, structured turn outcomes and
  pinned, verified CLI and code-mode host installation artifacts.
- A draft task-build image declaration with pinned build tools and native package prerequisites.
- OpenAI API-key broker routing through owner-only task-profile configuration, with unsafe or
  incomplete endpoints and task tokens refused before launch.
- Task-scoped workspace trust and measured startup, selection-dialog and safe wake boundaries
  that exclude a working turn even when its composer remains visible.
- ChatGPT device-code login and bounded OAuth credential collection for host-side renewal,
  with account routing preserved and malformed credentials refused without echoing tokens.
  ChatGPT is the default provider; API keys continue through the explicit OpenAI provider.
- The Debian and RPM packages are published to the snapshot channel on every push to main.

### Fixed

- Session-file lookup reads the conversation ID from its metadata instead of the rollout name,
  and unattended summaries use the final assistant message rather than tool output. Packages
  require Porelo 0.5.1~snapshot.317 or newer for these selectors.
- Standalone task invocations avoid the shared background server so interactive resume does not
  require an uninstalled full CLI package.
