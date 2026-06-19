package com.trego.model;

import java.util.Map;

/**
 * A user's report of a {@link SocialPost}. Stored in the "social_reports"
 * collection for asynchronous moderation review — the report endpoint is
 * fire-and-forget from the client's perspective.
 */
public class PostReport extends BaseEntity {

    private String postId;
    private String reporterId;
    private String reason;

    public PostReport() {
        super();
    }

    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("postId", postId);
        map.put("reporterId", reporterId);
        map.put("reason", reason);
        return map;
    }

    public static PostReport fromFirestoreMap(Map<String, Object> map) {
        PostReport r = new PostReport();
        r.setId((String) map.get("id"));
        if (map.get("createdAt") != null) r.setCreatedAt(timestampToLocalDateTime(map.get("createdAt")));
        if (map.get("updatedAt") != null) r.setUpdatedAt(timestampToLocalDateTime(map.get("updatedAt")));
        r.postId = (String) map.get("postId");
        r.reporterId = (String) map.get("reporterId");
        r.reason = (String) map.get("reason");
        return r;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
