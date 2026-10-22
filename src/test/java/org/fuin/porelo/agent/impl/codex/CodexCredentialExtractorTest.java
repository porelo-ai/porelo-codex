package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.Credential;
import org.fuin.porelo.agent.api.CredentialExtractor;
import org.fuin.porelo.wire.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Guards lost refresh/account metadata and credential disclosure while collecting a login. */
class CodexCredentialExtractorTest {
    private static final String REFRESH = "synthetic-refresh-never-issued";
    private static final String ID_TOKEN = "synthetic-id-never-issued";
    private final CredentialExtractor extractor = new CodexAgent().credentialExtractor();
    @TempDir Path directory;

    @Test
    void aMissingLoginLeavesNothingToImport() {
        assertThat(extractor.extract(directory))
                .as("a login that has not written its credential must not look authenticated")
                .isEmpty();
    }

    @Test
    void aChatgptLoginRetainsEverythingTheHostNeedsToRouteAndRenewIt() throws IOException {
        final String access = accessToken("42");
        write(login(access, REFRESH, "account-one"));

        final Optional<Credential> result = extractor.extract(directory);

        assertThat(result).as("the measured nested OAuth record is collectable").isPresent();
        final Credential credential = result.orElseThrow();
        assertThat(credential.type())
                .as("a sign-in must not be treated as an API key")
                .isEqualTo("oauth");
        assertThat(credential.secret())
                .as("the access token is the broker's bearer")
                .isEqualTo(access);
        assertThat(credential.attributes())
                .as("renewal and account routing survive, but the unused ID token does not")
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                Credential.REFRESH_TOKEN,
                                REFRESH,
                                Credential.EXPIRES_AT,
                                "42000",
                                Credential.TOKEN_URL,
                                "https://auth.openai.com/oauth/token",
                                Credential.CLIENT_ID,
                                "app_EMoamEEZ73f0CkXaXp7hrann",
                                "account_id",
                                "account-one"));
        assertThat(credential.toString())
                .as("diagnostics must not disclose credential values")
                .doesNotContain(access, REFRESH, ID_TOKEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"access_token", "refresh_token", "account_id"})
    void anIncompleteSignInCannotSilentlyBecomeAnUnrenewableOrUnroutableCredential(String field)
            throws IOException {
        final Map<String, Object> tokens = tokens(accessToken("42"), REFRESH, "account-one");
        tokens.put(field, " ");
        write(Json.write(Map.of("auth_mode", "chatgpt", "tokens", tokens)));

        assertThatThrownBy(() -> extractor.extract(directory))
                .as("a present but incomplete sign-in must fail before reaching the vault")
                .isInstanceOf(AgentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "1e40", "\"not-an-expiry\"", "null"})
    void anInvalidExpiryCannotDisableHostRenewal(String expiry) throws IOException {
        write(login(accessToken(expiry), REFRESH, "account-one"));
        assertThatThrownBy(() -> extractor.extract(directory))
                .as("expiry must be positive integral epoch seconds that fit the host contract")
                .isInstanceOf(AgentException.class);
    }

    @Test
    void anApiKeyCannotBeImportedAsAChatgptSubscription() throws IOException {
        write("{\"auth_mode\":\"apikey\",\"OPENAI_API_KEY\":\"synthetic-api-key\"}");
        assertThatThrownBy(() -> extractor.extract(directory))
                .as("API keys belong to their API provider, not the subscription provider")
                .isInstanceOf(AgentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "{\"auth_mode\":\"chatgpt\",\"tokens\":null}"})
    void anExistingFileWithNoSignInIsNotMistakenForAnUnfinishedLogin(String json)
            throws IOException {
        write(json);
        assertThatThrownBy(() -> extractor.extract(directory))
                .as("an existing malformed record must not quietly report no credential")
                .isInstanceOf(AgentException.class);
    }

    @Test
    void parseErrorsCannotEchoTheCredentialThroughTheirCause() throws IOException {
        write("{\"" + REFRESH + "\":1,\"" + REFRESH + "\":2}");
        assertThatThrownBy(() -> extractor.extract(directory))
                .as("JSON diagnostics can repeat keys containing a token")
                .isInstanceOf(AgentException.class)
                .satisfies(
                        error ->
                                assertThat(stackTrace(error))
                                        .as(
                                                "neither the message nor a nested exception exposes token bytes")
                                        .doesNotContain(REFRESH));
    }

    @Test
    void invalidTokenMetadataCannotEchoItsContents() throws IOException {
        write(login("not-a-jwt-" + REFRESH, REFRESH, "account-one"));
        assertThatThrownBy(() -> extractor.extract(directory))
                .as("invalid access-token metadata must fail without printing it")
                .isInstanceOf(AgentException.class)
                .satisfies(
                        error ->
                                assertThat(stackTrace(error))
                                        .as(
                                                "decoding errors must not retain the supplied credential in diagnostics")
                                        .doesNotContain(REFRESH));
    }

    @Test
    void anAccountCannotInjectAnotherBrokerHeader() throws IOException {
        write(login(accessToken("42"), REFRESH, "account\r\nOther: injected"));
        assertThatThrownBy(() -> extractor.extract(directory))
                .as("the account attribute is later used as an HTTP header value")
                .isInstanceOf(AgentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"access_token", "refresh_token"})
    void aTokenWithControlCharactersCannotReachTheBroker(String field) throws IOException {
        final Map<String, Object> fields = tokens(accessToken("42"), REFRESH, "account-one");
        fields.put(field, fields.get(field) + "\r\nInjected: value");
        write(Json.write(Map.of("auth_mode", "chatgpt", "tokens", fields)));
        assertThatThrownBy(() -> extractor.extract(directory))
                .as("malformed token values must be refused before broker or renewal requests")
                .isInstanceOf(AgentException.class);
    }

    @Test
    void aSymlinkCannotRedirectCollectionToAnotherCredentialFile() throws IOException {
        final Path other =
                Files.writeString(
                        directory.resolve("other.json"),
                        login(accessToken("42"), REFRESH, "account-one"));
        Files.createSymbolicLink(directory.resolve("auth.json"), other);
        assertThatThrownBy(() -> extractor.extract(directory))
                .as("collection reads only the regular credential file it was handed")
                .isInstanceOf(AgentException.class);
    }

    @Test
    void anOversizedCredentialFileCannotExhaustTheAdapter() throws IOException {
        write(" ".repeat(1024 * 1024 + 1));
        assertThatThrownBy(() -> extractor.extract(directory))
                .as("copied login input is bounded before JSON parsing")
                .isInstanceOf(AgentException.class);
    }

    private void write(String json) throws IOException {
        Files.writeString(directory.resolve("auth.json"), json, StandardCharsets.UTF_8);
    }

    static String login(String access, String refresh, String account) {
        return Json.write(
                Map.of("auth_mode", "chatgpt", "tokens", tokens(access, refresh, account)));
    }

    private static Map<String, Object> tokens(String access, String refresh, String account) {
        final Map<String, Object> fields = new java.util.LinkedHashMap<>();
        fields.put("access_token", access);
        fields.put("refresh_token", refresh);
        fields.put("account_id", account);
        fields.put("id_token", ID_TOKEN);
        return fields;
    }

    static String accessToken(String expiry) {
        return "eyJhbGciOiJub25lIn0."
                + Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                ("{\"exp\":" + expiry + "}").getBytes(StandardCharsets.UTF_8))
                + ".synthetic-signature";
    }

    private static String stackTrace(Throwable error) {
        final java.io.StringWriter output = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(output));
        return output.toString();
    }
}
