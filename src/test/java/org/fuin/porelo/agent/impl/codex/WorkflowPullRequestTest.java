package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

/** Guards the DCO boundary between the repository's own branches and outside contributions. */
class WorkflowPullRequestTest {

    private static final String STEP = "The pull request's commit messages";
    private static final String REPOSITORY = "porelo-ai/porelo-codex";
    private static final String GITHUB = "https://github.com";
    private static final String FORGE = "https://10.73.0.1:3000";

    @Test
    void theStepBindsThePullRequestIdentityAndRange() throws Exception {
        final Map<?, ?> workflow =
                new Yaml()
                        .load(
                                Files.readString(
                                        RepositoryDocuments.root()
                                                .resolve(".github/workflows/shared-rules.yml")));
        final Map<?, ?> jobs = (Map<?, ?>) workflow.get("jobs");
        final Map<?, ?> job = (Map<?, ?>) jobs.get("check-shared");
        final List<?> steps = (List<?>) job.get("steps");
        final List<Map<?, ?>> matches =
                steps.stream()
                        .<Map<?, ?>>map(step -> (Map<?, ?>) step)
                        .filter(step -> STEP.equals(step.get("name")))
                        .toList();
        assertThat(matches).as("exactly one step checks the pull request").hasSize(1);
        final Map<?, ?> step = matches.getFirst();
        assertThat(step.get("if"))
                .as("only pull requests use their supplied range")
                .isEqualTo("github.event_name == 'pull_request'");
        assertThat(step.get("env"))
                .as("identity and range come from the event rather than shell interpolation")
                .isEqualTo(
                        Map.of(
                                "PR_BASE", "${{ github.event.pull_request.base.sha }}",
                                "PR_HEAD", "${{ github.event.pull_request.head.sha }}",
                                "PR_REPOSITORY",
                                        "${{ github.event.pull_request.head.repo.full_name }}",
                                "PR_ASSOCIATION",
                                        "${{ github.event.pull_request.author_association }}",
                                "PR_AUTHOR", "${{ github.event.pull_request.user.login }}",
                                "PR_SERVER", "${{ github.server_url }}"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pullRequests")
    void theStepChecksTheRangeWithTheRequiredDcoBoundary(
            final String description,
            final String server,
            final String repository,
            final String association,
            final String author,
            final boolean outside,
            @TempDir final Path dir)
            throws Exception {
        final WorkflowSteps.Run run = runStep(dir, server, repository, association, author, "0");
        assertThat(run.exit()).as("%s: %s", description, run.output()).isZero();
        final List<String> expected =
                new java.util.ArrayList<>(List.of("check-commits", "base..head"));
        if (outside) {
            expected.add("--outside");
        }
        assertThat(Files.readAllLines(dir.resolve("arguments")))
                .as(
                        "%s must check the exact range with the appropriate DCO requirement",
                        description)
                .containsExactlyElementsOf(expected);
    }

    static Stream<Arguments> pullRequests() {
        return Stream.of(
                Arguments.of("forge own branch", FORGE, REPOSITORY, "", "agent-codex", false),
                Arguments.of("forge fork", FORGE, "someone/codex", "", "agent-codex", true),
                Arguments.of(
                        "GitHub contributor", GITHUB, REPOSITORY, "CONTRIBUTOR", "person", true),
                Arguments.of("GitHub member", GITHUB, REPOSITORY, "MEMBER", "person", false),
                Arguments.of("GitHub owner", GITHUB, REPOSITORY, "OWNER", "person", false),
                Arguments.of(
                        "GitHub own bot", GITHUB, REPOSITORY, "", "github-actions[bot]", false),
                Arguments.of(
                        "GitHub fork member", GITHUB, "someone/codex", "MEMBER", "person", true),
                Arguments.of(
                        "GitHub fork bot",
                        GITHUB,
                        "someone/codex",
                        "",
                        "github-actions[bot]",
                        true));
    }

    @Test
    void aRejectedCommitFailsTheStep(@TempDir final Path dir) throws Exception {
        final WorkflowSteps.Run run = runStep(dir, FORGE, REPOSITORY, "", "agent-codex", "37");
        assertThat(run.exit())
                .as("a failed commit check must fail the workflow step: %s", run.output())
                .isEqualTo(37);
    }

    private static WorkflowSteps.Run runStep(
            final Path dir,
            final String server,
            final String repository,
            final String association,
            final String author,
            final String exit)
            throws Exception {
        final Path bin = Files.createDirectory(dir.resolve("bin"));
        final Path java = bin.resolve("java");
        Files.writeString(
                java,
                """
                #!/bin/bash
                set -eu
                [ "$1" = '-cp' ]
                [ "$3" = 'org.fuin.porelo.release.Main' ]
                shift 3
                printf '%s\\n' "$@" > arguments
                exit "$CHECK_EXIT"
                """);
        assertThat(java.toFile().setExecutable(true))
                .as("the stand-in release command must execute")
                .isTrue();
        Files.createDirectory(dir.resolve("target"));
        Files.writeString(dir.resolve("target/cp.txt"), "unused-classpath");
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", bin + ":/usr/bin:/bin");
        env.put("GIT_CONFIG_GLOBAL", "/dev/null");
        env.put("GIT_CONFIG_NOSYSTEM", "1");
        env.put("GITHUB_REPOSITORY", REPOSITORY);
        env.put("PR_SERVER", server);
        env.put("PR_REPOSITORY", repository);
        env.put("PR_ASSOCIATION", association);
        env.put("PR_AUTHOR", author);
        env.put("PR_BASE", "base");
        env.put("PR_HEAD", "head");
        env.put("CHECK_EXIT", exit);
        final List<String> scripts = WorkflowSteps.scripts("shared-rules.yml", STEP);
        assertThat(scripts).as("one real workflow script is exercised").hasSize(1);
        return WorkflowSteps.run(scripts.getFirst(), dir, env, WorkflowSteps.LIMIT);
    }
}
