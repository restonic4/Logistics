package com.restonic4.logistics.blocks.computer.screen.triggers.editors.types;

import com.restonic4.logistics.blocks.computer.automation.triggers.types.EnergyLevelTrigger;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.TriggerEditor;
import net.minecraft.network.chat.Component;

public class EnergyLevelTriggerEditor implements TriggerEditor<EnergyLevelTrigger> {
    @Override
    public String summary(EnergyLevelTrigger energy) {
        String cmp = energy.getComparison() == EnergyLevelTrigger.Comparison.BELOW ? "<" : ">";
        return "Energy " + cmp + " " + (int) energy.getThresholdPercent() + "%";
    }

    @Override
    public void buildConfig(EnergyLevelTrigger energy, EditorBuilder b) {
        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.comparison"),
                Component.translatable("screen.logistics.computer.tab.triggers.threshold"));
        b.enumDropdown(0, EnergyLevelTrigger.Comparison.values(), energy.getComparison(),
                cmp -> Component.translatable("screen.logistics.computer.tab.triggers.comparison." + cmp.name().toLowerCase()),
                energy::setComparison);
        b.number(1, energy.getThresholdPercent(), 0, 100, 0, energy::setThresholdPercent);
        b.endRow();
    }
}
