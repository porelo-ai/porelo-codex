# Changelog

## [Unreleased]

### Changed

- The Build takes the runner's distribution packages from the Actions cache, keyed by the package list's hash, each file checked against the hash apt names before it is installed, so an outage of a package mirror fails only a Build on a new list.
- The package says that the `porelo-wire` classes in its executable are under Apache-2.0, in its
  Debian copyright and its RPM license, with the license's text beside the GPL's in the RPM.

### Added

- Unattended Codex tool calls report their start and end by item ID, their kind, and shell
  command lines for Porelo to mask and time, without copying tool output or file contents.
- A task's agent starts no subagent unless the task allows it, and then no more at once than it says. Skills are hidden from it, not switched off: a skill's file can still be read through its shell.
- The definition names how its sign-in is answered, `login.flow: device-code`, so a person signing
  in on a host over SSH can be told to confirm the one-time code on OpenAI's page; the Porelo it
  builds against reads the key, and its agent listing names the flow.
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
- The package carries the full license texts of what its executable compiles in from outside Porelo - SnakeYAML, JSpecify and the GraalVM runtime - in `THIRD-PARTY-NOTICES`, in its doc directory and as an RPM license file, and its Debian copyright names them.

### Fixed

- Session-file lookup reads the conversation ID from its metadata instead of the rollout name,
  and unattended summaries use the final assistant message rather than tool output. Packages
  require Porelo 0.5.1~snapshot.317 or newer for these selectors.
- Standalone task invocations avoid the shared background server so interactive resume does not
  require an uninstalled full CLI package.
