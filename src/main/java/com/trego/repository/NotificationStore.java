package com.trego.repository;

import com.trego.model.Notification;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationStore extends FirestoreRepository<Notification> {
    public NotificationStore() {
        super("social_notifications", Notification::fromFirestoreMap);
    }
}
