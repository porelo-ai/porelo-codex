# Changelog

## [Unreleased]

### Changed

- Both packages depend, beside `porelo`, on a Porelo whose lowest agent protocol this adapter speaks, `porelo-agent-protocol` 6, the protocol in which Porelo starts an agent with `--limits` before its mode, so the package manager refuses a pairing Porelo would refuse at the first task.
- The hook that keeps a task's subagents from starting is given its 10 seconds times the `machines` factor of the limits Porelo passes with `--limits`, rounded up to whole seconds, and the hash Codex trusts it by is taken of that hook, so a slow machine neither cuts it off nor leaves it untrusted.
- The definition says how Codex takes a reasoning effort - `-c model_reasoning_effort={effort}` with none, low, medium, high, xhigh, max, ultra - for a Porelo that offers it at a start; an older Porelo skips it.
- An unattended task's log says every tool Codex used on one line with what it was used on - a file change by its path, an MCP call by its server and tool, a web search by its query, a todo list by its number of items - and a command by its first line only; a tool that failed - by its status, its exit, an error mark or an error it carries - says so with the first three lines of its error or output.
- An unattended task's log shows what Codex says and does and no event as raw JSON: an event it does not word is left out, and one that reports an error - an error item, a failed status, an error it carries - is always shown as such, on one line.
- Codex and its code-mode helper are installed by root in `/usr/local/bin`, so the agent in a task cannot replace what the next start runs, and a self-update fails to write over them.
- Central's snapshots (`central.sonatype.snapshots`) are asked only for `org.fuin.porelo`, the group listed in `.mvn/rrf/`, through the switch in the now committed `.mvn/maven.config`, on a forge too, where they are read beside its registry and the newest wins; the forge's own repository stays unfiltered. The daily shared rules check refuses a filter that would let another name out.
- The Build takes the runner's distribution packages from the Actions cache, keyed by the package list's hash, each file checked against the hash apt names before it is installed, so an outage of a package mirror fails only a Build on a new list.
- The package says that the `porelo-wire` classes in its executable are under Apache-2.0, in its
  Debian copyright and its RPM license, with the license's text beside the GPL's in the RPM.

### Added

- A Codex agent in a task is given Porelo's guide - its mailbox, `/porelo/files`, the builds of its pushes, which task it is - through the `AGENTS.md` in Codex's home folder (`CODEX_HOME`), a link the image makes to the guide Porelo mounts, which Codex adds to its own instructions when a session starts.
- The adapter says which Codex sign-in it finds in its configuration directory - a ChatGPT sign-in, an OpenAI API key, or one kept in the system keyring - with its provider and kind, and for the sign-in the import takes its account and last change, but never its value, and whether it can be carried to a host, which only the ChatGPT sign-in the import takes can.
- Asked for the provider `chatgpt`, the adapter answers the ChatGPT sign-in named so, as Porelo's listing of what can be carried to a host asks, and asked for any other provider it answers nothing.
- The definition says how an attended session takes an opening text, `session.opening_flag: ""`, so Porelo
  opens a discussion on Codex CLI with the person's text as its first turn, after which it waits for them.
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
