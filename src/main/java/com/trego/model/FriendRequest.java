package com.trego.model;

import java.util.Map;

/**
 * A directional friend request. Stored in "social_friend_requests".
 *
 * <p>Requests are directional ({@code fromUid} → {@code toUid}); an accepted
 * request materializes a bidirectional {@link Friendship}. At most one request
 * with {@code status == STATUS_PENDING} should exist per ordered pair.
 */
public class FriendRequest extends BaseEntity {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_DECLINED = "declined";
    public static final String STATUS_CANCELLED = "cancelled";

    private String fromUid;
    private String toUid;
    private String status = STATUS_PENDING;
    private String message;

    public FriendRequest() {
        super();
    }

    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("fromUid", fromUid);
        map.put("toUid", toUid);
        map.put("status", status);
        map.put("message", message);
        return map;
    }

    public static FriendRequest fromFirestoreMap(Map<String, Object> map) {
        FriendRequest r = new FriendRequest();
        r.setId((String) map.get("id"));
        if (map.get("createdAt") != null) r.setCreatedAt(timestampToLocalDateTime(map.get("createdAt")));
        if (map.get("updatedAt") != null) r.setUpdatedAt(timestampToLocalDateTime(map.get("updatedAt")));
        r.fromUid = (String) map.get("fromUid");
        r.toUid = (String) map.get("toUid");
        r.status = map.get("status") != null ? (String) map.get("status") : STATUS_PENDING;
        r.message = (String) map.get("message");
        return r;
    }

    public String getFromUid() { return fromUid; }
    public void setFromUid(String fromUid) { this.fromUid = fromUid; }

    public String getToUid() { return toUid; }
    public void setToUid(String toUid) { this.toUid = toUid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
