package com.restonic4.logistics.blocks.lamp;

import com.restonic4.logistics.experiment.Sounds;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class LampNode extends EnergyNode {
    public static final long ENERGY_PER_CYCLE = 1L;
    public static final int TICKS_PER_CYCLE = 20;

    // Flicker is a short turn-on animation. Kept comfortably under one cycle so it always
    // settles before the next energy re-evaluation.
    private static final int FLICKER_MIN_FRAMES = 3;
    private static final int FLICKER_FRAME_RANGE = 4;   // 3..6 toggles
    private static final int FLICKER_FRAME_MIN_TICKS = 1;
    private static final int FLICKER_FRAME_JITTER = 1;  // 1..2 ticks per frame

    // Rare ambient flicker even while steadily powered. Rolled once per tick; the mean gap between
    // flickers is therefore ~this many ticks (~30 min at 20 TPS).
    private static final int RANDOM_FLICKER_MEAN_TICKS = 30 * 60 * 20;

    private final RandomSource random = RandomSource.create();

    private int tickCounter = 0;

    /**
     * Authoritative logical state, driven purely by energy. This is the only "truth"; it is what
     * gets saved and synced and what the safety rules below always defer to. Visuals never feed
     * back into it.
     */
    private boolean lit = false;

    /** What the block currently shows in the world. A transient visual; never persisted. */
    private boolean displayLit = false;

    // Turn-on flicker animation state.
    private boolean flickering = false;
    private int flickerFramesRemaining = 0;
    private int flickerFrameTimer = 0;

    public LampNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        super.tick();

        EnergyNetwork network = getNetwork();
        if (network == null) return;

        if (++tickCounter >= TICKS_PER_CYCLE) {
            tickCounter = 0;
            long consumed = network.requestEnergyConsumption(ENERGY_PER_CYCLE);
            setLit(consumed >= ENERGY_PER_CYCLE);
        }

        ServerLevel level = network.getServerLevel();
        boolean loaded = level != null && level.hasChunkAt(getBlockPos());

        maybeStartRandomFlicker(loaded);
        updateVisualState(level, loaded);

        if (loaded) {
            pushBlockState(level);
        }
    }

    /**
     * Reconciles the visible state ({@link #displayLit}) with the authoritative {@link #lit}.
     *
     * <p>The ordering here is what makes the effect race-proof against an unstable network:
     * <ul>
     *   <li>If we are logically OFF, we force the lamp off and kill any in-flight flicker
     *       <em>unconditionally and immediately</em> — so a depower can never be "lost" behind an
     *       on-flicker, and the lamp is never left lit when it shouldn't be.</li>
     *   <li>A flicker only ever animates <em>towards on</em>, and when it ends it snaps to
     *       {@code lit} — so it can never strand the lamp off when it should be on.</li>
     * </ul>
     * Because {@code lit} is re-evaluated at cycle boundaries (every {@link #TICKS_PER_CYCLE}) and a
     * flicker is shorter than a cycle, a fresh on-transition always starts a clean flicker.
     */
    private void updateVisualState(ServerLevel level, boolean loaded) {
        if (!lit) {
            flickering = false;
            displayLit = false;
            return;
        }

        if (!flickering) return;

        if (!loaded) {
            // Nobody can see a flicker on an unloaded chunk; settle straight to the real state.
            flickering = false;
            displayLit = true;
            return;
        }

        advanceFlicker(level);
    }

    /**
     * Occasionally kicks off a flicker while the lamp is steadily powered, purely for ambiance.
     *
     * <p>It only fires when we are logically on, the chunk is loaded, and no flicker is already
     * running — so it can never fight the energy-driven logic. It reuses the exact same flicker
     * machinery, which always settles back to the authoritative {@link #lit} (and is force-cancelled
     * the instant power is lost), so this adds no new way to end up in a wrong state.
     */
    private void maybeStartRandomFlicker(boolean loaded) {
        if (!loaded || !lit || flickering) return;
        if (random.nextInt(RANDOM_FLICKER_MEAN_TICKS) == 0) {
            startFlicker();
        }
    }

    private void startFlicker() {
        flickering = true;
        flickerFramesRemaining = FLICKER_MIN_FRAMES + random.nextInt(FLICKER_FRAME_RANGE + 1);
        flickerFrameTimer = 0;   // first frame fires on the next visual update
        displayLit = false;      // begin from dark so the first toggle is a flash on
    }

    private void advanceFlicker(ServerLevel level) {
        if (flickerFrameTimer > 0) {
            flickerFrameTimer--;
            return;
        }

        if (flickerFramesRemaining <= 0) {
            flickering = false;
            displayLit = lit;   // final frame always equals the authoritative state (on)
            return;
        }

        displayLit = !displayLit;
        flickerFramesRemaining--;
        flickerFrameTimer = FLICKER_FRAME_MIN_TICKS + random.nextInt(FLICKER_FRAME_JITTER + 1);

        playFlickerSound(level);
    }

    private void playFlickerSound(ServerLevel level) {
        float pitch = 1.0f + (random.nextFloat() * 0.2f - 0.1f); // 0.9 .. 1.1
        BlockPos pos = getBlockPos();
        level.playSound(
                null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                Sounds.LAMP_FLICKER.getSoundEvent(), SoundSource.BLOCKS,
                1.0f, pitch
        );
    }

    /**
     * Pushes {@link #displayLit} into the world. Only ever called when the chunk is loaded, so we
     * never force-load anything; the node keeps ticking (and consuming energy) while unloaded and
     * simply re-syncs the visible block/light the moment the chunk is available again.
     * {@code setBlock} drives the light engine for us.
     */
    private void pushBlockState(ServerLevel level) {
        BlockPos pos = getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LampBlock) {
            boolean blockLit = state.getValue(LampBlock.LIT);
            if (blockLit != this.displayLit) {
                level.setBlock(pos, state.setValue(LampBlock.LIT, this.displayLit), 3);
            }
        }
    }

    private void setLit(boolean value) {
        if (this.lit == value) return;
        this.lit = value;

        if (value) {
            startFlicker();
        } else {
            flickering = false;
            displayLit = false;
        }

        setNetworkDirty();
    }

    public boolean isLit() { return this.lit; }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.keyValue("Energy/cycle", ENERGY_PER_CYCLE + " per " + TICKS_PER_CYCLE + "t", ChatFormatting.YELLOW);
        builder.keyValue("Lit", isLit() ? "Yes" : "No", isLit() ? ChatFormatting.GREEN : ChatFormatting.RED);
        builder.keyValue("Flickering", flickering ? "Yes" : "No", flickering ? ChatFormatting.YELLOW : ChatFormatting.GRAY);

        return true;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putBoolean("lit", lit);
        tag.putInt("tickCounter", tickCounter);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        this.lit = tag.getBoolean("lit");
        this.tickCounter = tag.getInt("tickCounter");

        // No flicker on load: the world should come back already settled to the real state.
        this.displayLit = this.lit;
        this.flickering = false;
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        super.writeExtraSyncData(buf);
        buf.writeBoolean(lit);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        super.readExtraSyncData(buf);
        this.lit = buf.readBoolean();
    }
}
