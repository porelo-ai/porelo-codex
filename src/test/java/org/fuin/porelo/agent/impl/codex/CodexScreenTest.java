package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.fuin.porelo.agent.api.AtRest;
import org.fuin.porelo.agent.api.Waiting;
import org.junit.jupiter.api.Test;

/** Guards wake input against measured busy screens and open selection dialogs. */
class CodexScreenTest {
    private static final String TRUST =
            """
            Folder access
            /workspace
            Trust this folder?
            › 1. Trust and continue
              2. Quit
              enter continue · esc quit
            """;

    private static final String PICKER =
            """
            Select Model and Effort
            enter select · esc back
            """;

    @Test
    void wakeInputIsAllowedOnlyAtTheComposer() throws IOException {
        final AtRest rest = new CodexAgent().definition().atRest();
        assertThat(rest).as("a task needs an explicit safe wake boundary").isNotNull();
        assertThat(rest.matches(screen("ready")))
                .as("the measured idle composer accepts input")
                .isTrue();
        assertThat(rest.matches(screen("busy")))
                .as("a working turn must not receive wake input")
                .isFalse();
        assertThat(rest.matches(TRUST)).as("a wake must not confirm folder trust").isFalse();
        assertThat(rest.matches(PICKER)).as("a wake must not choose a model").isFalse();
        assertThat(rest.matches("")).as("an absent CLI cannot receive wake input").isFalse();
    }

    @Test
    void startupReadinessDoesNotPassAFirstRunDialog() throws IOException {
        final var ready = new CodexAgent().definition().ready();
        assertThat(ready).as("attended acceptance must wait for actual readiness").isNotNull();
        assertThat(screen("ready"))
                .as("readiness is observed at the composer")
                .contains(ready.text());
        assertThat(TRUST).as("folder trust still needs an answer").doesNotContain(ready.text());
        assertThat(PICKER).as("a selection dialog is not readiness").doesNotContain(ready.text());
    }

    @Test
    void observedSelectionsRequireInputButWorkingDoesNot() throws IOException {
        final Waiting waiting = new CodexAgent().definition().waiting();
        assertThat(waiting).as("the adapter must declare observed input boundaries").isNotNull();
        assertThat(waiting.read(TRUST).seen())
                .as("folder trust needs a decision")
                .isEqualTo(Waiting.Seen.WAITING);
        assertThat(waiting.read(PICKER).seen())
                .as("the model picker needs a selection")
                .isEqualTo(Waiting.Seen.WAITING);
        assertThat(waiting.read(screen("ready")).seen())
                .as("an idle composer is not a dialog")
                .isEqualTo(Waiting.Seen.NOT_WAITING);
        assertThat(waiting.read(screen("busy")).seen())
                .as("a busy turn is not a dialog")
                .isEqualTo(Waiting.Seen.NOT_WAITING);
    }

    private static String screen(String name) throws IOException {
        try (var input =
                CodexScreenTest.class.getResourceAsStream("/screens/" + name + ".screen")) {
            if (input == null) {
                throw new IOException("Missing measured screen: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
