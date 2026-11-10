# CX09 — A Sign-In Without A Browser Is Tested On Every CLI Update

```json
{
  "$schema": "issue-1.json",
  "id": "CX09",
  "title": "A Sign-In Without A Browser Is Tested On Every CLI Update",
  "state": "Idea",
  "size": "-",
  "priority": "Medium",
  "rank": 2,
  "blockedBy": ["CX06"],
  "covers": "the sign-in at a terminal without a browser, checked on every CLI update so a release that drops it fails its pin bump"
}
```

**How it stands.** The update job does not propose new CLI versions yet.

**What must be true.** `porelo vault login` for Codex CLI, started over `ssh -t` in a host account with no browser
opener set, shows its device page and one-time code; the weekly update job runs that login against each new CLI release,
and a release that no longer does so fails its pin bump, so a person signing in from a host is never left with a login
that waits for a callback nobody can reach.
