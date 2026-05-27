package com.restonic4.logistics.migration;

import com.restonic4.logistics.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import static com.restonic4.logistics.migration.MigrationManager.isItemStack;

public class NbtWalker {
    public static void processContainer(CompoundTag containerTag) {
        int dataVersion = containerTag.contains(MigrationManager.VERSION_KEY) ? containerTag.getInt(MigrationManager.VERSION_KEY) : MigrationManager.MISSING_VERSION;

        if (dataVersion < MigrationManager.CURRENT_DATA_VERSION) {
            walkAndFix(containerTag, dataVersion);
            containerTag.putInt(MigrationManager.VERSION_KEY, MigrationManager.CURRENT_DATA_VERSION);
        }
    }

    private static void walkAndFix(CompoundTag tag, int dataVersion) {
        if (isItemStack(tag)) {
            String id = tag.getString("id");
            if (id.startsWith(Constants.MOD_ID + ":")) {
                MigrationManager.upgradeItemNbt(tag, dataVersion);
            }
        }

        for (String key : tag.getAllKeys()) {
            Tag child = tag.get(key);
            if (child instanceof CompoundTag compoundChild) {
                walkAndFix(compoundChild, dataVersion);
            } else if (child instanceof ListTag listChild) {
                walkList(listChild, dataVersion);
            }
        }
    }

    private static void walkList(ListTag listTag, int dataVersion) {
        for (int i = 0; i < listTag.size(); i++) {
            Tag element = listTag.get(i);
            if (element instanceof CompoundTag compoundElement) {
                walkAndFix(compoundElement, dataVersion);
            } else if (element instanceof ListTag nestedList) {
                walkList(nestedList, dataVersion);
            }
        }
    }
}
