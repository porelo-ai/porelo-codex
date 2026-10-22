package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.ContainerFile;
import org.fuin.porelo.agent.api.SetupContext;
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
                .hasSize(1);
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
    void anUnbrokeredShellNeedsNoCredentialFile() {
        assertThat(files("", ""))
                .as("a shell without a provider needs no fabricated login")
                .isEmpty();
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
