package com.revaro.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ProfanityFilter {

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

    // Word boundaries so things like "class" or "Dickson" don't get caught
    private static final List<Pattern> PATTERNS = BAD_WORDS.stream()
            .map(word -> Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE))
            .toList();

    public record FilterResult(String filtered, boolean wasFlagged) {
    }

    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return PATTERNS.stream().anyMatch(pattern -> pattern.matcher(text).find());
    }

    // Replaces each bad word with asterisks of the same length
    public String filter(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String result = text;
        for (Pattern pattern : PATTERNS) {
            result = pattern.matcher(result).replaceAll(match -> "*".repeat(match.group().length()));
        }
        return result;
    }

    public FilterResult filterAndFlag(String text) {
        return new FilterResult(filter(text), containsProfanity(text));
    }
}
