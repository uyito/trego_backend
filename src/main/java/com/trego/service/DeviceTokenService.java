package com.trego.service;

import com.trego.model.DeviceToken;
import com.trego.repository.DeviceTokenRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Registers/looks up FCM device tokens for push delivery. */
@Service
public class DeviceTokenService {

    private final DeviceTokenRepository repo;

    public DeviceTokenService(DeviceTokenRepository repo) {
        this.repo = repo;
    }

    /** Register (upsert) a token for a user. */
    public void register(String uid, String token, String platform) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token is required");
        }
        DeviceToken t = new DeviceToken();
        t.setToken(token);
        t.setUid(uid);
        t.setPlatform(platform != null ? platform : "unknown");
        repo.save(t);
    }

    public void unregister(String token) {
        if (token != null && !token.isEmpty()) {
            repo.deleteByToken(token);
        }
    }

    /** The FCM tokens registered to a user (may be empty). */
    public List<String> tokensFor(String uid) {
        List<String> out = new ArrayList<>();
        for (DeviceToken t : repo.findByUid(uid)) {
            if (t.getToken() != null) out.add(t.getToken());
        }
        return out;
    }
}
