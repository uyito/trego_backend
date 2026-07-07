package com.trego.service;

import com.trego.model.Notification;
import com.trego.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** In-memory {@link NotificationRepository} for unit tests. Newest-first by insertion. */
public class InMemoryNotificationRepository implements NotificationRepository {

    final List<Notification> items = new ArrayList<>();   // oldest-first insertion

    @Override
    public Notification save(Notification n) {
        if (n.getId() == null) {
            n.setId(UUID.randomUUID().toString());
            items.add(n);
        } else {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(n.getId())) {
                    items.set(i, n);
                    return n;
                }
            }
            items.add(n);
        }
        return n;
    }

    @Override
    public Optional<Notification> findById(String id) {
        return items.stream().filter(n -> id.equals(n.getId())).findFirst();
    }

    @Override
    public List<Notification> findForRecipient(String recipientUid, int limit) {
        List<Notification> out = new ArrayList<>();
        for (int i = items.size() - 1; i >= 0; i--) {   // newest first
            Notification n = items.get(i);
            if (recipientUid.equals(n.getRecipientUid())) out.add(n);
        }
        if (limit > 0 && out.size() > limit) return new ArrayList<>(out.subList(0, limit));
        return out;
    }

    @Override
    public int unreadCount(String recipientUid) {
        int c = 0;
        for (Notification n : items) {
            if (recipientUid.equals(n.getRecipientUid()) && !n.isRead()) c++;
        }
        return c;
    }

    @Override
    public void markAllRead(String recipientUid) {
        for (Notification n : items) {
            if (recipientUid.equals(n.getRecipientUid())) n.setRead(true);
        }
    }

    @Override
    public void deleteById(String id) {
        items.removeIf(n -> id.equals(n.getId()));
    }
}
