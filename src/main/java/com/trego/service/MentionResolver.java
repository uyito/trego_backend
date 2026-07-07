package com.trego.service;

import java.util.Optional;

/**
 * Narrow seam for resolving a {@code @username} to a user UID, letting
 * {@link SocialService} resolve mentions without depending on the username
 * repository directly. Implemented by {@link UsernameMentionResolver}; tests can
 * supply a fake or a no-op.
 */
public interface MentionResolver {
    /** Resolve a (lowercased) username to its owning UID, or empty if none. */
    Optional<String> resolveUid(String username);
}
