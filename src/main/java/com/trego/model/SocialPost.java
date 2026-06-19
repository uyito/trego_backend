package com.trego.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A social feed post. Stored in the top-level "social_posts" collection.
 *
 * <p>{@code likedBy} holds the firebase UIDs that liked the post — likesCount is
 * derived from its size and per-viewer {@code userLiked} from membership, so the
 * like state stays consistent without a separate counter to keep in sync.
 */
public class SocialPost extends BaseEntity {

    private String authorId;
    private String authorName;
    private String authorPhotoUrl;
    private String content;
    private String type;        // general | workout | nutrition | achievement
    private String visibility;  // public | friends | private
    private List<String> attachments = new ArrayList<>();
    private List<String> likedBy = new ArrayList<>();
    private int commentsCount = 0;

    public SocialPost() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("authorId", authorId);
        map.put("authorName", authorName);
        map.put("authorPhotoUrl", authorPhotoUrl);
        map.put("content", content);
        map.put("type", type);
        map.put("visibility", visibility);
        map.put("attachments", attachments);
        map.put("likedBy", likedBy);
        map.put("commentsCount", commentsCount);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static SocialPost fromFirestoreMap(Map<String, Object> map) {
        SocialPost p = new SocialPost();
        p.setId((String) map.get("id"));
        if (map.get("createdAt") != null) p.setCreatedAt(timestampToLocalDateTime(map.get("createdAt")));
        if (map.get("updatedAt") != null) p.setUpdatedAt(timestampToLocalDateTime(map.get("updatedAt")));
        p.authorId = (String) map.get("authorId");
        p.authorName = (String) map.get("authorName");
        p.authorPhotoUrl = (String) map.get("authorPhotoUrl");
        p.content = (String) map.get("content");
        p.type = (String) map.get("type");
        p.visibility = (String) map.get("visibility");
        Object att = map.get("attachments");
        if (att instanceof List) p.attachments = new ArrayList<>((List<String>) att);
        Object liked = map.get("likedBy");
        if (liked instanceof List) p.likedBy = new ArrayList<>((List<String>) liked);
        Object cc = map.get("commentsCount");
        if (cc instanceof Number) p.commentsCount = ((Number) cc).intValue();
        return p;
    }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorPhotoUrl() { return authorPhotoUrl; }
    public void setAuthorPhotoUrl(String authorPhotoUrl) { this.authorPhotoUrl = authorPhotoUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    public List<String> getLikedBy() { return likedBy; }
    public void setLikedBy(List<String> likedBy) {
        this.likedBy = likedBy != null ? likedBy : new ArrayList<>();
    }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
}
