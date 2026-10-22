package org.fuin.porelo.agent.impl.codex;

import java.util.ArrayList;
import java.util.List;
import org.fuin.porelo.agent.api.AgentEnd;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.AgentMain;
import org.fuin.porelo.agent.api.ContainerSetup;
import org.fuin.porelo.agent.api.LogFormatter;
import org.fuin.porelo.agent.api.RunRequest;
import org.fuin.porelo.agent.api.YamlAgent;
import org.jspecify.annotations.Nullable;

/** Codex CLI's definition and agent protocol entry point. */
public final class CodexAgent extends YamlAgent {

    /** Loads the packaged Codex definition. */
    public CodexAgent() {
        super("codex");
    }

    /** {@inheritDoc} */
    @Override
    public ContainerSetup containerSetup() {
        return new CodexContainerSetup();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> headlessCommand(RunRequest request) {
        if (request.maxTurns() != null) {
            throw new AgentException("Codex CLI does not support a turn limit");
        }
        final List<String> command = new ArrayList<>(definition().sandboxedCommand());
        command.add("exec");
        if (request.resumeSession() != null) {
            command.add("resume");
        }
        if (request.model() != null) {
            command.add("--model");
            command.add(request.model());
        }
        if (request.machineReadable()) {
            command.add("--json");
        }
        command.add("--");
        if (request.resumeSession() != null) {
            command.add(request.resumeSession());
        }
        command.add(request.prompt());
        return List.copyOf(command);
    }

    /** {@inheritDoc} */
    @Override
    public LogFormatter logFormatter() {
        return new CodexJsonFormatter();
    }

    /** {@inheritDoc} */
    @Override
    public @Nullable AgentEnd ended(String line) {
        return CodexJsonFormatter.ended(line);
    }

    /**
     * Serves the published agent protocol.
     *
     * @param args Protocol mode and its arguments.
     */
    public static void main(String[] args) {
        AgentMain.run(new CodexAgent(), args);
    }
}
