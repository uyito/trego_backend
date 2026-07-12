package com.trego.repository;

import com.trego.model.DeviceToken;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceTokenStore extends FirestoreRepository<DeviceToken> {
    public DeviceTokenStore() {
        super("device_tokens", DeviceToken::fromFirestoreMap);
    }
}
