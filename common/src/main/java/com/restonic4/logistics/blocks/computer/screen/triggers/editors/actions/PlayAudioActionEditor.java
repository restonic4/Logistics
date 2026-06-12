package com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions;

import com.restonic4.logistics.blocks.computer.automation.triggers.actions.PlayAudioAction;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.ActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;

public class PlayAudioActionEditor implements ActionEditor<PlayAudioAction> {
    @Override
    public void buildConfig(PlayAudioAction play, EditorBuilder b) {
        b.stationDropdown(0, play.getTarget());
        b.soundDropdown(1, play.getSoundPath(), play::setSoundPath);
        b.endRow();
    }
}
