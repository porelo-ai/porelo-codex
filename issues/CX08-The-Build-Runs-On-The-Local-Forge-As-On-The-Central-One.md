# CX08 — The Build Runs On The Local Forge As On The Central One

**Status:** Idea.

**What must be true.** `porelo-codex`'s workflows run green on the local Forgejo and on the central forge on the same
commit, with no edit between them: the registry and the place it publishes to are variables of the setup.

From porelo-project [PJ60](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj60), for the move's
wave 7b, before [CX04](README.md#cx04).

## The shape

Read in today's `.github/workflows/`: the Build rents no machine yet (its legs are [CX06](README.md#cx06)), so this
is the smallest of the adapters' changes.

- `settings.xml` reads the forge's registry through `MAVEN_SNAPSHOTS_URL`, as porelo-message-sluice's does.
- `vars.PORELO_PUBLISH` switches Publish and its index check, which today stop on an empty `JF_URL`.
- Whichever of this issue and [CX06](README.md#cx06) comes second asks for its legs by kind
  (porelo-buildtools [BT25](https://github.com/porelo-ai/porelo-buildtools/blob/main/issues/README.md#bt25)) and adds
  `porelo-release check-providers` to the Build.

## Acceptance

- Seen to fail first: today's `build.yml` on the local Forgejo.
- The same commit's Build green on the local Forgejo and on GitHub.
