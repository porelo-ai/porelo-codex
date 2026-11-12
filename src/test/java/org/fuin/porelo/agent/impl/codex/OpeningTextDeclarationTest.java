package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * How an attended Codex CLI session takes its opening text: declared as positional, not left to the
 * fallback from its headless prompt flag, and with no flag that would run it once and end after its
 * first answer instead of waiting for the person.
 */
class OpeningTextDeclarationTest {

    @Test
    void declaresThatTheOpeningTextIsPositional() {

        assertThat(new CodexAgent().definition().openingFlag())
                .as("codex.yaml declares session.opening_flag, positional for an interactive codex")
                .isEqualTo("");
    }

    @Test
    void startsTheSessionWithTheTextLast() {

        final List<String> command =
                new CodexAgent().definition().attendedCommand(null, "Name the file you read first");

        assertThat(command.getLast())
                .as("the opening text is the last argument: %s", command)
                .isEqualTo("Name the file you read first");
    }
}
