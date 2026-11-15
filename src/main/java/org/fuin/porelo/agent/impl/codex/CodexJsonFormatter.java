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
            return trouble(event);
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
        if (event.get("item") instanceof Map<?, ?> item) {
            final String trouble = trouble(item);
            if (trouble != null) {
                return trouble;
            }
        }
        return trouble(event);
    }

    /** The words a status uses for a step that went wrong. */
    private static final java.util.Set<String> FAILED =
            java.util.Set.of(
                    "failed", "failure", "error", "killed", "rejected", "denied", "aborted");

    /**
     * Words for a reported error or warning, or {@code null}, so an event that says nothing stays
     * out. An error is named by the event's type, its level, a status of failure, an error mark, or
     * an error it carries; a warning by its type or level.
     */
    private static @Nullable String trouble(Map<?, ?> event) {
        final boolean error =
                names(event.get("type"), "error", "fail", "fatal", "exception")
                        || names(event.get("level"), "error", "fatal")
                        || failure(event.get("status"))
                        || marked(event.get("is_error"))
                        || marked(event.get("isError"))
                        || carries(event.get("error"));
        final boolean warning =
                names(event.get("type"), "warn") || names(event.get("level"), "warn");
        if (!error && !warning) {
            return null;
        }
        return printable((error ? "[error] " : "[warning] ") + oneLine(troubleText(event)));
    }

    private static boolean names(@Nullable Object value, String... words) {
        if (!(value instanceof String text)) {
            return false;
        }
        final String lower = text.toLowerCase(java.util.Locale.ROOT);
        for (final String word : words) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean failure(@Nullable Object status) {
        return status instanceof String text
                && FAILED.contains(text.strip().toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean marked(@Nullable Object value) {
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }

    /**
     * Whether a field holds an error: any text, a flag that is set, a number, a list or an object.
     */
    private static boolean carries(@Nullable Object error) {
        return nonBlank(error) != null
                || Boolean.TRUE.equals(error)
                || error instanceof Number
                || error instanceof java.util.List<?> list && !list.isEmpty()
                || error instanceof Map<?, ?> map && !map.isEmpty();
    }

    /**
     * What an error says: its own text or message, the event's message, the error, or what it is.
     */
    private static String troubleText(Map<?, ?> event) {
        final Object error = event.get("error");
        final String own = nonBlank(error);
        if (own != null) {
            return own;
        }
        if (error instanceof Map<?, ?> nested
                && nonBlank(nested.get("message")) instanceof String m) {
            return m;
        }
        final String message = nonBlank(event.get("message"));
        if (message != null) {
            return message;
        }
        if (error != null && !Boolean.TRUE.equals(error)) {
            return String.valueOf(error);
        }
        return String.valueOf(event.get("type"));
    }

    private static @Nullable String nonBlank(@Nullable Object value) {
        return value instanceof String text && !text.isBlank() ? text.strip() : null;
    }

    /**
     * Folds a message onto one line, so a line break in it - a Unicode separator included - cannot
     * pass for a line of the log, and drops the invisible marks that turn text around.
     */
    private static String oneLine(String text) {
        return text.replaceAll("\\p{Cf}", "")
                .replaceAll("[\\s\\u0085\\u2028\\u2029]+", " ")
                .strip();
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
