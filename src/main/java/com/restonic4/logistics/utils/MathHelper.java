package com.restonic4.logistics.utils;

import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MathHelper {
    public static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r, g, b;
        int i = (int) (h * 6);
        switch (i % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return new float[] { r + m, g + m, b + m };
    }

    public static int packColor(float r, float g, float b, float a) {
        return ((int) (a * 255) << 24)
                | ((int) (r * 255) << 16)
                | ((int) (g * 255) << 8)
                | (int) (b * 255);
    }

    public static Vec3 closestTo(Set<Vec3> positions, Vec3 camPos) {
        if (positions.isEmpty()) return null;

        double minDistSq = Double.MAX_VALUE;
        Vec3 closestCenter = null;

        for (Vec3 position : positions) {
            double distSq = position.distanceToSqr(camPos);

            if (distSq < minDistSq) {
                minDistSq = distSq;
                closestCenter = position;
            }
        }

        return closestCenter;
    }

    public static Set<Vec3> toVec3(Collection<NetworkNode> networkNodeSet) {
        Set<Vec3> vec3Set = new HashSet<>();
        networkNodeSet.forEach(networkNode -> {
            BlockPos blockPos = networkNode.getBlockPos();
            vec3Set.add(new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5));
        });
        return vec3Set;
    }
}
