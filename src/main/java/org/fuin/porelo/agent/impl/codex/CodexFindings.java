package org.fuin.porelo.agent.impl.codex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.Finder;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.fuin.porelo.agent.api.Finding.SignedIn;
import org.fuin.porelo.agent.api.Finding.What;
import org.fuin.porelo.wire.Json;
import org.fuin.porelo.wire.JsonException;
import org.jspecify.annotations.Nullable;

/**
 * What Codex CLI finds of itself in its configuration directory: the sign-in in {@code auth.json},
 * and whether its configuration keeps the sign-in in the system keyring instead.
 *
 * <p>Only reads files in the directory it is given: it runs no command, reaches no network and
 * reads no environment. A sign-in is carriable exactly when {@link CodexCredentialExtractor} takes
 * it, which is the ChatGPT sign-in alone; an API key is found but not carried.
 */
final class CodexFindings implements Finder {

    /** The provider of the definition a ChatGPT sign-in belongs to. */
    static final String SUBSCRIPTION_PROVIDER = "chatgpt";

    /** The provider of the definition an OpenAI API key belongs to. */
    static final String API_KEY_PROVIDER = "openai";

    static final String CONFIG_FILE = "config.toml";

    /** The configuration's key for where Codex keeps its sign-in, also set by the login. */
    static final String STORE_KEY = "cli_auth_credentials_store";

    /** The stores that may put the sign-in into the system keyring, which is never read here. */
    private static final Set<String> KEYRING_STORES = Set.of("keyring", "auto");

    private static final Pattern STORE =
            Pattern.compile("\\s*" + STORE_KEY + "\\s*=\\s*([\"']?)([A-Za-z]+)\\1\\s*(?:#.*)?");

    private static final int MAX_BYTES = 1024 * 1024;

    @Override
    public List<Finding> find(final Path configDirectory) {

        final Path directory = configDirectory.toAbsolutePath();
        final List<Finding> findings = new ArrayList<>();
        if (Files.isDirectory(directory)) {
            findings.add(
                    new Finding(What.CONFIGURATION, directory.toString(), null, null, null, null));
        }
        findKeyring(directory.resolve(CONFIG_FILE), findings);
        findAuthFile(directory, findings);
        return List.copyOf(findings);
    }

    private static void findKeyring(final Path config, final List<Finding> findings) {
        final List<String> lines;
        try {
            lines = Files.readAllLines(config, StandardCharsets.UTF_8);
        } catch (NoSuchFileException ex) {
            return;
        } catch (IOException ex) {
            findings.add(cannotSay(config));
            return;
        }
        for (final String line : lines) {
            if (line.strip().startsWith("[")) {
                return;
            }
            final Matcher matcher = STORE.matcher(line);
            if (matcher.matches() && KEYRING_STORES.contains(matcher.group(2))) {
                findings.add(
                        new Finding(What.SIGN_IN, null, SignedIn.CANNOT_SAY, false, null, null));
                return;
            }
        }
    }

    private static void findAuthFile(final Path directory, final List<Finding> findings) {
        final AuthFile file = AuthFile.read(directory.resolve(CodexCredentialExtractor.AUTH_FILE));
        switch (file.state()) {
            case ABSENT -> {}
            case UNREADABLE -> findings.add(cannotSay(file.path()));
            case OBJECT -> {
                if (holdsSubscription(file.root())) {
                    findings.add(
                            signedIn(
                                    file.path(),
                                    extractorTakes(directory),
                                    SUBSCRIPTION_PROVIDER,
                                    Kind.SUBSCRIPTION));
                } else if (isSet(file.root().get("OPENAI_API_KEY"))) {
                    findings.add(signedIn(file.path(), false, API_KEY_PROVIDER, Kind.API_KEY));
                } else {
                    findings.add(notSignedIn(file.path()));
                }
            }
            case BROKEN -> findings.add(notSignedIn(file.path()));
        }
    }

    /** The shape the extractor reads: a ChatGPT sign-in with an access token to send. */
    static boolean holdsSubscription(final Map<?, ?> root) {
        return SUBSCRIPTION_PROVIDER.equals(root.get("auth_mode"))
                && root.get("tokens") instanceof Map<?, ?> tokens
                && isSet(tokens.get("access_token"));
    }

    /** Asks the extractor itself, so carriable never differs from what an import takes. */
    private static boolean extractorTakes(final Path directory) {
        try {
            return new CodexCredentialExtractor().extract(directory).isPresent();
        } catch (AgentException ex) {
            return false;
        }
    }

    private static Finding signedIn(
            final Path path, final boolean importable, final String provider, final Kind kind) {
        return new Finding(
                What.SIGN_IN, path.toString(), SignedIn.SIGNED_IN, importable, provider, kind);
    }

    private static Finding notSignedIn(final Path path) {
        return new Finding(What.SIGN_IN, path.toString(), SignedIn.NOT_SIGNED_IN, null, null, null);
    }

    private static Finding cannotSay(final Path path) {
        return new Finding(What.SIGN_IN, path.toString(), SignedIn.CANNOT_SAY, null, null, null);
    }

    private static boolean isSet(final @Nullable Object value) {
        return value instanceof String text && !text.isBlank();
    }

    /** {@code auth.json}, read once as the extractor reads it; a value never leaves this class. */
    record AuthFile(Path path, State state, Map<?, ?> root) {

        enum State {
            ABSENT,
            UNREADABLE,
            BROKEN,
            OBJECT
        }

        /** A link or an oversized file is not read, as the extractor reads neither. */
        static AuthFile read(final Path path) {
            final byte[] bytes;
            try {
                if (!Files.readAttributes(
                                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
                        .isRegularFile()) {
                    return new AuthFile(path, State.UNREADABLE, Map.of());
                }
                try (InputStream input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                    bytes = input.readNBytes(MAX_BYTES + 1);
                }
            } catch (NoSuchFileException ex) {
                return new AuthFile(path, State.ABSENT, Map.of());
            } catch (IOException ex) {
                return new AuthFile(path, State.UNREADABLE, Map.of());
            }
            if (bytes.length > MAX_BYTES) {
                return new AuthFile(path, State.UNREADABLE, Map.of());
            }
            try {
                return Json.parse(new String(bytes, StandardCharsets.UTF_8))
                                instanceof Map<?, ?> map
                        ? new AuthFile(path, State.OBJECT, map)
                        : new AuthFile(path, State.BROKEN, Map.of());
            } catch (JsonException ex) {
                return new AuthFile(path, State.BROKEN, Map.of());
            }
        }
    }
}
