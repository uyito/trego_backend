package com.trego.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Production {@link PushSender} over Firebase Cloud Messaging. Sends a
 * notification+data multicast to all of a user's device tokens and reactively
 * prunes tokens that FCM reports as unregistered/invalid. Best-effort: any
 * failure is logged and swallowed so it never breaks the notification flow.
 */
@Component
public class FcmPushSender implements PushSender {

    private static final Logger logger = LoggerFactory.getLogger(FcmPushSender.class);

    private final DeviceTokenService tokens;

    @Autowired
    public FcmPushSender(DeviceTokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    public void sendToUser(String recipientUid, String title, String body, Map<String, String> data) {
        try {
            List<String> deviceTokens = tokens.tokensFor(recipientUid);
            if (deviceTokens.isEmpty()) return;

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(deviceTokens)
                    .setNotification(Notification.builder()
                            .setTitle(title != null ? title : "Trego")
                            .setBody(body != null ? body : "")
                            .build())
                    .putAllData(data != null ? data : Map.of())
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            pruneInvalidTokens(deviceTokens, response);
        } catch (Exception e) {
            logger.warn("Push send to {} failed: {}", recipientUid, e.getMessage());
        }
    }

    /** Delete tokens FCM reported as unregistered/invalid so they aren't retried. */
    private void pruneInvalidTokens(List<String> deviceTokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (r.isSuccessful()) continue;
            FirebaseMessagingException ex = r.getException();
            MessagingErrorCode code = ex != null ? ex.getMessagingErrorCode() : null;
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                tokens.unregister(deviceTokens.get(i));
            }
        }
    }
}
