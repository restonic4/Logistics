package com.restonic4.logistics.blocks.computer.automation.triggers.core;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.audio_station.AudioStationNode;
import com.restonic4.logistics.blocks.computer.ComputerNode;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * An immutable, per-tick snapshot of the world state surrounding a {@link ComputerNode}.
 * <p>
 * A fresh instance is captured once per game tick inside {@code ComputerNode#tickTriggers()}
 * and handed to every {@link Trigger#evaluate} and {@link TriggerAction#execute} call of that
 * tick, so all triggers and actions observe a consistent view of the node.
 */
public final class TriggerContext {
    private final ComputerNode node;
    private final ServerLevel level;
    private final BlockPos pos;
    private final long gameTime;
    private final long storedEnergy;
    private final long totalEnergyCapacity;
    private final double energyPercent;
    private final List<AudioStationNode> audioStations;
    private final List<AccessorNode> accessors;

    private TriggerContext(
            ComputerNode node, ServerLevel level, BlockPos pos, long gameTime,
            long storedEnergy, long totalEnergyCapacity, double energyPercent,
            List<AudioStationNode> audioStations, List<AccessorNode> accessors
    ) {
        this.node = node;
        this.level = level;
        this.pos = pos;
        this.gameTime = gameTime;
        this.storedEnergy = storedEnergy;
        this.totalEnergyCapacity = totalEnergyCapacity;
        this.energyPercent = energyPercent;
        this.audioStations = audioStations;
        this.accessors = accessors;
    }

    /**
     * Captures a snapshot for the given node. The node's network statistics (energy buffers)
     * are read once here so trigger conditions stay stable for the whole tick.
     */
    public static TriggerContext capture(ComputerNode node, ServerLevel level) {
        long stored = 0L;
        long total = 0L;
        List<AudioStationNode> stations = Collections.emptyList();
        List<AccessorNode> accessors = Collections.emptyList();

        EnergyNetwork network = node.getNetwork();
        if (network != null) {
            stored = network.getStoredEnergyBuffer();
            total = network.getTotalEnergyBuffer();
            stations = Collections.unmodifiableList(network.getAudioStations());
            accessors = Collections.unmodifiableList(network.getAccessors());
        }

        double percent = total > 0 ? (stored * 100.0D) / total : 0.0D;
        return new TriggerContext(node, level, node.getBlockPos(), level.getGameTime(), stored, total, percent, stations, accessors);
    }

    /** The computer node currently being ticked. */
    public ComputerNode getNode() { return node; }

    /** The server level the node lives in. Trigger evaluation only happens server side. */
    public ServerLevel getLevel() { return level; }

    /** The block position of the node. */
    public BlockPos getBlockPos() { return pos; }

    /** The game time of the tick this snapshot was captured on. */
    public long getGameTime() { return gameTime; }

    /** Energy currently buffered in the node's energy network at capture time. */
    public long getStoredEnergy() { return storedEnergy; }

    /** Total energy buffer capacity of the node's energy network at capture time. */
    public long getTotalEnergyCapacity() { return totalEnergyCapacity; }

    /** Fill level of the energy network in percent (0-100). Zero if no network or no capacity. */
    public double getEnergyPercent() { return energyPercent; }

    /** Every audio station on the computer's network at capture time. */
    public List<AudioStationNode> getAudioStations() { return audioStations; }

    /** Looks up an audio station on the computer's network by node UUID, or {@code null}. */
    public AudioStationNode findAudioStation(UUID stationId) {
        for (AudioStationNode station : audioStations) {
            if (station.getUUID().equals(stationId)) return station;
        }
        return null;
    }

    /** Every accessor bridged to the computer's network at capture time. */
    public List<AccessorNode> getAccessors() { return accessors; }

    /** Looks up a bridged accessor by node UUID, or {@code null}. */
    public AccessorNode findAccessor(UUID accessorId) {
        for (AccessorNode accessor : accessors) {
            if (accessor.getUUID().equals(accessorId)) return accessor;
        }
        return null;
    }
}
