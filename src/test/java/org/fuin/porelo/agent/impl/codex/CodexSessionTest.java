package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.fuin.porelo.agent.api.SessionIds;
import org.fuin.porelo.agent.api.Waiting;
import org.fuin.porelo.wire.Json;
import org.junit.jupiter.api.Test;

/** Guards lost conversations and tool output mistaken for the agent's last reply. */
class CodexSessionTest {

    private final CodexAgent agent = new CodexAgent();

    @Test
    void anAttendedConversationUsesItsMetadataIdInsteadOfTheRolloutName() {
        final SessionIds ids = agent.definition().sessionIds();
        assertThat(ids)
                .as("an attended task must retain its conversation when stopped")
                .isNotNull();
        assertThat(ids.directory())
                .as("the CLI keeps dated rollout files below its task-local profile")
                .isEqualTo(".codex/sessions");
        assertThat(ids.suffix())
                .as("only the CLI's JSONL transcripts are sessions")
                .isEqualTo(".jsonl");
        assertThat(
                        ids.ofFile(
                                List.of(
                                        Json.parse(
                                                """
                        {"type":"session_meta","payload":{"id":"attended-session","cwd":"/workspace"}}
                        """))))
                .as("the filename also contains a timestamp and cannot be passed to resume")
                .isEqualTo("attended-session");
    }

    @Test
    void aRunAndARolloutUseDifferentRecordsForTheirConversation() {
        final SessionIds ids = agent.definition().sessionIds();
        assertThat(ids).as("unattended output names a resumable conversation").isNotNull();
        final List<?> run =
                List.of(
                        Json.parse(
                                """
                {"type":"thread.started","thread_id":"unattended-session"}
                """));
        assertThat(ids.of(run))
                .as("exec output keeps the original session-id grammar")
                .isEqualTo("unattended-session");
        assertThat(ids.ofFile(run))
                .as("an exec-shaped record in a transcript is not its session metadata")
                .isNull();
    }

    @Test
    void theLastReplyExcludesCommandsAndUnfinishedMessages() {
        final Waiting.LastMessage message = lastMessage();
        final List<?> records =
                List.of(
                        Json.parse(
                                """
                        {"type":"item.completed","item":{"type":"agent_message","text":"First reply"}}
                        """),
                        Json.parse(
                                """
                        {"type":"item.completed","item":{"type":"agent_message","text":"Final reply"}}
                        """),
                        Json.parse(
                                """
                        {"type":"item.completed","item":{"type":"command_execution","aggregated_output":"Tool output","exit_code":0,"status":"completed"}}
                        """),
                        Json.parse(
                                """
                        {"type":"item.completed","item":{"type":"reasoning","text":"Internal reasoning"}}
                        """),
                        Json.parse(
                                """
                        {"type":"item.started","item":{"type":"agent_message","text":"Unfinished reply"}}
                        """));
        assertThat(message.of(records))
                .as("a person's next action must be based on the final assistant reply, not a tool")
                .isEqualTo("Final reply");
    }

    @Test
    void aRunWithoutAnAssistantReplyReportsNoLastMessage() {
        assertThat(
                        lastMessage()
                                .of(
                                        List.of(
                                                Json.parse(
                                                        """
                        {"type":"item.completed","item":{"type":"command_execution","aggregated_output":"Not a reply","exit_code":0,"status":"completed"}}
                        """))))
                .as("tool output must never stand in for a missing assistant reply")
                .isNull();
    }

    private Waiting.LastMessage lastMessage() {
        final Waiting waiting = agent.definition().waiting();
        assertThat(waiting).as("the definition declares how to read its last reply").isNotNull();
        final Waiting.LastMessage message = waiting.lastMessage();
        assertThat(message).as("the nested reply must be read by the shared contract").isNotNull();
        return message;
    }
}
