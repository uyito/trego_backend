package com.trego.repository;

import com.trego.model.Notification;

import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction for in-app notifications.
 * Production impl: {@link FirestoreNotificationRepository}.
 * Test impl: InMemoryNotificationRepository (test sources).
 */
public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(String id);

    /** Recipient's notifications, newest first, capped at [limit]. */
    List<Notification> findForRecipient(String recipientUid, int limit);

    /** Count of unread notifications for the recipient. */
    int unreadCount(String recipientUid);

    /** Mark all of the recipient's unread notifications as read. */
    void markAllRead(String recipientUid);

    void deleteById(String id);
}
