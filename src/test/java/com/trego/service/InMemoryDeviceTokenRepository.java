package com.trego.service;

import com.trego.model.DeviceToken;
import com.trego.repository.DeviceTokenRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory {@link DeviceTokenRepository} for unit tests, keyed by token. */
public class InMemoryDeviceTokenRepository implements DeviceTokenRepository {

    final Map<String, DeviceToken> byToken = new LinkedHashMap<>();

    @Override
    public void save(DeviceToken token) {
        token.setId(token.getToken());
        byToken.put(token.getToken(), token);
    }

    @Override
    public List<DeviceToken> findByUid(String uid) {
        List<DeviceToken> out = new ArrayList<>();
        for (DeviceToken t : byToken.values()) {
            if (uid.equals(t.getUid())) out.add(t);
        }
        return out;
    }

    @Override
    public void deleteByToken(String token) {
        byToken.remove(token);
    }
}
