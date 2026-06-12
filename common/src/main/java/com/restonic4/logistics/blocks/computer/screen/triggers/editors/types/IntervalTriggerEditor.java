package com.restonic4.logistics.blocks.computer.screen.triggers.editors.types;

import com.restonic4.logistics.blocks.computer.automation.triggers.types.IntervalTrigger;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.TriggerEditor;
import net.minecraft.network.chat.Component;

public class IntervalTriggerEditor implements TriggerEditor<IntervalTrigger> {
    @Override
    public String summary(IntervalTrigger interval) {
        return "Every " + interval.getPeriodTicks() + "t";
    }

    @Override
    public void buildConfig(IntervalTrigger interval, EditorBuilder b) {
        b.label(Component.translatable("screen.logistics.computer.tab.triggers.period"));
        b.number(0, interval.getPeriodTicks(), 1, 72000, 0, v -> interval.setPeriodTicks(v.intValue()));
        b.endRow();
    }
}
