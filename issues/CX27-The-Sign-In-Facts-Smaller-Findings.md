# CX27 — The Sign-In Facts' Smaller Findings

```json
{ "$schema": "issue-1.json", "id": "CX27", "title": "The Sign-In Facts' Smaller Findings", "state": "Idea",
  "size": "-", "priority": "Medium", "rank": 1, "blockedBy": [], "covers": "the smaller gaps left in how the adapter finds and describes a local Codex sign-in for carrying to a host, each fixed or measured" }
```

**What must be true.** What the adapter says of a Codex sign-in on the person's machine, before it is carried to a
host, holds in the cases below too, and each guard there has a test that shows it gone.

## To be checked

- **A key beside a broken ChatGPT sign-in.** With `OPENAI_API_KEY` set and an `auth.json` whose ChatGPT sign-in Codex cannot use, the finder says signed in by key; whether Codex itself falls back to the key is measured.
- **The no-value guard has no API-key fixture.** The test that no finding or fact holds a value of its fixture has no fixture with an API key in `auth.json`.
- **The keyring values are not measured.** `keyring` and `auto` for `cli_auth_credentials_store` are taken from Codex's configuration as read, not measured on the pinned CLI.
- **The facts on the wire.** Porelo's listing of what can be carried names agent, provider and kind only; the account, expiry and last change stay in the adapter until a listing takes them, and are then handed over.
