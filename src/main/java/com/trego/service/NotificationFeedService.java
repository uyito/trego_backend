package com.trego.service;

import com.trego.model.Notification;
import com.trego.repository.NotificationRepository;
import com.trego.service.ActorDirectory.ActorInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-app notification feed: creation (via {@link NotificationEmitter}) plus
 * read/list/count operations. Actor display + message are denormalized at write
 * time so the feed renders without joins. Distinct from the email-only
 * {@code NotificationService}.
 */
@Service
public class NotificationFeedService implements NotificationEmitter {

    public static final int DEFAULT_LIMIT = 50;

    private final NotificationRepository repo;
    private final ActorDirectory actors;

    public NotificationFeedService(NotificationRepository repo, ActorDirectory actors) {
        this.repo = repo;
        this.actors = actors;
    }

    @Override
    public void emit(String recipientUid, String type, String actorUid, String targetType, String targetId) {
        // Never notify a user about their own action.
        if (recipientUid == null || actorUid == null || recipientUid.equals(actorUid)) {
            return;
        }
        ActorInfo actor = actors.lookup(actorUid).orElse(new ActorInfo(actorUid, null, null));

        Notification n = new Notification();
        n.setRecipientUid(recipientUid);
        n.setType(type);
        n.setActorUid(actorUid);
        n.setActorName(actor.name);
        n.setActorPhotoUrl(actor.photoUrl);
        n.setTargetType(targetType);
        n.setTargetId(targetId);
        n.setMessage(buildMessage(type, actor.name));
        n.setRead(false);
        repo.save(n);
    }

    public Map<String, Object> list(String uid, int limit) {
        int cap = limit > 0 ? limit : DEFAULT_LIMIT;
        List<Map<String, Object>> items = new ArrayList<>();
        int unread = 0;
        for (Notification n : repo.findForRecipient(uid, cap)) {
            items.add(toView(n));
            if (!n.isRead()) unread++;
        }
        // unreadCount reflects the full unread set, not just this page.
        int unreadTotal = repo.unreadCount(uid);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("notifications", items);
        out.put("unreadCount", unreadTotal >= unread ? unreadTotal : unread);
        return out;
    }

    public int unreadCount(String uid) {
        return repo.unreadCount(uid);
    }

    /** Mark a single notification read. Only the recipient may. */
    public void markRead(String uid, String notificationId) {
        Notification n = repo.findById(notificationId).orElse(null);
        if (n == null || !uid.equals(n.getRecipientUid())) return;
        if (!n.isRead()) {
            n.setRead(true);
            repo.save(n);
        }
    }

    public void markAllRead(String uid) {
        repo.markAllRead(uid);
    }

    /** Delete a notification. Only the recipient may. */
    public void delete(String uid, String notificationId) {
        Notification n = repo.findById(notificationId).orElse(null);
        if (n == null || !uid.equals(n.getRecipientUid())) return;
        repo.deleteById(notificationId);
    }

    // --- helpers ---

    private static String buildMessage(String type, String actorName) {
        String who = (actorName != null && !actorName.isEmpty()) ? actorName : "Someone";
        switch (type) {
            case Notification.TYPE_POST_LIKE:
                return who + " liked your post";
            case Notification.TYPE_POST_COMMENT:
                return who + " commented on your post";
            case Notification.TYPE_FRIEND_REQUEST:
                return who + " sent you a friend request";
            case Notification.TYPE_FRIEND_ACCEPT:
                return who + " accepted your friend request";
            default:
                return who + " sent you a notification";
        }
    }

    private static Map<String, Object> toView(Notification n) {
        Map<String, Object> actor = new LinkedHashMap<>();
        actor.put("uid", n.getActorUid());
        actor.put("name", n.getActorName());
        actor.put("photoURL", n.getActorPhotoUrl());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("type", n.getType());
        m.put("actor", actor);
        m.put("targetType", n.getTargetType());
        m.put("targetId", n.getTargetId());
        m.put("message", n.getMessage());
        m.put("read", n.isRead());
        m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        return m;
    }
}
