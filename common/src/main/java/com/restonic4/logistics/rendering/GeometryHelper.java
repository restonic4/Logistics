package com.restonic4.logistics.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GeometryHelper {
    public static void line(
            BufferBuilder buf, Matrix4f matrix,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float r, float g, float b, float a
    ) {
        buf.vertex(matrix, x0,y0,z0).color(r,g,b,a).endVertex();
        buf.vertex(matrix, x1,y1,z1).color(r,g,b,a).endVertex();
    }

    public static void quad(
            BufferBuilder buf, Matrix4f matrix,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float r, float g, float b, float a
    ) {
        buf.vertex(matrix, x0,y0,z0).color(r,g,b,a).endVertex();
        buf.vertex(matrix, x1,y1,z1).color(r,g,b,a).endVertex();
        buf.vertex(matrix, x2,y2,z2).color(r,g,b,a).endVertex();
        buf.vertex(matrix, x3,y3,z3).color(r,g,b,a).endVertex();
    }

    public static void filledCube(
            BufferBuilder buf, Matrix4f matrix, BlockPos pos, Vec3 cam,
            float r, float g, float b, float a,
            float expand
    ) {
        float x0 = (float) (pos.getX() - cam.x - expand);
        float y0 = (float) (pos.getY() - cam.y - expand);
        float z0 = (float) (pos.getZ() - cam.z - expand);
        float x1 = (float) (pos.getX() - cam.x + 1 + expand);
        float y1 = (float) (pos.getY() - cam.y + 1 + expand);
        float z1 = (float) (pos.getZ() - cam.z + 1 + expand);

        quad(buf, matrix, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, r,g,b,a);
        quad(buf, matrix, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, r,g,b,a);
        quad(buf, matrix, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, r,g,b,a);
        quad(buf, matrix, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, r,g,b,a);
        quad(buf, matrix, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, r,g,b,a);
        quad(buf, matrix, x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1, r,g,b,a);
    }

    public static void wireframeCube(
            BufferBuilder buf, Matrix4f matrix, BlockPos pos, Vec3 cam,
            float r, float g, float b, float a,
            float expand
    ) {
        float x0 = (float) (pos.getX() - cam.x - expand);
        float y0 = (float) (pos.getY() - cam.y - expand);
        float z0 = (float) (pos.getZ() - cam.z - expand);
        float x1 = (float) (pos.getX() - cam.x + 1 + expand);
        float y1 = (float) (pos.getY() - cam.y + 1 + expand);
        float z1 = (float) (pos.getZ() - cam.z + 1 + expand);

        // Bottom ring
        line(buf, matrix, x0,y0,z0, x1,y0,z0, r,g,b,a);
        line(buf, matrix, x1,y0,z0, x1,y0,z1, r,g,b,a);
        line(buf, matrix, x1,y0,z1, x0,y0,z1, r,g,b,a);
        line(buf, matrix, x0,y0,z1, x0,y0,z0, r,g,b,a);

        // Top ring
        line(buf, matrix, x0,y1,z0, x1,y1,z0, r,g,b,a);
        line(buf, matrix, x1,y1,z0, x1,y1,z1, r,g,b,a);
        line(buf, matrix, x1,y1,z1, x0,y1,z1, r,g,b,a);
        line(buf, matrix, x0,y1,z1, x0,y1,z0, r,g,b,a);

        // Verticals
        line(buf, matrix, x0,y0,z0, x0,y1,z0, r,g,b,a);
        line(buf, matrix, x1,y0,z0, x1,y1,z0, r,g,b,a);
        line(buf, matrix, x1,y0,z1, x1,y1,z1, r,g,b,a);
        line(buf, matrix, x0,y0,z1, x0,y1,z1, r,g,b,a);
    }

    public static void culledMesh(
            BufferBuilder buf, Matrix4f matrix, Collection<BlockPos> positions, Vec3 cam,
            float r, float g, float b, float a
    ) {
        Set<BlockPos> posSet = (positions instanceof Set<BlockPos> s) ? s : new HashSet<>(positions);

        for (BlockPos pos : posSet) {
            float x0 = (float) (pos.getX() - cam.x);
            float y0 = (float) (pos.getY() - cam.y);
            float z0 = (float) (pos.getZ() - cam.z);
            float x1 = (float) (pos.getX() - cam.x + 1);
            float y1 = (float) (pos.getY() - cam.y + 1);
            float z1 = (float) (pos.getZ() - cam.z + 1);

            // Down
            if (!posSet.contains(pos.relative(Direction.DOWN))) {
                quad(buf, matrix, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
            }

            // Up
            if (!posSet.contains(pos.relative(Direction.UP))) {
                quad(buf, matrix, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
            }

            // North (Negative Z)
            if (!posSet.contains(pos.relative(Direction.NORTH))) {
                quad(buf, matrix, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
            }

            // South (Positive Z)
            if (!posSet.contains(pos.relative(Direction.SOUTH))) {
                quad(buf, matrix, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
            }

            // West (Negative X)
            if (!posSet.contains(pos.relative(Direction.WEST))) {
                quad(buf, matrix, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
            }

            // East (Positive X)
            if (!posSet.contains(pos.relative(Direction.EAST))) {
                quad(buf, matrix, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
            }
        }
    }
}
