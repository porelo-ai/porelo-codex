package org.fuin.porelo.agent.impl.codex;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.ContainerFile;
import org.fuin.porelo.agent.api.ContainerSetup;
import org.fuin.porelo.agent.api.SetupContext;

/** Points Codex's task-local Responses provider at the broker using only its task token. */
public final class CodexContainerSetup implements ContainerSetup {
    private static final Pattern BEARER = Pattern.compile("[A-Za-z0-9._~+/-]+=*");

    /** {@inheritDoc} */
    @Override
    public List<ContainerFile> files(SetupContext context) {
        if (context.endpoint().isEmpty() && context.token().isEmpty()) {
            return List.of();
        }
        validateEndpoint(context.endpoint());
        if (!BEARER.matcher(context.token()).matches()) {
            throw new AgentException(
                    "Codex needs a nonempty bearer task token without unsafe characters");
        }
        final String workspace = trustedWorkspace(context.workspace());
        final String config =
                """
                model_provider = "broker"

                [model_providers.broker]
                name = "Task broker"
                base_url = "%s"
                experimental_bearer_token = "%s"
                wire_api = "responses"
                requires_openai_auth = false
                supports_websockets = false

                [projects."%s"]
                trust_level = "trusted"
                """
                        .formatted(context.endpoint(), context.token(), workspace);
        return List.of(ContainerFile.secret("/home/agent/.codex/config.toml", config));
    }

    private static String trustedWorkspace(String workspace) {
        if (!workspace.startsWith("/")
                || workspace.chars().anyMatch(Character::isISOControl)
                || List.of(workspace.substring(1).split("/", -1)).stream()
                        .anyMatch(
                                part -> part.isEmpty() || part.equals(".") || part.equals(".."))) {
            throw new AgentException(
                    "Codex needs an absolute, normalized, non-root task workspace");
        }
        return workspace.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void validateEndpoint(String endpoint) {
        try {
            final URI uri = new URI(endpoint);
            if (!("http".equalsIgnoreCase(uri.getScheme())
                            || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getPort() == 0
                    || uri.getPort() > 65535
                    || endpoint.chars()
                            .anyMatch(c -> c < 0x21 || c > 0x7e || c == '"' || c == '\\')) {
                throw new AgentException("Codex needs a plain HTTP or HTTPS broker endpoint");
            }
        } catch (URISyntaxException exception) {
            // URI diagnostics repeat input that may contain credentials or injected configuration.
            throw new AgentException("Codex needs a plain HTTP or HTTPS broker endpoint");
        }
    }
}
