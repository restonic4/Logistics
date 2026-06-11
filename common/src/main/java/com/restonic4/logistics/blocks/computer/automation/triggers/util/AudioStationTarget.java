package com.restonic4.logistics.blocks.computer.automation.triggers.util;

import com.restonic4.logistics.blocks.audio_station.AudioStationNode;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A reusable "which audio station(s)?" selection shared by audio triggers and actions:
 * either every station on the computer's network, or one specific station by node UUID.
 * Embeds its own serialization so owning triggers/actions just delegate to it.
 */
public final class AudioStationTarget {
    private static final String TAG_ALL = "allStations";
    private static final String TAG_STATION = "stationId";

    private boolean allStations = true;
    private UUID stationId = null;

    /** Whether this targets every station on the network. */
    public boolean isAllStations() { return allStations; }

    /** The targeted station's node UUID; only meaningful when {@link #isAllStations()} is false. */
    public UUID getStationId() { return stationId; }

    public void setAllStations() {
        this.allStations = true;
        this.stationId = null;
    }

    public void setStation(UUID stationId) {
        this.allStations = false;
        this.stationId = stationId;
    }

    /**
     * Resolves this target against the tick snapshot.
     *
     * @return the targeted stations; empty if a specific station no longer exists
     */
    public List<AudioStationNode> resolve(TriggerContext ctx) {
        if (allStations) return ctx.getAudioStations();
        if (stationId == null) return Collections.emptyList();
        AudioStationNode station = ctx.findAudioStation(stationId);
        return station != null ? List.of(station) : Collections.emptyList();
    }

    public void save(CompoundTag tag) {
        tag.putBoolean(TAG_ALL, allStations);
        if (stationId != null) {
            tag.putUUID(TAG_STATION, stationId);
        }
    }

    public void load(CompoundTag tag) {
        this.allStations = tag.getBoolean(TAG_ALL);
        this.stationId = tag.hasUUID(TAG_STATION) ? tag.getUUID(TAG_STATION) : null;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(allStations);
        buf.writeBoolean(stationId != null);
        if (stationId != null) {
            buf.writeUUID(stationId);
        }
    }

    public void read(FriendlyByteBuf buf) {
        this.allStations = buf.readBoolean();
        this.stationId = buf.readBoolean() ? buf.readUUID() : null;
    }
}
