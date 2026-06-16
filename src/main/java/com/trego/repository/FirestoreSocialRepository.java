package com.trego.repository;

import com.trego.model.Comment;
import com.trego.model.PostReport;
import com.trego.model.SocialPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore-backed {@link SocialRepository}. Delegates to the three Spring-managed
 * {@link FirestoreRepository} beans (posts, comments, reports) and adapts checked
 * Firestore exceptions into runtime exceptions so the service layer stays clean —
 * mirroring how the metrics repository shields the service from
 * {@code ExecutionException}.
 */
@Repository
@Primary
public class FirestoreSocialRepository implements SocialRepository {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreSocialRepository.class);

    private final PostRepository posts;
    private final CommentRepository comments;
    private final ReportRepository reports;

    @Autowired
    public FirestoreSocialRepository(PostRepository posts,
                                     CommentRepository comments,
                                     ReportRepository reports) {
        this.posts = posts;
        this.comments = comments;
        this.reports = reports;
    }

    @Override
    public SocialPost savePost(SocialPost post) {
        try {
            return post.getId() == null ? posts.save(post) : posts.update(post);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("savePost", e);
        }
    }

    @Override
    public Optional<SocialPost> findPost(String id) {
        try {
            return posts.findById(id);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findPost", e);
        }
    }

    @Override
    public List<SocialPost> findAllPostsNewestFirst() {
        try {
            List<SocialPost> all = posts.findAll();
            all.sort(Comparator.comparing(
                    SocialPost::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return all;
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findAllPostsNewestFirst", e);
        }
    }

    @Override
    public void deletePost(String id) {
        try {
            posts.deleteById(id);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("deletePost", e);
        }
    }

    @Override
    public Comment saveComment(Comment comment) {
        try {
            return comment.getId() == null ? comments.save(comment) : comments.update(comment);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("saveComment", e);
        }
    }

    @Override
    public List<Comment> findCommentsByPost(String postId) {
        try {
            List<Comment> list = comments.findByField("postId", postId);
            list.sort(Comparator.comparing(
                    Comment::getCreatedAt,
                    Comparator.nullsFirst(Comparator.naturalOrder())));
            return list;
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findCommentsByPost", e);
        }
    }

    @Override
    public void deleteCommentsByPost(String postId) {
        try {
            for (Comment c : comments.findByField("postId", postId)) {
                comments.deleteById(c.getId());
            }
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("deleteCommentsByPost", e);
        }
    }

    @Override
    public PostReport saveReport(PostReport report) {
        try {
            return reports.save(report);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("saveReport", e);
        }
    }

    private RuntimeException rethrow(String op, Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        logger.error("Social repository {} failed: {}", op, e.getMessage());
        return new RuntimeException("Social repository operation failed: " + op, e);
    }
}
