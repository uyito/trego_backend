package com.trego.repository;

import com.trego.model.Comment;
import com.trego.model.PostReport;
import com.trego.model.SocialPost;

import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction for the social feed.
 * Production impl: {@link FirestoreSocialRepository}.
 * Test impl: InMemorySocialRepository (test sources).
 */
public interface SocialRepository {

    SocialPost savePost(SocialPost post);

    Optional<SocialPost> findPost(String id);

    /** All posts, most-recent first. Visibility filtering happens in the service. */
    List<SocialPost> findAllPostsNewestFirst();

    void deletePost(String id);

    Comment saveComment(Comment comment);

    /** Comments for a post, oldest first. */
    List<Comment> findCommentsByPost(String postId);

    void deleteCommentsByPost(String postId);

    PostReport saveReport(PostReport report);
}
