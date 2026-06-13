package com.restonic4.logistics.migration;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.migration.migrations.Migration1;
import com.restonic4.logistics.migration.migrations.Migration2;
import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MigrationManager {
    public static final int CURRENT_DATA_VERSION = 2;
    public static final String VERSION_KEY = Constants.MOD_ID + "_migration_version";

    public static final int MISSING_VERSION = -1;

    private static final List<Migration> MIGRATIONS = new ArrayList<>();

    static {
        registerFix(new Migration1());
        registerFix(new Migration2());

        MIGRATIONS.sort(Comparator.comparingInt(Migration::getTargetVersion));
    }

    private static void registerFix(Migration migration) {
        Constants.LOG.info("Registered migration patch for {}", migration.getTargetVersion());
        MIGRATIONS.add(migration);
    }

    public static void upgradeItemNbt(CompoundTag itemStackRootTag, int containerDataVersion) {
        CompoundTag customDataTag = itemStackRootTag.contains("tag", compoundTag())
                ? itemStackRootTag.getCompound("tag")
                : new CompoundTag();

        int currentVersion = containerDataVersion;

        for (Migration migration : MIGRATIONS) {
            if (currentVersion < migration.getTargetVersion()) {
                String id = itemStackRootTag.getString("id");
                migration.fixItem(itemStackRootTag, customDataTag, id);
                Constants.LOG.warn("ItemStack migrated from version {} to {} -> {}", currentVersion, migration.getTargetVersion(), id);
                currentVersion = migration.getTargetVersion();
            }
        }

        if (!customDataTag.isEmpty() && !itemStackRootTag.contains("tag", compoundTag())) {
            itemStackRootTag.put("tag", customDataTag);
        }
    }

    public static void upgradeNodeNbt(CompoundTag nodeTag, int containerDataVersion) {
        int currentVersion = containerDataVersion;

        for (Migration migration : MIGRATIONS) {
            if (currentVersion < migration.getTargetVersion()) {
                String type = nodeTag.getString("type");
                migration.fixNode(nodeTag, type);
                Constants.LOG.warn("Network node migrated from version {} to {} -> {}", currentVersion, migration.getTargetVersion(), type);
                currentVersion = migration.getTargetVersion();
            }
        }
    }

    public static void upgradeBlockNbt(CompoundTag blockStateTag, int containerDataVersion) {
        int currentVersion = containerDataVersion;

        for (Migration migration : MIGRATIONS) {
            if (currentVersion < migration.getTargetVersion()) {
                String name = blockStateTag.getString("Name");
                migration.fixBlock(blockStateTag, name);
                Constants.LOG.warn("Placed block migrated from version {} to {} -> {}", currentVersion, migration.getTargetVersion(), name);
                currentVersion = migration.getTargetVersion();
            }
        }
    }

    public static boolean isItemStack(CompoundTag tag) {
        return tag.contains("id", stringTag()) && tag.contains("Count", byteTag());
    }

    public static boolean isNetworkNode(CompoundTag tag) {
        return tag.contains("type", stringTag()) && tag.contains("uuid", intArrayTag()) && tag.contains("pos", longTag());
    }

    public static boolean isBlockState(CompoundTag tag) {
        return tag.contains("Name", stringTag()) && !tag.contains("Count", byteTag());
    }

    public static int byteTag() {
        return 1;
    }

    public static int longTag() {
        return 4;
    }

    public static int stringTag() {
        return 8;
    }

    public static int compoundTag() {
        return 10;
    }

    public static int intArrayTag() {
        return 11;
    }
}
