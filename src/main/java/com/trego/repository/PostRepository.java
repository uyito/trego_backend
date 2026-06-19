package com.trego.repository;

import com.trego.model.SocialPost;
import org.springframework.stereotype.Repository;

@Repository
public class PostRepository extends FirestoreRepository<SocialPost> {
    public PostRepository() {
        super("social_posts", SocialPost::fromFirestoreMap);
    }
}
