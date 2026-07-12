package com.trego.repository;

import com.trego.model.DeviceToken;

import java.util.List;

/**
 * Storage abstraction for FCM device tokens.
 * Production impl: {@link FirestoreDeviceTokenRepository}.
 * Test impl: InMemoryDeviceTokenRepository (test sources).
 */
public interface DeviceTokenRepository {

    /** Upsert a token → uid registration (keyed by the token). */
    void save(DeviceToken token);

    /** All tokens registered to a user. */
    List<DeviceToken> findByUid(String uid);

    /** Remove a token (unregister, or reactive cleanup of a dead token). */
    void deleteByToken(String token);
}
