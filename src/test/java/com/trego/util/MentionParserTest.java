package com.trego.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MentionParserTest {

    @Test
    void extractsSingleHandleLowercased() {
        assertEquals(List.of("runner_42"), MentionParser.extractHandles("hey @Runner_42 nice pace"));
    }

    @Test
    void extractsMultipleAndDedupes() {
        assertEquals(List.of("alice", "bob"),
                MentionParser.extractHandles("@alice and @bob and @Alice again"));
    }

    @Test
    void ignoresEmailsAndMidWordAt() {
        assertTrue(MentionParser.extractHandles("mail me at foo@bar.com").isEmpty());
        assertTrue(MentionParser.extractHandles("someone@example").isEmpty());
    }

    @Test
    void ignoresTooShortHandles() {
        // "@ab" is 2 chars — below the 3-char minimum.
        assertTrue(MentionParser.extractHandles("hi @ab").isEmpty());
    }

    @Test
    void stopsAtNonHandleChars() {
        assertEquals(List.of("alice"), MentionParser.extractHandles("great run, @alice!"));
    }

    @Test
    void handlesNullAndEmpty() {
        assertTrue(MentionParser.extractHandles(null).isEmpty());
        assertTrue(MentionParser.extractHandles("").isEmpty());
    }

    @Test
    void handleAtStartOfString() {
        assertEquals(List.of("alice"), MentionParser.extractHandles("@alice hi"));
    }

    @Test
    void capsHandleLengthAt20() {
        // 21-char run: only the first 20 chars form a valid handle; regex is greedy
        // up to 20, so the match is the 20-char prefix.
        List<String> out = MentionParser.extractHandles("@" + "a".repeat(21));
        assertEquals(List.of("a".repeat(20)), out);
    }
}
