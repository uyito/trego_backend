package com.trego.service;

import com.trego.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NotificationFeedServiceTest {

    static final String ALICE = "alice-uid";
    static final String BOB = "bob-uid";

    InMemoryNotificationRepository repo;
    NotificationFeedService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryNotificationRepository();
        // Actor directory returns a fixed display name per uid.
        ActorDirectory actors = uid -> Optional.of(
                new ActorDirectory.ActorInfo(uid, uid.equals(BOB) ? "Bob B" : "Alice A", null));
        service = new NotificationFeedService(repo, actors);
    }

    @Test
    void emitAlsoPushesWithMessageAndDeepLinkData() {
        // A recording push sender captures what would be pushed.
        final java.util.List<Object[]> pushes = new java.util.ArrayList<>();
        ActorDirectory actors = uid -> Optional.of(new ActorDirectory.ActorInfo(uid, "Bob B", null));
        NotificationFeedService svc = new NotificationFeedService(repo, actors,
                (recipientUid, title, body, data) -> pushes.add(new Object[]{recipientUid, title, body, data}));

        svc.emit(ALICE, Notification.TYPE_MENTION, BOB, "post", "p7");

        assertEquals(1, pushes.size());
        assertEquals(ALICE, pushes.get(0)[0]);
        assertEquals("Bob B", pushes.get(0)[1]);
        assertEquals("Bob B mentioned you", pushes.get(0)[2]);
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> data = (java.util.Map<String, String>) pushes.get(0)[3];
        assertEquals("mention", data.get("type"));
        assertEquals("post", data.get("targetType"));
        assertEquals("p7", data.get("targetId"));
    }

    @Test
    void selfActionDoesNotPush() {
        final java.util.List<Object[]> pushes = new java.util.ArrayList<>();
        ActorDirectory actors = uid -> Optional.of(new ActorDirectory.ActorInfo(uid, "A", null));
        NotificationFeedService svc = new NotificationFeedService(repo, actors,
                (r, t, b, d) -> pushes.add(new Object[]{r}));

        svc.emit(ALICE, Notification.TYPE_POST_LIKE, ALICE, "post", "p1");

        assertTrue(pushes.isEmpty());
    }

    @Test
    void emitCreatesNotificationWithDenormalizedActorAndMessage() {
        service.emit(ALICE, Notification.TYPE_POST_LIKE, BOB, "post", "p1");

        assertEquals(1, repo.items.size());
        Notification n = repo.items.get(0);
        assertEquals(ALICE, n.getRecipientUid());
        assertEquals("Bob B", n.getActorName());
        assertEquals("Bob B liked your post", n.getMessage());
        assertFalse(n.isRead());
    }

    @Test
    void emitIsNoOpForSelfAction() {
        service.emit(ALICE, Notification.TYPE_POST_LIKE, ALICE, "post", "p1");
        assertTrue(repo.items.isEmpty());
    }

    @Test
    void emitIsNoOpForNullRecipient() {
        service.emit(null, Notification.TYPE_POST_LIKE, BOB, "post", "p1");
        assertTrue(repo.items.isEmpty());
    }

    @Test
    void messagesPerType() {
        service.emit(ALICE, Notification.TYPE_POST_COMMENT, BOB, "post", "p1");
        service.emit(ALICE, Notification.TYPE_FRIEND_REQUEST, BOB, "friend_request", "r1");
        service.emit(ALICE, Notification.TYPE_FRIEND_ACCEPT, BOB, "friendship", null);
        service.emit(ALICE, Notification.TYPE_MENTION, BOB, "post", "p1");

        List<String> messages = repo.items.stream().map(Notification::getMessage).toList();
        assertTrue(messages.contains("Bob B commented on your post"));
        assertTrue(messages.contains("Bob B sent you a friend request"));
        assertTrue(messages.contains("Bob B accepted your friend request"));
        assertTrue(messages.contains("Bob B mentioned you"));
    }

    @Test
    void listReturnsNewestFirstWithUnreadCount() {
        service.emit(ALICE, Notification.TYPE_POST_LIKE, BOB, "post", "p1");
        service.emit(ALICE, Notification.TYPE_POST_COMMENT, BOB, "post", "p2");

        Map<String, Object> result = service.list(ALICE, 50);
        assertEquals(2, result.get("unreadCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("notifications");
        assertEquals(2, items.size());
        // Newest first: the comment was emitted last.
        assertEquals("Bob B commented on your post", items.get(0).get("message"));
    }

    @Test
    void listIsScopedToRecipient() {
        service.emit(ALICE, Notification.TYPE_POST_LIKE, BOB, "post", "p1");
        service.emit(BOB, Notification.TYPE_POST_LIKE, ALICE, "post", "p2");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aliceItems =
                (List<Map<String, Object>>) service.list(ALICE, 50).get("notifications");
        assertEquals(1, aliceItems.size());
    }

    @Test
    void markReadFlipsAndOnlyForRecipient() {
        service.emit(ALICE, Notification.TYPE_POST_LIKE, BOB, "post", "p1");
        String id = repo.items.get(0).getId();

        // A non-recipient can't mark it read.
        service.markRead(BOB, id);
        assertEquals(1, service.unreadCount(ALICE));

        service.markRead(ALICE, id);
        assertEquals(0, service.unreadCount(ALICE));
    }

    @Test
    void markAllReadZeroesUnread() {
        service.emit(ALICE, Notification.TYPE_POST_LIKE, BOB, "post", "p1");
        service.emit(ALICE, Notification.TYPE_POST_COMMENT, BOB, "post", "p2");

        service.markAllRead(ALICE);
        assertEquals(0, service.unreadCount(ALICE));
    }

    @Test
    void deleteRemovesOnlyForRecipient() {
        service.emit(ALICE, Notification.TYPE_POST_LIKE, BOB, "post", "p1");
        String id = repo.items.get(0).getId();

        service.delete(BOB, id);     // wrong user — no-op
        assertEquals(1, repo.items.size());

        service.delete(ALICE, id);
        assertTrue(repo.items.isEmpty());
    }

    @Test
    void unknownActorFallsBackToSomeone() {
        NotificationFeedService svc = new NotificationFeedService(repo, uid -> Optional.empty());
        svc.emit(ALICE, Notification.TYPE_POST_LIKE, BOB, "post", "p1");
        assertEquals("Someone liked your post", repo.items.get(0).getMessage());
    }
}
