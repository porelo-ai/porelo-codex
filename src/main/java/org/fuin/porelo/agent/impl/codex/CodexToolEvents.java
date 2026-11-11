package org.fuin.porelo.agent.impl.codex;

import java.util.List;
import java.util.Map;
import org.fuin.porelo.agent.api.ToolEvent;
import org.fuin.porelo.agent.api.ToolEvent.Kind;
import org.fuin.porelo.agent.api.ToolEvent.Phase;
import org.fuin.porelo.wire.Json;
import org.jspecify.annotations.Nullable;

/** Reads tool lifecycle events from one Codex JSONL line without keeping call state. */
public final class CodexToolEvents {

    private CodexToolEvents() {}

    /**
     * Maps a raw output line to the tool moment it records.
     *
     * @param line One line of Codex output.
     * @return The tool event, or an empty list for unrelated or unreadable input.
     */
    public static List<ToolEvent> read(String line) {
        final Map<?, ?> event = event(line);
        if (event == null) {
            return List.of();
        }
        final Phase phase;
        if ("item.started".equals(event.get("type"))) {
            phase = Phase.START;
        } else if ("item.completed".equals(event.get("type"))) {
            phase = Phase.END;
        } else {
            return List.of();
        }
        if (!(event.get("item") instanceof Map<?, ?> item)
                || !(item.get("id") instanceof String id)
                || id.isBlank()
                || !(item.get("type") instanceof String type)
                || type.isBlank()
                || "agent_message".equals(type)
                || "reasoning".equals(type)) {
            return List.of();
        }
        final Kind kind =
                switch (type) {
                    case "command_execution" -> Kind.SHELL;
                    case "file_change" -> Kind.EDIT;
                    case "mcp_tool_call" -> Kind.MCP;
                    case "web_search" -> Kind.WEB;
                    case "collab_tool_call" -> Kind.SUB_AGENT;
                    default -> Kind.OTHER;
                };
        final String tool =
                item.get("tool") instanceof String name && !name.isBlank() ? name : type;
        final String command =
                kind == Kind.SHELL && item.get("command") instanceof String value ? value : null;
        return List.of(
                new ToolEvent(
                        0, id, phase, tool, kind, command, false, null, null, null, null, null));
    }

    private static @Nullable Map<?, ?> event(String line) {
        if (!line.stripLeading().startsWith("{")) {
            return null;
        }
        try {
            final Object value = Json.parse(line);
            return value instanceof Map<?, ?> map ? map : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
