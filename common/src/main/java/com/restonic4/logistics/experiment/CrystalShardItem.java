package com.restonic4.logistics.experiment;

import com.restonic4.logistics.blocks.battery.BatteryNode;
import com.restonic4.logistics.networking.ServerNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CrystalShardItem extends Item implements DyeableLeatherItem {
    public static final String COLOR_KEY = "color";
    public static final int TOTAL = 10000;

    public CrystalShardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int getColor(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(COLOR_KEY)) {
            return tag.getInt(COLOR_KEY);
        }
        return 0xFFFFFF;
    }

    public void setColor(ItemStack stack, int color) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(COLOR_KEY, color);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long stored = stack.getOrCreateTag().getLong("stored_energy");
        return Math.round(13.0f * stored / TOTAL);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        long stored = stack.getOrCreateTag().getLong("stored_energy");
        float t = (float) stored / TOTAL;
        int r = (int) ((1f - t) * 255);
        int g = (int) (t * 255);
        return (r << 16) | (g << 8);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide()) {
            long storedEnergy = stack.getOrCreateTag().getLong("stored_energy");
            if (storedEnergy == 0) {
                level.playSound(null,
                        player.blockPosition(),
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS,
                        1.0F, 1.0F
                );
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }

            float energyPercent = (float) (storedEnergy - 1) / (TOTAL - 1);
            double maxRadius = 8.0 + (energyPercent * (320.0 - 8.0));

            double radiusRatio = maxRadius / 320.0;
            double expansionDuration = radiusRatio * 5.0;
            double fadeOutDuration = radiusRatio * 2.0;
            double thickness = 1.0;

            level.playSound(null,
                    player.blockPosition(),
                    SoundEvents.AMETHYST_CLUSTER_BREAK,
                    SoundSource.PLAYERS,
                    1.0F, 1.0F
            );

            ServerNetworking.sendToAllInLevel((ServerLevel) level, new ShockwavePacket(player.blockPosition(), maxRadius, thickness, expansionDuration, fadeOutDuration, getColor(stack)));

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
