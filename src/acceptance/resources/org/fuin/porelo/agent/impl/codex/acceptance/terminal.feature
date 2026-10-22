Feature: Safe refusals before a task has a credential

    Background:
        Given the suite runs as an unprivileged user

    Scenario: Previewing a login does not start authentication
        When a script runs "porelo vault login codex --dry-run"
        Then it exits zero
        And its output contains "previewed nothing was built or run"

    Scenario: The deliberately refused telemetry host is visible
        Given a terminal on the machine
        When I run "porelo agents --verbose"
        Then the terminal shows "ab.chatgpt.com"

    Scenario: Missing authentication is refused before building a task
        Given a project called "nocred" of class "guarded" with a file in it
        And a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds no credential for "chatgpt"
        When a task nobody is watching is started in "nocred" for the "codex" agent
        Then its output mentions one of "no credential, is locked, cannot authenticate"
        And its output contains "chatgpt"

    Scenario: A script is not asked an interactive launch question
        Given a project called "noask" of class "guarded" with a file in it
        When a script asks what a task in "noask" for the "codex" agent would do
        Then its output does not contain "[Y/n]"
        And its output contains no escape sequences
