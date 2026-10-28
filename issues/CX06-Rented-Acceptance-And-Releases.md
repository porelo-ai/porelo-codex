# CX06 — Rented Acceptance and Releases

**Status:** Idea.

**What must be true.** Like the other adapters, a push to main and a release tag run the
acceptance on rented ubuntu and fedora machines against the published package, a tag publishes a
release only after that acceptance passed, and the update job proposes new Codex CLI versions
behind the same gate.
