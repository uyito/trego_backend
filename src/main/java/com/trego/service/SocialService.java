package com.trego.service;

import com.trego.model.Comment;
import com.trego.model.PostReport;
import com.trego.model.SocialPost;
import com.trego.repository.SocialRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Social feed business logic. Builds per-viewer view-models (flat maps with a
 * nested {@code author} object plus {@code userLiked}/{@code isOwn} flags) that
 * match exactly what the Flutter {@code SocialService} + feed screen read off the
 * response — so the existing create/feed/like/comment calls and the new
 * comments/report/edit/delete calls share one response shape.
 *
 * <p><b>Feed visibility (MVP):</b> a viewer sees every {@code public} post plus
 * all of their own posts (any visibility). {@code friends}-scoped posts are not
 * yet fanned out to friends because there is no friend graph in the backend; that
 * filtering is the documented follow-up once friends are persisted.
 */
@Service
public class SocialService {

    private final SocialRepository repo;
    private final FriendshipLookup friends;

    /**
     * Spring constructor — friends-visibility enabled via the injected
     * {@link FriendshipLookup} (FriendService).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public SocialService(SocialRepository repo, FriendshipLookup friends) {
        this.repo = repo;
        this.friends = friends;
    }

    /**
     * Convenience constructor with friends-visibility disabled (friends-scoped
     * posts are author-only). Used by tests that don't exercise the friend graph.
     */
    public SocialService(SocialRepository repo) {
        this(repo, (a, b) -> false);
    }

    public List<Map<String, Object>> getFeed(String viewerUid, int limit, int offset) {
        List<SocialPost> visible = new ArrayList<>();
        for (SocialPost p : repo.findAllPostsNewestFirst()) {
            if (isVisibleTo(p, viewerUid)) {
                visible.add(p);
            }
        }
        int from = Math.max(0, offset);
        if (from >= visible.size()) return new ArrayList<>();
        int to = Math.min(visible.size(), from + Math.max(0, limit));
        List<Map<String, Object>> out = new ArrayList<>();
        for (SocialPost p : visible.subList(from, to)) {
            out.add(toPostView(p, viewerUid));
        }
        return out;
    }

    public Map<String, Object> createPost(String authorUid, String authorName, String authorPhotoUrl,
                                          String content, String type, List<String> attachments,
                                          String visibility) {
        SocialPost p = new SocialPost();
        p.setAuthorId(authorUid);
        p.setAuthorName(authorName);
        p.setAuthorPhotoUrl(authorPhotoUrl);
        p.setContent(content);
        p.setType(type != null ? type : "general");
        p.setVisibility(visibility != null ? visibility : "friends");
        p.setAttachments(attachments);
        SocialPost saved = repo.savePost(p);
        return toPostView(saved, authorUid);
    }

    /** Toggle the viewer's like. Returns {userLiked, likesCount}. */
    public Map<String, Object> toggleLike(String viewerUid, String postId) {
        SocialPost p = requirePost(postId);
        boolean nowLiked;
        if (p.getLikedBy().contains(viewerUid)) {
            p.getLikedBy().remove(viewerUid);
            nowLiked = false;
        } else {
            p.getLikedBy().add(viewerUid);
            nowLiked = true;
        }
        repo.savePost(p);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("userLiked", nowLiked);
        r.put("likesCount", p.getLikedBy().size());
        return r;
    }

    /** Add a comment, bump the post's comment counter. Returns {comment, commentsCount}. */
    public Map<String, Object> addComment(String authorUid, String authorName, String authorPhotoUrl,
                                          String postId, String content) {
        SocialPost p = requirePost(postId);
        Comment c = new Comment();
        c.setPostId(postId);
        c.setAuthorId(authorUid);
        c.setAuthorName(authorName);
        c.setAuthorPhotoUrl(authorPhotoUrl);
        c.setContent(content);
        Comment saved = repo.saveComment(c);
        p.setCommentsCount(p.getCommentsCount() + 1);
        repo.savePost(p);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("comment", toCommentView(saved));
        r.put("commentsCount", p.getCommentsCount());
        return r;
    }

    public List<Map<String, Object>> getComments(String postId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Comment c : repo.findCommentsByPost(postId)) {
            out.add(toCommentView(c));
        }
        return out;
    }

    public void report(String reporterUid, String postId, String reason) {
        PostReport r = new PostReport();
        r.setPostId(postId);
        r.setReporterId(reporterUid);
        r.setReason(reason != null ? reason : "unspecified");
        repo.saveReport(r);
    }

    /** Edit a post's content. Only the author may edit. */
    public Map<String, Object> updatePost(String viewerUid, String postId, String content) {
        SocialPost p = requirePost(postId);
        requireOwner(p, viewerUid);
        p.setContent(content);
        SocialPost saved = repo.savePost(p);
        return toPostView(saved, viewerUid);
    }

    /** Delete a post and its comments. Only the author may delete. */
    public void deletePost(String viewerUid, String postId) {
        SocialPost p = requirePost(postId);
        requireOwner(p, viewerUid);
        repo.deleteCommentsByPost(postId);
        repo.deletePost(postId);
    }

    // --- helpers ---

    private boolean isVisibleTo(SocialPost p, String viewerUid) {
        if (viewerUid != null && viewerUid.equals(p.getAuthorId())) return true;
        if ("public".equals(p.getVisibility())) return true;
        if ("friends".equals(p.getVisibility())) {
            return friends.areFriends(viewerUid, p.getAuthorId());
        }
        return false; // "private" — author-only (handled above)
    }

    private SocialPost requirePost(String postId) {
        return repo.findPost(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));
    }

    private void requireOwner(SocialPost p, String viewerUid) {
        if (viewerUid == null || !viewerUid.equals(p.getAuthorId())) {
            throw new SecurityException("Not the author of post: " + p.getId());
        }
    }

    private Map<String, Object> toPostView(SocialPost p, String viewerUid) {
        Map<String, Object> author = new LinkedHashMap<>();
        author.put("id", p.getAuthorId());
        author.put("name", p.getAuthorName());
        author.put("photoURL", p.getAuthorPhotoUrl());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("content", p.getContent());
        m.put("type", p.getType());
        m.put("visibility", p.getVisibility());
        m.put("author", author);
        m.put("attachments", p.getAttachments());
        m.put("likesCount", p.getLikedBy().size());
        m.put("commentsCount", p.getCommentsCount());
        m.put("userLiked", viewerUid != null && p.getLikedBy().contains(viewerUid));
        m.put("isOwn", viewerUid != null && viewerUid.equals(p.getAuthorId()));
        m.put("createdAt", isoOrNull(p.getCreatedAt()));
        return m;
    }

    private Map<String, Object> toCommentView(Comment c) {
        Map<String, Object> author = new LinkedHashMap<>();
        author.put("id", c.getAuthorId());
        author.put("name", c.getAuthorName());
        author.put("photoURL", c.getAuthorPhotoUrl());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("postId", c.getPostId());
        m.put("content", c.getContent());
        m.put("author", author);
        m.put("createdAt", isoOrNull(c.getCreatedAt()));
        return m;
    }

    private static String isoOrNull(LocalDateTime t) {
        return t != null ? t.toString() : null;
    }
}
