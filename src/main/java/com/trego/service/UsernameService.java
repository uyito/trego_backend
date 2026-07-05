package com.trego.service;

import com.trego.repository.UsernameDirectory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Username validation + claiming. Names are normalized to lowercase; uniqueness
 * is enforced by the {@link UsernameDirectory} index. Rename is supported: a
 * successful claim of the new name releases the caller's previous name.
 */
@Service
public class UsernameService {

    /** 3–20 chars, starts with a letter, lowercase alphanumeric + underscore. */
    private static final Pattern VALID = Pattern.compile("^[a-z][a-z0-9_]{2,19}$");

    private static final Set<String> RESERVED = Set.of("admin", "root", "trego", "support");

    private final UsernameDirectory directory;

    public UsernameService(UsernameDirectory directory) {
        this.directory = directory;
    }

    /**
     * Claim [desired] for [uid], releasing [currentUsername] if the caller is
     * renaming. Returns the normalized (lowercased) username actually stored.
     *
     * @throws IllegalArgumentException if the format is invalid or reserved
     * @throws IllegalStateException    if the name is taken by another user
     */
    public String claim(String uid, String currentUsername, String desired) {
        if (desired == null) {
            throw new IllegalArgumentException("Username is required");
        }
        String normalized = desired.trim().toLowerCase();
        if (!VALID.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3–20 characters, start with a letter, and use only "
                            + "lowercase letters, numbers, or underscores");
        }
        if (RESERVED.contains(normalized)) {
            throw new IllegalArgumentException("That username is not available");
        }

        // No-op if unchanged (idempotent re-claim of the same name).
        if (normalized.equals(currentUsername)) {
            return normalized;
        }

        if (!directory.tryClaim(normalized, uid)) {
            throw new IllegalStateException("That username is already taken");
        }

        // Release the old name only after the new one is secured.
        if (currentUsername != null && !currentUsername.isEmpty()) {
            directory.release(currentUsername);
        }
        return normalized;
    }
}
