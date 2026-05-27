package com.restonic4.logistics.mixin.migration;

import com.restonic4.logistics.migration.MigrationManager;
import com.restonic4.logistics.migration.NbtWalker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

    @Inject(method = "read", at = @At("HEAD"))
    private static void onChunkLoad(ServerLevel level, PoiManager poiManager, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ChunkAccess> cir) {
        if (tag != null) {
            NbtWalker.processContainer(tag);
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private static void onChunkSave(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        if (tag != null) {
            tag.putInt(MigrationManager.VERSION_KEY, MigrationManager.CURRENT_DATA_VERSION);
        }
    }
}