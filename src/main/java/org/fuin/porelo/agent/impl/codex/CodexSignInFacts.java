package org.fuin.porelo.agent.impl.codex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.SignedIn;
import org.fuin.porelo.agent.api.Finding.What;
import org.jspecify.annotations.Nullable;

/**
 * What a sign-in Codex CLI keeps in {@code auth.json} says of itself beside its finding, for a
 * person who chooses which one to carry to a host: the account, when it expires and when it last
 * changed. Never its value; the value goes to Porelo through {@link CodexCredentialExtractor}
 * alone.
 *
 * <p>The account is the ChatGPT sign-in's {@code tokens.account_id}, an identifier the extractor
 * also keeps for routing; an API key names none. The expiry is always {@code null}: the file holds
 * it only inside the access token, which is the sign-in's value and is not opened for a fact. The
 * last change is the file's own time, which a renewal moves.
 *
 * @param finding The finding these facts belong to.
 * @param account The account the sign-in is for, or {@code null} where the file names none.
 * @param expires When the sign-in expires; {@code null}, since the file holds no expiry of its own.
 * @param changed When its file was last written, or {@code null} where that cannot be read.
 */
record CodexSignInFacts(
        Finding finding,
        @Nullable String account,
        @Nullable Instant expires,
        @Nullable Instant changed) {

    /** What the extractor takes as an account, so no other text reaches the person as one. */
    private static final Pattern ACCOUNT = Pattern.compile("[\\x21-\\x7e]{1,256}");

    /**
     * Returns the facts of each sign-in in Codex CLI's {@code auth.json} that the import takes, in
     * the findings' order; an API key, which it does not take, has none.
     *
     * @param findings What {@link CodexFindings} found in the directory.
     * @param configDirectory Codex CLI's configuration directory.
     * @return The facts, one per sign-in found in the file.
     */
    static List<CodexSignInFacts> of(final List<Finding> findings, final Path configDirectory) {
        final Path auth =
                configDirectory
                        .toAbsolutePath()
                        .resolve(CodexCredentialExtractor.AUTH_FILE)
                        .normalize();
        return findings.stream()
                .filter(finding -> finding.what() == What.SIGN_IN)
                .filter(finding -> finding.signedIn() == SignedIn.SIGNED_IN)
                .filter(finding -> Boolean.TRUE.equals(finding.importable()))
                .filter(finding -> finding.path() != null)
                .filter(finding -> Path.of(finding.path()).normalize().equals(auth))
                .map(
                        finding -> {
                            final CodexFindings.AuthFile file = CodexFindings.AuthFile.read(auth);
                            return new CodexSignInFacts(
                                    finding, account(file.root()), null, changed(auth));
                        })
                .toList();
    }

    private static @Nullable String account(final Map<?, ?> root) {
        if (CodexFindings.holdsSubscription(root)
                && root.get("tokens") instanceof Map<?, ?> tokens
                && tokens.get("account_id") instanceof String id
                && ACCOUNT.matcher(id).matches()) {
            return id;
        }
        return null;
    }

    private static @Nullable Instant changed(final Path file) {
        try {
            return Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toInstant();
        } catch (IOException ex) {
            return null;
        }
    }
}
