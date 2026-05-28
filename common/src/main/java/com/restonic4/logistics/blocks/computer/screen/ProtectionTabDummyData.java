package com.restonic4.logistics.blocks.computer.screen;

import com.mojang.authlib.GameProfile;
import com.restonic4.logistics.blocks.computer.protection.ProtectionSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class ProtectionTabDummyData {
    public enum ActionType {
        DENY, DAMAGE, MESSAGE
    }

    public static class ProtectionNode {
        public final UUID id;
        public String name;
        public final BlockPos pos;

        public ProtectionNode(UUID id, String name, BlockPos pos) {
            this.id = id;
            this.name = name;
            this.pos = pos;
        }
    }

    public static class AssignedPlayer {
        public final UUID id;
        public final String username;

        public AssignedPlayer(UUID id, String username) {
            this.id = id;
            this.username = username;
        }
    }

    public static class FlagState {
        public boolean enabled;
        public ActionType action;
        public double damageValue;
        public String message;

        public FlagState(boolean enabled, ActionType action, double damageValue, String message) {
            this.enabled = enabled;
            this.action = action;
            this.damageValue = damageValue;
            this.message = message;
        }

        public FlagState copy() {
            return new FlagState(enabled, action, damageValue, message);
        }
    }

    public static class Role {
        public final UUID id;
        public String name;
        public int order;
        public final ResourceLocation icon;
        public final Map<String, FlagState> flags;
        public final List<AssignedPlayer> players;

        public Role(UUID id, String name, int order, ResourceLocation icon, Map<String, FlagState> flags) {
            this.id = id;
            this.name = name;
            this.order = order;
            this.icon = icon;
            this.flags = new HashMap<>();
            for (Map.Entry<String, FlagState> entry : flags.entrySet()) {
                this.flags.put(entry.getKey(), entry.getValue().copy());
            }
            this.players = new ArrayList<>();
        }
    }

    public record NodeCache(int radius, UUID id) {}

    public static class ProtectionFlag {
        public final String id;
        public final String name;
        public final List<ActionType> supportedActions;
        public final boolean isOP;

        public ProtectionFlag(String id, String name, List<ActionType> supportedActions) {
            this.id = id;
            this.name = name;
            this.supportedActions = new ArrayList<>(supportedActions);
            this.isOP = false;
        }

        public ProtectionFlag(String id, String name, List<ActionType> supportedActions, boolean isOP) {
            this.id = id;
            this.name = name;
            this.supportedActions = new ArrayList<>(supportedActions);
            this.isOP = isOP;
        }
    }

    public static final List<ProtectionFlag> FLAGS = new ArrayList<>();
    public static final List<ProtectionNode> NODES = new ArrayList<>();
    public static final List<Integer> RADIUS = new ArrayList<>();
    public static final Map<UUID, List<Role>> NODE_ROLES = new HashMap<>();

    public static final Map<ResourceLocation, Map<ReworkThisNonsense, List<Role>>> REPLICATED_CACHES = new HashMap<>();

    public record ReworkThisNonsense(BlockPos pos, int radius) {}

    // Global player pool for assignment dropdown
    public static final List<GameProfile> ALL_PLAYERS = new ArrayList<>();

    static {
        initFlags();
    }

    /**
     * Core method to check the state of any protection flag at a given position.
     * Handles both player-driven actions and environmental/non-player events.
     * * @param dimension The dimension registry location (e.g., player.level().dimension().location())
     * @param targetPos The block position where the action is happening
     * @param player    The player performing the action, or null for non-player events (explosions, fire spread, etc.)
     * @param flagId    The ID of the flag to check (e.g., "pvp", "fire_spread", "break_blocks")
     * @return The active FlagState, or null if the position is unprotected (outside any claims).
     */
    public static FlagState getFlagState(ResourceLocation dimension, BlockPos targetPos, Player player, String flagId) {
        Map<ReworkThisNonsense, List<Role>> zones = REPLICATED_CACHES.get(dimension);
        if (zones == null || zones.isEmpty()) {
            return null; // Outside any protection zones -> unrestricted global area
        }

        for (Map.Entry<ReworkThisNonsense, List<Role>> entry : zones.entrySet()) {
            ReworkThisNonsense zone = entry.getKey();
            BlockPos center = zone.pos();
            int radius = zone.radius();

            // Check if the target position falls within this zone's radius (Euclidean distance)
            double distSqr = targetPos.distSqr(center);
            if (distSqr <= (double) radius * radius) {
                List<Role> roles = entry.getValue();

                // CASE 1: Player-driven action
                if (player != null) {
                    UUID playerUuid = player.getUUID();

                    // Iterate roles in order (Index 0 is highest priority, like Discord)
                    for (Role role : roles) {
                        boolean playerHasRole = false;
                        for (AssignedPlayer ap : role.players) {
                            if (ap.id.equals(playerUuid)) {
                                playerHasRole = true;
                                break;
                            }
                        }

                        // If the player belongs to this high-priority role, return its flag state
                        if (playerHasRole) {
                            FlagState state = role.flags.get(flagId);
                            if (state != null) {
                                return state;
                            }
                        }
                    }

                    // If we are inside a protected zone and the player has NO assigned roles:
                    // "if a player is not present, he is completely rejected at all"
                    return new FlagState(true, ActionType.DENY, 0, "You do not have permission here.");
                }

                // CASE 2: Non-player/Environmental action (fire spread, explosions, mob grief, etc.)
                else {
                    // Scan roles from highest to lowest priority to find the first one configuring this environmental flag
                    for (Role role : roles) {
                        FlagState state = role.flags.get(flagId);
                        if (state != null) {
                            return state;
                        }
                    }
                }
            }
        }

        return null; // Position is inside the dimension but not inside any specific claim radius
    }

    /**
     * Quick helper to check if an action is allowed outright.
     * @return true if allowed (outside claims or explicitly allowed), false if denied/penalized.
     */
    public static boolean isActionAllowed(ResourceLocation dimension, BlockPos targetPos, Player player, String flagId) {
        FlagState state = getFlagState(dimension, targetPos, player, flagId);
        if (state == null) {
            return true; // Unprotected wilderness is always allowed
        }

        return !state.enabled || state.action != ActionType.DENY;
    }



    private static void initFlags() {
        FLAGS.add(new ProtectionFlag("pvp", "PvP", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("break_blocks", "Break Blocks", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("place_blocks", "Place Blocks", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("open_containers", "Open Containers", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("block_interaction", "Block Interaction", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("use_buckets", "Use Buckets", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("trample_crops", "Trample Crops", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("entity_interaction", "Entity Interaction", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("attack_entities", "Attack Entities", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("villager_trade", "Villager Trade", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("fire_spread", "Fire Spread", List.of(ActionType.DENY)));
        FLAGS.add(new ProtectionFlag("explosions", "Explosions", List.of(ActionType.DENY)));
        FLAGS.add(new ProtectionFlag("walk_in", "Walk In", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("item_pickup", "Item Pickup", List.of(ActionType.DENY)));
        FLAGS.add(new ProtectionFlag("item_drop", "Item Drop", List.of(ActionType.DENY)));
        FLAGS.add(new ProtectionFlag("mob_grief", "Mob Grief", List.of(ActionType.DENY)));
        FLAGS.add(new ProtectionFlag("ender_pearl", "Ender Pearl", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("chorus_fruit", "Chorus Fruit", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("sneaking", "Sneaking", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));

        FLAGS.add(new ProtectionFlag("fire_damage", "Fire Damage", List.of(ActionType.DENY), true));
        FLAGS.add(new ProtectionFlag("fall_damage", "Fall Damage", List.of(ActionType.DENY), true));
        FLAGS.add(new ProtectionFlag("hunger", "Hunger", List.of(ActionType.DENY), true));
        FLAGS.add(new ProtectionFlag("lose_health", "Lose Health", List.of(ActionType.DENY), true));
        FLAGS.add(new ProtectionFlag("mob_spawning", "Mob Spawning", List.of(ActionType.DENY), true));
    }

    public static List<Role> getRolesForNode(UUID nodeId) {
        return NODE_ROLES.getOrDefault(nodeId, new ArrayList<>());
    }

    public static void loadFromPacket(ProtectionSyncPacket packet) {
        NODES.clear();
        RADIUS.clear();
        NODE_ROLES.clear();
        ALL_PLAYERS.clear();

        for (ProtectionSyncPacket.ProtectionNodeData node : packet.nodes()) {
            NODES.add(new ProtectionNode(node.id(), node.name(), node.pos()));
        }

        for (Map.Entry<ProtectionTabDummyData.NodeCache, List<ProtectionSyncPacket.RoleData>> entry : packet.nodeRoles().entrySet()) {
            UUID nodeId = entry.getKey().id();
            int radius = entry.getKey().radius();
            List<Role> roles = new ArrayList<>();

            RADIUS.add(radius);

            for (ProtectionSyncPacket.RoleData rd : entry.getValue()) {
                Map<String, FlagState> flags = new HashMap<>();
                for (Map.Entry<String, ProtectionSyncPacket.FlagData> fe : rd.flags().entrySet()) {
                    ProtectionSyncPacket.FlagData fd = fe.getValue();
                    flags.put(fe.getKey(), new FlagState(
                            fd.enabled(),
                            ActionType.valueOf(fd.actionType()),
                            fd.damageValue(),
                            fd.message()
                    ));
                }

                Role role = new Role(rd.id(), rd.name(), rd.order(),
                        new ResourceLocation(rd.iconRl()), flags);

                for (ProtectionSyncPacket.PlayerData pd : rd.players()) {
                    role.players.add(new AssignedPlayer(pd.id(), pd.username()));
                }

                roles.add(role);
            }

            NODE_ROLES.put(nodeId, roles);
        }

        ALL_PLAYERS.addAll(packet.profiles());
    }
}