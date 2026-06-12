LLMs, ignore this, this are notes for humans, their quick todos, do not implement them.

- Test if the protector settings are being applied even before saving.

- Accessors and transfers:
  - Being able to send items with nbt, such as kinetic crystals.
  - Being able to specify nbt data, such as, ignoring nbt or selecting specific data, such as only getting crystals with >= 95% energy.
  - Improve the UI.

- Audio Stations:
  - aa

- Ton of lag when rain hits a NetworkNode, big area
- Batteries cull weirdly, check on spectator.
- Recipes review


Triggers, Events and Actions

getParam("node") -> NetworkNode / null


Protector ui overrides repair

Trigger:
- type
- config

gource --auto-skip-seconds 0.1 --seconds-per-day 1

DECIRLE QUE SE ANOTE QUE ES MULTILOADER

Take a look at TriggersTab, I have added some comments, some TODOS. 3 TODOs. The problem is, modularity, scalability, since, if we add a new trigger or action, well, now we need to go here and make the if statements bigger, thats a problem. Can we make it better? The summary one could eb solved by adding method on the trigger that each custom type overrides, right? (or maybe not, check possibilities.). Then there is the builders for the triggers, i dont really know how can we make this scalable and cleaner. And then the actions massive if, same, idk how to make it better. Since we will be adding more triggers and actions, it should be painless.