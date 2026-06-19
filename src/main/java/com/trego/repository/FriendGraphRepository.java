package com.trego.repository;

import com.trego.model.FriendRequest;
import com.trego.model.Friendship;

import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction for the friend graph plus the minimal user-directory
 * lookups the friend feature needs (resolve-by-email, name/photo for view
 * models). Folding the user lookups in here keeps {@code FriendService} free of
 * checked Firestore exceptions and direct {@code UserRepository} coupling.
 *
 * Production impl: {@link FirestoreFriendGraphRepository}.
 * Test impl: InMemoryFriendGraphRepository (test sources).
 */
public interface FriendGraphRepository {

    FriendRequest saveRequest(FriendRequest request);

    Optional<FriendRequest> findRequest(String id);

    /** Pending request in the exact direction from→to, if any. */
    Optional<FriendRequest> findPending(String fromUid, String toUid);

    /** All pending requests where [uid] is either the sender or recipient. */
    List<FriendRequest> findPendingInvolving(String uid);

    Friendship saveFriendship(Friendship friendship);

    Optional<Friendship> findFriendship(String pairKey);

    List<Friendship> findFriendshipsFor(String uid);

    void deleteFriendship(String id);

    /** Resolve a user UID by exact email (case-insensitive). */
    Optional<String> resolveUidByEmail(String email);

    /** Lightweight profile view for rendering friend/request cards. */
    Optional<UserView> getUserView(String uid);

    /** Minimal user projection: UID + display name + optional photo URL. */
    final class UserView {
        public final String uid;
        public final String name;
        public final String photoUrl;

        public UserView(String uid, String name, String photoUrl) {
            this.uid = uid;
            this.name = name;
            this.photoUrl = photoUrl;
        }
    }
}
