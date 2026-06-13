package com.restonic4.logistics.blocks.lamp;

import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Client-side owner of the lamp static hums. Each client tick it reconciles the set of looping sound
 * instances with the lamps that currently have static enabled, are powered, and are near the player:
 * starting one for every newly-relevant lamp and stopping the rest. Driven from the client tick (see
 * {@code mixin/experiment/MinecraftMixin}).
 *
 * <p>It reads lamp state straight from the synced {@link ClientNetworkManager} nodes, so a server-side
 * toggle reaches it through the normal node sync.
 */
public class LampStaticSoundManager {
    // Past this distance the linear attenuation would silence the hum anyway, so we don't keep an
    // instance around. A little above the audible range so it can fade in.
    private static final int RANGE = 24;
    private static final int RANGE_SQR = RANGE * RANGE;

    private static final Map<BlockPos, LampStaticSoundInstance> ACTIVE = new HashMap<>();

    private LampStaticSoundManager() {}

    public static void clientTick(Minecraft client) {
        Level level = client.level;
        Player player = client.player;
        if (level == null || player == null) {
            stopAll(client);
            return;
        }

        SoundManager soundManager = client.getSoundManager();
        BlockPos playerPos = player.blockPosition();

        Set<BlockPos> wanted = new HashSet<>();
        for (Network network : ClientNetworkManager.getNetworks(level.dimension())) {
            for (NetworkNode node : network.getNodeIndex().getAllNodes()) {
                if (node instanceof LampNode lamp && lamp.isStaticEnabled() && lamp.isLit()) {
                    BlockPos pos = node.getBlockPos();
                    if (pos.distSqr(playerPos) <= RANGE_SQR) {
                        wanted.add(pos);
                    }
                }
            }
        }

        // Drop instances that are no longer wanted (lamp removed, static toggled off, out of range)
        // or that the sound engine has already finished.
        Iterator<Map.Entry<BlockPos, LampStaticSoundInstance>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, LampStaticSoundInstance> entry = it.next();
            LampStaticSoundInstance instance = entry.getValue();
            if (!wanted.contains(entry.getKey()) || instance.isStopped()) {
                soundManager.stop(instance);
                it.remove();
            }
        }

        // Start a looping hum for every newly-relevant lamp.
        for (BlockPos pos : wanted) {
            if (!ACTIVE.containsKey(pos)) {
                LampStaticSoundInstance instance = new LampStaticSoundInstance(pos);
                ACTIVE.put(pos, instance);
                soundManager.play(instance);
            }
        }
    }

    public static void stopAll(Minecraft client) {
        if (ACTIVE.isEmpty()) return;

        SoundManager soundManager = client.getSoundManager();
        for (LampStaticSoundInstance instance : ACTIVE.values()) {
            soundManager.stop(instance);
        }
        ACTIVE.clear();
    }
}
