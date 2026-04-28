package com.restonic4.logistics.screens;

import com.restonic4.logistics.screens.Panel;
import com.restonic4.logistics.screens.PanelScreen;
import com.restonic4.logistics.screens.PanelScreenTheme;
import com.restonic4.logistics.energy.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Debug overlay that shows energy-network statistics.
 *
 * <p>Displays two panels side-by-side (or stacked on narrow windows):
 * <ul>
 *   <li><b>Left</b> — level-wide stats (network count, node count, game tick,
 *       player position).</li>
 *   <li><b>Right</b> — full data for the network whose nearest node is closest
 *       to the player, including a clipped node list with automatic
 *       {@code "… N more"} overflow.</li>
 * </ul>
 *
 * <p>Open with your chosen keybind:
 * <pre>{@code
 * Minecraft.getInstance().setScreen(new EnergyDebugScreen());
 * }</pre>
 *
 * <p>Requires client-side access to {@link EnergyNetworkManager}, which is
 * only available on the integrated server (single-player / LAN).  For a
 * dedicated server, replace {@link #getManagerOrNull} with data received via
 * a custom network packet.
 */
public class EnergyDebugScreen extends PanelScreen {

    // -------------------------------------------------------------------------
    // Theme — energy screens use the default dark-green palette
    // -------------------------------------------------------------------------

    /**
     * The energy-debug theme is the base DEFAULT with no changes.
     * Stored as a named constant so other energy-related screens can reuse it:
     * <pre>{@code
     * super(Component.literal("My Screen"), EnergyDebugScreen.THEME);
     * }</pre>
     */
    public static final PanelScreenTheme THEME = PanelScreenTheme.DEFAULT;

    // -------------------------------------------------------------------------
    // Internal data records — private, because callers only see the Panel API
    // -------------------------------------------------------------------------

    private record LevelStats(int totalNetworks, int totalMembers, int totalLoaded) {}

    private record NodeEntry(BlockPos pos, String role, String offlineMode, String extra) {}

    private record NetworkSnapshot(
            UUID   id,
            long   energyBuffer,
            long   maxBuffer,
            float  bufferPct,
            long   stableProd,
            long   stableCons,
            long   netFlow,
            int    memberCount,
            int    loadedCount,
            long   lastSimulatedTick,
            List<NodeEntry> nodes,
            double distanceSq
    ) {}

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public EnergyDebugScreen() {
        super(Component.literal("Energy Debug"), THEME);
    }

    // -------------------------------------------------------------------------
    // PanelScreen contract
    // -------------------------------------------------------------------------

    @Override
    protected List<Panel> buildPanels() {
        Minecraft mc     = Minecraft.getInstance();
        Player    player = mc.player;
        Level     level  = mc.level;

        if (player == null || level == null) {
            // Return a single warning panel instead of crashing
            return List.of(
                    Panel.create("ENERGY DEBUG")
                            .row("Status", "No player / level", theme.colWarn)
            );
        }

        EnergyNetworkManager manager = getManagerOrNull(level);

        return List.of(
                buildLevelPanel(manager, level, player),
                buildNetworkPanel(manager, player)
        );
    }

    // -------------------------------------------------------------------------
    // Panel builders
    // -------------------------------------------------------------------------

    private Panel buildLevelPanel(EnergyNetworkManager manager, Level level, Player player) {
        LevelStats s   = computeLevelStats(manager);
        BlockPos   pp  = player.blockPosition();
        long       tick = level.getGameTime();

        return Panel.create("LEVEL  /  WORLD")
                .row("Networks",    String.valueOf(s.totalNetworks()))
                .row("Total nodes", String.valueOf(s.totalMembers()))
                .row("Loaded nodes",String.valueOf(s.totalLoaded()))
                .gap()
                .row("Game tick",   String.valueOf(tick),                       theme.colNeutral)
                .row("Player pos",  pp.getX() + "  " + pp.getY() + "  " + pp.getZ(),
                        theme.colNeutral);
    }

    private Panel buildNetworkPanel(EnergyNetworkManager manager, Player player) {
        NetworkSnapshot n = findClosestNetwork(manager, player);

        if (n == null) {
            return Panel.create("CLOSEST NETWORK")
                    .row("Status", "No networks in level", theme.colWarn);
        }

        // Format the node list into display strings
        List<String> nodeLines = formatNodeLines(n.nodes());

        // Net-flow colour
        int flowCol = n.netFlow() > 0 ? theme.colGood
                : n.netFlow() < 0 ? theme.colBad
                : theme.colLabel;

        String flowStr = (n.netFlow() >= 0 ? "+" : "") + n.netFlow() + " EU/t";

        return Panel.create("CLOSEST NETWORK")
                .row("ID",          n.id().toString().substring(0, 8) + "…", theme.colLabel)
                .row("Distance",    String.format("%.1f blocks", Math.sqrt(n.distanceSq())),
                        theme.colNeutral)
                .gap()
                .bar("Buffer",
                        n.energyBuffer(), n.maxBuffer(), n.bufferPct(),
                        formatEnergy(n.energyBuffer()) + " / " + formatEnergy(n.maxBuffer()))
                .gap()
                .row("Net flow",    flowStr,                                  flowCol)
                .row("Production",  "+" + n.stableProd() + " EU/t (stable)", theme.colGood)
                .row("Consumption", "-" + n.stableCons() + " EU/t (stable)", theme.colBad)
                .gap()
                .row("Members",     n.memberCount() + " total  /  " + n.loadedCount() + " loaded")
                .row("Last sim tick", String.valueOf(n.lastSimulatedTick()),  theme.colNeutral)
                .gap()
                .list("Nodes (" + n.nodes().size() + ")", nodeLines, theme.colValue);
    }

    // -------------------------------------------------------------------------
    // Node line formatting
    // -------------------------------------------------------------------------

    /**
     * Converts the structured {@link NodeEntry} list into plain display strings
     * ready for {@link Panel#list}.
     *
     * <p>Format per line:
     * <pre>[R] x y z  &lt;extra&gt;  [offlineMode]</pre>
     * where {@code R} is the first letter of the role.
     */
    private List<String> formatNodeLines(List<NodeEntry> nodes) {
        List<String> lines = new ArrayList<>(nodes.size());

        for (NodeEntry node : nodes) {
            StringBuilder sb = new StringBuilder();

            // Role tag
            sb.append('[').append(node.role().charAt(0)).append("] ");

            // Position
            sb.append(node.pos().getX()).append(' ')
                    .append(node.pos().getY()).append(' ')
                    .append(node.pos().getZ());

            // Optional extra info
            if (node.extra() != null && !node.extra().isEmpty()) {
                sb.append("  §7").append(node.extra()).append("§r");
            }

            // Offline mode — right-aligned via spaces would need custom entry;
            // for the plain-string list we append it in brackets
            if (node.offlineMode() != null) {
                sb.append("  §8[").append(node.offlineMode()).append("]§r");
            }

            lines.add(sb.toString());
        }

        return lines;
    }

    // -------------------------------------------------------------------------
    // Data computation — pure logic, no rendering concerns
    // -------------------------------------------------------------------------

    /** Gets the server-side manager from the integrated server. Returns {@code null} on dedicated. */
    private EnergyNetworkManager getManagerOrNull(Level level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() == null) return null;
        var serverLevel = mc.getSingleplayerServer().getLevel(level.dimension());
        if (serverLevel == null) return null;
        return EnergyNetworkManager.get(serverLevel);
    }

    private LevelStats computeLevelStats(EnergyNetworkManager manager) {
        if (manager == null) return new LevelStats(0, 0, 0);
        int nets   = 0;
        int total  = 0;
        int loaded = 0;
        Collection<EnergyNetwork> networks = manager.getAllNetworks();
        try {
            networks = new ArrayList<>(networks.stream().toList());
        } catch (ConcurrentModificationException e) {
            networks = Collections.emptyList();
        }
        for (EnergyNetwork net : networks) {
            nets++;
            total  += net.getMemberCount();
            loaded += net.getLoadedNodes().size();
        }
        return new LevelStats(nets, total, loaded);
    }

    private NetworkSnapshot findClosestNetwork(EnergyNetworkManager manager, Player player) {
        if (manager == null) return null;

        BlockPos      playerPos = player.blockPosition();
        double        bestDist  = Double.MAX_VALUE;
        EnergyNetwork best      = null;

        Collection<EnergyNetwork> networks = manager.getAllNetworks();
        try {
            networks = new ArrayList<>(networks.stream().toList());
        } catch (ConcurrentModificationException e) {
            networks = Collections.emptyList();
        }

        for (EnergyNetwork net : networks) {
            List<BlockPos> memberPositions = net.getMemberPositions().stream().toList();
            try {
                memberPositions = new ArrayList<>(memberPositions.stream().toList());
            } catch (ConcurrentModificationException e) {
                memberPositions = Collections.emptyList();
            }

            for (BlockPos pos : memberPositions) {
                double d = pos.distSqr(playerPos);
                if (d < bestDist) {
                    bestDist = d;
                    best     = net;
                }
            }
        }

        if (best == null) return null;

        // Build node entry list — loaded nodes first, then unloaded stubs
        List<NodeEntry> entries = new ArrayList<>();

        Set<BlockPos> loadedPositions = new HashSet<>();
        for (EnergyNode node : best.getLoadedNodes()) {
            loadedPositions.add(node.getEnergyPos());
            entries.add(new NodeEntry(
                    node.getEnergyPos(),
                    roleOf(node),
                    offlineModeOf(node),
                    extraOf(node)
            ));
        }

        List<BlockPos> memberPositions = best.getMemberPositions().stream().toList();
        try {
            memberPositions = new ArrayList<>(memberPositions.stream().toList());
        } catch (ConcurrentModificationException e) {
            memberPositions = Collections.emptyList();
        }
        for (BlockPos pos : memberPositions) {
            if (!loadedPositions.contains(pos)) {
                entries.add(new NodeEntry(pos, "UNLOADED", null, null));
            }
        }

        entries.sort(Comparator.comparing(NodeEntry::role));

        float pct = best.getMaxBuffer() > 0
                ? (float) best.getEnergyBuffer() / best.getMaxBuffer()
                : 0f;

        return new NetworkSnapshot(
                best.getId(),
                best.getEnergyBuffer(),
                best.getMaxBuffer(),
                pct,
                best.getStableProductionPerTick(),
                best.getStableConsumptionPerTick(),
                best.getStableProductionPerTick() - best.getStableConsumptionPerTick(),
                best.getMemberCount(),
                best.getLoadedNodes().size(),
                best.getLastSimulatedTick(),
                entries,
                bestDist
        );
    }

    // ---- Role helpers -------------------------------------------------------

    private static String roleOf(EnergyNode node) {
        if (node.isPipe())                  return "PIPE";
        if (node instanceof EnergyStorage)  return "STORAGE";
        if (node instanceof EnergyProducer) return "PRODUCER";
        if (node instanceof EnergyConsumer) return "CONSUMER";
        return "NODE";
    }

    private static String offlineModeOf(EnergyNode node) {
        if (node instanceof EnergyProducer p) {
            return p.getOfflineProducerProfile().getMode().name();
        }
        if (node instanceof EnergyConsumer c) {
            return c.getOfflineConsumerProfile().getMode().name();
        }
        return null;
    }

    private static String extraOf(EnergyNode node) {
        if (node instanceof EnergyStorage s) {
            return formatEnergy(s.getStoredEnergy()) + "/" + formatEnergy(s.getMaxStoredEnergy())
                    + " (" + String.format("%.0f%%", s.getChargePercent() * 100) + ")";
        }
        if (node instanceof EnergyProducer p) {
            OfflineEnergyProfile profile = p.getOfflineProducerProfile();
            if (profile.getMode() == OfflineEnergyProfile.Mode.STABLE) {
                return "+" + profile.getRatePerTick() + " EU/t";
            }
        }
        if (node instanceof EnergyConsumer c) {
            return "-" + c.getMaxConsumptionPerTick() + " EU/t";
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Formatting utilities
    // -------------------------------------------------------------------------

    private static String formatEnergy(long eu) {
        if (eu >= 1_000_000) return String.format("%.2fMEU", eu / 1_000_000.0);
        if (eu >= 1_000)     return String.format("%.1fkEU", eu / 1_000.0);
        return eu + " EU";
    }
}