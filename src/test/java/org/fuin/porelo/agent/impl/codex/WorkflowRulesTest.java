package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/** Guards package checks and keeps CI free of live credentials and rented machines. */
class WorkflowRulesTest {
    @Test
    void packageBuildsCannotSkipTheirChecks() throws IOException {
        final List<String> scripts =
                WorkflowSteps.scripts("build.yml", "Native binary, protocol and packages");
        assertThat(scripts).as("the native package build is actually present").hasSize(1);
        assertThat(scripts.getFirst())
                .as("both package verification and native protocol checks run")
                .contains("-Pnative,dist verify")
                .doesNotContain("skipTests", "maven.test.skip");
    }

    @Test
    void workflowsDoNotRequestLiveCredentialsOrRentMachines() throws IOException {
        final List<Path> workflows;
        try (Stream<Path> paths = Files.list(Path.of(".github/workflows"))) {
            workflows = paths.filter(path -> path.toString().endsWith(".yml")).sorted().toList();
        }
        assertThat(workflows).as("all workflows").hasSize(4);
        for (final Path workflow : workflows) {
            assertThat(Files.readString(workflow))
                    .as("no live leg before a provider contract: %s", workflow)
                    .doesNotContain(
                            "machines.Main acceptance", "contents: write", "pull-requests: write");
        }
    }

    @Test
    void onlyThePublishStepOfAPushToMainReceivesTheOnlySecret() throws IOException {
        final List<String> secrets = new ArrayList<>();
        try (Stream<Path> paths =
                Files.list(RepositoryDocuments.root().resolve(".github/workflows"))) {
            for (final Path workflow : paths.sorted().toList()) {
                for (final String line : Files.readAllLines(workflow)) {
                    if (line.contains("secrets.")) {
                        secrets.add(workflow.getFileName() + ": " + line.strip());
                    }
                }
            }
        }
        assertThat(secrets)
                .as("every line of every workflow that reads a secret")
                .containsExactly("build.yml: JF_ACCESS_TOKEN: ${{ secrets.JF_ACCESS_TOKEN }}");

        final Map<?, ?> workflow =
                new Yaml()
                        .load(
                                Files.readString(
                                        RepositoryDocuments.root()
                                                .resolve(".github/workflows/build.yml")));
        final Map<?, ?> job = (Map<?, ?>) ((Map<?, ?>) workflow.get("jobs")).get("build");
        final List<Map<?, ?>> publish =
                ((List<?>) job.get("steps"))
                        .stream()
                                .<Map<?, ?>>map(step -> (Map<?, ?>) step)
                                .filter(step -> "Publish the packages".equals(step.get("name")))
                                .toList();
        assertThat(publish).as("the build job's publish step").hasSize(1);
        assertThat(publish.getFirst().get("if"))
                .as("a branch or a pull request never gets the token nor publishes")
                .isEqualTo("github.event_name == 'push' && github.ref == 'refs/heads/main'");
        assertThat(((Map<?, ?>) publish.getFirst().get("env")).get("JF_ACCESS_TOKEN"))
                .as("the publish step is the one that reads the token")
                .isEqualTo("${{ secrets.JF_ACCESS_TOKEN }}");
    }
}
