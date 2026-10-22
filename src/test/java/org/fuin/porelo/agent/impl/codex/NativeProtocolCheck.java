package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.fuin.porelo.agent.api.Credential;
import org.fuin.porelo.agent.api.InstalledAgent;
import org.fuin.porelo.wire.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the packaged native executable without a provider or credential. */
class NativeProtocolCheck {
    @TempDir Path directory;

    @Test
    void theNativeProtocolRetainsTheSignInAndItsRenewalAttributes() throws Exception {
        final String access = CodexCredentialExtractorTest.accessToken("42");
        Files.writeString(
                directory.resolve("auth.json"),
                CodexCredentialExtractorTest.login(access, "synthetic-refresh", "account-native"));
        try (InstalledAgent agent =
                new InstalledAgent(
                        Path.of(System.getProperty("porelo.native.binary")),
                        directory.resolve("s"))) {
            final var result = agent.extractCredential(directory);
            assertThat(result)
                    .as("the packaged native override, not a JVM-only extractor")
                    .isPresent();
            final Credential credential = result.orElseThrow();
            assertThat(credential.type())
                    .as("the wire preserves OAuth rather than an API key")
                    .isEqualTo("oauth");
            assertThat(credential.secret())
                    .as("the wire carries the intended synthetic access token")
                    .isEqualTo(access);
            assertThat(credential.attributes())
                    .as("vault routing and renewal need the complete native result")
                    .containsExactlyInAnyOrderEntriesOf(
                            Map.of(
                                    Credential.REFRESH_TOKEN,
                                    "synthetic-refresh",
                                    Credential.EXPIRES_AT,
                                    "42000",
                                    Credential.TOKEN_URL,
                                    "https://auth.openai.com/oauth/token",
                                    Credential.CLIENT_ID,
                                    "app_EMoamEEZ73f0CkXaXp7hrann",
                                    "account_id",
                                    "account-native"));
        }
    }

    @Test
    void theNativeExecutableDescribesBothInstallArtifacts() throws Exception {
        final Path binary = Path.of(System.getProperty("porelo.native.binary"));
        final Process process =
                new ProcessBuilder(binary.toString(), "describe").redirectErrorStream(true).start();
        try {
            assertThat(process.waitFor(15, TimeUnit.SECONDS))
                    .as("native describe must not hang")
                    .isTrue();
            final String output =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(process.exitValue()).as("native describe: %s", output).isZero();
            final Object parsed = Json.parse(output);
            assertThat(parsed).as("the published protocol envelope").isInstanceOf(Map.class);
            final Map<?, ?> envelope = (Map<?, ?>) parsed;
            assertThat(envelope.get("definition"))
                    .as("the embedded definition is available in a native image")
                    .isInstanceOf(Map.class);
            final Map<?, ?> definition = (Map<?, ?>) envelope.get("definition");
            assertThat(definition.get("artifacts"))
                    .as("both pinned executables survive native resource inclusion")
                    .isInstanceOf(List.class);
            assertThat((List<?>) definition.get("artifacts"))
                    .as("the complete declared install set")
                    .hasSize(2);
            assertThat(definition.get("name"))
                    .as("the installed adapter identifies itself")
                    .isEqualTo("codex");
            assertThat(definition.get("provider"))
                    .as("Porelo must create a URL broker for the Responses dialect")
                    .isEqualTo(
                            Map.of(
                                    "default", "chatgpt",
                                    "dialect", "openai",
                                    "endpoint", "url",
                                    "addsPath", false));
            assertThat(definition.get("refusedDomains"))
                    .as("the configured refusal must survive native resource inclusion")
                    .isEqualTo(List.of("ab.chatgpt.com"));
        } finally {
            process.destroyForcibly();
            process.waitFor();
        }
    }
}
