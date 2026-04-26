package com.restonic4.logistics;

import com.restonic4.logistics.energy.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * Debug overlay showing:
 *   - Level-wide stats: total networks, total nodes, total loaded nodes
 *   - The single closest network to the player: full data + node list
 *
 * Open with your chosen keybind by calling:
 *   Minecraft.getInstance().setScreen(new EnergyDebugScreen());
 *
 * Requires client-side access to EnergyNetworkManager, meaning this must run
 * on the integrated server (single-player / LAN). For a dedicated server you
 * would need to sync this data via a custom network packet instead.
 */
public class EnergyDebugScreen extends Screen {

    // -------------------------------------------------------------------------
    // Layout constants
    // -------------------------------------------------------------------------

    private static final int PAD          = 12;
    private static final int LINE         = 11;   // px per text line
    private static final int SECTION_GAP  = 6;
    private static final int PANEL_W      = 310;
    private static final int COL2_X       = 160;  // second column x inside panel

    // Colours — 0xAARRGGBB
    private static final int COL_BG       = 0xD0101010;
    private static final int COL_BORDER   = 0xFF2A4A2A;
    private static final int COL_ACCENT   = 0xFF44FF88;
    private static final int COL_LABEL    = 0xFF8FA88F;
    private static final int COL_VALUE    = 0xFFE8FFE8;
    private static final int COL_HEADER   = 0xFFFFFFFF;
    private static final int COL_WARN     = 0xFFFFAA33;
    private static final int COL_GOOD     = 0xFF44FF88;
    private static final int COL_BAD      = 0xFFFF4444;
    private static final int COL_NEUTRAL  = 0xFF88CCFF;
    private static final int COL_PIPE     = 0xFF888888;
    private static final int COL_PRODUCER = 0xFF88FF44;
    private static final int COL_CONSUMER = 0xFFFF6644;
    private static final int COL_STORAGE  = 0xFF44AAFF;
    private static final int COL_DIVIDER  = 0xFF1A3A1A;

    // -------------------------------------------------------------------------
    // Cached data — recomputed each render
    // -------------------------------------------------------------------------

    private record LevelStats(int totalNetworks, int totalMembers, int totalLoaded) {}

    private record NodeEntry(BlockPos pos, String role, String offlineMode, String extra) {}

    private record NetworkSnapshot(
            UUID id,
            long energyBuffer,
            long maxBuffer,
            float bufferPct,
            long stableProd,
            long stableCons,
            long netFlow,
            int memberCount,
            int loadedCount,
            long lastSimulatedTick,
            List<NodeEntry> nodes,
            double distanceSq
    ) {}

    // -------------------------------------------------------------------------

    public EnergyDebugScreen() {
        super(Component.literal("Energy Debug"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // keep the game running while open
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        Minecraft mc   = Minecraft.getInstance();
        Player player  = mc.player;
        Level level    = mc.level;

        if (player == null || level == null) {
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }

        // Pull data from the server-side manager (works for integrated server only).
        // For dedicated servers, replace this with data received via network packet.
        EnergyNetworkManager manager = getManagerOrNull(level);

        LevelStats    lvlStats       = computeLevelStats(manager);
        NetworkSnapshot closestNet   = findClosestNetwork(manager, player);

        int screenH = this.height;
        int y       = PAD;

        // ---- LEFT PANEL: level stats ----------------------------------------
        int panelH  = computeLevelPanelHeight();
        drawPanel(gfx, PAD, y, PANEL_W, panelH);
        y = renderLevelStats(gfx, PAD + PAD, y + PAD, lvlStats, level, player);

        // ---- RIGHT PANEL (or below): closest network ------------------------
        int netPanelY = PAD;
        int netPanelX = PAD + PANEL_W + PAD;

        // If not enough horizontal room, stack below
        if (netPanelX + PANEL_W > this.width) {
            netPanelX = PAD;
            netPanelY = y + SECTION_GAP;
        }

        if (closestNet != null) {
            int netPanelH = computeNetworkPanelHeight(closestNet);
            // Clamp so it doesn't overflow screen bottom
            int maxH      = screenH - netPanelY - PAD;
            netPanelH     = Math.min(netPanelH, maxH);
            drawPanel(gfx, netPanelX, netPanelY, PANEL_W, netPanelH);
            renderNetworkPanel(gfx, netPanelX + PAD, netPanelY + PAD, closestNet, netPanelH - PAD * 2);
        } else {
            drawPanel(gfx, netPanelX, netPanelY, PANEL_W, LINE * 3 + PAD * 2);
            gfx.drawString(mc.font, "§7No networks in level", netPanelX + PAD, netPanelY + PAD, COL_WARN, false);
        }

        // Dismiss hint
        gfx.drawString(mc.font, "§7[ESC] Close", PAD, screenH - PAD - LINE, COL_LABEL, false);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    // -------------------------------------------------------------------------
    // Level stats panel
    // -------------------------------------------------------------------------

    private int computeLevelPanelHeight() {
        // header + 3 stat rows + player pos row + tick row + padding
        return PAD * 2 + LINE + SECTION_GAP + LINE * 5;
    }

    private int renderLevelStats(GuiGraphics gfx, int x, int y, LevelStats s, Level level, Player player) {
        Minecraft mc = Minecraft.getInstance();

        sectionHeader(gfx, x, y, "LEVEL  /  WORLD");
        y += LINE + SECTION_GAP;

        y = twoCol(gfx, x, y, "Networks",    String.valueOf(s.totalNetworks()), COL_VALUE);
        y = twoCol(gfx, x, y, "Total nodes", String.valueOf(s.totalMembers()),  COL_VALUE);
        y = twoCol(gfx, x, y, "Loaded nodes",String.valueOf(s.totalLoaded()),   COL_VALUE);

        long tick = level.getGameTime();
        y = twoCol(gfx, x, y, "Game tick",   String.valueOf(tick),              COL_NEUTRAL);

        BlockPos pp = player.blockPosition();
        String posStr = pp.getX() + "  " + pp.getY() + "  " + pp.getZ();
        y = twoCol(gfx, x, y, "Player pos",  posStr, COL_NEUTRAL);

        return y;
    }

    // -------------------------------------------------------------------------
    // Network panel
    // -------------------------------------------------------------------------

    private int computeNetworkPanelHeight(NetworkSnapshot n) {
        int rows = 0;
        rows += 1;  // header
        rows += 1;  // gap
        rows += 1;  // id
        rows += 1;  // buffer bar + value
        rows += 1;  // net flow
        rows += 1;  // prod
        rows += 1;  // cons
        rows += 1;  // members / loaded
        rows += 1;  // last sim tick
        rows += 1;  // distance
        rows += 1;  // nodes header
        rows += n.nodes().size();
        rows += 2;  // bottom padding
        return PAD * 2 + rows * LINE + SECTION_GAP * 3;
    }

    private void renderNetworkPanel(GuiGraphics gfx, int x, int y, NetworkSnapshot n, int maxHeight) {
        Minecraft mc   = Minecraft.getInstance();
        int startY     = y;
        int bottomBound = startY + maxHeight;

        sectionHeader(gfx, x, y, "CLOSEST NETWORK");
        y += LINE + SECTION_GAP;

        // UUID (short)
        String shortId = n.id().toString().substring(0, 8) + "…";
        y = twoCol(gfx, x, y, "ID", shortId, COL_LABEL);

        // Distance
        String distStr = String.format("%.1f blocks", Math.sqrt(n.distanceSq()));
        y = twoCol(gfx, x, y, "Distance", distStr, COL_NEUTRAL);

        // Buffer bar
        y = renderBufferBar(gfx, x, y, n.energyBuffer(), n.maxBuffer(), n.bufferPct());
        y += SECTION_GAP;

        // Net flow
        long net      = n.netFlow();
        int  flowCol  = net > 0 ? COL_GOOD : net < 0 ? COL_BAD : COL_LABEL;
        String flowStr = (net >= 0 ? "+" : "") + net + " EU/t";
        y = twoCol(gfx, x, y, "Net flow", flowStr, flowCol);

        y = twoCol(gfx, x, y, "Production",  "+" + n.stableProd() + " EU/t (stable)", COL_PRODUCER);
        y = twoCol(gfx, x, y, "Consumption", "-" + n.stableCons() + " EU/t (stable)", COL_CONSUMER);

        String memberStr = n.memberCount() + " total  /  " + n.loadedCount() + " loaded";
        y = twoCol(gfx, x, y, "Members", memberStr, COL_VALUE);

        y = twoCol(gfx, x, y, "Last sim tick", String.valueOf(n.lastSimulatedTick()), COL_NEUTRAL);
        y += SECTION_GAP;

        // Node list — clipped to available space
        if (y + LINE <= bottomBound) {
            gfx.drawString(mc.font, "§7Nodes (" + n.nodes().size() + ")", x, y, COL_LABEL, false);
            y += LINE;

            for (NodeEntry node : n.nodes()) {
                if (y + LINE > bottomBound - LINE) {
                    // Show "… N more" if we ran out of space
                    int remaining = (int) n.nodes().stream().filter(nd -> n.nodes().indexOf(nd) >= n.nodes().indexOf(node)).count();
                    gfx.drawString(mc.font, "  §7… " + remaining + " more", x, y, COL_LABEL, false);
                    break;
                }
                y = renderNodeEntry(gfx, x, y, node);
            }
        }
    }

    private int renderBufferBar(GuiGraphics gfx, int x, int y, long buffer, long maxBuf, float pct) {
        Minecraft mc  = Minecraft.getInstance();
        int barW      = PANEL_W - PAD * 2;
        int barH      = 6;

        // Label row
        String bufStr = formatEnergy(buffer) + " / " + formatEnergy(maxBuf);
        String pctStr = String.format("%.1f%%", pct * 100f);
        gfx.drawString(mc.font, "Buffer", x, y, COL_LABEL, false);
        gfx.drawString(mc.font, bufStr,   x + COL2_X - PAD, y, COL_VALUE, false);
        y += LINE;

        // Background track
        gfx.fill(x, y, x + barW, y + barH, 0xFF1A2A1A);

        // Fill colour: green > 50%, yellow 20-50%, red < 20%
        int fillCol = pct > 0.5f ? 0xFF44CC66 : pct > 0.2f ? 0xFFCCA030 : 0xFFCC3333;
        int fillW   = (int) (barW * pct);
        if (fillW > 0) gfx.fill(x, y, x + fillW, y + barH, fillCol);

        // Percentage label centred on bar
        int textX = x + barW / 2 - mc.font.width(pctStr) / 2;
        gfx.drawString(mc.font, pctStr, textX, y - 1, COL_HEADER, false);

        y += barH + 2;
        return y;
    }

    private int renderNodeEntry(GuiGraphics gfx, int x, int y, NodeEntry node) {
        Minecraft mc   = Minecraft.getInstance();
        int roleColour = switch (node.role()) {
            case "PRODUCER" -> COL_PRODUCER;
            case "CONSUMER" -> COL_CONSUMER;
            case "STORAGE"  -> COL_STORAGE;
            case "PIPE"     -> COL_PIPE;
            default         -> COL_VALUE;
        };

        String posStr  = node.pos().getX() + " " + node.pos().getY() + " " + node.pos().getZ();
        String roleTag = "[" + node.role().charAt(0) + "]";

        gfx.drawString(mc.font, roleTag, x + 2, y, roleColour, false);
        gfx.drawString(mc.font, posStr,  x + 22, y, COL_VALUE, false);

        if (node.extra() != null && !node.extra().isEmpty()) {
            gfx.drawString(mc.font, node.extra(), x + 22 + mc.font.width(posStr) + 4, y, COL_LABEL, false);
        }

        if (node.offlineMode() != null) {
            String omStr  = "§8[" + node.offlineMode() + "]";
            int omX       = x + PANEL_W - PAD * 2 - mc.font.width(stripFormatting(omStr));
            gfx.drawString(mc.font, omStr, omX, y, COL_LABEL, false);
        }

        y += LINE;
        return y;
    }

    // -------------------------------------------------------------------------
    // Data computation
    // -------------------------------------------------------------------------

    /** Gets the server-side manager from the integrated server. Returns null on dedicated. */
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
        for (EnergyNetwork net : manager.getAllNetworks()) {
            nets++;
            total  += net.getMemberCount();
            loaded += net.getLoadedNodes().size();
        }
        return new LevelStats(nets, total, loaded);
    }

    private NetworkSnapshot findClosestNetwork(EnergyNetworkManager manager, Player player) {
        if (manager == null) return null;
        BlockPos playerPos = player.blockPosition();
        double   bestDist  = Double.MAX_VALUE;
        EnergyNetwork best = null;

        for (EnergyNetwork net : manager.getAllNetworks()) {
            for (BlockPos pos : net.getMemberPositions()) {
                double d = pos.distSqr(playerPos);
                if (d < bestDist) {
                    bestDist = d;
                    best     = net;
                }
            }
        }

        if (best == null) return null;

        List<NodeEntry> nodeEntries = new ArrayList<>();
        for (EnergyNode node : best.getLoadedNodes()) {
            String role = roleOf(node);
            String mode = offlineModeOf(node);
            String extra = extraOf(node);
            nodeEntries.add(new NodeEntry(node.getEnergyPos(), role, mode, extra));
        }
        // Show unloaded positions that have no live node
        Set<BlockPos> loadedPositions = new HashSet<>();
        for (EnergyNode n : best.getLoadedNodes()) loadedPositions.add(n.getEnergyPos());
        for (BlockPos pos : best.getMemberPositions()) {
            if (!loadedPositions.contains(pos)) {
                nodeEntries.add(new NodeEntry(pos, "UNLOADED", null, null));
            }
        }

        nodeEntries.sort(Comparator.comparing(NodeEntry::role));

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
                nodeEntries,
                bestDist
        );
    }

    private String roleOf(EnergyNode node) {
        if (node.isPipe()) return "PIPE";
        if (node instanceof EnergyStorage) return "STORAGE";
        if (node instanceof EnergyProducer) return "PRODUCER";
        if (node instanceof EnergyConsumer) return "CONSUMER";
        return "NODE";
    }

    private String offlineModeOf(EnergyNode node) {
        // Try producer profile first, then consumer
        if (node instanceof EnergyProducer p) {
            OfflineEnergyProfile profile = p.getOfflineProducerProfile();
            return profile.getMode().name();
        }
        if (node instanceof EnergyConsumer c) {
            OfflineEnergyProfile profile = c.getOfflineConsumerProfile();
            return profile.getMode().name();
        }
        return null;
    }

    private String extraOf(EnergyNode node) {
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
    // Drawing helpers
    // -------------------------------------------------------------------------

    private void drawPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        // Shadow
        gfx.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x60000000);
        // Background
        gfx.fill(x, y, x + w, y + h, COL_BG);
        // Border
        gfx.fill(x,         y,         x + w,     y + 1,     COL_BORDER);
        gfx.fill(x,         y + h - 1, x + w,     y + h,     COL_BORDER);
        gfx.fill(x,         y,         x + 1,     y + h,     COL_BORDER);
        gfx.fill(x + w - 1, y,         x + w,     y + h,     COL_BORDER);
        // Accent top stripe
        gfx.fill(x + 1, y + 1, x + w - 1, y + 2, COL_ACCENT);
    }

    private void sectionHeader(GuiGraphics gfx, int x, int y, String text) {
        Minecraft mc = Minecraft.getInstance();
        gfx.drawString(mc.font, text, x, y, COL_HEADER, false);
        // Underline
        gfx.fill(x, y + LINE, x + mc.font.width(text) + 2, y + LINE + 1, COL_ACCENT);
    }

    /** Draws a label + value in two columns, returns next y. */
    private int twoCol(GuiGraphics gfx, int x, int y, String label, String value, int valueColour) {
        Minecraft mc = Minecraft.getInstance();
        gfx.drawString(mc.font, label, x, y, COL_LABEL, false);
        gfx.drawString(mc.font, value, x + COL2_X - PAD, y, valueColour, false);
        return y + LINE;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static String formatEnergy(long eu) {
        if (eu >= 1_000_000) return String.format("%.2fMEU", eu / 1_000_000.0);
        if (eu >= 1_000)     return String.format("%.1fkEU", eu / 1_000.0);
        return eu + " EU";
    }

    private static String stripFormatting(String s) {
        return s.replaceAll("§.", "");
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC or the debug keybind closes the screen
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}