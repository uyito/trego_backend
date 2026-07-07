package com.trego.model;

import java.util.Map;

/**
 * An in-app notification for a single recipient. Stored in "social_notifications".
 *
 * <p>Actor display fields ({@code actorName}/{@code actorPhotoUrl}) and {@code
 * message} are denormalized at write time so the feed renders without joins.
 * {@code targetType}/{@code targetId} identify the subject for future deep-links.
 */
public class Notification extends BaseEntity {

    public static final String TYPE_POST_LIKE = "post_like";
    public static final String TYPE_POST_COMMENT = "post_comment";
    public static final String TYPE_FRIEND_REQUEST = "friend_request";
    public static final String TYPE_FRIEND_ACCEPT = "friend_accept";

    private String recipientUid;
    private String type;
    private String actorUid;
    private String actorName;
    private String actorPhotoUrl;
    private String targetType;
    private String targetId;
    private String message;
    private boolean read = false;

    public Notification() {
        super();
    }

    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("recipientUid", recipientUid);
        map.put("type", type);
        map.put("actorUid", actorUid);
        map.put("actorName", actorName);
        map.put("actorPhotoUrl", actorPhotoUrl);
        map.put("targetType", targetType);
        map.put("targetId", targetId);
        map.put("message", message);
        map.put("read", read);
        return map;
    }

    public static Notification fromFirestoreMap(Map<String, Object> map) {
        Notification n = new Notification();
        n.setId((String) map.get("id"));
        if (map.get("createdAt") != null) n.setCreatedAt(timestampToLocalDateTime(map.get("createdAt")));
        if (map.get("updatedAt") != null) n.setUpdatedAt(timestampToLocalDateTime(map.get("updatedAt")));
        n.recipientUid = (String) map.get("recipientUid");
        n.type = (String) map.get("type");
        n.actorUid = (String) map.get("actorUid");
        n.actorName = (String) map.get("actorName");
        n.actorPhotoUrl = (String) map.get("actorPhotoUrl");
        n.targetType = (String) map.get("targetType");
        n.targetId = (String) map.get("targetId");
        n.message = (String) map.get("message");
        Object r = map.get("read");
        n.read = r instanceof Boolean ? (Boolean) r : false;
        return n;
    }

    public String getRecipientUid() { return recipientUid; }
    public void setRecipientUid(String recipientUid) { this.recipientUid = recipientUid; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getActorUid() { return actorUid; }
    public void setActorUid(String actorUid) { this.actorUid = actorUid; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActorPhotoUrl() { return actorPhotoUrl; }
    public void setActorPhotoUrl(String actorPhotoUrl) { this.actorPhotoUrl = actorPhotoUrl; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
