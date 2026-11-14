package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.fuin.porelo.agent.api.Finding.SignedIn;
import org.fuin.porelo.agent.api.Finding.What;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The sign-in's shape is the one the extractor collects from a ChatGPT login. */
class CodexSignInFactsTest {

    private static final String SECRET = "fixture-secret-value";
    private static final String ACCOUNT = "account-one";
    private static final Instant CHANGED = Instant.parse("2026-11-01T10:00:00Z");

    @TempDir Path home;

    private Path config;

    @BeforeEach
    void createTheConfigDirectory() throws IOException {
        config = Files.createDirectory(home.resolve("codex"));
    }

    private List<CodexSignInFacts> facts() {
        return CodexSignInFacts.of(new CodexFindings().find(config), config);
    }

    private void auth(final String content) throws IOException {
        Files.setLastModifiedTime(
                Files.writeString(config.resolve("auth.json"), content), FileTime.from(CHANGED));
    }

    private void subscription(final String account) throws IOException {
        auth(
                CodexCredentialExtractorTest.login(
                        CodexCredentialExtractorTest.accessToken("1800000000"), SECRET, account));
    }

    // Prevents the person choosing a sign-in blind: which account and how fresh.
    @Test
    void aChatgptSignInSaysItsAccountAndLastChange() throws IOException {
        subscription(ACCOUNT);

        assertThat(facts())
                .singleElement()
                .satisfies(
                        facts -> {
                            assertThat(facts.finding().kind()).isEqualTo(Kind.SUBSCRIPTION);
                            assertThat(facts.finding().importable()).as("carriable").isTrue();
                            assertThat(facts.account()).isEqualTo(ACCOUNT);
                            assertThat(facts.expires())
                                    .as("the expiry lies only inside the token")
                                    .isNull();
                            assertThat(facts.changed()).isEqualTo(CHANGED);
                        });
    }

    // Prevents something that is not an account identifier from reaching the person's dialog: the
    // import refuses such a sign-in, so it is not offered at all.
    @Test
    void anAccountWithAControlCharacterIsNotOffered() throws IOException {
        subscription("account\u001b[2J");

        assertThat(facts()).isEmpty();
    }

    // Prevents a file Codex cannot use from being described as a sign-in.
    @Test
    void anAuthFileWithoutASignInHasNoFacts() throws IOException {
        auth("{\"other\":1}");

        assertThat(facts()).isEmpty();
    }

    // Prevents a keyring sign-in, whose place is unknown, from being given a file's facts.
    @Test
    void aKeyringSignInHasNoFacts() throws IOException {
        Files.writeString(
                config.resolve("config.toml"), "cli_auth_credentials_store = \"keyring\"");

        assertThat(facts()).isEmpty();
    }

    // Prevents an API key, which the import does not take, from being offered with the facts.
    @Test
    void anApiKeyHasNoFacts() throws IOException {
        auth("{\"auth_mode\":\"apikey\",\"OPENAI_API_KEY\":\"" + SECRET + "\"}");

        assertThat(facts()).isEmpty();
    }

    // Prevents the file's facts being given to a sign-in found elsewhere, or to one without a path.
    @Test
    void aSignInNotInTheAuthFileHasNoFacts() throws IOException {
        subscription(ACCOUNT);
        final List<Finding> elsewhere =
                List.of(
                        new Finding(
                                What.SIGN_IN,
                                home.resolve("other.json").toString(),
                                SignedIn.SIGNED_IN,
                                true,
                                CodexFindings.SUBSCRIPTION_PROVIDER,
                                Kind.SUBSCRIPTION),
                        new Finding(
                                What.SIGN_IN,
                                null,
                                SignedIn.SIGNED_IN,
                                true,
                                CodexFindings.SUBSCRIPTION_PROVIDER,
                                Kind.SUBSCRIPTION),
                        new Finding(
                                What.CONFIGURATION,
                                config.resolve("auth.json").toAbsolutePath().toString(),
                                SignedIn.SIGNED_IN,
                                true,
                                null,
                                null));

        assertThat(CodexSignInFacts.of(elsewhere, config)).isEmpty();
    }

    // Prevents a credential's value leaving the adapter in what is shown to the person.
    @Test
    void noFactCarriesACredentialsValue() throws IOException {
        subscription(ACCOUNT);

        final List<CodexSignInFacts> facts = facts();

        assertThat(facts).as("the sign-in was read").hasSize(1);
        assertThat(facts.toString())
                .as("no value in any field")
                .doesNotContain(SECRET, CodexCredentialExtractorTest.accessToken("1800000000"));
    }

    // Prevents offering a sign-in Porelo's listing of what can be carried would not hold: it asks
    // the extractor per provider and keeps only an answer that names that provider.
    @Test
    void eachSignInWithFactsIsOneTheExtractorAnswersForItsProvider() throws IOException {
        subscription(ACCOUNT);

        final List<CodexSignInFacts> facts = facts();

        assertThat(facts).as("the fixture holds a sign-in to carry").isNotEmpty();
        assertThat(facts)
                .allSatisfy(
                        fact ->
                                assertThat(
                                                new CodexCredentialExtractor()
                                                        .extract(config, fact.finding().provider()))
                                        .map(found -> found.provider())
                                        .contains(fact.finding().provider()));
    }
}
