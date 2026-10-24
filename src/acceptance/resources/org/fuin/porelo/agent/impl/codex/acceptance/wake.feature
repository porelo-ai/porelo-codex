@credential
Feature: A mailbox message wakes the composer without interrupting work

    Background:
        Given the suite runs as an unprivileged user
        And the environment variable "OPEN_AI_API" is set
        And the environment variable "PORELO_E2E_MODEL" is set

    @slow
    Scenario: A message to an idle task obtains a computed answer
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "OPEN_AI_API" as "openai" of kind "api-key"
        And a daemon of this scenario's own
        And a project called "wake" of class "guarded" with a file in it
        When a script runs:
            """
            set -eu
            fixture="$HOME/wake"
            test -f "$fixture/.git/porelo-acceptance-fixture"
            actual=$(sed -n 's/^  base_image: //p' "$fixture/project.yml")
            if [ "$actual" != '"ubuntu:24.04"' ]; then
              printf 'Expected the kit base "ubuntu:24.04"; got %s\n' "$actual"
              exit 1
            fi
            sed -i 's|^  base_image: "ubuntu:24.04"$|  base_image: "docker.io/library/ubuntu:24.04@sha256:68ee22004a9109b44973313b02ff7b1de250f1de0f5ecb628d73f579aaff33ac"|' "$fixture/project.yml"
            printf '%s\n' \
              '  snippet: |' \
              "    RUN mkdir -p /etc/codex && printf '%s\\n' 'model_reasoning_effort = \"none\"' 'web_search = \"disabled\"' '[model_providers.broker]' 'request_max_retries = 0' 'stream_max_retries = 0' > /etc/codex/config.toml" \
              'limits:' '  memory: 2g' '  cpus: "1.0"' '  pids: 2048' >> "$fixture/project.yml"
            env GIT_CONFIG_GLOBAL=/dev/null git -C "$fixture" add project.yml
            env GIT_CONFIG_GLOBAL=/dev/null git -C "$fixture" commit -m 'Configure the bounded model fixture'
            porelo project unfollow wake --force
            porelo project follow wake "$fixture" --unverified
            """
        And a terminal on the machine
        When I run "porelo task start wake --project wake --repository wake --agent codex --provider openai --model ${PORELO_E2E_MODEL} --clearance deny"
        Then the "codex" agent in task "wake" of "wake" reaches work without being asked anything
        When a script runs "echo 'What is 1234 plus 4321? Reply with the digits only.' | porelo talk tell porelo-wake-wake"
        Then it exits zero
        And within 120 seconds this script exits zero:
            """
            held=$(porelo talk held porelo-wake-wake)
            for id in $(printf '%s\n' "$held" | awk '$1 ~ /\.json$/ && $2 == "held" {print $1}'); do
              porelo talk read porelo-wake-wake "$id" | grep -qE '5,?555' && exit 0
            done
            podman exec porelo-wake-wake tmux capture-pane -p -S - -t porelo | grep -qE '5,?555'
            """
        When a script runs "porelo task remove porelo-wake-wake --force"
        And a script runs "porelo project unfollow wake --force"
        Then it exits zero

    @slow
    Scenario: A message arriving during work is handled after the turn
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "OPEN_AI_API" as "openai" of kind "api-key"
        And a daemon of this scenario's own
        And a project called "busy" of class "guarded" with a file in it
        When a script runs:
            """
            set -eu
            fixture="$HOME/busy"
            test -f "$fixture/.git/porelo-acceptance-fixture"
            actual=$(sed -n 's/^  base_image: //p' "$fixture/project.yml")
            if [ "$actual" != '"ubuntu:24.04"' ]; then
              printf 'Expected the kit base "ubuntu:24.04"; got %s\n' "$actual"
              exit 1
            fi
            sed -i 's|^  base_image: "ubuntu:24.04"$|  base_image: "docker.io/library/ubuntu:24.04@sha256:68ee22004a9109b44973313b02ff7b1de250f1de0f5ecb628d73f579aaff33ac"|' "$fixture/project.yml"
            printf '%s\n' \
              '  snippet: |' \
              "    RUN mkdir -p /etc/codex && printf '%s\\n' 'model_reasoning_effort = \"none\"' 'web_search = \"disabled\"' '[model_providers.broker]' 'request_max_retries = 0' 'stream_max_retries = 0' > /etc/codex/config.toml" \
              'limits:' '  memory: 2g' '  cpus: "1.0"' '  pids: 2048' >> "$fixture/project.yml"
            env GIT_CONFIG_GLOBAL=/dev/null git -C "$fixture" add project.yml
            env GIT_CONFIG_GLOBAL=/dev/null git -C "$fixture" commit -m 'Configure the bounded model fixture'
            porelo project unfollow busy --force
            porelo project follow busy "$fixture" --unverified
            """
        And a terminal on the machine
        When I run "porelo task start busy --project busy --repository busy --agent codex --provider openai --model ${PORELO_E2E_MODEL} --clearance deny"
        Then the "codex" agent in task "busy" of "busy" reaches work without being asked anything
        When I type "Run the shell command 'sleep 10' with your shell tool, then reply with the single word DONE."
        And I press Enter
        Then within 60 seconds this script exits zero:
            """
            podman exec porelo-busy-busy tmux capture-pane -p -t porelo | grep -qF 'esc to interrupt'
            """
        When a script runs "echo 'What is 1234 plus 4321? Reply with the digits only.' | porelo talk tell porelo-busy-busy"
        Then it exits zero
        And within 120 seconds this script exits zero:
            """
            held=$(porelo talk held porelo-busy-busy)
            for id in $(printf '%s\n' "$held" | awk '$1 ~ /\.json$/ && $2 == "held" {print $1}'); do
              porelo talk read porelo-busy-busy "$id" | grep -qE '5,?555' && exit 0
            done
            podman exec porelo-busy-busy tmux capture-pane -p -S - -t porelo | grep -qE '5,?555'
            """
        When a script runs "porelo task remove porelo-busy-busy --force"
        And a script runs "porelo project unfollow busy --force"
        Then it exits zero
