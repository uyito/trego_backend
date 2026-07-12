package com.trego.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTokenServiceTest {

    static final String ALICE = "alice-uid";
    static final String BOB = "bob-uid";

    InMemoryDeviceTokenRepository repo;
    DeviceTokenService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryDeviceTokenRepository();
        service = new DeviceTokenService(repo);
    }

    @Test
    void registerThenTokensFor() {
        service.register(ALICE, "tok-a1", "android");
        service.register(ALICE, "tok-a2", "ios");
        service.register(BOB, "tok-b1", "android");

        List<String> aliceTokens = service.tokensFor(ALICE);
        assertEquals(2, aliceTokens.size());
        assertTrue(aliceTokens.contains("tok-a1"));
        assertTrue(aliceTokens.contains("tok-a2"));
        assertEquals(List.of("tok-b1"), service.tokensFor(BOB));
    }

    @Test
    void registerIsIdempotentUpsert() {
        service.register(ALICE, "tok", "android");
        service.register(ALICE, "tok", "ios"); // same token, platform changes
        assertEquals(1, service.tokensFor(ALICE).size());
    }

    @Test
    void reregisteringAtokenReassignsOwner() {
        service.register(ALICE, "shared", "android");
        service.register(BOB, "shared", "android"); // device switched accounts
        assertTrue(service.tokensFor(ALICE).isEmpty());
        assertEquals(List.of("shared"), service.tokensFor(BOB));
    }

    @Test
    void unregisterRemovesToken() {
        service.register(ALICE, "tok", "android");
        service.unregister("tok");
        assertTrue(service.tokensFor(ALICE).isEmpty());
    }

    @Test
    void emptyTokenRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.register(ALICE, "", "android"));
        assertThrows(IllegalArgumentException.class, () -> service.register(ALICE, null, "android"));
    }

    @Test
    void tokensForUnknownUserIsEmpty() {
        assertTrue(service.tokensFor("nobody").isEmpty());
    }
}
