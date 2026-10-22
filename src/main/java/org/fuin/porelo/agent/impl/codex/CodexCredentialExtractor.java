package org.fuin.porelo.agent.impl.codex;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.Credential;
import org.fuin.porelo.agent.api.CredentialExtractor;
import org.fuin.porelo.wire.Json;
import org.fuin.porelo.wire.JsonException;

/** Collects Codex's nested ChatGPT sign-in for host-side routing and renewal. */
public final class CodexCredentialExtractor implements CredentialExtractor {
    private static final int MAX_BYTES = 1024 * 1024;
    private static final long MAX_EXPIRY_MILLIS = 999_999_999_999_999L;
    private static final Pattern BEARER = Pattern.compile("[A-Za-z0-9._~+/-]+=*");
    private static final Pattern ACCOUNT = Pattern.compile("[\\x21-\\x7e]{1,256}");
    private static final String TOKEN_URL = "https://auth.openai.com/oauth/token";
    private static final String CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann";

    /** {@inheritDoc} */
    @Override
    public Optional<Credential> extract(Path configDirectory) {
        final Path file = configDirectory.resolve("auth.json");
        final byte[] bytes;
        try {
            final BasicFileAttributes attributes =
                    Files.readAttributes(
                            file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile()) {
                throw new AgentException("Codex auth.json must be a regular file, not a link");
            }
            try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
                bytes = input.readNBytes(MAX_BYTES + 1);
            }
        } catch (NoSuchFileException exception) {
            return Optional.empty();
        } catch (IOException exception) {
            throw new AgentException("Cannot read Codex auth.json");
        }
        if (bytes.length > MAX_BYTES) {
            throw new AgentException("Codex auth.json exceeds the one-mebibyte collection limit");
        }
        final Map<?, ?> root = object(new String(bytes, StandardCharsets.UTF_8));
        if (!"chatgpt".equals(root.get("auth_mode"))
                || !(root.get("tokens") instanceof Map<?, ?> tokens)) {
            throw new AgentException("Codex auth.json does not hold a ChatGPT OAuth sign-in");
        }
        final String access = token(tokens, "access_token");
        final String refresh = token(tokens, "refresh_token");
        final String account = required(tokens, "account_id");
        if (!ACCOUNT.matcher(account).matches()) {
            throw new AgentException("Codex auth.json has an unusable account identifier");
        }
        return Optional.of(
                new Credential(
                        "oauth",
                        access,
                        Map.of(
                                Credential.REFRESH_TOKEN,
                                refresh,
                                Credential.EXPIRES_AT,
                                expiry(access),
                                Credential.TOKEN_URL,
                                TOKEN_URL,
                                Credential.CLIENT_ID,
                                CLIENT_ID,
                                "account_id",
                                account)));
    }

    private static Map<?, ?> object(String json) {
        final Object parsed;
        try {
            parsed = Json.parse(json);
        } catch (JsonException exception) {
            // Parser diagnostics and their causes can repeat credential-bearing input.
            throw new AgentException("Cannot parse Codex credential JSON");
        }
        if (!(parsed instanceof Map<?, ?> fields)) {
            throw new AgentException("Codex credential JSON must be an object");
        }
        return fields;
    }

    private static String required(Map<?, ?> fields, String key) {
        if (!(fields.get(key) instanceof String value) || value.isBlank()) {
            throw new AgentException("Codex auth.json is missing a required OAuth field: " + key);
        }
        return value;
    }

    private static String token(Map<?, ?> fields, String key) {
        final String value = required(fields, key);
        if (!BEARER.matcher(value).matches()) {
            throw new AgentException("Codex auth.json has an unusable OAuth token field: " + key);
        }
        return value;
    }

    private static String expiry(String access) {
        final String[] segments = access.split("\\.", -1);
        if (segments.length != 3
                || segments[0].isEmpty()
                || segments[1].isEmpty()
                || segments[2].isEmpty()) {
            throw new AgentException("Codex access token has no usable expiry metadata");
        }
        final Map<?, ?> claims;
        try {
            claims =
                    object(
                            new String(
                                    Base64.getUrlDecoder().decode(segments[1]),
                                    StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            throw new AgentException("Codex access token has invalid expiry metadata");
        }
        // The provider validates authentication; this reads renewal metadata only.
        if (!(claims.get("exp") instanceof Number number)) {
            throw new AgentException("Codex access token is missing its numeric expiry");
        }
        try {
            final long seconds = new BigDecimal(number.toString()).longValueExact();
            final long milliseconds = Math.multiplyExact(seconds, 1000L);
            if (milliseconds <= 0 || milliseconds > MAX_EXPIRY_MILLIS) {
                throw new ArithmeticException();
            }
            return Long.toString(milliseconds);
        } catch (ArithmeticException exception) {
            throw new AgentException("Codex access token has an unusable expiry");
        }
    }
}
