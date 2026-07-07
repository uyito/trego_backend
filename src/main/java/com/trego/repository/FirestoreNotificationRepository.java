package com.trego.repository;

import com.trego.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore-backed {@link NotificationRepository}. Delegates to the
 * {@link NotificationStore} bean and adapts checked Firestore exceptions to
 * runtime ones. Recipient queries use a single-field {@code whereEqualTo} +
 * in-memory sort/cap to avoid needing a composite index.
 */
@Repository
@Primary
public class FirestoreNotificationRepository implements NotificationRepository {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreNotificationRepository.class);

    private final NotificationStore store;

    @Autowired
    public FirestoreNotificationRepository(NotificationStore store) {
        this.store = store;
    }

    @Override
    public Notification save(Notification notification) {
        try {
            return notification.getId() == null ? store.save(notification) : store.update(notification);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("save", e);
        }
    }

    @Override
    public Optional<Notification> findById(String id) {
        try {
            return store.findById(id);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findById", e);
        }
    }

    @Override
    public List<Notification> findForRecipient(String recipientUid, int limit) {
        try {
            List<Notification> all = store.findByField("recipientUid", recipientUid);
            all.sort(Comparator.comparing(
                    Notification::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            if (limit > 0 && all.size() > limit) {
                return new ArrayList<>(all.subList(0, limit));
            }
            return all;
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("findForRecipient", e);
        }
    }

    @Override
    public int unreadCount(String recipientUid) {
        try {
            int count = 0;
            for (Notification n : store.findByField("recipientUid", recipientUid)) {
                if (!n.isRead()) count++;
            }
            return count;
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("unreadCount", e);
        }
    }

    @Override
    public void markAllRead(String recipientUid) {
        try {
            for (Notification n : store.findByField("recipientUid", recipientUid)) {
                if (!n.isRead()) {
                    n.setRead(true);
                    store.update(n);
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("markAllRead", e);
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            store.deleteById(id);
        } catch (ExecutionException | InterruptedException e) {
            throw rethrow("deleteById", e);
        }
    }

    private RuntimeException rethrow(String op, Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        logger.error("Notification repository {} failed: {}", op, e.getMessage());
        return new RuntimeException("Notification repository operation failed: " + op, e);
    }
}
