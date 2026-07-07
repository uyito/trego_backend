package com.trego.service;

import java.util.ArrayList;
import java.util.List;

/** Records emitted notifications for assertion in service tests. */
public class RecordingNotificationEmitter implements NotificationEmitter {

    public static final class Emitted {
        public final String recipientUid;
        public final String type;
        public final String actorUid;
        public final String targetType;
        public final String targetId;

        Emitted(String recipientUid, String type, String actorUid, String targetType, String targetId) {
            this.recipientUid = recipientUid;
            this.type = type;
            this.actorUid = actorUid;
            this.targetType = targetType;
            this.targetId = targetId;
        }
    }

    public final List<Emitted> emitted = new ArrayList<>();

    @Override
    public void emit(String recipientUid, String type, String actorUid, String targetType, String targetId) {
        emitted.add(new Emitted(recipientUid, type, actorUid, targetType, targetId));
    }
}
