package org.fuin.porelo.agent.impl.codex;

import java.util.ArrayList;
import java.util.List;
import org.fuin.porelo.agent.api.AgentEnd;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.AgentMain;
import org.fuin.porelo.agent.api.ContainerSetup;
import org.fuin.porelo.agent.api.CredentialExtractor;
import org.fuin.porelo.agent.api.Finder;
import org.fuin.porelo.agent.api.LogFormatter;
import org.fuin.porelo.agent.api.RunRequest;
import org.fuin.porelo.agent.api.ToolEvent;
import org.fuin.porelo.agent.api.YamlAgent;
import org.fuin.porelo.wire.LimitFactors;
import org.jspecify.annotations.Nullable;

/** Codex CLI's definition and agent protocol entry point. */
public final class CodexAgent extends YamlAgent {

    private final LimitFactors limits;

    /** Loads the packaged Codex definition, every time limit at its default. */
    public CodexAgent() {
        this(LimitFactors.DEFAULT);
    }

    private CodexAgent(LimitFactors limits) {
        super("codex");
        this.limits = limits;
    }

    /** {@inheritDoc} */
    @Override
    public CodexAgent withLimits(LimitFactors limits) {
        return new CodexAgent(limits);
    }

    /** {@inheritDoc} */
    @Override
    public CredentialExtractor credentialExtractor() {
        return new CodexCredentialExtractor();
    }

    /**
     * Returns what Codex CLI finds of itself.
     *
     * @return Finder reading Codex CLI's own files.
     */
    @Override
    public Finder finder() {
        return new CodexFindings();
    }

    /** {@inheritDoc} */
    @Override
    public ContainerSetup containerSetup() {
        return new CodexContainerSetup(limits);
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

    /** {@inheritDoc} */
    @Override
    public List<ToolEvent> toolEvents(String line) {
        return CodexToolEvents.read(line);
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
