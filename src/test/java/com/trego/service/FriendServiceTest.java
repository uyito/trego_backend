package com.trego.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class FriendServiceTest {

    static final String ALICE = "alice-uid";
    static final String BOB = "bob-uid";
    static final String CAROL = "carol-uid";

    InMemoryFriendGraphRepository repo;
    FriendService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryFriendGraphRepository();
        repo.addUser(ALICE, "alice@test.example", "Alice A");
        repo.addUser(BOB, "bob@test.example", "Bob B");
        repo.addUser(CAROL, "carol@test.example", "Carol C");
        service = new FriendService(repo);
    }

    @Test
    void sendRequestCreatesPending() {
        Map<String, Object> result = service.sendRequest(ALICE, "bob@test.example", "hi!");
        assertEquals("pending", result.get("status"));

        List<Map<String, Object>> bobRequests = service.getRequests(BOB);
        assertEquals(1, bobRequests.size());
        assertEquals("incoming", bobRequests.get(0).get("type"));
        assertFalse(service.areFriends(ALICE, BOB));
    }

    @Test
    void emailIsCaseInsensitive() {
        Map<String, Object> result = service.sendRequest(ALICE, "BOB@TEST.EXAMPLE", null);
        assertEquals("pending", result.get("status"));
    }

    @Test
    void unknownEmailThrows() {
        assertThrows(NoSuchElementException.class,
                () -> service.sendRequest(ALICE, "nobody@test.example", null));
    }

    @Test
    void selfRequestRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendRequest(ALICE, "alice@test.example", null));
    }

    @Test
    void duplicatePendingRejected() {
        service.sendRequest(ALICE, "bob@test.example", null);
        assertThrows(IllegalStateException.class,
                () -> service.sendRequest(ALICE, "bob@test.example", null));
    }

    @Test
    void reversePendingAutoAccepts() {
        service.sendRequest(ALICE, "bob@test.example", null); // alice → bob pending
        Map<String, Object> result = service.sendRequest(BOB, "alice@test.example", null); // bob → alice

        assertEquals("friends", result.get("status"));
        assertTrue(service.areFriends(ALICE, BOB));
        assertTrue(service.areFriends(BOB, ALICE));
        // No dangling pending requests for either side.
        assertTrue(service.getRequests(ALICE).isEmpty());
        assertTrue(service.getRequests(BOB).isEmpty());
    }

    @Test
    void acceptCreatesMutualFriendship() {
        Map<String, Object> sent = service.sendRequest(ALICE, "bob@test.example", null);
        @SuppressWarnings("unchecked")
        String reqId = (String) ((Map<String, Object>) sent.get("request")).get("id");

        service.respondToRequest(BOB, reqId, true);

        assertTrue(service.areFriends(ALICE, BOB));
        assertEquals(1, service.getFriends(ALICE).size());
        assertEquals(BOB, service.getFriends(ALICE).get(0).get("uid"));
        assertEquals("Bob B", service.getFriends(ALICE).get(0).get("name"));
        assertEquals(1, service.getFriends(BOB).size());
        assertEquals(ALICE, service.getFriends(BOB).get(0).get("uid"));
    }

    @Test
    void declineDoesNotCreateFriendship() {
        Map<String, Object> sent = service.sendRequest(ALICE, "bob@test.example", null);
        @SuppressWarnings("unchecked")
        String reqId = (String) ((Map<String, Object>) sent.get("request")).get("id");

        service.respondToRequest(BOB, reqId, false);

        assertFalse(service.areFriends(ALICE, BOB));
        assertTrue(service.getRequests(BOB).isEmpty());
    }

    @Test
    void onlyRecipientCanRespond() {
        Map<String, Object> sent = service.sendRequest(ALICE, "bob@test.example", null);
        @SuppressWarnings("unchecked")
        String reqId = (String) ((Map<String, Object>) sent.get("request")).get("id");

        assertThrows(SecurityException.class, () -> service.respondToRequest(CAROL, reqId, true));
    }

    @Test
    void onlySenderCanCancel() {
        Map<String, Object> sent = service.sendRequest(ALICE, "bob@test.example", null);
        @SuppressWarnings("unchecked")
        String reqId = (String) ((Map<String, Object>) sent.get("request")).get("id");

        assertThrows(SecurityException.class, () -> service.cancelRequest(BOB, reqId));

        service.cancelRequest(ALICE, reqId);
        assertTrue(service.getRequests(BOB).isEmpty());
    }

    @Test
    void unfriendRemovesFriendship() {
        Map<String, Object> sent = service.sendRequest(ALICE, "bob@test.example", null);
        @SuppressWarnings("unchecked")
        String reqId = (String) ((Map<String, Object>) sent.get("request")).get("id");
        service.respondToRequest(BOB, reqId, true);
        assertTrue(service.areFriends(ALICE, BOB));

        service.unfriend(ALICE, BOB);

        assertFalse(service.areFriends(ALICE, BOB));
        assertTrue(service.getFriends(ALICE).isEmpty());
    }

    @Test
    void sendingToExistingFriendRejected() {
        Map<String, Object> sent = service.sendRequest(ALICE, "bob@test.example", null);
        @SuppressWarnings("unchecked")
        String reqId = (String) ((Map<String, Object>) sent.get("request")).get("id");
        service.respondToRequest(BOB, reqId, true);

        assertThrows(IllegalStateException.class,
                () -> service.sendRequest(ALICE, "bob@test.example", null));
    }
}
