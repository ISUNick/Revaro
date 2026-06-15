package com.revaro.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Filters profanity from user-submitted text.
 * Replaces bad words with asterisks and flags the content for review.
 */
@Component
public class ProfanityFilter {

    // Word list — add more as needed
    private static final List<String> BAD_WORDS = List.of(
        "fuck", "fucker", "fucking", "fucked", "fucks",
        "shit", "shits", "shitting", "shitty",
        "bitch", "bitches", "bitching",
        "asshole", "assholes", "ass",
        "bastard", "bastards",
        "cunt", "cunts",
        "dick", "dicks",
        "cock", "cocks",
        "pussy", "pussies",
        "nigger", "niggers", "nigga",
        "faggot", "faggots", "fag",
        "retard", "retarded", "retards",
        "whore", "whores",
        "slut", "sluts",
        "piss", "pissed",
        "damn", "dammit",
        "crap",
        "twat", "twats",
        "wanker", "wankers",
        "bollocks",
        "motherfucker", "motherfucking",
        "bullshit"
    );

    private static final List<Pattern> PATTERNS;

    static {
        PATTERNS = BAD_WORDS.stream()
            .map(word -> Pattern.compile(
                "\\b" + Pattern.quote(word) + "\\b",
                Pattern.CASE_INSENSITIVE
            ))
            .toList();
    }

    /**
     * Returns true if the text contains any flagged words.
     */
    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) return false;
        for (Pattern p : PATTERNS) {
            if (p.matcher(text).find()) return true;
        }
        return false;
    }

    /**
     * Replaces bad words with asterisks. e.g. "fuck" -> "****"
     */
    public String filter(String text) {
        if (text == null || text.isBlank()) return text;
        for (Pattern p : PATTERNS) {
            text = p.matcher(text).replaceAll(m -> "*".repeat(m.group().length()));
        }
        return text;
    }

    /**
     * Filter result — gives you both the cleaned text and whether it was flagged.
     */
    public record FilterResult(String filtered, boolean wasFlagged) {}

    public FilterResult filterAndFlag(String text) {
        if (text == null || text.isBlank()) return new FilterResult(text, false);
        boolean flagged = containsProfanity(text);
        return new FilterResult(filter(text), flagged);
    }
}
