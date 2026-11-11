# CX20 — The Build Takes Its Downloads From A Cache

```json
{
  "$schema": "issue-1.json",
  "id": "CX20",
  "title": "The Build Takes Its Downloads From A Cache",
  "state": "Backlog",
  "size": "XS",
  "priority": "Medium",
  "rank": 2,
  "blockedBy": [],
  "covers": "the runner's packages come from the Actions cache, keyed by their package manifest, so an outage of the package mirrors no longer fails a Build that already ran once; the CI fetches no CLI archive, and what must stay live is named"
}
```

**Why this size.** one composite action in one workflow of this repository; the CI downloads no CLI archive, so there
is no binary to cache.

**What must be true.** A Build, an update run or the workflow check that runs on a pin it has seen before takes every
outside download it can from the Actions cache, not from the vendor's download host or the runner's package mirrors: an
outage of one of those hosts then fails only the first run on a new pin. Each cached file is still checked against its
pin before it is used, and an entry of an old pin goes by itself, so the cache never fills up.

## Plan

- Already cached by the shared `pinned-jdk` action, and left as it is: the pinned GraalVM (`pinned-graalvm-<sha256>`),
  the wrapper's Maven and the Maven repository.
- The runner's packages (`gcc`, `rpm`, `dpkg`, `zlib1g-dev` in `build.yml`): the package list is resolved from fresh, signed apt metadata, the `.deb` files
  are kept under `runner-apt-<runner image>-<manifest sha256>`, and each restored file is checked against the hash the
  metadata names before it is installed. The indexes stay live.
- No CLI archive: the Codex CLI and the code-mode host are pinned by digest but not fetched by any workflow; the
  build only records them in the bill of materials, and the repository has no acceptance job.
- Not cacheable, and kept online on purpose: the release answers `check-pin` reads for the publisher's digests, the
  snapshot publication index on Artifactory, and Maven snapshots asked for anew with `-U`.
- Retention: GitHub removes a cache entry nobody read for 7 days, and every key carries the digest or the manifest's
  hash, so an old pin's entries go by themselves while the current ones are read on every run. No step deletes
  entries.

## Acceptance

- The second Build on unchanged packages shows a cache hit for the runner's packages and fetches none from the
  mirrors.
- A cached package changed on purpose (one byte) is refused by its hash and fetched again.
- After the package list changes, the Build writes an entry under the new manifest hash, and the old one is gone once
  nobody read it for 7 days.
