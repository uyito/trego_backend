package com.trego.service;

import com.trego.model.FriendRequest;
import com.trego.model.Friendship;
import com.trego.repository.FriendGraphRepository;
import com.trego.repository.FriendGraphRepository.UserView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Friend-graph business logic. Requests are directional; an accepted request
 * materializes a bidirectional {@link Friendship}. Implements {@link
 * FriendshipLookup} so {@link SocialService} can gate friends-visibility posts.
 *
 * <p>Identifier resolution is email-only for now (case-insensitive, exact).
 * Username resolution is a documented follow-up (no username field on User yet).
 */
@Service
public class FriendService implements FriendshipLookup {

    private final FriendGraphRepository repo;

    public FriendService(FriendGraphRepository repo) {
        this.repo = repo;
    }

    /**
     * Send a friend request to the user resolved from [identifier] (email).
     * Returns {status, request?} where status is "friends" when a reverse pending
     * request was auto-accepted, otherwise "pending".
     */
    public Map<String, Object> sendRequest(String fromUid, String identifier, String message) {
        String toUid = repo.resolveUidByEmail(identifier)
                .orElseThrow(() -> new NoSuchElementException("No user found for that email"));

        if (toUid.equals(fromUid)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself");
        }
        if (areFriends(fromUid, toUid)) {
            throw new IllegalStateException("You are already friends");
        }
        if (repo.findPending(fromUid, toUid).isPresent()) {
            throw new IllegalStateException("A request to this user is already pending");
        }

        // Reverse pending request → auto-accept into a friendship.
        var reverse = repo.findPending(toUid, fromUid);
        if (reverse.isPresent()) {
            FriendRequest r = reverse.get();
            r.setStatus(FriendRequest.STATUS_ACCEPTED);
            repo.saveRequest(r);
            repo.saveFriendship(Friendship.of(fromUid, toUid));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "friends");
            return result;
        }

        FriendRequest req = new FriendRequest();
        req.setFromUid(fromUid);
        req.setToUid(toUid);
        req.setMessage(message);
        req.setStatus(FriendRequest.STATUS_PENDING);
        FriendRequest saved = repo.saveRequest(req);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "pending");
        result.put("request", toRequestView(saved, fromUid));
        return result;
    }

    /** Accept or decline a pending request. Only the recipient may respond. */
    public void respondToRequest(String viewerUid, String requestId, boolean accept) {
        FriendRequest r = repo.findRequest(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));
        if (!viewerUid.equals(r.getToUid())) {
            throw new SecurityException("Only the recipient can respond to this request");
        }
        if (!FriendRequest.STATUS_PENDING.equals(r.getStatus())) {
            throw new IllegalStateException("Request is no longer pending");
        }
        if (accept) {
            r.setStatus(FriendRequest.STATUS_ACCEPTED);
            repo.saveRequest(r);
            if (repo.findFriendship(Friendship.pairKeyFor(r.getFromUid(), r.getToUid())).isEmpty()) {
                repo.saveFriendship(Friendship.of(r.getFromUid(), r.getToUid()));
            }
        } else {
            r.setStatus(FriendRequest.STATUS_DECLINED);
            repo.saveRequest(r);
        }
    }

    /** Cancel an outgoing pending request. Only the sender may cancel. */
    public void cancelRequest(String viewerUid, String requestId) {
        FriendRequest r = repo.findRequest(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));
        if (!viewerUid.equals(r.getFromUid())) {
            throw new SecurityException("Only the sender can cancel this request");
        }
        r.setStatus(FriendRequest.STATUS_CANCELLED);
        repo.saveRequest(r);
    }

    public List<Map<String, Object>> getFriends(String uid) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Friendship f : repo.findFriendshipsFor(uid)) {
            String other = f.otherUser(uid);
            if (other == null) continue;
            UserView u = repo.getUserView(other).orElse(new UserView(other, null, null));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("uid", other);
            m.put("name", u.name);
            m.put("photoURL", u.photoUrl);
            out.add(m);
        }
        return out;
    }

    public List<Map<String, Object>> getRequests(String uid) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FriendRequest r : repo.findPendingInvolving(uid)) {
            out.add(toRequestView(r, uid));
        }
        return out;
    }

    /** Remove an existing friendship. No-op if not friends. */
    public void unfriend(String viewerUid, String friendUid) {
        repo.findFriendship(Friendship.pairKeyFor(viewerUid, friendUid))
                .ifPresent(f -> repo.deleteFriendship(f.getId()));
    }

    @Override
    public boolean areFriends(String a, String b) {
        if (a == null || b == null) return false;
        return repo.findFriendship(Friendship.pairKeyFor(a, b)).isPresent();
    }

    // --- helpers ---

    private Map<String, Object> toRequestView(FriendRequest r, String viewerUid) {
        boolean incoming = viewerUid.equals(r.getToUid());
        String otherUid = incoming ? r.getFromUid() : r.getToUid();
        UserView u = repo.getUserView(otherUid).orElse(new UserView(otherUid, null, null));

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("uid", otherUid);
        user.put("name", u.name);
        user.put("photoURL", u.photoUrl);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("type", incoming ? "incoming" : "outgoing");
        m.put("user", user);
        m.put("message", r.getMessage());
        m.put("createdAt", isoOrNull(r.getCreatedAt()));
        return m;
    }

    private static String isoOrNull(LocalDateTime t) {
        return t != null ? t.toString() : null;
    }
}
