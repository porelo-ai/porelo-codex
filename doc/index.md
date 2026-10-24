# Codex CLI adapter

The adapter is discovered through Porelo's published agent protocol. Its package supplies the
adapter executable and bill of materials; a task image downloads the pinned Codex CLI and code-mode
host archives and verifies each digest before extraction.

The adapter builds and is discovered by Porelo. Its OpenAI API-key setup points Codex at the task's
broker using only a phantom token. A real model reply and credential containment are verified in
a shell task; unattended launch and interactive session/wake acceptance remain pending. ChatGPT
is the default provider. The adapter declares device-code login and collects its OAuth record
for host-side vault storage, account routing and renewal. Attended device-code login and OAuth
vault collection pass; a brokered ChatGPT model turn and renewal still need acceptance.
API keys use the explicit OpenAI provider. The adapter requires Porelo 0.5.1~snapshot.317 or
newer to read attended session metadata and unattended final replies through the published selectors.

`porelo vault login codex --dry-run` previews login without starting it. A real
`porelo vault login codex` asks for the browser/device-code step. Its temporary plaintext-file
exposure is limited to the [accepted login collection route](decisions.md#login-collection), not
ordinary tasks. Store API keys under `openai` and select `--provider openai` for API-key tasks.

## Attended ChatGPT login acceptance

This walk checks the packaged login and vault collection. SSH access, account-local snapshot 323,
adapter discovery, scratch-vault creation, login preview and the device-code page have been run.
Attended terminal readback confirms browser approval, OAuth credential collection and Codex's
signed-in state. A successful login alone does not establish a brokered model turn, renewal,
session continuation or wake delivery.

1. Before arranging the walk, the test maintainer prepares a dedicated account with the exact
   verified Porelo package, this adapter and its ChatGPT provider definition. Record their versions
   and digests. Use the pinned Codex CLI and code-mode host from the adapter definition. Check
   that no other test uses the account and that its tasks are stopped. Use a version with the
   empty-vault startup correction, published in snapshot 320 and verified here against snapshot 323.
2. **Run:** open a private terminal on that account, using an SSH connection with a terminal if remote.
   Run `export PATH="$HOME/.local/bin:$PATH"`, then `porelo --version`, `porelo agents` and
   `porelo providers`. Check the prepared versions,
   that `codex` is discovered, and that `chatgpt` is available. Stop if a version or definition
   differs from the prepared test. Do not record the terminal during sign-in.
3. **Run:** in that same terminal, create a new private directory and select its vault:

       umask 077
       codex_login_dir=$(mktemp -d "$HOME/codex-login.XXXXXXXX")
       export PORELO_VAULT="$codex_login_dir/vault.bin"
       porelo vault init

   Choose a test passphrase at `New vault passphrase:` and repeat it at
   `New vault passphrase again:`. The terminal displays asterisks. Expect `created` with the new
   path and `unlocked` with the keyring-cache message. Keep this shell open so every remaining command uses this same vault;
   the account's ordinary vault is not the test vault.
4. **Run:** execute `porelo vault login codex --dry-run`. It must preview the Codex login without building or
   starting authentication. The definition requests device-code login with file credential
   collection. Its [temporary plaintext exposure](decisions.md#login-collection) applies during
   the real login.
5. **Run through the device-code page, then cancelled:** execute `porelo vault login codex`.
   If it asks for the vault passphrase, enter it at the masked prompt. Expect `Welcome to Codex`,
   `https://auth.openai.com/codex/device`, and a one-time code that expires in 15 minutes.
   This flow needs no SSH port forward. If the command refuses, hangs or asks an unexpected question,
   stop and report only the nonsecret error. On snapshot 323, Ctrl+C did not cancel this login.
   The verified cancellation route is a second terminal on the same account: find this run's name
   with `podman ps --format '{{.Names}}'`, then run `podman stop --time 5 <that-login-container>`.
   Porelo reports exit 137 with no credential and removes its container. Stop only this run's
   container; read back that it is absent.
6. **Passed attended acceptance:** open the displayed page in a browser, sign into the intended ChatGPT account and enter the
   one-time code there. Device-code login must be enabled in the account's security settings or
   allowed by its workspace. Keep the code, browser credentials and terminal transcript private.
   Return to the terminal and wait for the login command to finish successfully. Do not run a
   second login while the first is pending.
7. **Passed attended acceptance:** execute `porelo vault list` and `porelo agents --json` in the
   same shell. The login reports that `chatgpt` is stored as `oauth`. The list shows `client_id`,
   `expires_at` and `token_url = https://auth.openai.com/oauth/token`; Codex reports
   `credential: chatgpt` and `signed_in: true`, with the vault open.
   The test maintainer checks that expiry and renewal settings
   are present without printing access, refresh or ID tokens or opening `auth.json`. Record only
   pass/fail and nonsecret diagnostics. The maintainer also checks removal of the login container
   and temporary collection directories. A failed collection is not a successful packaged login.
8. **Passed cleanup readback:** stop here. A model request or forced renewal needs its own bounded test and approved budget;
   this walk starts neither. Run `porelo vault lock`, then `unset PORELO_VAULT` before leaving the
   shell. The maintainer retains or removes exactly this test's encrypted vault as agreed, verifies
   no login process/container or temporary plaintext copy remains, and restores the account's
   prepared overrides. Deleting a local copy does not revoke the sign-in or prove secure erasure.

## Building

Build with GraalVM and `./mvnw -U -B -s settings.xml verify`. The native executable and Debian/RPM
packages are built by `./mvnw -U -B -s settings.xml -Pnative,dist verify`. Main dependencies are the
latest published Porelo agent API and wire snapshot selected by the imported BOM; no Porelo
implementation checkout is required.

## Building in a task

The root `porelo.yml` drafts the build image: pinned Ubuntu and verified GraalVM and shfmt
downloads, with native-linkage and Debian/RPM packaging tools. It contains tools only; egress,
resource limits, agent selection and credentials belong to the project running the task.

Porelo does not read this repository-image declaration yet. A task-build measurement must derive
an equivalent disposable `project.yml` from its exact image block, outside the repository, and
run the applicable Build commands in that task. This demonstrates the declared toolchain, not
automatic repository-image discovery. GitHub checkout/cache/upload actions and `jdk --github`
are runner-specific: use a commit-scoped handover checkout, the image's pinned JDK and local
package inspection instead. No Maven verification or shared check is exempted.

Task profiles use `CODEX_HOME=/home/agent/.codex`. This includes Codex's temporary helper aliases,
so help and startup paths stay in the same task profile. Provider integration and any credential
store selection remain separate from this task-local path.

See [Decisions](decisions.md) for the adapter's differences from the scaffold.
