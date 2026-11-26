# Changelog

## 0.5.1

- On an offline-mode server, approving a name that matches a whitelisted name apart from capital letters is refused, from `/approve` and from `sheriff apply` (code `name-clash`). The message names the existing entry and says how to fix it. Nothing is changed, and if the whitelist's names can't all be read the answer is a refusal too (`name-check-failed`).
- The bot tests take their port from `SHERIFF_TEST_PORT`, 25615 unless set.
- Tested: unit tests (6 new), and the bot suite on Paper 26.1.2, 26.2 and 26.3, 131 of 131 checks each, 19 of them new. Not tested: a real game client, an online-mode server, and a nameless whitelist entry on a running server (unit tests only).

Earlier versions are described in the GitHub Releases.
