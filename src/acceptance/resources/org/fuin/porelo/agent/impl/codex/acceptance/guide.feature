@credential
Feature: A task's agent is given Porelo's guide

    Background:
        Given the suite runs as an unprivileged user
        And the environment variable "OPEN_AI_API" is set
        And the environment variable "PORELO_E2E_MODEL" is set

    @slow
    Scenario: a task's agent is told how its mailbox works
        # The image links CODEX_HOME's AGENTS.md to the guide Porelo mounts. Asked without a tool, only the guide in
        # its instructions can say where its new messages arrive; the prompt names no path.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "OPEN_AI_API" as "openai" of kind "api-key"
        And a project called "mailbox" of class "guarded" with a file in it
        When a script runs "timeout 300 porelo task start talk --project mailbox --repository mailbox --agent codex --provider openai --model ${PORELO_E2E_MODEL} --prompt 'Without using any tool: in which directory do new messages for you arrive? Reply with its absolute path only.'"
        Then it exits zero
        And its output contains "/run/porelo/mail/inbox/new"
        When a script runs "porelo project remove mailbox --force"
        Then it exits zero
