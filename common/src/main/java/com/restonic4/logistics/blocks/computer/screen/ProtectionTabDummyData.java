package com.restonic4.logistics.blocks.computer.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

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

    public static class ProtectionFlag {
        public final String id;
        public final String name;
        public final List<ActionType> supportedActions;

        public ProtectionFlag(String id, String name, List<ActionType> supportedActions) {
            this.id = id;
            this.name = name;
            this.supportedActions = new ArrayList<>(supportedActions);
        }
    }

    public static final List<ProtectionFlag> FLAGS = new ArrayList<>();
    public static final List<ProtectionNode> NODES = new ArrayList<>();
    public static final Map<UUID, List<Role>> NODE_ROLES = new HashMap<>();

    // Global player pool for assignment dropdown
    public static final Map<UUID, String> ALL_PLAYERS = new LinkedHashMap<>();

    static {
        initFlags();
        initNodes();
        initAllPlayers();
        initRoles();
        initPlayers();
    }

    private static void initFlags() {
        FLAGS.add(new ProtectionFlag("break_blocks", "Break Blocks", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("place_blocks", "Place Blocks", List.of(ActionType.DENY, ActionType.DAMAGE, ActionType.MESSAGE)));
        FLAGS.add(new ProtectionFlag("interact", "Interact", List.of(ActionType.DENY, ActionType.DAMAGE)));
        FLAGS.add(new ProtectionFlag("open_containers", "Open Containers", List.of(ActionType.DENY, ActionType.DAMAGE)));
        FLAGS.add(new ProtectionFlag("attack_entities", "Attack Entities", List.of(ActionType.DENY, ActionType.DAMAGE)));
        FLAGS.add(new ProtectionFlag("mob_spawning", "Mob Spawning", List.of(ActionType.DENY)));
        FLAGS.add(new ProtectionFlag("explosions", "Explosions", List.of(ActionType.DENY)));
        FLAGS.add(new ProtectionFlag("fire_spread", "Fire Spread", List.of(ActionType.DENY)));
        FLAGS.add(new ProtectionFlag("pvp", "PvP", List.of(ActionType.DENY, ActionType.DAMAGE)));
        FLAGS.add(new ProtectionFlag("sneaking", "Sneaking", List.of(ActionType.DENY, ActionType.DAMAGE)));
        FLAGS.add(new ProtectionFlag("item_pickup", "Item Pickup", List.of(ActionType.DENY)));
        FLAGS.add(new ProtectionFlag("item_drop", "Item Drop", List.of(ActionType.DENY)));
    }

    private static void initNodes() {
        NODES.add(new ProtectionNode(UUID.fromString("11111111-1111-1111-1111-111111111111"), "Base Protection Core", new BlockPos(0, 64, 0)));
        NODES.add(new ProtectionNode(UUID.fromString("22222222-2222-2222-2222-222222222222"), "North Outpost", new BlockPos(150, 70, -300)));
        NODES.add(new ProtectionNode(UUID.fromString("33333333-3333-3333-3333-333333333333"), "Storage Facility Alpha", new BlockPos(-80, 65, 120)));
    }

    private static void initAllPlayers() {
        ALL_PLAYERS.put(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "Notch");
        ALL_PLAYERS.put(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Jeb_");
        ALL_PLAYERS.put(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "Dinnerbone");
        ALL_PLAYERS.put(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"), "Grumm");
        ALL_PLAYERS.put(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"), "Steve");
        ALL_PLAYERS.put(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "Alex");
        ALL_PLAYERS.put(UUID.fromString("11111111-2222-3333-4444-555555555555"), "Herobrine");
        ALL_PLAYERS.put(UUID.fromString("66666666-6666-6666-6666-666666666666"), "CreeperKing");
    }

    private static void initRoles() {
        for (ProtectionNode node : NODES) {
            List<Role> roles = new ArrayList<>();

            Map<String, FlagState> ownerFlags = new HashMap<>();
            for (ProtectionFlag f : FLAGS) ownerFlags.put(f.id, new FlagState(true, ActionType.DENY, 0, ""));
            roles.add(new Role(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Owner", 0, new ResourceLocation("logistics", "textures/item/parcel.png"), ownerFlags));

            Map<String, FlagState> adminFlags = new HashMap<>();
            for (ProtectionFlag f : FLAGS) adminFlags.put(f.id, new FlagState(true, ActionType.DENY, 0, ""));
            adminFlags.get("mob_spawning").enabled = false;
            adminFlags.get("explosions").enabled = false;
            adminFlags.get("fire_spread").enabled = false;
            roles.add(new Role(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Admin", 1, new ResourceLocation("logistics", "textures/item/parcel.png"), adminFlags));

            Map<String, FlagState> builderFlags = new HashMap<>();
            for (ProtectionFlag f : FLAGS) builderFlags.put(f.id, new FlagState(false, ActionType.DENY, 0, ""));
            builderFlags.get("break_blocks").enabled = true;
            builderFlags.get("place_blocks").enabled = true;
            builderFlags.get("interact").enabled = true;
            roles.add(new Role(UUID.fromString("00000000-0000-0000-0000-000000000003"), "Builder", 2, new ResourceLocation("logistics", "textures/item/parcel.png"), builderFlags));

            Map<String, FlagState> visitorFlags = new HashMap<>();
            for (ProtectionFlag f : FLAGS) visitorFlags.put(f.id, new FlagState(false, ActionType.DENY, 0, ""));
            visitorFlags.get("interact").enabled = true;
            roles.add(new Role(UUID.fromString("00000000-0000-0000-0000-000000000004"), "Visitor", 3, new ResourceLocation("logistics", "textures/item/parcel.png"), visitorFlags));

            Map<String, FlagState> prisonerFlags = new HashMap<>();
            for (ProtectionFlag f : FLAGS) prisonerFlags.put(f.id, new FlagState(true, ActionType.DAMAGE, 2.0, ""));
            roles.add(new Role(UUID.fromString("00000000-0000-0000-0000-000000000005"), "Prisoner", 4, new ResourceLocation("logistics", "textures/item/parcel.png"), prisonerFlags));

            NODE_ROLES.put(node.id, roles);
        }
    }

    private static void initPlayers() {
        for (List<Role> roles : NODE_ROLES.values()) {
            if (roles.size() >= 5) {
                roles.get(0).players.add(new AssignedPlayer(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "Notch"));
                roles.get(0).players.add(new AssignedPlayer(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Jeb_"));
                roles.get(1).players.add(new AssignedPlayer(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "Dinnerbone"));
                roles.get(1).players.add(new AssignedPlayer(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"), "Grumm"));
                roles.get(2).players.add(new AssignedPlayer(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"), "Steve"));
                roles.get(2).players.add(new AssignedPlayer(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "Alex"));
                roles.get(3).players.add(new AssignedPlayer(UUID.fromString("11111111-2222-3333-4444-555555555555"), "Herobrine"));
                roles.get(4).players.add(new AssignedPlayer(UUID.fromString("66666666-6666-6666-6666-666666666666"), "CreeperKing"));
            }
        }
    }

    public static List<Role> getRolesForNode(UUID nodeId) {
        return NODE_ROLES.getOrDefault(nodeId, new ArrayList<>());
    }
}