package com.trego.service;

import com.trego.model.Comment;
import com.trego.model.PostReport;
import com.trego.model.SocialPost;
import com.trego.repository.SocialRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory {@link SocialRepository} for unit tests. Posts are returned
 * newest-first by insertion order (deterministic, independent of wall-clock).
 */
public class InMemorySocialRepository implements SocialRepository {

    final List<SocialPost> posts = new ArrayList<>();   // oldest-first insertion order
    final List<Comment> comments = new ArrayList<>();
    final List<PostReport> reports = new ArrayList<>();

    @Override
    public SocialPost savePost(SocialPost post) {
        if (post.getId() == null) {
            post.setId(UUID.randomUUID().toString());
            posts.add(post);
        } else {
            replace(posts, post, post.getId());
            if (posts.stream().noneMatch(p -> p.getId().equals(post.getId()))) {
                posts.add(post);
            }
        }
        return post;
    }

    @Override
    public Optional<SocialPost> findPost(String id) {
        return posts.stream().filter(p -> id.equals(p.getId())).findFirst();
    }

    @Override
    public List<SocialPost> findAllPostsNewestFirst() {
        List<SocialPost> copy = new ArrayList<>(posts);
        java.util.Collections.reverse(copy);
        return copy;
    }

    @Override
    public void deletePost(String id) {
        posts.removeIf(p -> id.equals(p.getId()));
    }

    @Override
    public Comment saveComment(Comment comment) {
        if (comment.getId() == null) {
            comment.setId(UUID.randomUUID().toString());
        }
        comments.add(comment);
        return comment;
    }

    @Override
    public List<Comment> findCommentsByPost(String postId) {
        List<Comment> out = new ArrayList<>();
        for (Comment c : comments) {
            if (postId.equals(c.getPostId())) out.add(c);
        }
        return out;
    }

    @Override
    public void deleteCommentsByPost(String postId) {
        comments.removeIf(c -> postId.equals(c.getPostId()));
    }

    @Override
    public PostReport saveReport(PostReport report) {
        if (report.getId() == null) {
            report.setId(UUID.randomUUID().toString());
        }
        reports.add(report);
        return report;
    }

    private static void replace(List<SocialPost> list, SocialPost post, String id) {
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).getId())) {
                list.set(i, post);
                return;
            }
        }
    }
}
