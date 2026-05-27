package com.restonic4.logistics.screens.widgets;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class PlayerHeadRenderer {

    public static void renderHead(GuiGraphics gfx, String username, UUID uuid, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();

        ResourceLocation skin = getSkinLocation(mc, username, uuid);

        // Draw the face region (8,8 to 16,16) of the 64x64 skin, scaled to desired size
        gfx.blit(skin, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64);
    }

    private static ResourceLocation getSkinLocation(Minecraft mc, String username, UUID uuid) {
        // Try player list first
        if (mc.player != null && mc.player.connection != null) {
            for (PlayerInfo info : mc.player.connection.getOnlinePlayers()) {
                GameProfile profile = info.getProfile();
                if (profile.getId().equals(uuid) || profile.getName().equalsIgnoreCase(username)) {
                    return mc.getSkinManager().getInsecureSkinLocation(profile);
                }
            }
        }

        // Fallback to default skin based on UUID parity (Steve vs Alex)
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }
}