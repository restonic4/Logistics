package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ComputerLogger extends SavedData {

    // ── Constants ─────────────────────────────────────────────────────────────

    public static final String DATA_NAME     = "logistics_computer_logs";
    /** Maximum entries kept per computer before the oldest are evicted. */
    public static final int    MAX_ENTRIES   = 64;

    // ── Data ──────────────────────────────────────────────────────────────────

    /**
     * Map-like structure: each computer's BlockPos serialised to a string key
     * pointing to a bounded deque of log entries.
     *
     * We use a simple parallel list of (key, deque) pairs to avoid a full
     * HashMap dependency — the number of computers per level is small.
     */
    private final List<String>                     keys    = new ArrayList<>();
    private final List<Deque<ComputerLogEntry>>    queues  = new ArrayList<>();

    // ── SavedData boilerplate ─────────────────────────────────────────────────

    private ComputerLogger() {}

    public static ComputerLogger create() {
        return new ComputerLogger();
    }

    public static ComputerLogger load(CompoundTag root) {
        ComputerLogger logger = new ComputerLogger();

        CompoundTag computers = root.getCompound("computers");
        for (String key : computers.getAllKeys()) {
            ListTag list = computers.getList(key, Tag.TAG_COMPOUND);
            Deque<ComputerLogEntry> deque = new ArrayDeque<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                deque.add(ComputerLogEntry.fromNbt(list.getCompound(i)));
            }
            logger.keys.add(key);
            logger.queues.add(deque);
        }

        return logger;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        CompoundTag computers = new CompoundTag();

        for (int i = 0; i < keys.size(); i++) {
            ListTag list = new ListTag();
            for (ComputerLogEntry entry : queues.get(i)) {
                list.add(entry.toNbt());
            }
            computers.put(keys.get(i), list);
        }

        root.put("computers", computers);
        return root;
    }

    // ── Static access helpers ─────────────────────────────────────────────────

    /** Retrieve (or create) the {@link ComputerLogger} for the given level. */
    public static ComputerLogger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ComputerLogger::load,
                ComputerLogger::create,
                DATA_NAME
        );
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Append a log entry for {@code computerPos} in {@code level}.
     * <p>
     * Mirrors the message to {@link Constants#LOG} at the matching level so
     * server-console output is unchanged.  Afterwards it fans the new entry
     * out to any player currently watching this computer via
     * {@link ComputerLogPushPacket}.
     *
     * <p>No chunks are loaded — only {@link ServerLevel#getGameTime()} is read.
     */
    public static void log(ServerLevel level,
                           BlockPos computerPos,
                           ComputerLogEntry.Severity severity,
                           String message) {
        // Mirror to server console at matching level
        switch (severity) {
            case WARN  -> Constants.LOG.warn("[Computer {}] {}", computerPos.toShortString(), message);
            case ERROR -> Constants.LOG.error("[Computer {}] {}", computerPos.toShortString(), message);
            default    -> Constants.LOG.info("[Computer {}] {}", computerPos.toShortString(), message);
        }

        ComputerLogEntry entry = new ComputerLogEntry(
                System.currentTimeMillis(),
                level.getGameTime(),
                severity,
                message
        );

        ComputerLogger logger = get(level);
        logger.append(computerPos, entry);
        logger.setDirty(); // marks SavedData for write on next auto-save

        // Push to any open ComputerScreen watching this computer
        ComputerLogPushPacket.sendIfWatching(level, computerPos, entry);
    }

    /**
     * Returns a snapshot list of all entries for {@code pos}, oldest first.
     * Safe to call from any thread — the returned list is a new copy.
     */
    public List<ComputerLogEntry> getEntries(BlockPos pos) {
        int idx = indexOf(pos);
        if (idx < 0) return List.of();
        return new ArrayList<>(queues.get(idx));
    }

    /** Remove all stored entries for this computer (e.g. on block break). */
    public void clearEntries(BlockPos pos) {
        int idx = indexOf(pos);
        if (idx >= 0) {
            queues.get(idx).clear();
            setDirty();
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void append(BlockPos pos, ComputerLogEntry entry) {
        int idx = indexOf(pos);
        Deque<ComputerLogEntry> deque;

        if (idx < 0) {
            deque = new ArrayDeque<>();
            keys.add(posKey(pos));
            queues.add(deque);
        } else {
            deque = queues.get(idx);
        }

        deque.addLast(entry);

        // Evict oldest entries when the cap is exceeded
        while (deque.size() > MAX_ENTRIES) {
            deque.pollFirst();
        }
    }

    private int indexOf(BlockPos pos) {
        String key = posKey(pos);
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equals(key)) return i;
        }
        return -1;
    }

    private static String posKey(BlockPos pos) {
        // Compact unique string: no spaces so it works as an NBT compound key
        return pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }
}