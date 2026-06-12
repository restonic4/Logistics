package com.restonic4.logistics.blocks.computer.screen.triggers.editors.types;

import com.restonic4.logistics.blocks.computer.automation.triggers.types.AudioStateTrigger;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.TriggerEditor;
import net.minecraft.network.chat.Component;

public class AudioStateTriggerEditor implements TriggerEditor<AudioStateTrigger> {
    @Override
    public String summary(AudioStateTrigger audio) {
        return EditorBuilder.stationLabel(audio.getTarget()) + " " + audio.getState().name().toLowerCase();
    }

    @Override
    public void buildConfig(AudioStateTrigger audio, EditorBuilder b) {
        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.station"),
                Component.translatable("screen.logistics.computer.tab.triggers.state"));
        b.stationDropdown(0, audio.getTarget());
        b.enumDropdown(1, AudioStateTrigger.State.values(), audio.getState(),
                state -> Component.translatable("screen.logistics.computer.tab.triggers.state." + state.name().toLowerCase()),
                audio::setState);
        b.endRow();
    }
}
