package com.extantape.sheriff;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Made-up names throughout. */
class NameClashTest {

    @Test
    void aNameThatDiffersOnlyInCapitalsIsRefused() {
        for (String typed : List.of("pebblefox", "PEBBLEFOX", "pebbleFox", "PebbleFoX")) {
            NameClash.Refusal no = NameClash.check(typed, List.of("Mossy_Owl", "PebbleFox"));
            assertNotNull(no, typed);
            assertEquals("name_clash", no.code());
        }
    }

    @Test
    void theRefusalNamesTheExistingEntryAndSaysHowToFixIt() {
        String text = NameClash.check("pebblefox", List.of("PebbleFox")).message();
        assertTrue(text.startsWith("pebblefox matches PebbleFox, who is already on the whitelist"), text);
        assertTrue(text.contains("offline mode"), text);
        assertTrue(text.contains("Nothing was changed."), text);
        assertTrue(text.contains("they join as PebbleFox"), text);
        assertTrue(text.contains("/whitelist remove PebbleFox"), text);
    }

    @Test
    void theExactSpellingAndOtherNamesPass() {
        assertNull(NameClash.check("PebbleFox", List.of("PebbleFox", "Mossy_Owl")));
        assertNull(NameClash.check("PebbleFox", List.of()));
        // Close, but a different name: an underscore, one more letter, a digit for a letter.
        assertNull(NameClash.check("PebbleFox", List.of("Pebble_Fox", "PebbleFoxx", "Pebb1eFox", "ebbleFox")));
    }

    @Test
    void withSeveralClashesTheSameOneIsNamedEveryTime() {
        assertTrue(NameClash.check("pebblefox", List.of("PebbleFox", "PEBBLEFOX")).message().contains("matches PEBBLEFOX,"));
        assertTrue(NameClash.check("pebblefox", List.of("PEBBLEFOX", "PebbleFox")).message().contains("matches PEBBLEFOX,"));
    }

    @Test
    void anEntryWithoutANameRefusesBecauseItCannotBeCompared() {
        for (String missing : Arrays.asList(null, "", " ")) {
            NameClash.Refusal no = NameClash.check("PebbleFox", Arrays.asList("Mossy_Owl", missing));
            assertNotNull(no);
            assertEquals("name_check_failed", no.code());
            assertTrue(no.message().contains("Nothing was changed."), no.message());
        }
        // A clash that can be named is the more useful answer.
        assertEquals("name_clash", NameClash.check("pebblefox", Arrays.asList(null, "PebbleFox")).code());
    }

    @Test
    void capitalsAreComparedTheSameWayInEveryLocale() {
        java.util.Locale before = java.util.Locale.getDefault();
        try {
            // Turkish lowercases I to a dotless i, which would hide this clash if the default locale were used.
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            assertEquals("name_clash", NameClash.check("MINTIBIS", List.of("mintibis")).code());
        } finally {
            java.util.Locale.setDefault(before);
        }
    }
}
