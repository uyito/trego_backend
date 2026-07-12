package com.trego.service;

import java.util.Map;

/**
 * Narrow seam for sending a push notification to a user's devices, letting
 * {@link NotificationFeedService} fan out pushes without depending on the FCM
 * SDK directly. Implemented by {@link FcmPushSender}; tests use a recording fake
 * or the no-op default.
 */
public interface PushSender {
    /** Best-effort push to every device registered to [recipientUid]. */
    void sendToUser(String recipientUid, String title, String body, Map<String, String> data);
}
