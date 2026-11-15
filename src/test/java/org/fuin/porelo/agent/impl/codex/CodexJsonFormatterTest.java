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
}
