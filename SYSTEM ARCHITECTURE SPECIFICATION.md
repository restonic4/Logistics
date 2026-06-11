### SYSTEM ARCHITECTURE SPECIFICATION & CODE GENERATION PROMPT

**Target Environment:** Java 17, Minecraft 1.20.1 (Fabric), Mojang Mappings, NO Fabric API.
**Target Package:** `com.restonic4.logistics.blocks.computer.automation`
**Concurreny Model:** Strictly single-threaded logical concurrency via the Minecraft main server tick loop (`ComputerNode#tick()`). No async Java threads.

---

### 1. OVERVIEW & GOAL
Implement a robust, highly modular, scalable, and professional Trigger and Action system for a custom `ComputerNode`. Users can configure multiple Triggers via a UI (data-only for this prompt). Each Trigger evaluates conditions on every game tick. When conditions are met, it executes an ordered list of Actions. Actions can include delays (e.g., "Wait 50 ticks"), which must run sequentially without blocking the game tick, other triggers, or subsequent evaluations of the same trigger if configured to overlap.

---

### 2. CORE CLASS REGISTRY PATHS & ARCHITECTURE
All components must follow this explicit package and directory structure. You must use static registries for types.

* `...computer.automation.triggers.core`: Base system architecture (`Trigger`, `TriggerAction`, `TriggerManager`, `TriggerContext`).
* `...computer.automation.triggers.types`: Built-in trigger implementations.
* `...computer.automation.triggers.actions`: Built-in action implementations.
* `...computer.automation.triggers.registry`: Static registries for `TriggerType<?>` and `ActionType<?>`.

---

### 3. DATA MODEL & COMPONENT SPECIFICATIONS

#### A. The Registries (`TriggerRegistry` & `ActionRegistry`)
* Create static registry classes to hold available `TriggerType` and `ActionType` definitions.
* Each type must have a unique `ResourceLocation` identifier.
* Provide a method to look up a type by its ID for serialization purposes.

#### B. Trigger Types (`TriggerType<T extends Trigger>`) and Triggers
* An abstract base `Trigger` class containing:
    * An execution mode enum: `ONCE_UNTIL_FALSE` (fires once when conditions go from false to true, resets when false) or `CONTINUOUS` (fires every tick conditions are true).
    * A state variable tracking whether it was active last tick (for `ONCE_UNTIL_FALSE`).
    * A `List<TriggerAction>` representing the configured sequential actions.
* Abstract methods to implement:
    * `boolean evaluate(TriggerContext ctx)`: Evaluates if conditions are met.
    * Serialization methods (see Section 5).

#### C. Action Types (`ActionType<A extends TriggerAction>`) and Actions
* An abstract base `TriggerAction` class.
* Must support a non-blocking delay mechanism. Provide an abstract method:
    * `ExecuteResult execute(TriggerContext ctx, ActionExecutionContext runCtx)`
* `ExecuteResult` is an Enum: 
    * `SUCCESS`: Action finished; proceed to the next action immediately.
    * `HOLD`: Action is waiting (e.g., a timer delay); do not proceed yet.
    * `FAIL`: Sequence aborted due to an error.

---

### 4. THE TICK & LOGICAL CONCURRENCY ENGINE

#### A. TriggerContext
* Instantiated every game tick inside `ComputerNode#tickTriggers()`.
* Must securely wrap references to the executing `ComputerNode`, the `ServerLevel`, the `BlockPos`, and localized node snapshots (such as current energy percentage or network stats) to feed trigger conditions.

#### B. ActionExecutionContext (State Snapshot)
* When a trigger fires, it spawns an active instance of an `ActionSequenceTracker`.
* This tracker maintains:
    * The index of the currently executing action.
    * A local mutable `CompoundTag` or integer counter for state tracking (e.g., tracking remaining wait ticks for a "Wait" action).

#### C. TriggerManager
* Owned directly by `ComputerNode`.
* Contains a `List<Trigger>` configured by the user.
* Contains a `List<ActionSequenceTracker>` tracking currently executing action lists.
* **Tick Behavior (`tick(TriggerContext ctx)`)**:
    1. Loop through all configured `Trigger` instances and run `evaluate(ctx)`.
    2. Based on the trigger's evaluation mode (`ONCE_UNTIL_FALSE` vs `CONTINUOUS`), determine if a new action sequence should be spawned. If yes, instantiate a new `ActionSequenceTracker` and add it to the active sequence tracking list.
    3. Process all active `ActionSequenceTracker` instances:
        * Get the current action by index.
        * Call `action.execute(ctx, tracker)`.
        * If `SUCCESS`, advance the index. If index exceeds the list size, remove the tracker from processing.
        * If `HOLD`, keep the tracker at the current index and process again next tick.

---

### 5. SERIALIZATION COMPLIANCE (NBT & NET)
Every `Trigger`, `TriggerAction`, and the `TriggerManager` itself must fully implement precise disk and network serialization methods. Use these exact naming conventions for consistency with `ComputerNode`:

#### For Disk Storage (NBT CompoundTag)
* `void saveExtra(CompoundTag tag)`
* `void loadExtra(CompoundTag tag)`
* *Note*: When saving list elements, write the `ResourceLocation` type ID first, so the factory can reconstruct the correct instance type during `loadExtra`. Active delayed action states must also be saved to disk so delays persist through server restarts!

#### For Network Synchronization (FriendlyByteBuf Buffer)
* `void writeExtraSyncData(FriendlyByteBuf buf)`
* `void readExtraSyncData(FriendlyByteBuf buf)`

---

### 6. IMPLEMENTATION REQUIREMENTS & DELIVERABLES
1. Do not use any third-party library or Fabric API features (no `FabricBlockEntityRefresher`, etc.). Use native vanilla/mojmap components.
2. Code must be highly clean, completely documented, and strongly typed. Avoid raw-casting generic types.
3. Provide a clear boilerplate implementation of a "Wait X Ticks" Action and an "Energy Level Trigger" to verify your architecture framework functions properly.
4. Provide a zip file to download easily.
