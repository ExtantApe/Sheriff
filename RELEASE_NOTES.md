# Sheriff 0.5.1

On an offline-mode server, Sheriff now refuses to approve a name that matches a whitelisted name apart from capital letters. There, `pebblefox` and `PebbleFox` would be two separate players who read as one person in chat and in logs.

- `/approve` says so in chat before any menu opens. `sheriff apply` answers with the code `name-clash`. Both name the entry already on the whitelist and say what to do: the same person joins under the existing spelling, someone else picks a different name, and `/whitelist remove` on the old spelling comes first if the spelling should change.
- Nothing is changed by a refused request: no whitelist entry and no profiles. The refusal goes in `changes.log` and fires a `SheriffProblemEvent`.
- If Sheriff can't read every name on the whitelist, it refuses (`name-check-failed`) and says how to check the list.
- People already on the whitelist can still have their profiles changed. Online-mode servers behave as before.

## Installing over 0.5.0

Stop the server, swap in `Sheriff-0.5.1.jar`, start it. No config changes.

## Tested

Unit tests pass (53, 6 of them new). The bot suite passed 131 of 131 checks on throwaway Paper 26.1.2, 26.2 and 26.3 servers in offline mode, 19 of them new: the refusal from the console and in the game, its wording, the whitelist and profiles left untouched, the refused name turned away at the door, and the same commands working for the exact spelling and for a different name.

Not tested: a real game client, so nobody has yet read the refusal in real chat. An online-mode server, where the check is skipped, wasn't run. The refusal for a whitelist entry with no name is covered by unit tests only.
