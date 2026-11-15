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
            final String tool = tool(item);
            if (tool != null) {
                return tool;
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

    /**
     * A tool Codex used, on one line with what it was used on and whether it failed, or {@code
     * null} for an item that is no tool this class words. Never its output or what it wrote.
     */
    private static @Nullable String tool(Map<?, ?> item) {
        final String used =
                switch (String.valueOf(item.get("type"))) {
                    case "command_execution" -> command(item);
                    case "file_change" -> "[edit] " + paths(item.get("changes"));
                    case "mcp_tool_call" ->
                            "[mcp] "
                                    + oneLine(text(item, "server", "?"))
                                    + "/"
                                    + oneLine(text(item, "tool", "?"));
                    case "web_search" -> "[search] " + oneLine(text(item, "query", ""));
                    case "todo_list" ->
                            item.get("items") instanceof java.util.List<?> items
                                    ? "[todo] " + count(items.size(), "item")
                                    : null;
                    default -> null;
                };
        if (used == null) {
            return null;
        }
        if (!failed(item)) {
            return printable(used);
        }
        final String reason = reason(item);
        return printable(used + " failed" + (reason == null ? "" : ": " + oneLine(reason)));
    }

    /** A command by its first line, a sign that it has more, and its exit code when it has one. */
    private static String command(Map<?, ?> item) {
        final String command = text(item, "command", "");
        final String first = firstLine(command);
        return "[command] "
                + oneLine(first)
                + (command.strip().lines().count() > 1 ? " …" : "")
                + (item.get("exit_code") instanceof Number exit
                        ? " (exit " + number(exit) + ")"
                        : "");
    }

    /** The first path a file change names, and how many more it names. */
    private static String paths(@Nullable Object changes) {
        final java.util.List<String> paths = new java.util.ArrayList<>();
        if (changes instanceof java.util.List<?> list) {
            for (final Object change : list) {
                if (change instanceof Map<?, ?> map
                        && nonBlank(map.get("path")) instanceof String p) {
                    paths.add(p);
                }
            }
        }
        if (paths.isEmpty()) {
            return "a file";
        }
        return oneLine(paths.getFirst())
                + (paths.size() > 1 ? " and " + (paths.size() - 1) + " more" : "");
    }

    /** Whether a tool's item says it failed: a status of failure, or a command's non-zero exit. */
    private static boolean failed(Map<?, ?> item) {
        return failure(item.get("status"))
                || item.get("exit_code") instanceof Number exit && exit.doubleValue() != 0;
    }

    /**
     * Why a tool failed, by the first line of its error, or of a command's output the first line
     * that names an error and else its last; {@code null} when it gives none.
     */
    private static @Nullable String reason(Map<?, ?> item) {
        final Object error = item.get("error");
        if (nonBlank(error) instanceof String own) {
            return firstLine(own);
        }
        if (error instanceof Map<?, ?> nested
                && nonBlank(nested.get("message")) instanceof String message) {
            return firstLine(message);
        }
        if (!(nonBlank(item.get("aggregated_output")) instanceof String output)) {
            return null;
        }
        final java.util.List<String> lines =
                output.lines().filter(line -> !line.isBlank()).toList();
        return lines.stream()
                .filter(line -> ERROR_LINE.matcher(line).find())
                .findFirst()
                .orElse(lines.getLast())
                .strip();
    }

    /** The words by which a line of a command's output says what went wrong. */
    private static final Pattern ERROR_LINE =
            Pattern.compile(
                    "(?i)error|fatal|fail|not found|no such|denied|cannot|can't|refused|"
                            + "exception|abort|invalid|unknown");

    /** The first line of a text that is not blank; the text itself when it has none. */
    private static String firstLine(String text) {
        return text.lines().filter(line -> !line.isBlank()).findFirst().orElse(text).strip();
    }

    private static String count(long n, String thing) {
        return n + " " + thing + (n == 1 ? "" : "s");
    }

    private static String number(Number value) {
        return value.doubleValue() == Math.floor(value.doubleValue())
                ? String.valueOf(value.longValue())
                : String.valueOf(value);
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
