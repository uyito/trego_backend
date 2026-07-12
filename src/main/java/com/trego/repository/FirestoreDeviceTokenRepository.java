package com.trego.repository;

import com.trego.model.DeviceToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Firestore-backed {@link DeviceTokenRepository}. Documents are keyed by the FCM
 * token (set as the entity id) so a save is an idempotent upsert.
 */
@Repository
@Primary
public class FirestoreDeviceTokenRepository implements DeviceTokenRepository {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreDeviceTokenRepository.class);

    private final DeviceTokenStore store;

    @Autowired
    public FirestoreDeviceTokenRepository(DeviceTokenStore store) {
        this.store = store;
    }

    @Override
    public void save(DeviceToken token) {
        try {
            // Key by the token itself so re-registration overwrites in place.
            token.setId(token.getToken());
            store.update(token);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("save", e);
        }
    }

    @Override
    public List<DeviceToken> findByUid(String uid) {
        try {
            return store.findByField("uid", uid);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findByUid", e);
        }
    }

    @Override
    public void deleteByToken(String token) {
        try {
            store.deleteById(token);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("deleteByToken", e);
        }
    }

    private RuntimeException rethrow(String op, Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        logger.error("Device token repository {} failed: {}", op, e.getMessage());
        return new RuntimeException("Device token operation failed: " + op, e);
    }
}
