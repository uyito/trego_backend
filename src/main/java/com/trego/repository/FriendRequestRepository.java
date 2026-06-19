package com.trego.repository;

import com.trego.model.FriendRequest;
import org.springframework.stereotype.Repository;

@Repository
public class FriendRequestRepository extends FirestoreRepository<FriendRequest> {
    public FriendRequestRepository() {
        super("social_friend_requests", FriendRequest::fromFirestoreMap);
    }
}
