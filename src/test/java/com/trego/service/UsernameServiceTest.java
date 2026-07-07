package com.trego.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsernameServiceTest {

    static final String ALICE = "alice-uid";
    static final String BOB = "bob-uid";

    InMemoryUsernameDirectory dir;
    UsernameService service;

    @BeforeEach
    void setUp() {
        dir = new InMemoryUsernameDirectory();
        service = new UsernameService(dir);
    }

    @Test
    void claimsValidUsernameLowercased() {
        String result = service.claim(ALICE, null, "Runner_42");
        assertEquals("runner_42", result);
        assertEquals(ALICE, dir.byName.get("runner_42"));
    }

    @Test
    void rejectsTooShort() {
        assertThrows(IllegalArgumentException.class, () -> service.claim(ALICE, null, "ab"));
    }

    @Test
    void rejectsTooLong() {
        assertThrows(IllegalArgumentException.class,
                () -> service.claim(ALICE, null, "a234567890123456789012"));
    }

    @Test
    void rejectsLeadingNonLetter() {
        assertThrows(IllegalArgumentException.class, () -> service.claim(ALICE, null, "1abc"));
        assertThrows(IllegalArgumentException.class, () -> service.claim(ALICE, null, "_abc"));
    }

    @Test
    void rejectsIllegalChars() {
        assertThrows(IllegalArgumentException.class, () -> service.claim(ALICE, null, "ab-cd"));
        assertThrows(IllegalArgumentException.class, () -> service.claim(ALICE, null, "ab.cd"));
        assertThrows(IllegalArgumentException.class, () -> service.claim(ALICE, null, "ab cd"));
    }

    @Test
    void rejectsReservedNames() {
        assertThrows(IllegalArgumentException.class, () -> service.claim(ALICE, null, "admin"));
        assertThrows(IllegalArgumentException.class, () -> service.claim(ALICE, null, "Trego"));
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.claim(ALICE, null, null));
    }

    @Test
    void takenByAnotherUserThrowsIllegalState() {
        service.claim(BOB, null, "runner");
        assertThrows(IllegalStateException.class, () -> service.claim(ALICE, null, "runner"));
    }

    @Test
    void caseInsensitiveCollision() {
        service.claim(BOB, null, "Runner");
        assertThrows(IllegalStateException.class, () -> service.claim(ALICE, null, "runner"));
    }

    @Test
    void reclaimingSameNameIsIdempotent() {
        service.claim(ALICE, null, "runner");
        // Same normalized value, currentUsername already set → no-op, no throw.
        assertEquals("runner", service.claim(ALICE, "runner", "Runner"));
        assertEquals(ALICE, dir.byName.get("runner"));
    }

    @Test
    void renameReleasesOldName() {
        service.claim(ALICE, null, "oldname");
        String result = service.claim(ALICE, "oldname", "newname");
        assertEquals("newname", result);
        assertEquals(ALICE, dir.byName.get("newname"));
        assertFalse(dir.byName.containsKey("oldname"), "old name should be released");
    }

    @Test
    void renameToTakenNameKeepsOldName() {
        service.claim(BOB, null, "taken");
        service.claim(ALICE, null, "mine");
        assertThrows(IllegalStateException.class, () -> service.claim(ALICE, "mine", "taken"));
        // Old name must remain claimed since the rename failed.
        assertEquals(ALICE, dir.byName.get("mine"));
    }
}
