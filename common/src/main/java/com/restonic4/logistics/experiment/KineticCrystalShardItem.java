package com.restonic4.logistics.experiment;

import com.restonic4.logistics.blocks.protector.ProtectorBlock;
import com.restonic4.logistics.blocks.protector.ProtectorNode;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KineticCrystalShardItem extends Item implements DyeableLeatherItem, EnergyTooltip {
    public static final String COLOR_KEY = "color";
    public static final String POTION_KEY = "stored_potion";
    public static final int TOTAL = 10000;

    public KineticCrystalShardItem(Properties properties) {
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

    public long getStored(ItemStack stack) {
        return stack.getOrCreateTag().getLong("stored_energy");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (state.getBlock() instanceof ProtectorBlock) {
            if (!level.isClientSide()) {
                NetworkNode node = NetworkManager.get((ServerLevel) level).getNodeByBlockPos(pos);
                if (node == null) return InteractionResult.PASS;
                if (!(node instanceof ProtectorNode protectorNode)) return InteractionResult.PASS;

                long requiredEnergy = (long) (TOTAL * 0.95);
                if (getStored(stack) < requiredEnergy) {
                    if (player != null) {
                        player.displayClientMessage(
                                Component.literal("Not enough energy in the crystal shard!").withStyle(ChatFormatting.RED),
                                true // 'true' sends it to the Action Bar, 'false' sends it to normal Chat
                        );
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }

                protectorNode.repair();

                // Consume the item
                if (player != null && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
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

            spawnShockwave((ServerLevel) level, player.blockPosition(), maxRadius, getColor(stack));

            if (stack.getTag() != null && stack.getTag().contains(POTION_KEY)) {
                ResourceLocation potionId = new ResourceLocation(stack.getTag().getString(POTION_KEY));
                Potion potion = BuiltInRegistries.POTION.get(potionId);

                if (potion != Potions.EMPTY) {
                    for (MobEffectInstance effect : potion.getEffects()) {
                        // Formula to double the strength (amplifiers are 0-indexed. 0->1, 1->3)
                        int newAmplifier = (effect.getAmplifier() + 1) * 2 - 1;

                        MobEffectInstance superchargedEffect = new MobEffectInstance(
                                effect.getEffect(),
                                effect.getDuration(),
                                newAmplifier,
                                effect.isAmbient(),
                                false, // Hides the particles!
                                effect.showIcon()
                        );
                        player.addEffect(superchargedEffect);
                    }
                }
            }

            if (!player.getAbilities().instabuild) {
                ItemStack brokenStack = new ItemStack(Items.BROKEN_KINETIC_CRYSTAL_SHARD.getItem());

                if (stack.getTag() != null && stack.getTag().contains(COLOR_KEY)) {
                    brokenStack.getOrCreateTag().putInt(COLOR_KEY, stack.getTag().getInt(COLOR_KEY));
                }

                return InteractionResultHolder.sidedSuccess(brokenStack, level.isClientSide());
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void spawnShockwave(ServerLevel serverLevel, BlockPos pos, double radius, int color) {
        double radiusRatio = radius / 320.0;
        double expansionDuration = radiusRatio * 5.0;
        double fadeOutDuration = radiusRatio * 2.0;
        double thickness = 1.0;

        serverLevel.playSound(null,
                pos,
                SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.PLAYERS,
                1.0F, 1.0F
        );

        ServerNetworking.sendToAllInLevel(serverLevel, new ShockwavePacket(pos, radius, thickness, expansionDuration, fadeOutDuration, color));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        CompoundTag tag = this.appendEnergyTooltip(stack, tooltipComponents, TOTAL);

        // Display Potion Effect
        if (tag != null && tag.contains(POTION_KEY)) {
            ResourceLocation potionId = new ResourceLocation(tag.getString(POTION_KEY));
            Potion potion = BuiltInRegistries.POTION.get(potionId);

            if (potion != Potions.EMPTY) {
                tooltipComponents.add(Component.empty()); // Adds a blank line for visual spacing
                tooltipComponents.add(Component.literal("Supercharged Effect:").withStyle(ChatFormatting.DARK_PURPLE));

                for (MobEffectInstance effect : potion.getEffects()) {
                    Component effectName = Component.translatable(effect.getDescriptionId());

                    // Calculate the doubled amplifier to match the use() method
                    int newAmplifier = (effect.getAmplifier() + 1) * 2 - 1;

                    // Append the Roman numeral if amplifier > 0 (e.g., Strength II, Speed IV)
                    if (newAmplifier > 0) {
                        effectName = Component.translatable("potion.withAmplifier", effectName, Component.translatable("potion.potency." + newAmplifier));
                    }

                    // Format duration (ticks to mm:ss). 20 ticks = 1 second.
                    int totalSeconds = effect.getDuration() / 20;
                    int minutes = totalSeconds / 60;
                    int seconds = totalSeconds % 60;
                    String durationStr = String.format("%d:%02d", minutes, seconds);

                    // Add the formatted effect line to the tooltip, colored by the effect's category (Beneficial, Harmful, Neutral)
                    tooltipComponents.add(
                            Component.literal(" ")
                                    .append(effectName)
                                    .append(Component.literal(" (" + durationStr + ")"))
                                    .withStyle(effect.getEffect().getCategory().getTooltipFormatting())
                    );
                }
            }
        }
    }
}
