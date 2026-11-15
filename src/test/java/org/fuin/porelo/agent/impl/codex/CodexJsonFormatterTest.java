package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.fuin.porelo.agent.api.AgentEnd;
import org.junit.jupiter.api.Test;

/**
 * Guards terminal injection, unknown or malformed lifecycle records, and that a person sees words
 * and every error, never an event as it came.
 */
class CodexJsonFormatterTest {

    /**
     * An unattended run recorded with Codex 0.163.0 in a task: a command that starts another in the
     * background, waits for it and commits, and a one-line answer; the thread's id replaced.
     */
    private static final Path BACKGROUND_RUN =
            Path.of("src/test/resources/streams/unattended-background-run-0.163.0.jsonl");

    private final CodexJsonFormatter formatter = new CodexJsonFormatter();

    @Test
    void aRecordedRunShowsNoLineOfRawJson() throws IOException {
        final List<String> lines = Files.readAllLines(BACKGROUND_RUN, StandardCharsets.UTF_8);

        assertThat(lines).as("the recording holds a started item and an error item").hasSize(8);
        assertThat(lines.stream().map(formatter::format).filter(Objects::nonNull))
                .as("every line shown is words, never an event as it came")
                .isNotEmpty()
                .noneMatch(shown -> shown.stripLeading().startsWith("{"))
                .anyMatch(shown -> shown.startsWith("[error] Model metadata"));
    }

    @Test
    void anEventThatSaysNothingToAPersonIsLeftOut() {
        assertThat(
                        List.of(
                                        "{\"type\":\"item.started\",\"item\":{\"type\":\"command_execution\",\"command\":\"ls\"}}",
                                        "{\"type\":\"item.completed\",\"item\":{\"type\":\"reasoning\",\"text\":\"thinking\"}}",
                                        "{\"type\":\"item.updated\",\"item\":{\"type\":\"todo_list\"}}",
                                        "{\"type\":\"something_new\"}")
                                .stream()
                                .map(formatter::format)
                                .toList())
                .as("known and unknown events that report no error")
                .containsOnlyNulls();
    }

    @Test
    void anEventThatReportsAnErrorIsAlwaysShown() {
        assertThat(
                        formatter.format(
                                "{\"type\":\"item.completed\",\"item\":{\"id\":\"item_0\",\"type\":\"error\","
                                        + "\"message\":\"Model metadata not found.\"}}"))
                .as("an error item, as 0.163.0 reports a model it does not know")
                .isEqualTo("[error] Model metadata not found.");
        assertThat(formatter.format("{\"type\":\"error\",\"message\":\"retrying\"}"))
                .as("an error event of the stream itself")
                .isEqualTo("[error] retrying");
        assertThat(formatter.format("{\"type\":\"something_new\",\"error\":\"rate limited\"}"))
                .as("an unknown event with an error text")
                .isEqualTo("[error] rate limited");
        assertThat(
                        formatter.format(
                                "{\"type\":\"something_new\",\"error\":{\"message\":\"overloaded\"}}"))
                .as("an error given as an object with a message")
                .isEqualTo("[error] overloaded");
        assertThat(formatter.format("{\"type\":\"error\"}"))
                .as("an error that gives no words is still shown, by what it is")
                .isEqualTo("[error] error");
        assertThat(
                        formatter.format(
                                "{\"type\":\"something_new\",\"status\":\"failed\",\"message\":\"gone\"}"))
                .as("an unknown event whose status says it failed")
                .isEqualTo("[error] gone");
        assertThat(formatter.format("{\"type\":\"something_new\",\"isError\":true}"))
                .as("an unknown event marked as an error")
                .isEqualTo("[error] something_new");
        assertThat(formatter.format("{\"type\":\"error\",\"message\":\"first\\n[error] forged\"}"))
                .as("a message cannot open a line of its own")
                .doesNotContain("\n");
    }

    @Test
    void aLineThatIsNotAnEventShowsAsItCame() {
        assertThat(formatter.format("Reading additional input from stdin..."))
                .as("plain text Codex writes, which may be the only trace of a failure")
                .isEqualTo("Reading additional input from stdin...");
        assertThat(formatter.format("{not valid json"))
                .as("a partial line")
                .isEqualTo("{not valid json");
    }

    @Test
    void terminalControlsFromModelOutputCannotExecute() {
        assertThat(
                        formatter.format(
                                "{\"type\":\"item.completed\",\"item\":{\"type\":\"agent_message\",\"text\":\"before\\u001b[2Jafter\"}}"))
                .as("an escape from model output must not reach a person's terminal")
                .doesNotContain("\u001b");
    }

    @Test
    void malformedAndUnfinishedEventsCannotFinishARun() {
        for (final String event :
                new String[] {
                    "{",
                    "plain text",
                    "[]",
                    "{\"type\":\"turn.started\"}",
                    "{\"type\":\"error\",\"message\":\"retrying\"}"
                }) {
            assertThat(CodexJsonFormatter.ended(event))
                    .as("not an explicit terminal event: %s", event)
                    .isNull();
        }
    }

    @Test
    void anUnstructuredFailedTurnStillCannotSucceed() {
        final AgentEnd end = CodexJsonFormatter.ended("{\"type\":\"turn.failed\"}");
        assertThat(end).as("the explicit failed-turn marker").isNotNull();
        assertThat(end.finished())
                .as("missing error details cannot turn failure into success")
                .isFalse();
    }

    @Test
    void structuredProviderStatusRemainsAProviderFailure() {
        final AgentEnd end =
                CodexJsonFormatter.ended(
                        "{\"type\":\"turn.failed\",\"error\":{\"message\":\"{\\\"status\\\":400,\\\"error\\\":{\\\"message\\\":\\\"unsupported value\\\"}}\"}}");
        assertThat(end).as("the measured JSON-encoded provider refusal").isNotNull();
        assertThat(end.source()).as("provider response provenance").isEqualTo(AgentEnd.PROVIDER);
        assertThat(end.status()).as("HTTP status in the structured response").isEqualTo(400);
    }

    @Test
    void anErrorIsShownWhereverItStands() {
        assertThat(
                        List.of(
                                        "{\"type\":\"turn.started\",\"error\":\"quota exceeded\"}",
                                        "{\"type\":\"item.started\",\"item\":{\"type\":\"error\",\"message\":\"quota exceeded\"}}",
                                        "{\"type\":\"item.updated\",\"item\":{\"type\":\"file_change\",\"status\":\"failed\","
                                                + "\"error\":\"quota exceeded\"}}",
                                        "{\"type\":\"something_new\",\"status\":\"Error\",\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"something_new\",\"error\":true,\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"stream_failure\",\"message\":\"quota exceeded\"}")
                                .stream()
                                .map(formatter::format)
                                .toList())
                .as(
                        "an error in a lifecycle event, in an item of any phase, by a status in any case,"
                                + " a flag or its type")
                .allSatisfy(
                        shown ->
                                assertThat(shown).startsWith("[error]").contains("quota exceeded"));
        assertThat(
                        formatter.format(
                                "{\"type\":\"something_new\",\"level\":\"warning\",\"message\":\"slow\"}"))
                .as("a warning")
                .startsWith("[warning]")
                .contains("slow");
        assertThat(
                        formatter.format(
                                "{\"type\":\"error\",\"message\":\"first\\u2028[error] forged\"}"))
                .as("a Unicode line separator cannot open a line of its own")
                .doesNotContain("\u2028");
    }

    /** A file change and a failed command, recorded from Codex's stream for the tool events. */
    private static final Path RECORDED_EDIT = Path.of("src/test/resources/tool-events/edit.jsonl");

    private static final Path RECORDED_FAILURE =
            Path.of("src/test/resources/tool-events/failed.jsonl");

    @Test
    void aRecordedCommandShowsOnlyItsFirstLine() throws IOException {
        assertThat(shown(BACKGROUND_RUN))
                .as("the recorded command by its first line, not the script it runs")
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .startsWith("[command]")
                                        .contains("sleep 4; echo BG-DONE > bg.txt")
                                        .doesNotContain("git add note.txt", "\n"))
                .noneMatch(line -> line.contains("failed"));
    }

    @Test
    void aRecordedFailedCommandSaysSoWithItsError() throws IOException {
        assertThat(shown(RECORDED_FAILURE))
                .as("the recorded command that exited 127")
                .singleElement()
                .satisfies(
                        line ->
                                assertThat(line)
                                        .startsWith("[command]")
                                        .contains("failed", "127", "rg: command not found")
                                        .doesNotContain("\n", "<im"));
    }

    @Test
    void aRecordedFileChangeShowsItsPath() throws IOException {
        assertThat(shown(RECORDED_EDIT))
                .as("the recorded file change, once, by its path")
                .singleElement()
                .satisfies(
                        line ->
                                assertThat(line)
                                        .startsWith("[edit]")
                                        .contains("/workspace/README"));
    }

    @Test
    void aFileChangeShowsItsFirstPathAndCountsTheOthers() {
        assertThat(
                        completed(
                                "{\"type\":\"file_change\",\"status\":\"completed\",\"changes\":["
                                        + "{\"path\":\"a.txt\",\"kind\":\"add\"},"
                                        + "{\"path\":\"b.txt\",\"kind\":\"update\"},"
                                        + "{\"path\":\"c.txt\",\"kind\":\"delete\"}]}"))
                .as("three paths: the first, and the others counted")
                .startsWith("[edit]")
                .contains("a.txt", "2")
                .doesNotContain("b.txt", "failed");
        assertThat(
                        completed(
                                "{\"type\":\"file_change\",\"status\":\"failed\",\"changes\":["
                                        + "{\"path\":\"a.txt\",\"kind\":\"add\"}]}"))
                .as("a file change that failed")
                .contains("a.txt", "failed");
    }

    @Test
    void anMcpCallShowsItsServerAndTool() {
        assertThat(
                        completed(
                                "{\"type\":\"mcp_tool_call\",\"server\":\"notes\",\"tool\":\"find\","
                                        + "\"arguments\":{\"q\":\"secret\"},\"status\":\"completed\"}"))
                .as("an MCP call by its server and tool, not its arguments")
                .startsWith("[mcp]")
                .contains("notes/find")
                .doesNotContain("secret", "failed");
        assertThat(
                        completed(
                                "{\"type\":\"mcp_tool_call\",\"server\":\"notes\",\"tool\":\"find\","
                                        + "\"status\":\"failed\",\"error\":{\"message\":\"no such note\\nmore\"}}"))
                .as("an MCP call that failed, with its error on one line")
                .contains("notes/find", "failed", "no such note", "more")
                .doesNotContain("\n");
    }

    @Test
    void aWebSearchShowsItsQuery() {
        assertThat(completed("{\"type\":\"web_search\",\"query\":\"jspecify nullmarked\"}"))
                .as("a web search by its query")
                .startsWith("[search]")
                .contains("jspecify nullmarked");
    }

    @Test
    void aTodoListShowsItsNumberOfItems() {
        assertThat(
                        completed(
                                "{\"type\":\"todo_list\",\"items\":[{\"text\":\"one\",\"completed\":true},"
                                        + "{\"text\":\"two\",\"completed\":false},"
                                        + "{\"text\":\"three\",\"completed\":false}]}"))
                .as("a todo list by its number of items, not their words")
                .contains("3")
                .doesNotContain("one");
    }

    @Test
    void aCommandShowsItsFirstLineAndItsFailure() {
        assertThat(command("\"/bin/bash -lc \\\"ls\\ncat a\\\"\"", "0", "\"completed\"", "\"x\""))
                .as("a command that succeeded, by its first line")
                .startsWith("[command]")
                .contains("ls")
                .doesNotContain("cat a", "failed", "\n");
        assertThat(command("\"false\"", "1", "\"completed\"", "\"\""))
                .as("a non-zero exit is a failure")
                .contains("failed", "1");
        assertThat(command("\"true\"", "null", "\"failed\"", "\"\""))
                .as("a failed status is a failure")
                .contains("failed")
                .doesNotContain("null");
        assertThat(
                        command(
                                "\"make\"",
                                "2",
                                "\"failed\"",
                                "\"building\\n\\nmake: *** [all] Error 2\\n0 failures\\nlast\""))
                .as("a failure by the first three lines of its output that are not blank")
                .contains("failed", "building", "make: *** [all] Error 2", "0 failures")
                .doesNotContain("last", "\n");
        assertThat(command("\"" + "x".repeat(5000) + "\"", "1", "\"failed\"", "\"boom\""))
                .as("a long command that failed keeps its failure, exit and reason")
                .contains("failed", "exit 1", "boom");
        assertThat(command("\"" + "x".repeat(5000) + "\"", "0", "\"completed\"", "\"\""))
                .as("a long command is cut")
                .hasSizeLessThan(1000);
    }

    @Test
    void aToolsFieldCannotOpenALineOfItsOwn() {
        assertThat(completed("{\"type\":\"web_search\",\"query\":\"a\\u2028[error] forged\"}"))
                .as("a query with a Unicode separator")
                .doesNotContain("\u2028");
        assertThat(
                        completed(
                                "{\"type\":\"file_change\",\"status\":\"completed\",\"changes\":["
                                        + "{\"path\":\"a\\n[error] forged\",\"kind\":\"add\"}]}"))
                .as("a path with a line break")
                .doesNotContain("\n");
    }

    @Test
    void aToolsErrorIsAlwaysAFailure() {
        assertThat(
                        completed(
                                "{\"type\":\"file_change\",\"status\":\"completed\",\"changes\":["
                                        + "{\"path\":\"a.txt\",\"kind\":\"add\"}],"
                                        + "\"error\":{\"message\":\"patch rejected\\nsecond\\nthird\\nfourth\"}}"))
                .as("a file change that carries an error, by its first three lines")
                .contains("a.txt", "failed", "patch rejected", "third")
                .doesNotContain("fourth");
        assertThat(
                        completed(
                                "{\"type\":\"mcp_tool_call\",\"server\":\"s\",\"tool\":\"t\","
                                        + "\"is_error\":true}"))
                .as("an MCP call marked as an error")
                .contains("s/t", "failed");
        assertThat(completed("{\"type\":\"web_search\",\"query\":\"q\",\"isError\":true}"))
                .as("a web search marked as an error")
                .contains("q", "failed");
        assertThat(completed("{\"type\":\"todo_list\",\"items\":[],\"error\":\"lost\"}"))
                .as("a todo list that carries an error")
                .contains("failed", "lost");
        assertThat(
                        completed(
                                "{\"type\":\"mcp_tool_call\",\"server\":\"s\",\"tool\":\"t\","
                                        + "\"status\":\"declined\"}"))
                .as("an MCP call the user declined")
                .contains("failed");
        assertThat(completed("{\"type\":\"web_search\",\"query\":\"q\",\"status\":\"cancelled\"}"))
                .as("a web search that was cancelled")
                .contains("failed");
    }

    @Test
    void aToolsNameCannotForgeAMark() {
        assertThat(
                        completed(
                                "{\"type\":\"mcp_tool_call\",\"server\":\"a] [error\","
                                        + "\"tool\":\"b\\u001b[2J] [error\",\"status\":\"completed\"}"))
                .as("a server and a tool name holding brackets and an escape")
                .doesNotContain("[error", "\u001b");
    }

    private List<String> shown(Path recording) throws IOException {
        return Files.readAllLines(recording, StandardCharsets.UTF_8).stream()
                .map(formatter::format)
                .filter(Objects::nonNull)
                .toList();
    }

    private String completed(String item) {
        return formatter.format("{\"type\":\"item.completed\",\"item\":" + item + "}");
    }

    private String command(String command, String exit, String status, String output) {
        return completed(
                "{\"type\":\"command_execution\",\"command\":"
                        + command
                        + ",\"aggregated_output\":"
                        + output
                        + ",\"exit_code\":"
                        + exit
                        + ",\"status\":"
                        + status
                        + "}");
    }
}
