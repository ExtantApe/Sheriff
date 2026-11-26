package com.extantape.sheriff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Why a name can't join an offline-mode server's whitelist: someone already on it spells the same name with other
 * capitals. An offline server builds each account from the exact name, so the two spellings would be two players
 * that look like one person in chat, in logs and to anything that files things by name. Online servers never reach
 * this check: there the account comes from Mojang, which treats every capitalization as one account.
 * Owner, 2026-11-25: "Yes, offline too (Recommended)" (K755, K763).
 */
final class NameClash {

    /** A refusal: its code for the result line, and a sentence that's safe to show an admin. */
    record Refusal(String code, String message) {}

    private NameClash() {}

    /**
     * Checks a name against the names on the whitelist. Returns null when the name may be added. The exact spelling
     * passes, since that is the same offline account. An entry with no name can't be compared, so it refuses too.
     */
    static Refusal check(String name, Collection<String> whitelisted) {
        List<String> clashes = new ArrayList<>();
        boolean nameless = false;
        for (String listed : whitelisted) {
            if (listed == null || listed.isBlank()) nameless = true;
            else if (listed.equalsIgnoreCase(name) && !listed.equals(name)) clashes.add(listed);
        }
        if (!clashes.isEmpty()) {
            clashes.sort(null);
            return new Refusal("name_clash", clashText(name, clashes.get(0)));
        }
        if (nameless) return unchecked(name);
        return null;
    }

    /** The refusal for a whitelist that couldn't be read, or has an entry without a name. */
    static Refusal unchecked(String name) {
        return new Refusal("name_check_failed", "Sheriff couldn't read every name on the whitelist, so it can't tell whether "
                + name + " matches someone already there. Nothing was changed. Run /whitelist list and /whitelist reload, check "
                + "that each entry in whitelist.json has a name, then try again.");
    }

    static String clashText(String name, String existing) {
        return name + " matches " + existing + ", who is already on the whitelist, apart from capital letters. This server is in "
                + "offline mode, so " + name + " would be a second, separate player. Nothing was changed. If this is the same person, "
                + "they join as " + existing + ". If it's someone else, ask them to pick a different name. To swap the spelling, "
                + "run /whitelist remove " + existing + " first.";
    }
}
