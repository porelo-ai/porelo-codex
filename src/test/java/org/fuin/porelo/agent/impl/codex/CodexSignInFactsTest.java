package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.fuin.porelo.agent.api.Finding.Kind;
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

    // Prevents an API key from claiming an account the file does not name for it.
    @Test
    void anApiKeySaysItsLastChangeAndNoAccount() throws IOException {
        auth("{\"auth_mode\":\"apikey\",\"OPENAI_API_KEY\":\"" + SECRET + "\"}");

        assertThat(facts())
                .singleElement()
                .satisfies(
                        facts -> {
                            assertThat(facts.finding().kind()).isEqualTo(Kind.API_KEY);
                            assertThat(facts.account()).isNull();
                            assertThat(facts.expires()).isNull();
                            assertThat(facts.changed()).isEqualTo(CHANGED);
                        });
    }

    // Prevents something that is not an account identifier from reaching the person's dialog.
    @Test
    void anAccountWithAControlCharacterIsNoAccount() throws IOException {
        subscription("account\u001b[2J");

        assertThat(facts()).singleElement().extracting(CodexSignInFacts::account).isNull();
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
}
