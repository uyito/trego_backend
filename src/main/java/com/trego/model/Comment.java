package com.trego.model;

import java.util.Map;

/** A comment on a {@link SocialPost}. Stored in the "social_comments" collection. */
public class Comment extends BaseEntity {

    private String postId;
    private String authorId;
    private String authorName;
    private String authorPhotoUrl;
    private String content;

    public Comment() {
        super();
    }

    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("postId", postId);
        map.put("authorId", authorId);
        map.put("authorName", authorName);
        map.put("authorPhotoUrl", authorPhotoUrl);
        map.put("content", content);
        return map;
    }

    public static Comment fromFirestoreMap(Map<String, Object> map) {
        Comment c = new Comment();
        c.setId((String) map.get("id"));
        if (map.get("createdAt") != null) c.setCreatedAt(timestampToLocalDateTime(map.get("createdAt")));
        if (map.get("updatedAt") != null) c.setUpdatedAt(timestampToLocalDateTime(map.get("updatedAt")));
        c.postId = (String) map.get("postId");
        c.authorId = (String) map.get("authorId");
        c.authorName = (String) map.get("authorName");
        c.authorPhotoUrl = (String) map.get("authorPhotoUrl");
        c.content = (String) map.get("content");
        return c;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorPhotoUrl() { return authorPhotoUrl; }
    public void setAuthorPhotoUrl(String authorPhotoUrl) { this.authorPhotoUrl = authorPhotoUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
