# Changelog

## [Unreleased]

### Added

- A provider-independent Codex CLI adapter with task-local profiles, structured turn outcomes and
  pinned, verified CLI and code-mode host installation artifacts.
- A draft task-build image declaration with pinned build tools and native package prerequisites.
- OpenAI API-key broker routing through owner-only task-profile configuration, with unsafe or
  incomplete endpoints and task tokens refused before launch.
- Task-scoped workspace trust and measured startup, selection-dialog and safe wake boundaries
  that exclude a working turn even when its composer remains visible.

### Fixed

- Standalone task invocations avoid the shared background server so interactive resume does not
  require an uninstalled full CLI package.
