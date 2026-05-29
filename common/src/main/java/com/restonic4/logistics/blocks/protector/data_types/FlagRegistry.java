package com.restonic4.logistics.blocks.protector.data_types;

import java.util.*;

public final class FlagRegistry {
    private static final List<FlagDefinition> FLAGS = new ArrayList<>();
    private static final Map<String, FlagDefinition> BY_ID = new HashMap<>();

    public static void init() {
        // STANDARD
        register("pvp", "PvP", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("break_blocks", "Break Blocks", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("place_blocks", "Place Blocks", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("open_containers", "Open Containers", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("block_interaction", "Block Interaction", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("use_buckets", "Use Buckets", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("trample_crops", "Trample Crops", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("entity_interaction", "Entity Interaction", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("attack_entities", "Attack Entities", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("villager_trade", "Villager Trade", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("fire_tick", "Fire Tick", List.of(ActionType.DENY), FlagDefinition.FlagCategory.STANDARD);
        register("explosions", "Explosions", List.of(ActionType.DENY), FlagDefinition.FlagCategory.STANDARD);
        register("walk_in", "Walk In", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("item_pickup", "Item Pickup", List.of(ActionType.DENY), FlagDefinition.FlagCategory.STANDARD);
        register("item_drop", "Item Drop", List.of(ActionType.DENY), FlagDefinition.FlagCategory.STANDARD);
        register("mob_grief", "Mob Grief", List.of(ActionType.DENY), FlagDefinition.FlagCategory.STANDARD);
        register("ender_pearl", "Ender Pearl", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("chorus_fruit", "Chorus Fruit", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);
        register("sneaking", "Sneaking", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE), FlagDefinition.FlagCategory.STANDARD);

        // CREATIVE
        register("fire_damage", "Fire Damage", List.of(ActionType.DENY), FlagDefinition.FlagCategory.CREATIVE);
        register("fall_damage", "Fall Damage", List.of(ActionType.DENY), FlagDefinition.FlagCategory.CREATIVE);
        register("hunger", "Hunger", List.of(ActionType.DENY), FlagDefinition.FlagCategory.CREATIVE);
        register("lose_health", "Lose Health", List.of(ActionType.DENY), FlagDefinition.FlagCategory.CREATIVE);
        register("mob_spawning", "Mob Spawning", List.of(ActionType.DENY), FlagDefinition.FlagCategory.CREATIVE);
    }

    private static void register(String id, String name, List<ActionType> actions, FlagDefinition.FlagCategory cat) {
        FlagDefinition def = new FlagDefinition(id, name, actions, cat);
        FLAGS.add(def);
        BY_ID.put(id, def);
    }

    public static List<FlagDefinition> all() { return List.copyOf(FLAGS); }

    public static List<FlagDefinition> forZone(boolean creative) {
        return FLAGS.stream()
                .filter(f -> creative || f.category() == FlagDefinition.FlagCategory.STANDARD)
                .toList();
    }

    public static Optional<FlagDefinition> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}