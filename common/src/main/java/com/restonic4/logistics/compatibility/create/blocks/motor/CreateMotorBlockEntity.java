package com.restonic4.logistics.compatibility.create.blocks.motor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CreateMotorBlockEntity extends GeneratingKineticBlockEntity {
    public static final int DEFAULT_SPEED = 16;
    public static final int MAX_SPEED = 256;
    private static final int RECOVERY_TICKS = 80;

    private boolean hasEnoughEnergy = true;
    private int overloadTimer = 0;

    public ScrollValueBehaviour generatedSpeed;
    public AbstractComputerBehaviour computerBehaviour;

    public CreateMotorBlockEntity(BlockPos pos, BlockState state) {
        super(CreateCompatibility.CREATE_MOTOR.getBlockEntityType(CreateMotorBlockEntity.class), pos, state);
    }

    public CreateMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        int max = MAX_SPEED;
        generatedSpeed = new KineticScrollValueBehaviour(CreateLang.translateDirect("kinetics.creative_motor.rotation_speed"),
                this, new MotorValueBox());
        generatedSpeed.between(-max, max);
        generatedSpeed.value = DEFAULT_SPEED;
        generatedSpeed.withCallback(i -> {
            this.updateGeneratedRotation();
        });
        behaviours.add(generatedSpeed);
        behaviours.add(computerBehaviour = ComputerCraftProxy.behaviour(this));
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
            updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (!hasEnoughEnergy || isOverloaded())
            return 0;
        return getSpeedSetting();
    }

    public float getSpeedSetting() {
        if (!getBlockState().is(CreateCompatibility.CREATE_MOTOR.getBlock()))
            return 0;
        return convertToDirection(generatedSpeed.getValue(), getBlockState().getValue(CreateMotorBlock.FACING));
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = 512f / 50f;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public boolean isSource() {
        return !isOverloaded() && hasEnoughEnergy && super.isSource();
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            if (isOverloaded()) spawnBrokenParticles();
            return;
        }

        if (overloadTimer > 0) {
            overloadTimer--;
            if (overloadTimer == 0) {
                // Recovery: Tell the network we are ready to provide speed again
                if (hasEnoughEnergy && getSpeedSetting() != 0) {
                    playStartupSound();
                }
                updateGeneratedRotation();
            }
        }
    }

    private void playStartupSound() {
        if (level == null) return;
        BlockPos pos = getBlockPos();
        this.level.playSound(
                null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS,
                1, 1
        );
    }

    private void spawnBrokenParticles() {
        if (level != null && level.random.nextInt(5) == 0) {
            Vec3 p = VecHelper.getCenterOf(worldPosition);
            level.addParticle(ParticleTypes.SMOKE, p.x, p.y + 0.5f, p.z, 0, 0.05f, 0);
            if (level.random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.5f, p.z, 0, 0.02f, 0);
            }
        }
    }

    public boolean hasEnoughEnergy() {
        return hasEnoughEnergy;
    }

    public boolean isOverloaded() {
        return overloadTimer > 0;
    }

    public void setEnergyState(boolean state) {
        if (this.hasEnoughEnergy == state) return;
        this.hasEnoughEnergy = state;

        if (!state) triggerOverload();
        else updateGeneratedRotation();

        this.sendData();
    }

    @Override
    public void applyNewSpeed(float prevSpeed, float speed) {
        // Catch direction conflict BEFORE the superclass destroys the block
        if (hasSource() && Math.abs(prevSpeed) >= Math.abs(speed)) {
            if (Math.signum(prevSpeed) != Math.signum(speed) && speed != 0) {
                triggerOverload();
                return; // Stop execution here so level.destroyBlock() isn't called
            }
        }
        super.applyNewSpeed(prevSpeed, speed);
    }

    public void triggerOverload() {
        if (overloadTimer > 0) return;

        this.overloadTimer = RECOVERY_TICKS;
        this.level.levelEvent(2001, worldPosition, 0); // Play "break" sound
        BlockPos pos = getBlockPos();
        this.level.playSound(
                null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                1, 1
        );

        // Notify network that we are now providing 0 speed
        this.updateGeneratedRotation();
        this.sendData();
    }

    public float getTheoreticalStressPerTick() {
        return calculateAddedStressCapacity() * Math.abs(getSpeedSetting());
    }

    public float getStressPerTick() {
        if (isOverStressed()) return 0;
        return getTheoreticalStressPerTick();
    }

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("hasEnoughEnergy", hasEnoughEnergy);
        tag.putInt("overloadTimer", overloadTimer);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        hasEnoughEnergy = !tag.contains("hasEnoughEnergy") || tag.getBoolean("hasEnoughEnergy");
        overloadTimer = tag.getInt("overloadTimer");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        tooltip.add(Component.literal("Needed: " + (long) (getTheoreticalStressPerTick() * CreateCompatibility.CONVERSION_RATE)));
        tooltip.add(Component.literal("Speed theory: " + getTheoreticalSpeed()));
        tooltip.add(Component.literal("Speed setting: " + getSpeedSetting()));

        if (!hasEnoughEnergy || isOverloaded()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal(" Motor Status:").withStyle(ChatFormatting.GRAY));

            if (!hasEnoughEnergy) {
                // Out of Energy Warning
                tooltip.add(Component.literal("  ⚠ ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal("Not Enough Energy").withStyle(ChatFormatting.DARK_RED)));
            } else if (isOverloaded()) {
                // Kinetic Conflict Warning
                tooltip.add(Component.literal("  ⚠ ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("Stalled / Overloaded").withStyle(ChatFormatting.YELLOW)));
            }

            added = true;
        }

        return added;
    }

    class MotorValueBox extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 12.5);
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = state.getValue(CreateMotorBlock.FACING);
            return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf(facing.getNormal())
                    .scale(-1 / 16f));
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            super.rotate(level, pos, state, ms);
            Direction facing = state.getValue(CreateMotorBlock.FACING);
            if (facing.getAxis() == Direction.Axis.Y)
                return;
            if (getSide() != Direction.UP)
                return;
            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            Direction facing = state.getValue(CreateMotorBlock.FACING);
            if (facing.getAxis() != Direction.Axis.Y && direction == Direction.DOWN)
                return false;
            return direction.getAxis() != facing.getAxis();
        }

    }
}
