# CX26 — The Local Sign-In Goes To A Host

```json
{
  "$schema": "issue-1.json",
  "id": "CX26",
  "title": "The Local Sign-In Goes To A Host",
  "state": "Review",
  "size": "M",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "on the person's machine the adapter lists each Codex sign-in it finds - provider, kind, account where known, age and expiry where known, and whether it can be carried, never its value - and hands a chosen one to Porelo, which carries it into a rented host's vault",
  "openQuestions": 1
}
```

**Why this size.** `CodexCredentialExtractor` exists; a finder, `CodexFindings`, is new. Each route below is read, told apart and tested against a fixture file; no new contract of
the adapter's own, the fields come from Porelo's half.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**What must be true.** A person with Codex signed in on their own machine carries that sign-in into a rented host's
vault from the window and is not asked to sign in a second time there. The adapter's half: for each route below it
says provider, kind, the account where the file names one, age and expiry where the file holds them, and whether it
can be carried, with no value in what it says; and taking one hands its value to Porelo through the extractor alone,
never through a file, a command line or a log.

Filed 2026-11-14 on the operator's order, under ST362 "Credentials carried to a host". Agent Core files Porelo's half,
the fields and the carrying by stdin or environment; Agent Frontend files the window's dialog.

## Plan

- `auth.json` in the definition's `config_dir`: the ChatGPT sign-in, its account id, and its age and expiry where the file holds them, carriable; or an OpenAI API key, which the extractor does not take, not carriable.
- A sign-in Codex keeps in the system keyring rather than the file: found where its configuration says so, not carriable.
- The finder fills the fields Porelo's half adds to a finding; the extractor is the one an import already uses.
- Tests: one fixture per route, each finding's fields asserted, and a test that no finding's text holds any value
  of its fixture. The definition's `config_dir` stays the place read.

## Acceptance

- Each route above is found and said as listed, with its carriable answer, by a test.
- No finding holds a value of its fixture.
- With Porelo's half in, a sign-in found on the person's machine is in the host's vault and a task there runs on it.

## What was built

- `CodexFindings`, the agent's finder, reads only `auth.json` and `config.toml` in the definition's `config_dir`; it runs
  no command, reaches no network and reads no environment.
- A ChatGPT sign-in is signed in, provider `chatgpt`, kind subscription; it is carriable exactly when
  `CodexCredentialExtractor` takes it, which the finder asks of the extractor itself.
- An OpenAI API key is signed in, provider `openai`, kind API key, and not carriable, since the extractor takes no key.
- A file Codex cannot sign in with is not signed in; an `auth.json` or `config.toml` that cannot be read, is a link,
  dangling or not, or is past 1 MiB cannot say.
- `cli_auth_credentials_store` set to `keyring` or `auto` at the top of `config.toml` is a sign-in that cannot say and is
  not carriable, said before the file.
- `CodexSignInFacts` gives each sign-in in the file the import takes its account (`tokens.account_id`), its last change (the file's
  time) and no expiry: the file holds one only inside the access token, which is the value and is not opened for a fact.
- Tests: `CodexFindingsTest` and `CodexSignInFactsTest`, one fixture per route, each finding's fields asserted, and no
  finding's or fact's text holds a value of its fixture.
- The reading by another model found `config.toml` read through a link and without a bound, facts given to an API
  key the import does not take, and tests that passed with the size check or the facts' filters gone; all fixed.
  Noted, not fixed: an `OPENAI_API_KEY` beside a broken ChatGPT sign-in reads as signed in by key, and the guard
  against a value in the text has no API-key fixture.
- Porelo's fields (account, age, expiry, carriable) are still to be mapped once Porelo's wire shape for them is posted.

## Open questions

- **Two machines on one sign-in.** An OAuth sign-in whose refresh token is replaced at each renewal works on one
  machine at a time: the first to renew signs the other out. Whether a carried sign-in is said to sign the person's
  machine out, or is carried only as a kind that does not rotate, is the operator's call.

## To be checked

- **Porelo's half.** Porelo seals the value it takes through this adapter's extractor, as an import does; the
  listing that shows these facts is still to come from Porelo, and the facts are mapped onto it then.
- **The keyring's values.** Only `file` is named in this repository; `keyring` and `auto` are taken from Codex's
  configuration as read, not measured on the pinned CLI.
