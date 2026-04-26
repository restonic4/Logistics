package com.restonic4.logistics.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {
    @Shadow
    public abstract FullChunkStatus getFullStatus();

    @Shadow
    public abstract ChunkPos getPos();

    @Inject(method = "setTicketLevel", at = @At("RETURN"))
    private void onTicketLevelChange(int ticketLevel, CallbackInfo ci) {
        ChunkPos pos = this.getPos();
        FullChunkStatus newStatus = this.getFullStatus();

        if (pos.x != 624 || pos.z != 625) {
            return;
        }

        if (newStatus == FullChunkStatus.FULL) {
            System.out.println("Chunk " + pos + " is now FULL.");
        }

        if (newStatus == FullChunkStatus.ENTITY_TICKING) {
            System.out.println("Chunk " + pos + " is now ENTITY_TICKING.");
        }

        if (newStatus == FullChunkStatus.BLOCK_TICKING) {
            System.out.println("Chunk " + pos + " is now BLOCK_TICKING!");
        }

        if (newStatus == FullChunkStatus.INACCESSIBLE) {
            System.out.println("Chunk " + pos + " is now INACCESSIBLE and will unload soon.");
        }
    }
}
