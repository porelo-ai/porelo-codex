# CX02 — Build in a Porelo task

**Status:** Review; task build and retained GitHub package artifacts are the agreed acceptance evidence; indexed adapter publication remains with [CX01](README.md#cx01).
**Size:** M - one pinned build-image declaration, documentation and a real task-level acceptance run covering the native packages and shared checks through an equivalent disposable project file.

**What must be true.** The repository carries its build tools in `porelo.yml`, and its build passes
inside a real Porelo task without depending on a host checkout or host toolchain. This covers
phases 2 and 5 of porelo-project [PJ39](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj39);
it does not require Codex to be the task's agent or the ChatGPT provider to exist.

## Plan

- Add root `porelo.yml` using `porelo-omp/porelo.yml` as the toolchain model: an Ubuntu base pinned
  by digest, the shared pinned GraalVM archive checked against its digest, and the pinned shfmt.
  Install `build-essential`, `zlib1g-dev`, `ca-certificates`, `curl`, `git`, `unzip`, `binutils`,
  `dpkg` and `rpm`: native linkage checks execute `readelf`, and package checks execute `dpkg-deb`
  and `rpm`. Maven comes from the repository's pinned wrapper, not a second installation.
- Keep `porelo.yml` image-only: no egress, limits, agent, provider or credentials. Read current
  published support before choosing keys. The file remains an unread draft until the
  repository-image reader is implemented; the inspected
  `core/.../ProjectReader.java` and `ProjectSchema.java` accept `image.base_image`, `snippet`,
  `snippet_file` and `package_sources` in `project.yml`, but do not implement a repository-image
  reader or `image.env`. Put `JAVA_HOME` and `GRAALVM_HOME` in supported Containerfile `ENV` lines
  where necessary; do not copy ignored settings or claim automatic discovery.
- Automatic repository-image selection belongs to porelo
  [B192](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b192), whose file is
  Backlog in the inspected checkout. The agreed measurement derives a disposable valid
  `project.yml` from the exact tracked image block, outside every repository. Report it as
  "builds green in a task with an equivalent `project.yml`", not automatic image discovery. Never
  replace the Porelo task with a plain Podman container and call the acceptance complete.
- Update `doc/index.md` with the supported task-build route and its limits, and `CHANGELOG.md`
  when the image declaration is implemented. `.github/CODEOWNERS` already protects `/porelo.yml`.
  Change no published API, other repository, workflow or production provider as part of this task.
- Before the VM run, post the exact commit, image pins, task/project names, applied configuration,
  existing working agent/provider, resource limits, commands and cleanup scope in the channel.
  Use only the `codex` account on the Ubuntu VM and a run-owned checkout from the handover area.
  Egress is a separate test-project setting: measure the Maven, Sonatype, GitHub API and release
  hosts needed by this Build; do not weaken the repository's image-only boundary.
- First demonstrate that the task's toolchain preflight refuses a missing/wrong JDK or missing
  native/package tool; then build with the declared image. This is an environment acceptance
  failure, not evidence that a unit regression test failed. Reuse the existing build tests;
  add no test that merely holds YAML wording or a pinned version verbatim.
- In the task, run `./mvnw -B -s settings.xml verify`, resolve `-Pci-tools`, execute all five
  shared checks and `exec:java@check-codex-artifacts`, then run `-Pnative,dist verify` with an
  explicit local snapshot counter. Keep packages online so the SBOM is produced. Capture exit
  codes and logs, inspect the actual packages and both fetched SBOM components, then remove only
  this run's task, project fixture and checkout, verifying their absence.

## Acceptance

- The committed `porelo.yml` names the reproducible build image and tools only and is documented
  as an unread draft. The measurement uses an equivalent disposable `project.yml`; automatic
  discovery is outside this issue's acceptance and stays with the repository-image reader.
- An actual Porelo task on the Ubuntu VM, running under `codex` with an already working agent,
  builds the exact repository commit. The required Maven verification, native linkage/protocol
  tests, Debian/RPM package checks, publisher checks and all five shared checks pass without
  skipped tests or host-installed tools/artifacts substituting for the task image.
- GitHub's checkout/cache/upload actions and `jdk --github` cannot execute as GitHub steps inside
  a task: they require GitHub's runner/runtime or environment files. Use the handover checkout,
  the same pinned image JDK and local package inspection; name these differences in the result.
  They do not exempt any Maven or shared check. Unpublished cross-repository citation targets
  remain a publication-order constraint, not an intrinsic task limitation.
- The current Build has no live provider test or remote acceptance leg. This issue adds neither:
  production provider acceptance stays in [CX01](README.md#cx01). No provider key or host SSH key
  enters the task or its image. No push, rented machine, restart or shared VM install is needed.

## Decided

- The completed real-task build and retained GitHub package artifacts are this issue's acceptance
  evidence. An indexed adapter snapshot is not required here; publication belongs to
  [CX01](README.md#cx01). Automatic repository-image discovery remains outside this issue.

## VM run

- Only `codex` on the Ubuntu VM, `/usr/bin/porelo` measured as `0.5.1~snapshot.307`; run-owned
  `/home/codex/pj39-cx02/{code,project}`. Transfer a commit-scoped handover bundle only after its
  exact commit and hash are posted. Project `cx02-build` is `guarded`, followed `--unverified`,
  with one local upstream to `code`; memory 5g, two CPUs and pids 4096. Start only in the assigned slot.
- Use the installed OMP adapter with `--attach shell --detach`, not a model turn, and execute
  build commands in `/workspace` through `podman exec`. Verify the published version permits
  this shell start without a credential; do not supply a real key to a build-only task.
- Egress: the `maven` set and explicit `api.github.com`, `github.com`,
  `raw.githubusercontent.com`, `release-assets.githubusercontent.com`; `--clearance deny`.
  Add another host only for a recorded blocked request. Unset `GIT_CONFIG_COUNT`,
  `GIT_CONFIG_KEY_0` and `GIT_CONFIG_VALUE_0` for build commands while the installed Porelo
  leaves the gate header unscoped. This workaround does not depend on the pending scoped-header
  fix; never print the header or change the gate's own operations.
- The measured Maven/check/package sequence takes about four minutes on two CPUs with an empty
  Maven cache, excluding image/source preparation and cleanup. Native image takes two minutes
  and reports peak RSS 1.13 GiB. Stop a repeated failure, retain logs and artifact hashes, then
  remove only this task, its generated image/follow and run directory, verifying absence and
  reporting any leftovers.

## Measured verification

- Exact source `a51dcf1b21e5d0ace64faaf29d8790c36b2703ef` passes in the real task
  `porelo-cx02-build-cx02-build-run` on Porelo `0.5.1~snapshot.307`, using the disposable project's
  image block byte-for-byte from `porelo.yml`. The working user is `agent`, locale `C.UTF-8`;
  inspected limits are 5,368,709,120 bytes, two CPUs and 4,096 processes. All declared tools are
  available. A deliberately missing JDK is refused with exit 69 before the real preflight passes.
- OMP runs in shell-only mode without a vault credential or model turn. Its default provider is
  OpenRouter, whose host is reachable in addition to the explicitly configured Maven/GitHub
  hosts; no extra host is granted during the run. Per-host minimality is not claimed.
- Each Build step exits zero: Maven verification (32 unit tests), shared-tooling resolution,
  `check-shared`, public `check-citations` without checkout overrides, `check-readmes`,
  `check-doc-site`, `check-actions`, both publisher-digest checks, native/dist verification
  (32 unit, three native linkage/protocol and three package checks) and the document profile
  (five tests). No test is skipped. No Maven or shared check is exempted.
- The actual bill contains exactly the two fetched executable components with the pinned
  versions/digests and `fetched-at-image-build` delivery. Debian/RPM contents and versions are
  inspected, and all three package/bill SHA-256 values match after transfer. Logs, project
  configuration and artifacts are retained on the VM and host under
  `~/.porelo/handover/porelo-codex/a51dcf1b21e5d0ace64faaf29d8790c36b2703ef/cx02/`.
- Cleanup is independently read back: no tasks, only the default project; the followed project,
  run directory, task container, generated image and pulled base image are absent. No machine-wide
  install, account reset, restart, rented machine or paid model is used.
- The implementation is already on `main` as `8e9c9aa35b1b69799b71ee8bf82ee66b8bec178a`.
  Its [GitHub Build](https://github.com/porelo-ai/porelo-codex/actions/runs/39730780306)
  passes and retains the `packages` artifact. The
  [current Build](https://github.com/porelo-ai/porelo-codex/actions/runs/40475112768) on
  `a53a8f97c3215a30abb0577571303cdd3a087638` also passes with a retained `packages` artifact.
  These are uploaded build artifacts, not an indexed adapter snapshot: the
  [build boundary](../doc/decisions.md#build-boundary) keeps indexed publication separate until
  provider acceptance is complete. No implementation commit remains to be pushed for this issue.
