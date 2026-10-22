package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Guards both fetched executables against partial or unfiltered pin updates. */
class PinAgreementTest {
    @Test
    void bothBuiltArtifactsAgreeWithThePom() throws Exception {
        final String pom = Files.readString(Path.of("pom.xml"));
        final var definition = new CodexAgent().definition();
        final String version = property(pom, "agent.cli.version");
        assertThat(definition.version())
                .as("the version actually described by the adapter")
                .isEqualTo(version);
        final Map<String, String> digests =
                Map.of(
                        "cli.tar.gz",
                        property(pom, "agent.cli.sha256"),
                        "helper.tar.gz",
                        property(pom, "agent.helper.sha256"));
        assertThat(definition.artifacts())
                .as("the complete installation reference set")
                .hasSize(digests.size());
        assertThat(definition.artifacts())
                .allSatisfy(
                        artifact -> {
                            assertThat(artifact.sha256())
                                    .as("filtered digest for %s", artifact.target())
                                    .isEqualTo(digests.get(artifact.fileName()));
                            assertThat(artifact.url())
                                    .as("both executables follow one CLI version")
                                    .contains("/rust-v" + version + "/")
                                    .doesNotContain("${");
                        });
    }

    private static String property(String pom, String name) {
        final Matcher value =
                Pattern.compile(
                                "<"
                                        + Pattern.quote(name)
                                        + ">([^<]+)</"
                                        + Pattern.quote(name)
                                        + ">")
                        .matcher(pom);
        assertThat(value.find()).as("pom property %s", name).isTrue();
        return value.group(1);
    }
}
