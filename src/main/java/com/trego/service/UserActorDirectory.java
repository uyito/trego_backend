package com.trego.service;

import com.trego.model.User;
import com.trego.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Production {@link ActorDirectory} backed by {@link UserRepository}. */
@Component
public class UserActorDirectory implements ActorDirectory {

    private static final Logger logger = LoggerFactory.getLogger(UserActorDirectory.class);

    private final UserRepository users;

    @Autowired
    public UserActorDirectory(UserRepository users) {
        this.users = users;
    }

    @Override
    public Optional<ActorInfo> lookup(String uid) {
        if (uid == null) return Optional.empty();
        try {
            return users.findById(uid).map(this::toInfo);
        } catch (Exception e) {
            logger.warn("Actor lookup failed for {}: {}", uid, e.getMessage());
            return Optional.empty();
        }
    }

    private ActorInfo toInfo(User u) {
        String name = u.getFullName();
        if (name == null || name.trim().isEmpty() || name.trim().equals(",")) {
            name = u.getEmail();
        }
        return new ActorInfo(u.getId(), name != null ? name.trim() : null, null);
    }
}
