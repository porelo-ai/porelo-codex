package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.porelo.agent.api.AgentEnd;
import org.junit.jupiter.api.Test;

/** Guards terminal injection and unknown or malformed lifecycle records. */
class CodexJsonFormatterTest {
    private final CodexJsonFormatter formatter = new CodexJsonFormatter();

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
}
