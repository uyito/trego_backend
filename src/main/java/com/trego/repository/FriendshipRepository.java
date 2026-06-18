package com.trego.repository;

import com.google.cloud.firestore.Query;
import com.trego.model.Friendship;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class FriendshipRepository extends FirestoreRepository<Friendship> {
    public FriendshipRepository() {
        super("social_friendships", Friendship::fromFirestoreMap);
    }

    /** Friendships that include [uid]. Single-field array-contains (auto-indexed). */
    public List<Friendship> findByUser(String uid) throws ExecutionException, InterruptedException {
        Query query = firestore.collection(collectionName).whereArrayContains("users", uid);
        return findByQuery(query);
    }
}
