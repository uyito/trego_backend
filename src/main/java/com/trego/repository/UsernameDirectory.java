package com.trego.repository;

import java.util.Optional;

/**
 * Uniqueness index for usernames. Firestore has no unique constraint, so the
 * "usernames" collection (doc id = lowercased username → {uid}) is the source of
 * truth: a {@link #tryClaim} is an atomic check-and-set so two racing claims for
 * the same name cannot both succeed.
 *
 * Production impl: {@link FirestoreUsernameRepository}.
 * Test impl: InMemoryUsernameDirectory (test sources).
 */
public interface UsernameDirectory {

    /** Resolve the owning UID for a (already-normalized, lowercased) username. */
    Optional<String> resolveUid(String username);

    /**
     * Atomically claim [username] for [uid]. Returns true if [uid] now owns it
     * (newly claimed, or already owned by [uid]); false if another UID holds it.
     */
    boolean tryClaim(String username, String uid);

    /** Release a username so it can be reclaimed. No-op if absent. */
    void release(String username);
}
