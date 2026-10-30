# CX09 — A Sign-In Without A Browser Is Tested On Every CLI Update

**Status:** Idea; blocked by [CX06](README.md#cx06), since the update job does not propose new CLI versions yet.

**What must be true.** `porelo vault login` for Codex CLI, started over `ssh -t` in a host account with no browser
opener set, shows its device page and one-time code; the weekly update job runs that login against each new CLI release,
and a release that no longer does so fails its pin bump, so a person signing in from a host is never left with a login
that waits for a callback nobody can reach.
