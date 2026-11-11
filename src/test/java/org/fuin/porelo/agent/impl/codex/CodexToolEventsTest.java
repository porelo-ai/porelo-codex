package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.fuin.porelo.agent.api.ToolEvent;
import org.fuin.porelo.agent.api.ToolEvent.Kind;
import org.fuin.porelo.agent.api.ToolEvent.Phase;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** Guards call correlation, exclusion of output, and resilience to unreadable stream lines. */
class CodexToolEventsTest {
    private final CodexAgent agent = new CodexAgent();

    @Test
    void overlappingCommandsKeepTheirOwnIdsAndWholeCommandLines() throws IOException {
        final String build =
                "/bin/bash -lc './mvnw -B -s settings.xml -pl filter/app -am test > /tmp/build.log 2>&1 &\nbuild_pid=$!\nprintf '\"'Build PID: %s\\\\n' \\\"\"'$build_pid\"\nwait \"$build_pid\"\nbuild_status=$?\nprintf '\"'Build exit code: %s\\\\n' \\\"\"'$build_status\"\ntail -n 15 /tmp/build.log\nexit \"$build_status\"'";
        final String read =
                "/bin/bash -lc \"cat README.md\nfind . -name '*.java' -not -path '*/target/*' -exec grep -l 'Sluice' {} +\"";

        assertThat(
                        fixture("overlapping").stream()
                                .flatMap(line -> agent.toolEvents(line).stream())
                                .toList())
                .as(
                        "overlapping calls must join by id without losing the shell wrapper or inventing times or background jobs")
                .containsExactly(
                        event("item_4", Phase.START, "command_execution", Kind.SHELL, build),
                        event("item_5", Phase.START, "command_execution", Kind.SHELL, read),
                        event("item_5", Phase.END, "command_execution", Kind.SHELL, read),
                        event("item_4", Phase.END, "command_execution", Kind.SHELL, build));
    }

    @Test
    void failedCommandsStillEndWithoutKeepingTheirOutput() throws IOException {
        final String command =
                "/bin/bash -lc \"pwd\nrg --files -g AGENTS.md -g .AGENTS.md -g README.md -g '*.java' -g '\"'!target'\"' | head -60\nhead -n 12 README.md\nrg -l -w Sluice --glob '*.java' --glob '\"'!**/target/**'\"' .\"";

        assertThat(
                        fixture("failed").stream()
                                .flatMap(line -> agent.toolEvents(line).stream())
                                .toList())
                .as(
                        "a failed exit must end the same call and must not copy aggregated output into any field")
                .containsExactly(
                        event("item_2", Phase.START, "command_execution", Kind.SHELL, command),
                        event("item_2", Phase.END, "command_execution", Kind.SHELL, command));
    }

    @Test
    void fileChangesReportOnlyTheToolLifecycle() throws IOException {
        assertThat(
                        fixture("edit").stream()
                                .flatMap(line -> agent.toolEvents(line).stream())
                                .toList())
                .as(
                        "edits must be recorded without treating file changes as shell commands or copying file contents")
                .containsExactly(
                        event("item_9", Phase.START, "file_change", Kind.EDIT, null),
                        event("item_9", Phase.END, "file_change", Kind.EDIT, null));
    }

    @Test
    void collaborationWaitsDoNotInventASubAgentNameOrParent() throws IOException {
        assertThat(
                        fixture("collaboration").stream()
                                .flatMap(line -> agent.toolEvents(line).stream())
                                .toList())
                .as("the stream exposes only the wait tool, not a named spawn or its child calls")
                .containsExactly(
                        event("item_1", Phase.START, "wait", Kind.SUB_AGENT, null),
                        event("item_1", Phase.END, "wait", Kind.SUB_AGENT, null));
    }

    @Test
    void anUnrecognizedToolItemIsStillRecordedWithoutItsPayload() throws IOException {
        final String line = fixture("edit").getFirst().replace("file_change", "future_tool");

        assertThat(agent.toolEvents(line))
                .as("a new tool item must not disappear or copy an unknown payload")
                .containsExactly(event("item_9", Phase.START, "future_tool", Kind.OTHER, null));
    }

    @Test
    void messagesAndRunLifecycleLinesDoNotCreateToolCalls() throws IOException {
        for (final String line : fixture("ignored")) {
            assertThat(agent.toolEvents(line))
                    .as("only tool item lifecycle lines may create tool calls: %s", line)
                    .isEmpty();
        }
    }

    @Test
    void malformedAndUnknownShapesCannotInterruptTheRun() throws IOException {
        final String recorded = fixture("edit").getFirst();
        for (final String line :
                List.of(
                        "Reading additional input from stdin...",
                        "",
                        "{",
                        "[]",
                        "null",
                        "{}",
                        recorded.replace("item.started", "item.updated"),
                        recorded.replace("\"id\":\"item_9\"", "\"id\":null"),
                        recorded.replace("\"type\":\"file_change\"", "\"type\":[]"),
                        "{\"type\":\"item.started\",\"item\":[]}")) {
            assertThat(agent.toolEvents(line))
                    .as(
                            "unreadable or unrelated input must yield no events and must not throw: %s",
                            line)
                    .isEmpty();
        }
    }

    private static ToolEvent event(
            String id, Phase phase, String tool, Kind kind, @Nullable String command) {
        return new ToolEvent(
                0, id, phase, tool, kind, command, false, null, null, null, null, null);
    }

    private static List<String> fixture(String name) throws IOException {
        try (InputStream input =
                CodexToolEventsTest.class.getResourceAsStream("/tool-events/" + name + ".jsonl")) {
            if (input == null) {
                throw new IOException("Missing tool-event fixture: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
