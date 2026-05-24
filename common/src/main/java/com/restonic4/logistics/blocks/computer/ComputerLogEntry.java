package com.restonic4.logistics.blocks.computer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record ComputerLogEntry(
        long epochMillis,
        long worldTime,
        Severity severity,
        String message
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public enum Severity {
        INFO  (0xFF64B5F6, "[INFO] "),
        WARN  (0xFFFFD54F, "[WARN] "),
        ERROR (0xFFEF5350, "[ERROR] ");

        public final int color;
        public final String prefix;

        Severity(int color, String prefix) {
            this.color  = color;
            this.prefix = prefix;
        }
    }

    public String formattedTime() {
        return FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("epochMillis", epochMillis);
        tag.putLong("worldTime",   worldTime);
        tag.putString("severity",  severity.name());
        tag.putString("message",   message);
        return tag;
    }

    public static ComputerLogEntry fromNbt(CompoundTag tag) {
        return new ComputerLogEntry(
                tag.getLong("epochMillis"),
                tag.getLong("worldTime"),
                parseSeverity(tag.getString("severity")),
                tag.getString("message")
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeLong(epochMillis);
        buf.writeLong(worldTime);
        buf.writeEnum(severity);
        buf.writeUtf(message);
    }

    public static ComputerLogEntry read(FriendlyByteBuf buf) {
        return new ComputerLogEntry(
                buf.readLong(),
                buf.readLong(),
                buf.readEnum(Severity.class),
                buf.readUtf()
        );
    }

    private static Severity parseSeverity(String name) {
        try {
            return Severity.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Severity.INFO;
        }
    }
}