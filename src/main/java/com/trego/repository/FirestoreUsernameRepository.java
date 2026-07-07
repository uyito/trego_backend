package com.trego.repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore-backed {@link UsernameDirectory}. Uses a transaction for {@link
 * #tryClaim} so uniqueness holds under concurrent claims. Documents live in the
 * "usernames" collection keyed by the lowercased username.
 */
@Repository
public class FirestoreUsernameRepository implements UsernameDirectory {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreUsernameRepository.class);
    private static final String COLLECTION = "usernames";

    private final Firestore firestore;

    @Autowired
    public FirestoreUsernameRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Optional<String> resolveUid(String username) {
        if (username == null || username.isEmpty()) return Optional.empty();
        try {
            DocumentSnapshot d = firestore.collection(COLLECTION).document(username).get().get();
            if (!d.exists()) return Optional.empty();
            return Optional.ofNullable((String) d.get("uid"));
        } catch (InterruptedException | ExecutionException e) {
            throw rethrow("resolveUid", e);
        }
    }

    @Override
    public boolean tryClaim(String username, String uid) {
        try {
            DocumentReference ref = firestore.collection(COLLECTION).document(username);
            return firestore.runTransaction(txn -> {
                DocumentSnapshot snap = txn.get(ref).get();
                if (snap.exists()) {
                    String owner = (String) snap.get("uid");
                    if (uid.equals(owner)) return true;   // already ours
                    return false;                         // held by someone else
                }
                Map<String, Object> data = new HashMap<>();
                data.put("uid", uid);
                data.put("username", username);
                txn.set(ref, data);
                return true;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw rethrow("tryClaim", e);
        }
    }

    @Override
    public void release(String username) {
        if (username == null || username.isEmpty()) return;
        try {
            firestore.collection(COLLECTION).document(username).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw rethrow("release", e);
        }
    }

    private RuntimeException rethrow(String op, Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        logger.error("Username directory {} failed: {}", op, e.getMessage());
        return new RuntimeException("Username directory operation failed: " + op, e);
    }
}
