# CX09 — A Sign-In Without A Browser Is Tested On Every CLI Update

```json
{
  "$schema": "issue-1.json",
  "id": "CX09",
  "title": "A Sign-In Without A Browser Is Tested On Every CLI Update",
  "state": "Estimated",
  "size": "M",
  "priority": "Medium",
  "rank": 2,
  "blockedBy": ["CX06"],
  "covers": "the sign-in at a terminal without a browser, checked on every CLI update so a release that drops it fails its pin bump",
  "openQuestions": 0
}
```

**Why this size.** one acceptance scenario and no Java, once [CX06](README.md#cx06) gives the update job its
acceptance legs against the candidate packages; Porelo's acceptance kit already has every step it needs. M because it
is an acceptance step, written against the real CLI's screens and proven on a VM.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**How it stands.** The update job does not propose new CLI versions yet: `update.yml` only checks the publishers'
digests against the pin, and runs no acceptance. [CX06](README.md#cx06) brings the job that pins a new release and runs
the suite on rented ubuntu and fedora machines before its pull request is merged.

**What must be true.** `porelo vault login` for Codex CLI, started over `ssh -t` in a host account with no browser
opener set, shows its device page and one-time code; the weekly update job runs that login against each new CLI release,
and a release that no longer does so fails its pin bump, so a person signing in from a host is never left with a login
that waits for a callback nobody can reach.

## Plan

- Today: `codex.yaml` declares `login: {arguments: ["-c", 'cli_auth_credentials_store="file"', "login",
  "--device-auth"], flow: device-code}`. `terminal.feature` only previews it (`porelo vault login codex --dry-run`),
  which builds and runs nothing. The attended walk in `doc/index.md` measured the device page
  `https://auth.openai.com/codex/device` and a one-time code, and that Ctrl+C did not cancel the login.
- Porelo runs the login in a throwaway container of a login image built from the agent's pinned artifacts, labelled
  `porelo.login.agent=codex`, with `BROWSER` unset.
- **`terminal.feature`, one scenario** "a sign-in at a terminal without a browser shows the device page and a code",
  made only of the kit's steps:
  - `a vault of this scenario's own, unlocked ...`, then `a terminal on the machine` (an SSH session with a pty, as
    `ssh -t` gives).
  - `I run "unset BROWSER; porelo vault login codex"`, then `within <n> seconds the terminal shows
    "auth.openai.com/codex/device"`; `<n>` covers the first build of the login image in a fresh account.
  - A script step reads `podman logs` of the container labelled `porelo.login.agent=codex`, strips escape sequences,
    and requires a one-time code in the measured shape and no `localhost` callback (Codex's browser login listens on
    one).
  - Cancel: `podman stop --time 5` of that container by its label, then a script that sees it gone and no `chatgpt`
    credential in the vault.
- **`codex.yaml`**: the comment on `--device-auth` says the acceptance checks it on every pin.
- **Red first**: the scenario's check, run by hand on a local VM against `codex login` without `--device-auth` in the
  same login image (the localhost callback flow), fails; against the declared login it passes.
- **Level**: acceptance only, on a local VM, then one green leg pair on rented machines in the update job
  [CX06](README.md#cx06) builds. No unit test: there is no logic of the adapter's in it.

## Acceptance

- The scenario passes on ubuntu and fedora against the pinned CLI, with no secret and no model.
- Its check is seen to fail against a localhost-callback login.
- The update job's legs run it (the run log of one pin bump, or a dispatched run, names the scenario).
- No login container and no credential is left after it.

## What makes it uncertain

- [CX06](README.md#cx06) and [CX14](README.md#cx14) both bring acceptance legs; which one the update job's legs come
  from may move where this scenario is proven first, not what it is.
- The device-code request reaches OpenAI from a rented machine on every run; a refusal of cloud addresses, or a rate
  limit, would read as a failed sign-in.
- A cosmetic change of the device page's words stops a pin bump until the scenario follows.

## To be checked

- On 0.163.0: the code's shape and the words around it, and that `podman logs` of the login container holds them.
- How long the first build of the login image takes on a rented machine.
