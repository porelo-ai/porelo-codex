@credential
Feature: An unattended task continues its recorded conversation

    Background:
        Given the suite runs as an unprivileged user
        And the environment variable "OPEN_AI_API" is set
        And the environment variable "PORELO_E2E_MODEL" is set

    @slow
    Scenario: An unattended task remembers across a stop and restart
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "OPEN_AI_API" as "openai" of kind "api-key"
        And a project called "cont" of class "guarded" with a file in it
        When a script runs:
            """
            set -eu
            fixture="$HOME/cont"
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
            porelo project unfollow cont --force
            porelo project follow cont "$fixture" --unverified
            """
        When a script runs "timeout 120 porelo task start talk --project cont --repository cont --agent codex --provider openai --model ${PORELO_E2E_MODEL} --prompt 'Remember the word PAPAYA. Reply with OK only.'"
        Then it exits zero
        And porelo says task "talk" of "cont" has a session to continue
        When a script runs "porelo task stop porelo-cont-talk"
        And a script runs "timeout 120 porelo task start talk --project cont --repository cont --agent codex --provider openai --model ${PORELO_E2E_MODEL} --prompt 'Which word did I ask you to remember? Reply with the word only.'"
        Then it exits zero
        And its output contains "session   continuing"
        And its output contains "PAPAYA"
        When a script runs "porelo project unfollow cont --force"
        Then it exits zero
