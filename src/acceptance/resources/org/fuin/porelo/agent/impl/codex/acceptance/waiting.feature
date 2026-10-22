@credential
Feature: An observed selection is not mistaken for an idle composer

    Background:
        Given the suite runs as an unprivileged user
        And the environment variable "OPEN_AI_API" is set
        And the environment variable "PORELO_E2E_MODEL" is set

    @slow
    Scenario: The model picker requires input and closing it restores readiness
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "OPEN_AI_API" as "openai" of kind "api-key"
        And a project called "select" of class "guarded" with a file in it
        When a script runs:
            """
            set -eu
            fixture="$HOME/select"
            test -f "$fixture/.git/porelo-acceptance-fixture"
            actual=$(sed -n 's/^  base_image: //p' "$fixture/project.yml")
            if [ "$actual" != '"ubuntu:24.04"' ]; then
              printf 'Expected the kit base "ubuntu:24.04"; got %s\n' "$actual"
              exit 1
            fi
            sed -i 's|^  base_image: "ubuntu:24.04"$|  base_image: "docker.io/library/ubuntu:24.04@sha256:68ee22004a9109b44973313b02ff7b1de250f1de0f5ecb628d73f579aaff33ac"|' "$fixture/project.yml"
            printf '%s\n' \
              '  snippet: |' \
              "    RUN mkdir -p /etc/codex && printf '%s\\n' 'model_reasoning_effort = \"minimal\"' 'web_search = \"disabled\"' '[model_providers.broker]' 'request_max_retries = 0' 'stream_max_retries = 0' > /etc/codex/config.toml" \
              'limits:' '  memory: 2g' '  cpus: "1.0"' '  pids: 2048' >> "$fixture/project.yml"
            env GIT_CONFIG_GLOBAL=/dev/null git -C "$fixture" add project.yml
            env GIT_CONFIG_GLOBAL=/dev/null git -C "$fixture" commit -m 'Configure the bounded model fixture'
            porelo project unfollow select --force
            porelo project follow select "$fixture" --unverified
            """
        And a terminal on the machine
        When I run "porelo task start select --project select --repository select --agent codex --provider openai --model ${PORELO_E2E_MODEL} --clearance deny"
        Then the "codex" agent in task "select" of "select" reaches work without being asked anything
        And porelo shows the "codex" agent in task "select" of "select" not waiting for a person
        When I type "/model"
        And I press Enter
        Then porelo shows the "codex" agent in task "select" of "select" waiting for a person
        When I press the key "Escape"
        Then porelo shows the "codex" agent in task "select" of "select" not waiting for a person
        When a script runs "porelo project unfollow select --force"
        Then it exits zero
