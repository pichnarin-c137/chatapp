package com.chatapp.backend.message.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans a message body for @username tokens. The regex matches the same
 * character class our registration validator accepts. v1 does not exclude
 * code blocks or quoted contexts — kept simple on purpose; we can swap in
 * a real parser later if false positives become annoying.
 *
 * Duplicate @mentions of the same username inside one message collapse to
 * the first occurrence (the composite PK on message_mentions would reject
 * the second insert anyway).
 */
@Component
public class MentionParser {

    private static final Pattern PATTERN = Pattern.compile("@([A-Za-z0-9_]{3,32})");
    private static final int MAX_MENTIONS = 10;

    public List<RawMention> extract(String body) {
        if (body == null || body.isBlank()) return List.of();
        Matcher m = PATTERN.matcher(body);
        List<RawMention> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        while (m.find()) {
            if (out.size() >= MAX_MENTIONS) break;
            String username = m.group(1);
            if (!seen.add(username.toLowerCase())) continue;
            out.add(new RawMention(username, m.start(), m.end()));
        }
        return out;
    }

    /**
     * @param username  the @-stripped username token
     * @param start     inclusive offset of the @ in the body
     * @param end       exclusive offset (start + length of the full @username token)
     */
    public record RawMention(String username, int start, int end) {}
}
