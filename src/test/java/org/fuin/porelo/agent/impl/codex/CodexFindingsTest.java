package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.fuin.porelo.agent.api.Finding.SignedIn;
import org.fuin.porelo.agent.api.Finding.What;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The sign-in's shape is the one the extractor collects from a ChatGPT login. */
class CodexFindingsTest {

    private static final String SECRET = "fixture-secret-value";

    @TempDir Path home;

    private Path config;

    @BeforeEach
    void createTheConfigDirectory() throws IOException {
        config = Files.createDirectory(home.resolve("codex"));
    }

    private List<Finding> find() {
        return new CodexFindings().find(config);
    }

    private List<Finding> signIns(final List<Finding> findings) {
        return findings.stream().filter(finding -> finding.what() == What.SIGN_IN).toList();
    }

    private List<Finding> signIns() {
        return signIns(find());
    }

    private void auth(final String content) throws IOException {
        Files.writeString(config.resolve("auth.json"), content);
    }

    private void subscription() throws IOException {
        auth(
                CodexCredentialExtractorTest.login(
                        CodexCredentialExtractorTest.accessToken("1800000000"),
                        SECRET,
                        "account-one"));
    }

    private void configToml(final String content) throws IOException {
        Files.writeString(config.resolve("config.toml"), content);
    }

    private String path(final String file) {
        return config.resolve(file).toAbsolutePath().toString();
    }

    private static Finding signedIn(
            final String path, final boolean importable, final String provider, final Kind kind) {
        return new Finding(What.SIGN_IN, path, SignedIn.SIGNED_IN, importable, provider, kind);
    }

    private static Finding cannotSay(final String path) {
        return new Finding(What.SIGN_IN, path, SignedIn.CANNOT_SAY, null, null, null);
    }

    private static Finding inTheKeyring() {
        return new Finding(What.SIGN_IN, null, SignedIn.CANNOT_SAY, false, null, null);
    }

    // Prevents a ChatGPT sign-in on the person's machine from being missed or not offered to carry.
    @Test
    void aChatgptSignInIsCarriableAndChatgpts() throws IOException {
        subscription();

        assertThat(signIns())
                .containsExactly(signedIn(path("auth.json"), true, "chatgpt", Kind.SUBSCRIPTION));
    }

    // Prevents offering to carry a sign-in the extractor would then refuse at the import.
    @Test
    void aChatgptSignInTheExtractorRefusesIsNotCarriable() throws IOException {
        auth(CodexCredentialExtractorTest.login("not-a-jwt-" + SECRET, SECRET, "account-one"));

        assertThat(signIns())
                .as("Codex may still use it, but an import cannot take it")
                .containsExactly(signedIn(path("auth.json"), false, "chatgpt", Kind.SUBSCRIPTION));
    }

    // Prevents an API key from being carried as a subscription or offered as carriable at all.
    @Test
    void anApiKeyIsFoundAsOpenaisButNotCarriable() throws IOException {
        auth("{\"auth_mode\":\"apikey\",\"OPENAI_API_KEY\":\"" + SECRET + "\"}");

        assertThat(signIns())
                .containsExactly(signedIn(path("auth.json"), false, "openai", Kind.API_KEY));
    }

    // Prevents a file Codex cannot sign in with from reading as a sign-in.
    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "[1]",
                "not json",
                "{\"auth_mode\":\"chatgpt\",\"tokens\":null}",
                "{\"auth_mode\":\"chatgpt\",\"tokens\":{\"access_token\":\" \"}}",
                "{\"auth_mode\":\"apikey\",\"OPENAI_API_KEY\":null}"
            })
    void aFileCodexCannotUseIsNotSignedIn(final String content) throws IOException {
        auth(content);

        assertThat(signIns())
                .containsExactly(
                        new Finding(
                                What.SIGN_IN,
                                path("auth.json"),
                                SignedIn.NOT_SIGNED_IN,
                                null,
                                null,
                                null));
    }

    // Prevents "not signed in" when the sign-in's file could not be read.
    @Test
    void anAuthFileThatCannotBeReadCannotSay() throws IOException {
        Files.createDirectory(config.resolve("auth.json"));

        assertThat(signIns()).containsExactly(cannotSay(path("auth.json")));
    }

    // Prevents a link from making the finder read a file elsewhere that the extractor never takes.
    @Test
    void anAuthFileThatIsALinkCannotSay() throws IOException {
        final Path other =
                Files.writeString(
                        home.resolve("other.json"),
                        CodexCredentialExtractorTest.login(
                                CodexCredentialExtractorTest.accessToken("1800000000"),
                                SECRET,
                                "account-one"));
        Files.createSymbolicLink(config.resolve("auth.json"), other);

        assertThat(signIns()).containsExactly(cannotSay(path("auth.json")));
    }

    // Prevents "not signed in" for a sign-in Codex keeps in the system keyring, never read here.
    @ParameterizedTest
    @ValueSource(
            strings = {
                "cli_auth_credentials_store = \"keyring\"\n",
                "model = \"x\"\ncli_auth_credentials_store='auto' # comment\n[tools]\n"
            })
    void aKeyringStoreCannotSayAndIsNotCarriable(final String toml) throws IOException {
        configToml(toml);
        subscription();

        assertThat(signIns())
                .as("the keyring comes first, the file stays as found")
                .containsExactly(
                        inTheKeyring(),
                        signedIn(path("auth.json"), true, "chatgpt", Kind.SUBSCRIPTION));
    }

    // Prevents a file store, or the key inside another table, from being taken for the keyring.
    @ParameterizedTest
    @ValueSource(
            strings = {
                "cli_auth_credentials_store = \"file\"\n",
                "[profiles.other]\ncli_auth_credentials_store = \"keyring\"\n"
            })
    void anotherStoreSaysNothingOfTheKeyring(final String toml) throws IOException {
        configToml(toml);

        assertThat(signIns()).isEmpty();
    }

    // Prevents an endless file from being read whole: the extractor refuses it, so it cannot say.
    @Test
    void anOversizedAuthFileCannotSay() throws IOException {
        auth("{\"auth_mode\":\"apikey\",\"OPENAI_API_KEY\":\"x\"}" + " ".repeat(1024 * 1024));

        assertThat(signIns()).containsExactly(cannotSay(path("auth.json")));
    }

    // Prevents a linked configuration from being followed to a file elsewhere, or a dangling one
    // from passing for none.
    @Test
    void aConfigurationThatIsALinkCannotSay() throws IOException {
        final Path other =
                Files.writeString(
                        home.resolve("other.toml"), "cli_auth_credentials_store = \"file\"\n");
        Files.createSymbolicLink(config.resolve("config.toml"), other);

        assertThat(signIns()).containsExactly(cannotSay(path("config.toml")));

        Files.delete(other);
        assertThat(signIns()).as("dangling").containsExactly(cannotSay(path("config.toml")));
    }

    // Prevents an endless configuration from being read whole.
    @Test
    void anOversizedConfigurationCannotSay() throws IOException {
        configToml("# " + "x".repeat(1024 * 1024) + "\n");

        assertThat(signIns()).containsExactly(cannotSay(path("config.toml")));
    }

    // Prevents a keyring sign-in from being missed because the configuration could not be read.
    @Test
    void aConfigurationThatCannotBeReadCannotSay() throws IOException {
        Files.createDirectory(config.resolve("config.toml"));

        assertThat(signIns()).containsExactly(cannotSay(path("config.toml")));
    }

    // Prevents an empty account from reading as an agent signed in.
    @Test
    void nothingThereIsNoSignInFinding() {
        assertThat(find())
                .as("only the directory itself is found")
                .containsExactly(
                        new Finding(
                                What.CONFIGURATION,
                                config.toAbsolutePath().toString(),
                                null,
                                null,
                                null,
                                null));
    }

    // Prevents an account that never ran Codex from reading as one that has.
    @Test
    void noDirectoryIsNoFindingAtAll() throws IOException {
        Files.delete(config);

        assertThat(find()).as("nothing is there").isEmpty();
    }

    // Prevents a credential's value from leaving the adapter in what is shown to the person.
    @Test
    void noFindingCarriesACredentialsValue() throws IOException {
        configToml("cli_auth_credentials_store = \"keyring\"\n");
        subscription();

        final List<Finding> found = find();

        assertThat(signIns(found)).as("the guard must have read every route").hasSize(2);
        assertThat(found.toString())
                .as("no value in any field")
                .doesNotContain(SECRET, CodexCredentialExtractorTest.accessToken("1800000000"));
    }

    // Prevents the adapter from answering "cannot say" through Porelo's default for ever.
    @Test
    void theAgentAnswersWithItsOwnFindings() throws IOException {
        subscription();

        assertThat(signIns(new CodexAgent().finder().find(config)))
                .as("the agent hands out this finder")
                .containsExactly(signedIn(path("auth.json"), true, "chatgpt", Kind.SUBSCRIPTION));
    }
}
