package com.trego.model;

import java.util.Map;

/**
 * A registered FCM device token. Stored in "device_tokens" keyed by the token
 * itself (globally unique) — re-registering a device that switched accounts
 * simply reassigns its {@code uid}. Used to fan out push notifications to all of
 * a user's devices.
 */
public class DeviceToken extends BaseEntity {

    private String token;
    private String uid;
    private String platform; // android | ios

    public DeviceToken() {
        super();
    }

    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("token", token);
        map.put("uid", uid);
        map.put("platform", platform);
        return map;
    }

    public static DeviceToken fromFirestoreMap(Map<String, Object> map) {
        DeviceToken t = new DeviceToken();
        t.setId((String) map.get("id"));
        if (map.get("createdAt") != null) t.setCreatedAt(timestampToLocalDateTime(map.get("createdAt")));
        if (map.get("updatedAt") != null) t.setUpdatedAt(timestampToLocalDateTime(map.get("updatedAt")));
        t.token = (String) map.get("token");
        t.uid = (String) map.get("uid");
        t.platform = (String) map.get("platform");
        return t;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}
