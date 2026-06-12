# Computer Automation System (Triggers & Actions)

A tick-driven automation engine for the `ComputerNode`. Users configure **Triggers** (conditions
evaluated every game tick) that, when fired, run an ordered list of **Actions**. Actions can span
multiple ticks (e.g. "Wait 50 ticks") without ever blocking the server tick, other triggers, or
other runs of the same trigger.

Everything lives under:

```
com.restonic4.logistics.blocks.computer.automation.triggers
├── core/       Trigger, TriggerAction, TriggerManager, TriggerContext,
│               ActionSequenceTracker, ActionExecutionContext, ExecuteResult
├── registry/   TriggerType, ActionType, TriggerRegistry, ActionRegistry
├── types/      Built-in triggers (EnergyLevelTrigger, AudioStateTrigger, IntervalTrigger)
├── actions/    Built-in actions (WaitTicksAction, PlayAudioAction, StopAudioAction,
│               WaitAudioAction, LogMessageAction)
└── util/       AudioStationTarget (shared station selection + serialization)
```

The save flow lives one package up: `automation/TriggerSavePacket` carries the Triggers tab's
edits to the server. The UI is `blocks.computer.screen.triggers.TriggersTab`.

There are **no threads** involved. All "concurrency" is logical: every running action sequence is
advanced once per game tick from `ComputerNode#tick()`, on the main server thread.

---

## Using it in game: the Triggers tab

The **Triggers** tab is always present on an installed computer (it's a computer feature, not
node-dependent like Audio/Transfer/Protection). Left side: the trigger list plus an
add-trigger dropdown. Right side: the selected trigger's firing settings (mode, overlap),
its type-specific config, and its ordered action list with move/delete controls.

Edits are local until **Save Changes** sends them via `TriggerSavePacket`; closing with
unsaved changes prompts like the Protection tab. On the server, saving preserves running
sequences of untouched triggers (matched by instance ID + identical action list) and keeps
edge-detection state, so an alarm that already fired doesn't re-fire on save.

### Built-in triggers

| Trigger | Condition |
|---|---|
| Energy level | Network energy % above/below a threshold |
| Audio state | Targeted station(s) playing / stopped |
| Interval | Every N game ticks (game-time based, so separate computers stay in sync) |
| Item count | Items matching an item filter in one accessor (or all combined) below/above a threshold; counts via virtual inventories, so it works in unloaded chunks |

### Built-in actions

| Action | Effect |
|---|---|
| Wait (ticks) | Non-blocking delay; survives restarts |
| Play audio | Start a sound (or each station's configured audio) on station(s) — same-tick start keeps multiple stations synced |
| Stop audio | Stop station(s); "All stations" = network-wide mute |
| Wait for audio | Hold the sequence until the targeted station(s) finish — the playlist building block |
| Log message | Write to the computer's Log tab (notifications/debugging) |
| Send items | Ship filter-matching items to a target accessor via `ItemTransferService` (the Transfer tab's path: energy billing, NBT filters, parcel trails). Fixed-amount mode, or top-up mode that only sends what the target is missing. Sends partially on a shortfall; quantities clamp to what the target can hold; items in flight count as delivered (no over-send/overflow on fast re-fires); the auto source never picks the target accessor or any accessor reading the target's container. Console logging and stop-on-failure (whether a failed send aborts the sequence) are per-action toggleable, both on by default |

### Recipes

- **Low-energy alarm**: Energy level (below 25%, Once) → Play audio (alarm on all stations) → Log message.
- **Playlist**: Interval (period 1, Continuous, overlap off) → Play track A → Wait for audio → Play track B → Wait for audio. The sequence ends, the trigger re-fires, the playlist loops. Use non-looping sounds and stations with auto play off.
- **Power-down mute**: Energy level (below 5%, Once) → Stop audio (all stations).
- **Auto-restock**: Item count (supply chest below 200 cobblestone, Once, overlap off) → Send items
  (top up to 200 cobblestone, source Auto, target supply chest) → Log message. The Once mode re-arms
  when the chest is refilled above the threshold; top-up mode makes re-fires idempotent.

### Item filters (NBT-aware matching)

Item triggers/actions (and the Transfer tab) select items through `networks.filter.ItemFilter`:
an item ID plus an NBT mode — **Any** (ignore NBT), **Exact** (match one captured tag; handles
arbitrary NBT), or **Rules** (numeric conditions like "energy ≥ 50%" over properties registered
in `NbtPropertyRegistry`; built-ins: stored energy, durability). This is how unstackable items
with unique NBT (e.g. kinetic crystal shards) are picked deterministically. Transfers larger
than one stack ship as a **parcel trail**: the first parcel leaves immediately, the rest follow
every 0.5 s (`ParcelTrail` in `ItemNetwork`).

### Audio station modes

`AudioStationNode` no longer has a redstone mode. Instead:
- **Auto play ON** — the station keeps its configured audio going by itself (plays on load /
  config apply; if looping, restarts after interruptions like power loss). Old saves migrate
  automatically (`RedstoneMode` → inverted `AutoPlay`).
- **Auto play OFF** — fully passive; only triggers (or the Audio tab's Play/Stop buttons)
  control it. This is the mode for trigger-driven setups.

Losing network energy always stops playback. Players entering a station's radius now join
mid-playback at the correct offset (looping included, pitch-corrected) instead of hearing
the audio restart.

---

## Quick start: configuring automation in code

This is what a UI would do under the hood. Example: *"When network energy drops below 25%, wait
100 ticks, then do it again only after energy has recovered above the threshold."*

```java
ComputerNode computer = ...;
TriggerManager manager = computer.getTriggerManager();

// 1. Create and configure a trigger
EnergyLevelTrigger trigger = TriggerRegistry.ENERGY_LEVEL.create();
trigger.setThresholdPercent(25.0D);
trigger.setComparison(EnergyLevelTrigger.Comparison.BELOW);
trigger.setMode(Trigger.ExecutionMode.ONCE_UNTIL_FALSE); // fire once per "dip", re-arm when false
trigger.setAllowOverlap(false); // don't start a new run while one is still waiting

// 2. Add actions, in execution order
WaitTicksAction wait = ActionRegistry.WAIT_TICKS.create();
wait.setWaitTicks(100);
trigger.addAction(wait);
// trigger.addAction(...more actions, they run sequentially...);

// 3. Hand it to the manager — it now evaluates every tick
manager.addTrigger(trigger);

// Persist the change through the node's normal dirty flow
computer.setNetworkDirty();
```

Removing: `manager.removeTrigger(trigger)` — this also aborts any of its still-running sequences.

The engine only runs while the computer is **powered**, **installed**, and on the **server side**
(see `ComputerNode#tickTriggers()`).

---

## Registering a new Trigger type

1. **Subclass `Trigger`** in `types/`. Implement `evaluate` and the four serialization hooks for
   your own config fields (the base class already handles mode, overlap, edge state, and the
   action list):

```java
public class RedstoneSignalTrigger extends Trigger {
    private int minSignal = 1;

    public RedstoneSignalTrigger() {
        super(TriggerRegistry.REDSTONE_SIGNAL); // your registered type
    }

    @Override
    public boolean evaluate(TriggerContext ctx) {
        return ctx.getLevel().getBestNeighborSignal(ctx.getBlockPos()) >= minSignal;
    }

    @Override protected void saveExtra(CompoundTag tag) { tag.putInt("minSignal", minSignal); }
    @Override protected void loadExtra(CompoundTag tag) { minSignal = tag.getInt("minSignal"); }
    @Override protected void writeExtraSyncData(FriendlyByteBuf buf) { buf.writeVarInt(minSignal); }
    @Override protected void readExtraSyncData(FriendlyByteBuf buf) { minSignal = buf.readVarInt(); }
}
```

2. **Register it** as a static constant in `TriggerRegistry`, next to `ENERGY_LEVEL`:

```java
public static final TriggerType<RedstoneSignalTrigger> REDSTONE_SIGNAL = register(
        new TriggerType<>(new ResourceLocation(Constants.MOD_ID, "redstone_signal"), RedstoneSignalTrigger::new));
```

That's it. Serialization, network sync, and reconstruction-by-ID all work automatically because
the base `Trigger.save()/writeSyncData()` writes the type ID first and `Trigger.createFromTag()/
createFromBuf()` looks the factory up in the registry.

If your trigger needs world data that `TriggerContext` doesn't expose yet, prefer adding a
snapshot field to `TriggerContext.capture()` over reading the world directly in `evaluate` — that
keeps all triggers of one tick observing a consistent state.

## Registering a new Action type

Same pattern, but subclass `TriggerAction` in `actions/` and register in `ActionRegistry`.
The one important rule:

> **Never store runtime progress in fields of the action.**
> Config (the "what") goes in fields. Progress (the "how far") goes in
> `runCtx.getActionState()` — a `CompoundTag` owned by the running sequence.

Why: the same configured action instance backs *every* run of its trigger. If overlap is enabled,
two runs may execute the same action object simultaneously (logically). The state tag is also what
gets saved to disk, which is how a half-finished delay survives a server restart.

```java
@Override
public ExecuteResult execute(TriggerContext ctx, ActionExecutionContext runCtx) {
    CompoundTag state = runCtx.getActionState(); // fresh & empty the first time this action runs

    // do work...
    // return SUCCESS  -> sequence advances to the next action immediately (same tick)
    // return HOLD     -> sequence stays here, execute() is called again next tick
    // return FAIL     -> whole sequence is aborted
}
```

`WaitTicksAction` is the reference implementation of a multi-tick (`HOLD`-based) action.
Single-shot actions (play a sound, log a message, toggle a block) just do their thing and return
`SUCCESS`.

---

## Class guide

### `TriggerManager` (core)
The engine. One per `ComputerNode`, owns the configured `List<Trigger>` and the live
`List<ActionSequenceTracker>`. Its `tick(ctx)` does two passes:

1. **Evaluate** every trigger. Whether a firing spawns a new sequence depends on the trigger's
   `ExecutionMode` and `allowOverlap` (a trigger with overlap disabled is skipped while it still
   has a running sequence).
2. **Advance** every active tracker; finished/failed ones are removed.

Important consequence of the two-pass order: a freshly spawned sequence starts executing **the
same tick** it was triggered.

### `Trigger` (core, abstract)
A condition plus its action list plus firing policy:

| Mode | Fires |
|---|---|
| `ONCE_UNTIL_FALSE` | On the false→true edge only; re-arms once the condition is false again. |
| `CONTINUOUS` | Every tick the condition is true. |

`wasActiveLastTick` is the edge-detection state — it is **persisted to disk** so a trigger that
was already "true" before a restart doesn't spuriously re-fire on boot. Each trigger also carries
a stable `instanceId` (UUID) used to re-link persisted sequences after a restart.

Heads-up on `CONTINUOUS` + `allowOverlap=true`: that spawns a new sequence *every tick* the
condition holds. It's valid, but it's the configuration to be careful with in a UI.

### `TriggerAction` (core, abstract)
One step of a sequence. Stateless at runtime (see the rule above). `ExecuteResult` is the entire
control-flow contract: `SUCCESS` advances, `HOLD` parks, `FAIL` aborts.

### `ActionSequenceTracker` (core)
One live run of a trigger's action list: the current action index + the current action's scratch
`CompoundTag`. It implements `ActionExecutionContext`, which is the read/write view actions get.
Within one tick it chains through as many consecutive `SUCCESS`es as possible, so a sequence of
five instant actions completes in a single tick; only `HOLD` yields to the next tick. When the
index advances, the scratch tag is replaced with a fresh empty one.

On load, a tracker is matched back to its trigger via the trigger's `instanceId`; if the trigger
was deleted in the meantime, the orphaned tracker is silently dropped.

### `TriggerContext` (core)
Immutable per-tick snapshot, built once in `ComputerNode#tickTriggers()` via
`TriggerContext.capture(node, level)`. Exposes the node, `ServerLevel`, `BlockPos`, game time, and
energy network stats (`getEnergyPercent()` etc.). Every trigger and action of one tick receives
the **same** instance — conditions are judged against one consistent world state.

### `TriggerRegistry` / `ActionRegistry` (registry)
Plain static registries: `ResourceLocation → TriggerType / ActionType` (id + factory). `getAll()`
returns registration order, handy for UI listings. `getOrThrow` is what deserialization uses — an
unknown ID in a save throws rather than silently corrupting.

---

## Serialization & persistence notes

- Conventions match the rest of the mod: `saveExtra`/`loadExtra` for NBT,
  `writeExtraSyncData`/`readExtraSyncData` for packets. `ComputerNode` delegates to the manager in
  all four; on disk everything sits under an `"automation"` sub-tag of the node (missing in old
  saves → loads as empty, no migration needed).
- **List elements are written type-ID-first**, so `Trigger.createFromTag(tag)` /
  `TriggerAction.createFromBuf(buf)` can reconstruct the concrete subclass via the registry.
- **Running sequences are persisted too** (trigger UUID + action index + scratch tag). A "Wait 100
  ticks" that was 60 ticks in resumes with 40 to go after a restart.
- Network sync includes both trigger configs and sequence positions, so a client UI can show
  what's configured *and* what's currently running. Sequence execution itself is server-only.
- Subclass hooks don't need to call `super` — the base classes write their common data in the
  final `save()`/`writeSyncData()` wrappers before invoking your hook.
- After mutating configuration at runtime, call `computer.setNetworkDirty()` so the node gets
  saved/synced through its normal flow.
