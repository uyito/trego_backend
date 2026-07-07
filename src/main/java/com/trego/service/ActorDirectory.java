package com.trego.service;

import java.util.Optional;

/**
 * Resolves an actor's display info (name + photo) for denormalizing onto a
 * notification at write time. Kept as a narrow interface so
 * {@link NotificationFeedService} is unit-testable without Firestore.
 */
public interface ActorDirectory {

    Optional<ActorInfo> lookup(String uid);

    final class ActorInfo {
        public final String uid;
        public final String name;
        public final String photoUrl;

        public ActorInfo(String uid, String name, String photoUrl) {
            this.uid = uid;
            this.name = name;
            this.photoUrl = photoUrl;
        }
    }
}
