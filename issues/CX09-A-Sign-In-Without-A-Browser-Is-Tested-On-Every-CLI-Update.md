# CX09 — A Sign-In Without A Browser Is Tested On Every CLI Update

**Status:** Idea.

**What must be true.** `porelo vault login` for Codex CLI, started over `ssh -t` in a host account with no browser opener
set, shows its device page and one-time code; a CLI release that no longer does fails the weekly update's pin bump, so a person signing in from a host
is never left with a login that waits for a callback nobody can reach.
