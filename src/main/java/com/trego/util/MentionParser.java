package com.trego.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts {@code @username} mentions from free text. Handles match the username
 * charset ({@code [a-z0-9_]{3,20}}, case-insensitive) and must not be preceded by
 * a word character — so emails like {@code foo@bar} are not treated as mentions.
 * Results are lowercased and deduped, preserving first-seen order.
 */
public final class MentionParser {

    private static final Pattern HANDLE =
            Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z0-9_]{3,20})");

    private MentionParser() {}

    public static List<String> extractHandles(String content) {
        if (content == null || content.isEmpty()) return new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Matcher m = HANDLE.matcher(content);
        while (m.find()) {
            seen.add(m.group(1).toLowerCase());
        }
        return new ArrayList<>(seen);
    }
}
