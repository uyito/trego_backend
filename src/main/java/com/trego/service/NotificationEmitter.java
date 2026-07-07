package com.trego.service;

/**
 * Narrow seam for producing an in-app notification, letting {@code SocialService}
 * and {@code FriendService} emit events without depending on the whole
 * notification subsystem. Implemented by {@link NotificationFeedService}; tests
 * can supply a recording fake or a no-op.
 *
 * <p>Implementations must no-op when {@code recipientUid == actorUid} or either
 * is null (a user shouldn't be notified about their own action).
 */
public interface NotificationEmitter {
    void emit(String recipientUid, String type, String actorUid, String targetType, String targetId);
}
