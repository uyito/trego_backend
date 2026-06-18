package com.trego.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An accepted, bidirectional friendship. Stored in "social_friendships", one doc
 * per pair. {@code users} holds both UIDs sorted ascending; {@code pairKey} is the
 * sorted pair joined with "_", giving an O(1) "are these two friends?" lookup by
 * document field without scanning request history.
 */
public class Friendship extends BaseEntity {

    private List<String> users = new ArrayList<>();
    private String pairKey;

    public Friendship() {
        super();
    }

    /** Deterministic key for an unordered pair: sorted UIDs joined with "_". */
    public static String pairKeyFor(String a, String b) {
        String[] pair = {a, b};
        Arrays.sort(pair);
        return pair[0] + "_" + pair[1];
    }

    public static Friendship of(String a, String b) {
        String[] pair = {a, b};
        Arrays.sort(pair);
        Friendship f = new Friendship();
        f.users = new ArrayList<>(Arrays.asList(pair));
        f.pairKey = pair[0] + "_" + pair[1];
        return f;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("users", users);
        map.put("pairKey", pairKey);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Friendship fromFirestoreMap(Map<String, Object> map) {
        Friendship f = new Friendship();
        f.setId((String) map.get("id"));
        if (map.get("createdAt") != null) f.setCreatedAt(timestampToLocalDateTime(map.get("createdAt")));
        if (map.get("updatedAt") != null) f.setUpdatedAt(timestampToLocalDateTime(map.get("updatedAt")));
        Object u = map.get("users");
        if (u instanceof List) f.users = new ArrayList<>((List<String>) u);
        f.pairKey = (String) map.get("pairKey");
        return f;
    }

    /** The other member of this friendship relative to [uid], or null. */
    public String otherUser(String uid) {
        for (String u : users) {
            if (!u.equals(uid)) return u;
        }
        return null;
    }

    public List<String> getUsers() { return users; }
    public void setUsers(List<String> users) {
        this.users = users != null ? users : new ArrayList<>();
    }

    public String getPairKey() { return pairKey; }
    public void setPairKey(String pairKey) { this.pairKey = pairKey; }
}
