package org.fuin.porelo.agent.impl.codex;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.ContainerFile;
import org.fuin.porelo.agent.api.ContainerSetup;
import org.fuin.porelo.agent.api.SetupContext;
import org.fuin.porelo.wire.Json;

/**
 * Points Codex's task-local Responses provider at the broker using only its task token, and holds
 * the task to the subagents it allows.
 *
 * <p><strong>No subagent is a hook.</strong> Measured on 0.163.0: the feature switches leave {@code
 * spawn_agent} offered and a subagent starting, and {@code agents.max_depth} does not hold; a
 * {@code PreToolUse} hook answering deny does. Codex runs a hook only once trusted, and records
 * that as the hash of the hook's normalized form under a key of its file, its event and its place.
 * Written here ahead of the task, read in 0.163.0's {@code hooks/src/engine/discovery.rs}, so
 * {@code --dangerously-bypass-hook-trust}, which also runs any hook a project brings, is not
 * passed. If a pin changes the hash, the hook silently does not run; the scenario in {@code
 * session.feature} sees that.
 */
public final class CodexContainerSetup implements ContainerSetup {
    private static final Pattern BEARER = Pattern.compile("[A-Za-z0-9._~+/-]+=*");

    static final String CONFIG = "/home/agent/.codex/config.toml";
    static final String HOOKS = "/home/agent/.codex/hooks.json";

    /** The hook sees namespace and name joined; {@code spawn_agent} alone never fires. */
    private static final String MATCHER = "collaborationspawn_agent";

    /** Inline, since a {@link ContainerFile} carries no executable bit. */
    private static final String DENY =
            "cat >/dev/null; printf '%s' '{\"hookSpecificOutput\":{\"hookEventName\":\"PreToolUse\","
                    + "\"permissionDecision\":\"deny\","
                    + "\"permissionDecisionReason\":\"this task allows no subagents\"}}'";

    private static final long TIMEOUT_SECONDS = 10;

    /**
     * Returns the normalized form of the hook that Codex hashes: sorted keys, no spaces.
     *
     * @return The text its hash is taken of.
     */
    static String hookIdentity() {
        final Map<String, Object> handler = new LinkedHashMap<>();
        handler.put("async", Boolean.FALSE);
        handler.put("command", DENY);
        handler.put("timeout", Long.valueOf(TIMEOUT_SECONDS));
        handler.put("type", "command");
        final Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("event_name", "pre_tool_use");
        identity.put("hooks", List.of(handler));
        identity.put("matcher", MATCHER);
        return Json.write(identity);
    }

    private static String hooksFile() {
        final Map<String, Object> handler = new LinkedHashMap<>();
        handler.put("type", "command");
        handler.put("command", DENY);
        handler.put("timeout", Long.valueOf(TIMEOUT_SECONDS));
        final Map<String, Object> group = new LinkedHashMap<>();
        group.put("matcher", MATCHER);
        group.put("hooks", List.of(handler));
        return Json.write(Map.of("hooks", Map.of("PreToolUse", List.of(group))));
    }

    private static String subagentSettings(int subagents) {
        if (subagents > 0) {
            return "[agents]\nmax_concurrent_threads_per_session = " + subagents + "\n";
        }
        final byte[] digest;
        try {
            digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(hookIdentity().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new AgentException("This runtime has no SHA-256");
        }
        return "[hooks.state.\"%s:pre_tool_use:0:0\"]\ntrusted_hash = \"sha256:%s\"\n"
                .formatted(HOOKS, HexFormat.of().formatHex(digest));
    }

    private static List<ContainerFile> withHook(int subagents, ContainerFile config) {
        return subagents > 0
                ? List.of(config)
                : List.of(config, ContainerFile.of(HOOKS, hooksFile()));
    }

    /** {@inheritDoc} */
    @Override
    public List<ContainerFile> files(SetupContext context) {
        final String subagents = subagentSettings(context.subagents());
        if (context.endpoint().isEmpty() && context.token().isEmpty()) {
            return withHook(context.subagents(), ContainerFile.of(CONFIG, subagents));
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
                                .formatted(context.endpoint(), context.token(), workspace)
                        + "\n"
                        + subagents;
        return withHook(context.subagents(), ContainerFile.secret(CONFIG, config));
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
