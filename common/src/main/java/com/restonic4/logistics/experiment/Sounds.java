package com.restonic4.logistics.experiment;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.registry.PlatformRegistry;
import com.restonic4.logistics.registry.entries.SoundEventEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public class Sounds {
    public static final SoundEventEntry SHOCKWAVE = PlatformRegistry
            .sound(new ResourceLocation(Constants.MOD_ID, "effects/shockwave"))
            .subtitle("subtitles.logistics.effects.shockwave")
            .sound("effects/shockwave")
            .register();

    public static final SoundEventEntry COMPUTER_BOOT = PlatformRegistry
            .sound(new ResourceLocation(Constants.MOD_ID, "computer/boot"))
            .subtitle("subtitles.logistics.computer.boot")
            .sound("computer/boot")
            .register();

    public static final SoundEventEntry COMPUTER_BOOT_BEEP = PlatformRegistry
            .sound(new ResourceLocation(Constants.MOD_ID, "computer/boot_beep"))
            .subtitle("subtitles.logistics.computer.boot_beep")
            .sound("computer/boot_beep")
            .register();

    public static final SoundEventEntry COMPUTER_OFF = PlatformRegistry
            .sound(new ResourceLocation(Constants.MOD_ID, "computer/off"))
            .subtitle("subtitles.logistics.computer.off")
            .sound("computer/off")
            .register();

    public static final SoundEventEntry COMPUTER_OPEN = PlatformRegistry
            .sound(new ResourceLocation(Constants.MOD_ID, "computer/open"))
            .subtitle("subtitles.logistics.computer.open")
            .sound("computer/open")
            .register();

    public static final SoundEventEntry COMPUTER_CLOSE = PlatformRegistry
            .sound(new ResourceLocation(Constants.MOD_ID, "computer/close"))
            .subtitle("subtitles.logistics.computer.close")
            .sound("computer/close")
            .register();

    public static final SoundEventEntry COMPUTER_AMBIENT = PlatformRegistry
            .sound(new ResourceLocation(Constants.MOD_ID, "computer/ambient"))
            .subtitle("subtitles.logistics.computer.ambient")
            .sound("computer/ambient")
            .register();

    public static void register() {

    }
}
