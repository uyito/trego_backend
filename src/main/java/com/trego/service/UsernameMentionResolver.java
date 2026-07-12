package com.trego.service;

import com.trego.repository.UsernameDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Production {@link MentionResolver} backed by the username uniqueness index. */
@Component
public class UsernameMentionResolver implements MentionResolver {

    private final UsernameDirectory usernames;

    @Autowired
    public UsernameMentionResolver(UsernameDirectory usernames) {
        this.usernames = usernames;
    }

    @Override
    public Optional<String> resolveUid(String username) {
        if (username == null) return Optional.empty();
        return usernames.resolveUid(username.toLowerCase());
    }
}
