package com.extantape.sheriff;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * WelcomeWagon invites (owner, 2026-11-09; crossing welcomewagon-sheriff). When an admin makes an invite naming profiles, Sheriff
 * runs its usual checks then, so a bad combination is refused before anyone arrives. When the invite is used, Sheriff
 * applies the profiles through the same change path as any approval, with the yeses given when the invite was made.
 *
 * <p>WelcomeWagon is optional and Sheriff doesn't build against it: its events are found by name at startup and read through
 * their public methods, which are the agreed contract. If WelcomeWagon isn't there, or its events don't match, this does
 * nothing and says so once.
 */
final class WelcomeWagonHook implements Listener {
    static final String CHECK = "com.extantape.welcomewagon.api.InviteCheckEvent";
    static final String REDEEMED = "com.extantape.welcomewagon.api.InviteRedeemedEvent";
    static final String ACTOR = "WelcomeWagon invite";

    /** What Sheriff says about an invite's profiles: a refusal, or questions keyed by their command-line id. */
    record Check(String refusalCode, String refusalText, Map<String, String> questions) {
        boolean refused() { return refusalCode != null; }
    }

    private WelcomeWagonHook() {}

    /**
     * Sheriff's checks for a new player. Invites are for people who haven't arrived, so the plan starts from no groups;
     * when the invite is used the real plan runs again against their actual groups.
     */
    static Check check(Settings settings, GroupStore store, Collection<String> profiles, Set<String> yesIds) {
        if (!store.ready()) return new Check("luckperms-missing", "LuckPerms isn't running, so Sheriff can't check these profiles.", Map.of());
        Set<String> confirmed = confirmedCodes(settings, yesIds);
        Planner.Plan plan = Planner.plan(settings, store, new GroupStore.UserState(Set.of(), Set.of()),
                new LinkedHashSet<>(profiles), Set.of(), confirmed);
        if (plan.refused()) return new Check(plan.refusal().replace('_', '-'), plan.refusalText(), Map.of());
        Map<String, String> q = new LinkedHashMap<>();
        for (Planner.Warning w : plan.warnings()) q.put(toYesId(w.code()), w.text());
        return new Check(null, null, q);
    }

    /**
     * Sheriff's warning codes as command-line ids, which allow only {@code a-z 0-9 -}: {@code confirm:grown_sky} becomes
     * {@code confirm-grown-sky}.
     */
    static String toYesId(String code) { return code.replace(':', '-').replace('_', '-'); }

    /**
     * The warning codes a set of yes-ids stands for. A yes-id that two profiles would share counts for neither, so a
     * yes can never be applied to a question the admin didn't see.
     */
    static Set<String> confirmedCodes(Settings settings, Set<String> yesIds) {
        Set<String> out = new LinkedHashSet<>();
        for (String y : yesIds) {
            if (y.equals("bridge")) { out.add("bridge"); continue; }
            List<String> matches = new ArrayList<>();
            for (String id : settings.profiles().keySet()) if (toYesId("confirm:" + id).equals(y)) matches.add("confirm:" + id);
            if (matches.size() == 1) out.add(matches.get(0));
        }
        return out;
    }

    /** Hooks into WelcomeWagon's events if WelcomeWagon is installed. */
    static void register(SheriffPlugin sheriff) {
        Plugin welcomewagon = Bukkit.getPluginManager().getPlugin("WelcomeWagon");
        if (welcomewagon == null) return;
        try {
            ClassLoader cl = welcomewagon.getClass().getClassLoader();
            Class<? extends Event> checkClass = cl.loadClass(CHECK).asSubclass(Event.class);
            Class<? extends Event> redeemedClass = cl.loadClass(REDEEMED).asSubclass(Event.class);
            Method getProfiles = checkClass.getMethod("getProfiles"), getConfirmed = checkClass.getMethod("getConfirmed");
            Method refuse = checkClass.getMethod("refuse", String.class, String.class), need = checkClass.getMethod("need", String.class, String.class);
            Method checkedBy = checkClass.getMethod("checkedBy", String.class);
            Method rUuid = redeemedClass.getMethod("getUuid"), rName = redeemedClass.getMethod("getName");
            Method rProfiles = redeemedClass.getMethod("getProfiles"), rConfirmed = redeemedClass.getMethod("getConfirmed");
            Listener owner = new WelcomeWagonHook();

            Bukkit.getPluginManager().registerEvent(checkClass, owner, EventPriority.NORMAL, (l, event) -> {
                if (!checkClass.isInstance(event)) return;
                try {
                    @SuppressWarnings("unchecked") List<String> profiles = (List<String>) getProfiles.invoke(event);
                    @SuppressWarnings("unchecked") Set<String> yes = (Set<String>) getConfirmed.invoke(event);
                    Check c = check(sheriff.settings(), sheriff.store(), profiles, yes);
                    checkedBy.invoke(event, "Sheriff");
                    if (c.refused()) refuse.invoke(event, c.refusalCode(), c.refusalText());
                    else for (var q : c.questions().entrySet()) need.invoke(event, q.getKey(), q.getValue());
                } catch (ReflectiveOperationException | RuntimeException e) {
                    sheriff.getLogger().log(Level.WARNING, "Couldn't check a WelcomeWagon invite's profiles", e);
                    try { refuse.invoke(event, "internal", "Sheriff couldn't check these profiles. See the server log."); } catch (ReflectiveOperationException ignored) {}
                }
            }, sheriff);

            Bukkit.getPluginManager().registerEvent(redeemedClass, owner, EventPriority.MONITOR, (l, event) -> {
                if (!redeemedClass.isInstance(event)) return;
                try {
                    UUID uuid = (UUID) rUuid.invoke(event);
                    String name = (String) rName.invoke(event);
                    @SuppressWarnings("unchecked") List<String> profiles = (List<String>) rProfiles.invoke(event);
                    @SuppressWarnings("unchecked") Set<String> yes = (Set<String>) rConfirmed.invoke(event);
                    apply(sheriff, uuid, name, profiles, yes);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    sheriff.getLogger().log(Level.WARNING, "Couldn't read a used WelcomeWagon invite, so its profiles weren't applied", e);
                }
            }, sheriff);
            sheriff.getLogger().info("Checking and applying the profiles on WelcomeWagon invites.");
        } catch (ReflectiveOperationException | ClassCastException e) {
            sheriff.getLogger().warning("WelcomeWagon is installed but its invite events don't match what Sheriff expects, so invite profiles "
                    + "won't be applied. Update both plugins. (" + e.getClass().getSimpleName() + ")");
        }
    }

    /** Applies an invite's profiles as a change made by the admin who made the invite, with their earlier yeses. */
    static void apply(SheriffPlugin sheriff, UUID uuid, String name, List<String> profiles, Set<String> yesIds) {
        if (profiles.isEmpty()) {
            sheriff.getLogger().info(name + " joined through an invite with no profiles, so they have lobby-only access for now.");
            return;
        }
        Set<String> confirmed = confirmedCodes(sheriff.settings(), yesIds);
        var request = new ChangeService.Request(null, new ChangeService.Actor(ACTOR, null, "welcomewagon"), uuid, name,
                new LinkedHashSet<>(profiles), Set.of(), confirmed, false);
        sheriff.changes().submit(request).thenAccept(result -> {
            if (result.ok()) return;
            List<String> asks = new ArrayList<>();
            for (Planner.Warning w : result.warnings()) asks.add(w.text());
            sheriff.getLogger().warning("The profiles on " + name + "'s invite weren't applied: " + result.message()
                    + (asks.isEmpty() ? "" : " " + String.join(" ", asks)) + " Apply them with /sheriff once that's sorted.");
        });
    }
}
