# CX06 — Rented Acceptance and Releases

**Status:** Idea.

**What must be true.** Like the other adapters, a push to main and a release tag run the
acceptance on rented ubuntu and fedora machines against the published package, a tag publishes a
release only after that acceptance passed, and the update job proposes new Codex CLI versions
behind the same gate.

## To be checked

- How a leg gets the packages its run built: `actions/upload-artifact` v7 and `actions/download-artifact` v8 refuse
  every server but GitHub's, so the `build` job uploads them only where `vars.PORELO_PUBLISH` is `true`.
