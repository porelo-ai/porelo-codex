package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.fuin.porelo.agent.api.InstallArtifact;
import org.fuin.porelo.wire.Json;
import org.junit.jupiter.api.Test;

/** Guards the actual build bill against silently omitting a fetched executable. */
class FetchedArtifactsCheck {

    private static final Path TARGET = Path.of(System.getProperty("porelo.target", "target"));

    @Test
    void theBuildBillRecordsBothFetchedExecutablesWithTheirCheckedDigests() throws Exception {
        final var definition = new CodexAgent().definition();
        final Map<?, ?> document =
                (Map<?, ?>)
                        Json.parse(Files.readString(TARGET.resolve("porelo-agent-codex.cdx.json")));
        final List<?> components = (List<?>) document.get("components");
        final var fetched =
                components.stream()
                        .map(item -> (Map<?, ?>) item)
                        .filter(
                                item ->
                                        item.get("properties") instanceof List<?> properties
                                                && properties.contains(
                                                        Map.of(
                                                                "name", "porelo:delivery",
                                                                "value", "fetched-at-image-build")))
                        .toList();
        assertThat(fetched)
                .as("both executables must be visible in the actual supply-chain bill")
                .hasSize(2);
        assertThat(fetched.stream().map(item -> String.valueOf(item.get("name"))).toList())
                .as("the complete fetched executable reference set")
                .containsExactlyInAnyOrder("codex", "codex-code-mode-host");
        for (final Map<?, ?> item : fetched) {
            final String archive =
                    "codex".equals(item.get("name")) ? "cli.tar.gz" : "helper.tar.gz";
            final InstallArtifact artifact =
                    definition.artifacts().stream()
                            .filter(value -> value.fileName().equals(archive))
                            .findFirst()
                            .orElseThrow();
            assertThat(item.get("version"))
                    .as("each executable follows the built definition's version")
                    .isEqualTo(definition.version());
            assertThat(item.get("hashes"))
                    .as("each fetched archive's checked digest")
                    .isEqualTo(List.of(Map.of("alg", "SHA-256", "content", artifact.sha256())));
            assertThat(item.get("properties"))
                    .as("neither executable is claimed as shipped inside the package")
                    .isEqualTo(
                            List.of(
                                    Map.of(
                                            "name", "porelo:delivery",
                                            "value", "fetched-at-image-build")));
            assertThat(item.get("externalReferences"))
                    .as("each digest identifies its actual download")
                    .isEqualTo(List.of(Map.of("type", "distribution", "url", artifact.url())));
        }
    }
}
