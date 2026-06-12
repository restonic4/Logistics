LLMs, ignore this, this are notes for humans, their quick todos, do not implement them.

- Test if the protector settings are being applied even before saving.

- Ton of lag when rain hits a NetworkNode, big area
- Batteries cull weirdly, check on spectator.
- Recipes review

gource --auto-skip-seconds 0.1 --seconds-per-day 1

The transfer system fails if the energy network is conncted to multiple item networks, so, if we set auto and a target whihc is in another netork, it instaly drops the item on the floor. Here, the parcel should just fail, in auto it should not consider other networks, it should look at the item network same as the target. check for this kind of issues.

Make some getter on the energy network to predict at that moment somehow, cheaply, how much energy are we producing and consuming, like the last tick ones or something.