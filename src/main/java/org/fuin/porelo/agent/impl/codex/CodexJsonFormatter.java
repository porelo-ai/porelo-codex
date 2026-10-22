package org.fuin.porelo.agent.impl.codex;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fuin.porelo.agent.api.AgentEnd;
import org.fuin.porelo.agent.api.LogFormatter;
import org.fuin.porelo.wire.Json;
import org.jspecify.annotations.Nullable;

/** Reads Codex's JSONL output without treating a failed turn as an answered prompt. */
public final class CodexJsonFormatter implements LogFormatter {
    private static final int MAX_CODE_POINTS = 300;
    private static final Pattern STATUS = Pattern.compile("^unexpected status ([45][0-9]{2})\\b");

    /** {@inheritDoc} */
    @Override
    public @Nullable String format(String line) {
        final Map<?, ?> event = event(line);
        if (event == null) {
            return printable(line);
        }
        final Object type = event.get("type");
        if ("thread.started".equals(type) || "turn.started".equals(type)) {
            return null;
        }
        final AgentEnd end = ended(line);
        if (end != null) {
            return end.finished() ? "-- done" : "-- error: " + end.error();
        }
        if ("item.completed".equals(type) && event.get("item") instanceof Map<?, ?> item) {
            if ("agent_message".equals(item.get("type"))
                    && item.get("text") instanceof String text) {
                return printable(text);
            }
            if ("command_execution".equals(item.get("type"))) {
                return printable(
                        "[command] "
                                + text(item, "command", "")
                                + " (exit "
                                + item.get("exit_code")
                                + ")");
            }
        }
        return printable(line);
    }

    static @Nullable AgentEnd ended(String line) {
        final Map<?, ?> event = event(line);
        if (event == null) {
            return null;
        }
        if ("turn.completed".equals(event.get("type"))) {
            return new AgentEnd(true, "", AgentEnd.AGENT, null);
        }
        if (!"turn.failed".equals(event.get("type"))) {
            return null;
        }
        final String message =
                event.get("error") instanceof Map<?, ?> error
                        ? text(error, "message", "Codex turn failed")
                        : "Codex turn failed";
        final Integer status = status(message);
        return new AgentEnd(
                false,
                printable(message),
                status == null ? AgentEnd.AGENT : AgentEnd.PROVIDER,
                status);
    }

    private static @Nullable Integer status(String message) {
        final Matcher match = STATUS.matcher(message);
        if (match.find()) {
            return Integer.valueOf(match.group(1));
        }
        final Map<?, ?> detail = event(message);
        if (detail != null && detail.get("status") instanceof Number number) {
            final int status = number.intValue();
            if (status >= 400 && status <= 599) {
                return status;
            }
        }
        return null;
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

    private static String text(Map<?, ?> map, String key, String fallback) {
        return map.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static String printable(String text) {
        final StringBuilder clean = new StringBuilder();
        text.codePoints()
                .filter(value -> !Character.isISOControl(value) || value == '\n' || value == '\t')
                .limit(MAX_CODE_POINTS)
                .forEach(clean::appendCodePoint);
        return clean.toString();
    }
}
