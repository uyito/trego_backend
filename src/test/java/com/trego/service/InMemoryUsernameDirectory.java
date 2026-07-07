package com.trego.service;

import com.trego.repository.UsernameDirectory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link UsernameDirectory} for unit tests. */
public class InMemoryUsernameDirectory implements UsernameDirectory {

    final Map<String, String> byName = new LinkedHashMap<>();   // username → uid

    @Override
    public Optional<String> resolveUid(String username) {
        return Optional.ofNullable(byName.get(username));
    }

    @Override
    public boolean tryClaim(String username, String uid) {
        String owner = byName.get(username);
        if (owner != null) return owner.equals(uid);
        byName.put(username, uid);
        return true;
    }

    @Override
    public void release(String username) {
        byName.remove(username);
    }
}
