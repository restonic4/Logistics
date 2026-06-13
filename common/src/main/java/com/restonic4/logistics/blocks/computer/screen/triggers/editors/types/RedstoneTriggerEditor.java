package com.restonic4.logistics.blocks.computer.screen.triggers.editors.types;

import com.restonic4.logistics.blocks.computer.automation.triggers.types.RedstoneTrigger;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.TriggerEditor;
import net.minecraft.network.chat.Component;

public class RedstoneTriggerEditor implements TriggerEditor<RedstoneTrigger> {
    private static String allReadersLabel() {
        return Component.translatable("screen.logistics.computer.tab.triggers.all_readers").getString();
    }

    @Override
    public String summary(RedstoneTrigger redstone) {
        String reader = EditorBuilder.nodeLabel(redstone.getTarget(), EditorBuilder.redstoneReaders(), allReadersLabel());
        return switch (redstone.getCondition()) {
            case POWERED -> reader + " powered";
            case UNPOWERED -> reader + " unpowered";
            case STRENGTH -> {
                String cmp = switch (redstone.getComparison()) {
                    case AT_LEAST -> ">=";
                    case AT_MOST -> "<=";
                    case EQUAL -> "=";
                };
                yield reader + " " + cmp + " " + redstone.getLevel();
            }
        };
    }

    @Override
    public void buildConfig(RedstoneTrigger redstone, EditorBuilder b) {
        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.reader"),
                Component.translatable("screen.logistics.computer.tab.triggers.condition"));
        b.nodeDropdown(0, redstone.getTarget(), EditorBuilder.redstoneReaders(),
                Component.translatable("screen.logistics.computer.tab.triggers.all_readers"));
        b.enumDropdown(1, RedstoneTrigger.Condition.values(), redstone.getCondition(),
                condition -> Component.translatable("screen.logistics.computer.tab.triggers.condition." + condition.name().toLowerCase()),
                redstone::setCondition);
        b.endRow();

        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.comparison"),
                Component.translatable("screen.logistics.computer.tab.triggers.level"));
        b.enumDropdown(0, RedstoneTrigger.Comparison.values(), redstone.getComparison(),
                cmp -> Component.translatable("screen.logistics.computer.tab.triggers.rcomparison." + cmp.name().toLowerCase()),
                redstone::setComparison);
        b.number(1, redstone.getLevel(), 0, 15, 0, v -> redstone.setLevel((int) Math.round(v)));
        b.endRow();
    }
}
