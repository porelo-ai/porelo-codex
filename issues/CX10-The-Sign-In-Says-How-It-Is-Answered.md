# CX10 — The Sign-In Says How It Is Answered

**Status:** Accepted.
**Size:** XS - the key in `codex.yaml` and the Porelo version in `pom.xml`, under an hour.

**What must be true.** A person signing in to Codex CLI from a host, at a terminal or in the GUI, is told to confirm the
one-time code on OpenAI's device page.

## Plan

- **The contract built against:** Porelo's agent API takes `login.flow` with the values `paste-code`, `device-code`
  and `loopback`, refuses a `loopback` flow on a remote account by naming the agent, and never sets a browser opener for
  a login on a host; the agent listing it gives the frontend names the flow.
- **`src/main/resources/agent/codex.yaml`** gains `login.flow: device-code`.
  Its login runs `login --device-auth`, which shows a device page and a one-time code.
- **`pom.xml`:** `porelo.version` moves to the Porelo version whose agent API takes the key.
- No test: a wrong value either shows the wrong hint, which only reads wrong, or, as `loopback`, is refused by name
  at once; neither does harm.

## Acceptance

- Seen to fail first: the agent listing for today's definition names no flow.
- With the change, the definition loads and the agent listing names `device-code`.
