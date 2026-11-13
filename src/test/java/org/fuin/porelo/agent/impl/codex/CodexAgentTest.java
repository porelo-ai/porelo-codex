package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.fuin.porelo.agent.api.AgentEnd;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.AgentRegistry;
import org.fuin.porelo.agent.api.RunRequest;
import org.junit.jupiter.api.Test;

/** Guards command execution and terminal outcomes consumed by Porelo. */
class CodexAgentTest {
    private final CodexAgent agent = new CodexAgent();

    @Test
    void aFailedTurnCannotBeReportedAsSuccess() {
        final AgentEnd end =
                agent.ended(
                        "{\"type\":\"turn.failed\",\"error\":{\"message\":\"unexpected status 401 Unauthorized\"}}");
        assertThat(end).as("a failed turn must have a terminal outcome").isNotNull();
        assertThat(end.finished()).as("an authentication refusal cannot succeed").isFalse();
        assertThat(end.status()).as("the provider's observed HTTP refusal").isEqualTo(401);
    }

    @Test
    void aCompleteTurnHasAnExplicitSuccessfulOutcome() {
        final AgentEnd end =
                agent.ended(
                        "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":12,\"output_tokens\":2}}");
        assertThat(end).as("the measured completion event").isNotNull();
        assertThat(end.finished()).as("a completed turn succeeds").isTrue();
    }

    @Test
    void commandsUseTheExecSubcommandAndKeepThePromptAsOneArgument() {
        assertThat(
                        agent.headlessCommand(
                                new RunRequest(
                                        "two words", "chosen-model", null, null, false, true)))
                .as("the actual Codex exec grammar")
                .containsExactly(
                        "codex",
                        "--dangerously-bypass-approvals-and-sandbox",
                        "--no-daemon",
                        "exec",
                        "--model",
                        "chosen-model",
                        "--json",
                        "--",
                        "two words");
    }

    @Test
    void aResumedCommandPlacesTheSessionAfterResumeOptions() {
        assertThat(
                        agent.headlessCommand(
                                new RunRequest(
                                        "--not-an-option", null, null, "thread-id", false, true)))
                .as("a prompt cannot become an option and resume is an exec subcommand")
                .containsExactly(
                        "codex",
                        "--dangerously-bypass-approvals-and-sandbox",
                        "--no-daemon",
                        "exec",
                        "resume",
                        "--json",
                        "--",
                        "thread-id",
                        "--not-an-option");
    }

    @Test
    void anUnsupportedTurnLimitIsRefused() {
        assertThatThrownBy(
                        () ->
                                agent.headlessCommand(
                                        new RunRequest("prompt", null, 3, null, false, false)))
                .as("silently ignoring a requested turn limit permits unbounded paid work")
                .isInstanceOf(AgentException.class);
    }

    @Test
    void theTaskImageKeepsHelperAliasesInTheTaskProfile() {
        assertThat(agent.imageLayer().asAgent())
                .as("explicit profile placement even for CLI help calls")
                .contains("ENV CODEX_HOME=/home/agent/.codex");
    }

    @Test
    void discoveryNeedsOnlyThePublishedAgentContract() {
        assertThat(AgentRegistry.discover().require("codex").definition().binary())
                .as("the service descriptor makes the adapter discoverable")
                .isEqualTo("codex");
    }

    @Test
    void aSignInIsCollectedForItsSubscriptionProviderRatherThanTheApiProvider() {
        assertThat(agent.definition().provider())
                .as("the sign-in needs an unambiguous vault destination")
                .isNotNull();
        assertThat(agent.definition().provider().defaultProvider())
                .as("a ChatGPT OAuth token must not be stored under the API-key provider")
                .isEqualTo("chatgpt");
        assertThat(agent.definition().configDirectory())
                .as("the collector reads the same explicit profile as the login container")
                .isEqualTo("~/.codex");
        assertThat(agent.definition().loginArguments())
                .as("remote login uses a device code and the agreed file collector, not a keyring")
                .containsExactly(
                        "-c", "cli_auth_credentials_store=\"file\"", "login", "--device-auth");
    }

    @Test
    void bothArtifactsArePinnedAndVerifiedBeforeExtraction() {
        assertThat(agent.definition().artifacts()).as("CLI and code-mode host archives").hasSize(2);
        assertThat(agent.definition().artifacts())
                .allSatisfy(
                        artifact -> {
                            assertThat(artifact.url())
                                    .as("each archive follows the same CLI pin")
                                    .contains("/rust-v" + agent.definition().version() + "/");
                            assertThat(artifact.sha256())
                                    .as("publisher digest")
                                    .matches("[a-f0-9]{64}");
                        });
        final List<String> layer = agent.imageLayer().asRoot();
        assertThat(layer.stream().filter(line -> line.contains("sha256sum -c -")).count())
                .as("each of the two archives is checked, in the root part of the image")
                .isEqualTo(2);
    }

    @Test
    void bothBinariesAreRootsSoTheAgentCannotReplaceWhatTheNextStartRuns() {
        assertThat(agent.definition().artifacts())
                .as("both archives are fetched by root, outside the agent's home")
                .allSatisfy(
                        artifact -> {
                            assertThat(artifact.rootOwned()).isTrue();
                            assertThat(artifact.target()).doesNotStartWith("/home/agent");
                        });
        final List<String> root = agent.imageLayer().asRoot();
        assertThat(root)
                .as(
                        "root installs both binaries, root's, into its own directory on the task's path")
                .anyMatch(
                        line ->
                                line.contains(
                                        "install -D -o root -g root -m 0755"
                                                + " \"$staging/codex-x86_64-unknown-linux-musl\""
                                                + " /usr/local/bin/codex;"))
                .anyMatch(
                        line ->
                                line.contains(
                                        "install -D -o root -g root -m 0755"
                                                + " \"$staging/codex-code-mode-host-x86_64-unknown-linux-musl\""
                                                + " /usr/local/bin/codex-code-mode-host;"));
        assertThat(agent.imageLayer().asAgent())
                .as("nothing of the binaries is fetched, unpacked or placed as the agent")
                .noneMatch(
                        line ->
                                line.contains("curl ")
                                        || line.contains("install ")
                                        || line.contains("tar ")
                                        || line.contains("codex-x86_64")
                                        || line.contains(".local/bin")
                                        || line.contains("/usr/local/"));
    }

    @Test
    void declaresThatItHoldsATaskToACountOfSubagents() {

        assertThat(new CodexAgent().definition().subagents())
                .isEqualTo(org.fuin.porelo.agent.api.SubagentLimit.COUNT);
    }
}
