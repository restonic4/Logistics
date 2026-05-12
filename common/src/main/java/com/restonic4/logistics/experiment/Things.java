package com.restonic4.logistics.experiment;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Things {
    public static int RED = hexToMinecraftInt("FF2B00");

    public static int hexToMinecraftInt(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return -1; // Default/invalid color indicator
        }

        // Clean up standard prefixes
        if (hexColor.startsWith("#")) {
            hexColor = hexColor.substring(1);
        } else if (hexColor.startsWith("0x") || hexColor.startsWith("0X")) {
            hexColor = hexColor.substring(2);
        }

        try {
            // Using Long.parseLong prevents overflow errors if an 8-character ARGB hex is passed
            return (int) Long.parseLong(hexColor, 16);
        } catch (NumberFormatException e) {
            // Fallback or log error if the string isn't valid hex
            return -1;
        }
    }

    public static List<BlockPos> generateHorizontalRing(BlockPos center, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        int h = center.getX();
        int k = center.getZ();
        int y = center.getY();

        int x = radius;
        int z = 0;
        int p = 1 - radius; // Initial decision parameter

        // Add standard 4 points if radius > 0
        if (radius > 0) {
            positions.add(new BlockPos(h + radius, y, k));
            positions.add(new BlockPos(h - radius, y, k));
            positions.add(new BlockPos(h,        y, k + radius));
            positions.add(new BlockPos(h,        y, k - radius));
        }

        // Iterate through one octant and apply 8-way symmetry
        while (x > z) {
            z++;
            if (p <= 0) {
                p = p + 2 * z + 1;
            } else {
                x--;
                p = p + 2 * z - 2 * x + 1;
            }

            if (x < z) break; // Octant boundary crossed

            // Apply octant symmetry around center (h, y, k)
            positions.add(new BlockPos(h + x, y, k + z));
            positions.add(new BlockPos(h - x, y, k + z));
            positions.add(new BlockPos(h + x, y, k - z));
            positions.add(new BlockPos(h - x, y, k - z));

            positions.add(new BlockPos(h + z, y, k + x));
            positions.add(new BlockPos(h - z, y, k + x));
            positions.add(new BlockPos(h + z, y, k - x));
            positions.add(new BlockPos(h - z, y, k - x));
        }
        return positions;
    }
}
