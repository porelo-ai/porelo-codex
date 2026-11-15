package org.fuin.porelo.agent.impl.codex;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fuin.porelo.agent.api.AgentEnd;
import org.fuin.porelo.agent.api.LogFormatter;
import org.fuin.porelo.wire.Json;
import org.jspecify.annotations.Nullable;

/**
 * Reads Codex's JSONL output without treating a failed turn as an answered prompt. A person reads
 * what the agent says and does: an event that reports an error is always shown, as such; any other
 * event this class does not word is left out rather than shown as it came; a line that is not JSON
 * still shows as it came.
 */
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
            final String trouble = trouble(item);
            if (trouble != null) {
                return trouble;
            }
        }
        return trouble(event);
    }

    /** Words for a reported failure, or {@code null}, so an event that says nothing stays out. */
    private static @Nullable String trouble(Map<?, ?> event) {
        if (!reportsTrouble(event)) {
            return null;
        }
        return printable("[error] " + oneLine(troubleText(event)));
    }

    /** Whether an event reports an error by its kind, its status or an error it carries. */
    private static boolean reportsTrouble(Map<?, ?> event) {
        return names(event.get("type"), "error")
                || names(event.get("status"), "failed")
                || Boolean.TRUE.equals(event.get("is_error"))
                || Boolean.TRUE.equals(event.get("isError"))
                || nonBlank(event.get("error")) != null
                || event.get("error") instanceof Map<?, ?>;
    }

    private static boolean names(@Nullable Object value, String word) {
        return value instanceof String text
                && text.toLowerCase(java.util.Locale.ROOT).contains(word);
    }

    /** What an error says: its own text, its message, or what the event is. */
    private static String troubleText(Map<?, ?> event) {
        final String error = nonBlank(event.get("error"));
        if (error != null) {
            return error;
        }
        if (event.get("error") instanceof Map<?, ?> nested) {
            final String message = nonBlank(nested.get("message"));
            if (message != null) {
                return message;
            }
        }
        final String message = nonBlank(event.get("message"));
        return message != null ? message : String.valueOf(event.get("type"));
    }

    private static @Nullable String nonBlank(@Nullable Object value) {
        return value instanceof String text && !text.isBlank() ? text.strip() : null;
    }

    /** Folds a message onto one line, so a line it holds cannot pass for a line of its own. */
    private static String oneLine(String text) {
        return text.replaceAll("\\s+", " ");
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
