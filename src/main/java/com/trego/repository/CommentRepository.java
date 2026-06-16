package com.trego.repository;

import com.trego.model.Comment;
import org.springframework.stereotype.Repository;

@Repository
public class CommentRepository extends FirestoreRepository<Comment> {
    public CommentRepository() {
        super("social_comments", Comment::fromFirestoreMap);
    }
}
