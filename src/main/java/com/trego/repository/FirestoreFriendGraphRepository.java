package com.trego.repository;

import com.trego.model.FriendRequest;
import com.trego.model.Friendship;
import com.trego.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore-backed {@link FriendGraphRepository}. Delegates to the friend-request
 * and friendship {@link FirestoreRepository} beans plus {@link UserRepository},
 * adapting checked Firestore exceptions to runtime ones (matching
 * {@code FirestoreSocialRepository}).
 *
 * <p>Composite predicates (e.g. pending request in a given direction) are done by
 * a single-field query + in-memory filter to avoid requiring Firestore composite
 * indexes.
 */
@Repository
@Primary
public class FirestoreFriendGraphRepository implements FriendGraphRepository {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreFriendGraphRepository.class);

    private final FriendRequestRepository requests;
    private final FriendshipRepository friendships;
    private final UserRepository users;

    @Autowired
    public FirestoreFriendGraphRepository(FriendRequestRepository requests,
                                          FriendshipRepository friendships,
                                          UserRepository users) {
        this.requests = requests;
        this.friendships = friendships;
        this.users = users;
    }

    @Override
    public FriendRequest saveRequest(FriendRequest request) {
        try {
            return request.getId() == null ? requests.save(request) : requests.update(request);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("saveRequest", e);
        }
    }

    @Override
    public Optional<FriendRequest> findRequest(String id) {
        try {
            return requests.findById(id);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findRequest", e);
        }
    }

    @Override
    public Optional<FriendRequest> findPending(String fromUid, String toUid) {
        try {
            for (FriendRequest r : requests.findByField("fromUid", fromUid)) {
                if (toUid.equals(r.getToUid()) && FriendRequest.STATUS_PENDING.equals(r.getStatus())) {
                    return Optional.of(r);
                }
            }
            return Optional.empty();
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findPending", e);
        }
    }

    @Override
    public List<FriendRequest> findPendingInvolving(String uid) {
        try {
            List<FriendRequest> out = new ArrayList<>();
            for (FriendRequest r : requests.findByField("toUid", uid)) {
                if (FriendRequest.STATUS_PENDING.equals(r.getStatus())) out.add(r);
            }
            for (FriendRequest r : requests.findByField("fromUid", uid)) {
                if (FriendRequest.STATUS_PENDING.equals(r.getStatus())) out.add(r);
            }
            return out;
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findPendingInvolving", e);
        }
    }

    @Override
    public Friendship saveFriendship(Friendship friendship) {
        try {
            return friendship.getId() == null ? friendships.save(friendship) : friendships.update(friendship);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("saveFriendship", e);
        }
    }

    @Override
    public Optional<Friendship> findFriendship(String pairKey) {
        try {
            List<Friendship> matches = friendships.findByField("pairKey", pairKey);
            return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findFriendship", e);
        }
    }

    @Override
    public List<Friendship> findFriendshipsFor(String uid) {
        try {
            return friendships.findByUser(uid);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findFriendshipsFor", e);
        }
    }

    @Override
    public void deleteFriendship(String id) {
        try {
            friendships.deleteById(id);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("deleteFriendship", e);
        }
    }

    @Override
    public Optional<String> resolveUidByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return Optional.empty();
        try {
            Optional<User> user = users.findByEmail(email.trim());
            if (user.isEmpty()) {
                user = users.findByEmail(email.trim().toLowerCase());
            }
            return user.map(User::getId);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("resolveUidByEmail", e);
        }
    }

    @Override
    public Optional<UserView> getUserView(String uid) {
        try {
            return users.findById(uid).map(u -> {
                String name = u.getFullName();
                if (name == null || name.trim().isEmpty() || name.trim().equals(",")) {
                    name = u.getEmail();
                }
                return new UserView(uid, name != null ? name.trim() : null, null);
            });
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("getUserView", e);
        }
    }

    private RuntimeException rethrow(String op, Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        logger.error("Friend graph repository {} failed: {}", op, e.getMessage());
        return new RuntimeException("Friend graph operation failed: " + op, e);
    }
}
