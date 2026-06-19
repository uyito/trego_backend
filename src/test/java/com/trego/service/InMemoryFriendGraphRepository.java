package com.trego.service;

import com.trego.model.FriendRequest;
import com.trego.model.Friendship;
import com.trego.repository.FriendGraphRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory {@link FriendGraphRepository} for unit tests. */
public class InMemoryFriendGraphRepository implements FriendGraphRepository {

    final List<FriendRequest> requests = new ArrayList<>();
    final List<Friendship> friendships = new ArrayList<>();
    final Map<String, String> emailToUid = new LinkedHashMap<>();   // lowercased email → uid
    final Map<String, UserView> usersByUid = new LinkedHashMap<>();

    /** Test helper: register a user so it can be resolved + rendered. */
    void addUser(String uid, String email, String name) {
        if (email != null) emailToUid.put(email.toLowerCase(), uid);
        usersByUid.put(uid, new UserView(uid, name, null));
    }

    @Override
    public FriendRequest saveRequest(FriendRequest request) {
        if (request.getId() == null) {
            request.setId(UUID.randomUUID().toString());
            requests.add(request);
        } else {
            for (int i = 0; i < requests.size(); i++) {
                if (requests.get(i).getId().equals(request.getId())) {
                    requests.set(i, request);
                    return request;
                }
            }
            requests.add(request);
        }
        return request;
    }

    @Override
    public Optional<FriendRequest> findRequest(String id) {
        return requests.stream().filter(r -> id.equals(r.getId())).findFirst();
    }

    @Override
    public Optional<FriendRequest> findPending(String fromUid, String toUid) {
        return requests.stream()
                .filter(r -> fromUid.equals(r.getFromUid())
                        && toUid.equals(r.getToUid())
                        && FriendRequest.STATUS_PENDING.equals(r.getStatus()))
                .findFirst();
    }

    @Override
    public List<FriendRequest> findPendingInvolving(String uid) {
        List<FriendRequest> out = new ArrayList<>();
        for (FriendRequest r : requests) {
            if (!FriendRequest.STATUS_PENDING.equals(r.getStatus())) continue;
            if (uid.equals(r.getFromUid()) || uid.equals(r.getToUid())) out.add(r);
        }
        return out;
    }

    @Override
    public Friendship saveFriendship(Friendship friendship) {
        if (friendship.getId() == null) {
            friendship.setId(UUID.randomUUID().toString());
        }
        friendships.add(friendship);
        return friendship;
    }

    @Override
    public Optional<Friendship> findFriendship(String pairKey) {
        return friendships.stream().filter(f -> pairKey.equals(f.getPairKey())).findFirst();
    }

    @Override
    public List<Friendship> findFriendshipsFor(String uid) {
        List<Friendship> out = new ArrayList<>();
        for (Friendship f : friendships) {
            if (f.getUsers().contains(uid)) out.add(f);
        }
        return out;
    }

    @Override
    public void deleteFriendship(String id) {
        friendships.removeIf(f -> id.equals(f.getId()));
    }

    @Override
    public Optional<String> resolveUidByEmail(String email) {
        if (email == null) return Optional.empty();
        return Optional.ofNullable(emailToUid.get(email.toLowerCase()));
    }

    @Override
    public Optional<UserView> getUserView(String uid) {
        return Optional.ofNullable(usersByUid.get(uid));
    }
}
