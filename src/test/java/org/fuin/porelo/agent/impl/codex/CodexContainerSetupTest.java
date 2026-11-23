package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.fuin.porelo.agent.api.Agent;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.ContainerFile;
import org.fuin.porelo.agent.api.SetupContext;
import org.fuin.porelo.wire.Json;
import org.fuin.porelo.wire.LimitFactors;
import org.junit.jupiter.api.Test;

/** Guards broker routing, credential containment and configuration injection. */
class CodexContainerSetupTest {
    private final CodexAgent agent = new CodexAgent();

    @Test
    void brokeredTasksUseOnlyTheSuppliedEndpointAndPhantomToken() {
        final String endpoint = "http://broker.example:43121/custom/v1";
        final List<ContainerFile> files = files("phantom_credential-123", endpoint);
        assertThat(files)
                .as("a brokered task must not fall back to upstream authentication")
                .hasSize(2);
        final ContainerFile config = files.getFirst();
        assertThat(config.path())
                .as("provider settings belong in the explicit user profile")
                .isEqualTo("/home/agent/.codex/config.toml");
        assertThat(config.ownerOnly()).as("the task token must be owner-only").isTrue();
        assertThat(config.content())
                .as("the CLI must use the broker's supplied route without appending a dialect")
                .contains(
                        "model_provider = \"broker\"",
                        "[model_providers.broker]",
                        "base_url = \"" + endpoint + "\"",
                        "experimental_bearer_token = \"phantom_credential-123\"",
                        "wire_api = \"responses\"",
                        "requires_openai_auth = false",
                        "supports_websockets = false")
                .doesNotContain("api.openai.com", "OPENAI_API_KEY", "env_key", "auth.json");
    }

    @Test
    void hidesItsSkillsFromATaskBrokeredOrNot() {
        // Measured on 0.163.0: without this the agent lists the bundled skills and one in its
        // home; with it, none. It hides them: a skill's file can still be read through the shell.
        final List<ContainerFile> brokered =
                files("phantom_credential-123", "https://broker.example.org/v1");
        final List<ContainerFile> unbrokered = files("", "");

        for (final List<ContainerFile> given : List.of(brokered, unbrokered)) {
            assertThat(given.getFirst().content())
                    .as("the list of skills is left out of the agent's instructions")
                    .contains("[skills]\ninclude_instructions = false\n");
        }
    }

    @Test
    void anUnbrokeredTaskIsWrittenNoLogin() {
        final ContainerFile config = files("", "").getFirst();
        assertThat(config.path()).isEqualTo(CodexContainerSetup.CONFIG);
        assertThat(config.ownerOnly()).as("it holds no token").isFalse();
        assertThat(config.content())
                .as("a shell without a provider needs no fabricated login")
                .doesNotContain("model_provider", "bearer");
    }

    @Test
    void aTaskThatAllowsNoSubagentsDeniesTheToolWithATrustedHook() {
        for (final List<ContainerFile> given :
                List.of(files("", ""), files("phantom", "http://broker.example:1/v1"))) {
            final ContainerFile hooks =
                    given.stream()
                            .filter(file -> file.path().equals(CodexContainerSetup.HOOKS))
                            .findFirst()
                            .orElseThrow();
            final Map<?, ?> entry =
                    (Map<?, ?>)
                            ((List<?>)
                                            ((Map<?, ?>)
                                                            ((Map<?, ?>)
                                                                            Json.parse(
                                                                                    hooks
                                                                                            .content()))
                                                                    .get("hooks"))
                                                    .get("PreToolUse"))
                                    .getFirst();
            // Measured on 0.163.0: the hook sees namespace and name joined.
            assertThat(entry.get("matcher")).isEqualTo("collaborationspawn_agent");
            final Map<?, ?> hook = (Map<?, ?>) ((List<?>) entry.get("hooks")).getFirst();
            assertThat((String) hook.get("command")).contains("permissionDecision");

            // Codex runs a hook only when this hash is the one it computes for the hook.
            assertThat(given.getFirst().content())
                    .contains(
                            "[hooks.state.\"/home/agent/.codex/hooks.json:pre_tool_use:0:0\"]\n"
                                    + "trusted_hash = \"sha256:");
            assertThat(given.getFirst().content()).doesNotContain("max_concurrent_threads");
        }
    }

    @Test
    void aTaskThatAllowsSubagentsIsHeldToTheCountAndGetsNoHook() {
        final List<ContainerFile> files =
                new CodexContainerSetup()
                        .files(new SetupContext("", "api-key", "/workspace", "", "openai", 2));
        assertThat(files).hasSize(1);
        assertThat(files.getFirst().content())
                .contains("[agents]\nmax_concurrent_threads_per_session = 2\n")
                .doesNotContain("hooks.state");
    }

    @Test
    void theDenyHookIsGivenItsTenSecondsTimesTheMachinesFactorAndStaysTrusted() throws Exception {
        // On a slow machine a hook cut off before it answers must not let a subagent through, and
        // a hook whose hash no longer matches would not run at all.
        assertThat(hookTimeout(agent)).as("all four factors 1.0").isEqualTo(10L);
        final Agent slow = agent.withLimits(LimitFactors.parse("machines=2.5"));
        assertThat(hookTimeout(slow)).as("ten seconds times 2.5").isEqualTo(25L);
        assertThat(hookTimeout(agent.withLimits(LimitFactors.parse("machines=0.15"))))
                .as("1.5 seconds, rounded up to whole seconds as the hook file takes them")
                .isEqualTo(2L);
        assertThat(hookTimeout(agent.withLimits(LimitFactors.parse("services=9,polling=9"))))
                .as("only the machines factor applies")
                .isEqualTo(10L);

        final List<ContainerFile> files =
                slow.containerSetup().files(new SetupContext("", "", "", "", ""));
        assertThat(files.getFirst().content())
                .as("the trusted hash is the one of the hook with 25 seconds")
                .contains("trusted_hash = \"sha256:" + identityHash(files) + "\"");
    }

    private static long hookTimeout(final Agent given) {
        return ((Number)
                        hook(given.containerSetup().files(new SetupContext("", "", "", "", "")))
                                .get("timeout"))
                .longValue();
    }

    private static Map<?, ?> hookEntry(final List<ContainerFile> files) {
        final ContainerFile hooks =
                files.stream()
                        .filter(file -> file.path().equals(CodexContainerSetup.HOOKS))
                        .findFirst()
                        .orElseThrow();
        final Map<?, ?> root = (Map<?, ?>) Json.parse(hooks.content());
        return (Map<?, ?>) ((List<?>) ((Map<?, ?>) root.get("hooks")).get("PreToolUse")).getFirst();
    }

    private static Map<?, ?> hook(final List<ContainerFile> files) {
        return (Map<?, ?>) ((List<?>) hookEntry(files).get("hooks")).getFirst();
    }

    /** What Codex hashes of the hook, read from the file it reads. */
    private static String identityHash(final List<ContainerFile> files) throws Exception {
        final Map<?, ?> entry = hookEntry(files);
        final Map<?, ?> hook = hook(files);
        final String identity =
                "{\"event_name\":\"pre_tool_use\",\"hooks\":[{\"async\":false,\"command\":"
                        + Json.write(hook.get("command"))
                        + ",\"timeout\":"
                        + ((Number) hook.get("timeout")).longValue()
                        + ",\"type\":\"command\"}],\"matcher\":"
                        + Json.write(entry.get("matcher"))
                        + "}";
        return HexFormat.of()
                .formatHex(
                        MessageDigest.getInstance("SHA-256")
                                .digest(identity.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void trustsExactlyTheHookItWrites() throws Exception {
        final List<ContainerFile> files = files("", "");

        // What Codex 0.163.0 hashes, read in its version_for_toml, here from the file it reads, so
        // a hook and a hash that part ways fail. This cannot show that Codex computes the same:
        // the literal below is the hash the scenario on the VM saw it accept and run.
        final String digest = identityHash(files);

        assertThat(digest)
                .isEqualTo("8c865e67cea45239b854f325aa9fa6ca5ef12f5a1e2a32f2043cae94fd803511");
        assertThat(files.getFirst().content()).contains("trusted_hash = \"sha256:" + digest + "\"");
    }

    @Test
    void aBrokeredTaskTrustsOnlyItsExplicitWorkspace() {
        assertThat(files("phantom", "http://broker.example/v1", "/work/task").getFirst().content())
                .as("the reviewed task workspace must not hang at a first-run trust dialog")
                .contains("[projects.\"/work/task\"]\ntrust_level = \"trusted\"")
                .doesNotContain("[projects.\"/\"]", "[projects.\"/home/agent\"]");
    }

    @Test
    void workspaceTextCannotEscapeItsTrustTable() {
        assertThat(
                        files("phantom", "http://broker.example/v1", "/work/quoted\"\\name")
                                .getFirst()
                                .content())
                .as("quotes and backslashes in a task path cannot add TOML settings")
                .contains("[projects.\"/work/quoted\\\"\\\\name\"]\ntrust_level = \"trusted\"");
    }

    @Test
    void invalidWorkspacesCannotBroadenTrust() {
        for (final String workspace :
                List.of(
                        "",
                        "/",
                        "relative",
                        "/work/../",
                        "/work/./task",
                        "/work//task",
                        "/work/task/",
                        "/work/secret-marker\n")) {
            assertThatThrownBy(() -> files("phantom", "http://broker.example/v1", workspace))
                    .as("trust must not expand to an ambiguous or broader path")
                    .isInstanceOf(AgentException.class)
                    .hasMessageNotContaining("secret-marker")
                    .hasNoCause();
        }
    }

    @Test
    void incompleteRoutingCannotFallBackToAnUpstream() {
        for (final String[] pair :
                List.of(
                        new String[] {"", "http://broker.example/v1"},
                        new String[] {"phantom", ""},
                        new String[] {"phantom", "   "})) {
            assertThatThrownBy(() -> files(pair[0], pair[1]))
                    .as("endpoint and task token must be supplied together")
                    .isInstanceOf(AgentException.class);
        }
    }

    @Test
    void unsafeEndpointsAreRefusedWithoutEchoingTheirContents() {
        for (final String endpoint :
                List.of(
                        "file:///secret-marker",
                        "http://secret-marker@broker.example/v1",
                        "http://broker.example/v1?key=secret-marker",
                        "http://broker.example/v1#secret-marker",
                        "http://broker.example:70000/secret-marker",
                        "http://broker.example/secret-marker\n\"",
                        "http://broker.example/secret-marker\\")) {
            assertThatThrownBy(() -> files("phantom", endpoint))
                    .as("external endpoint input cannot inject TOML or leak via diagnostics")
                    .isInstanceOf(AgentException.class)
                    .hasMessageNotContaining("secret-marker")
                    .hasNoCause();
        }
    }

    @Test
    void unsafeTokensCannotInjectConfigurationOrAppearInDiagnostics() {
        for (final String token :
                List.of(
                        "secret-marker\n",
                        "secret-marker\r",
                        "secret-marker\"",
                        "secret-marker\\",
                        "secret-marker ",
                        "secret-marker\t")) {
            assertThatThrownBy(() -> files(token, "http://broker.example/v1"))
                    .as("a bearer token cannot escape its TOML value or leak on refusal")
                    .isInstanceOf(AgentException.class)
                    .hasMessageNotContaining("secret-marker")
                    .hasNoCause();
        }
    }

    private List<ContainerFile> files(String token, String endpoint) {
        return files(token, endpoint, "/workspace");
    }

    private List<ContainerFile> files(String token, String endpoint, String workspace) {
        return agent.containerSetup()
                .files(new SetupContext(token, "api-key", workspace, endpoint, "openai"));
    }
}
