package com.restonic4.logistics.blocks.computer.screen.triggers.editors.types;

import com.restonic4.logistics.blocks.computer.automation.triggers.types.ItemCountTrigger;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.ItemFilterRows;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.TriggerEditor;
import net.minecraft.network.chat.Component;

public class ItemCountTriggerEditor implements TriggerEditor<ItemCountTrigger> {
    @Override
    public String summary(ItemCountTrigger trigger) {
        String item = trigger.getFilter().getItemId() == null ? "?" : trigger.getFilter().getItemId().getPath();
        String cmp = trigger.getComparison() == ItemCountTrigger.Comparison.BELOW ? "<" : ">";
        return item + " " + cmp + " " + trigger.getThreshold();
    }

    @Override
    public void buildConfig(ItemCountTrigger trigger, EditorBuilder b) {
        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.accessor"),
                Component.translatable("screen.logistics.computer.tab.triggers.item_count"));
        ItemFilterRows.accessorTargetDropdown(b, 0, trigger.getScope(),
                Component.translatable("screen.logistics.computer.tab.triggers.all_accessors"));
        b.number(1, trigger.getThreshold(), 0, 1_000_000_000, 0,
                value -> trigger.setThreshold((int) (double) value));
        b.endRow();

        b.label(Component.translatable("screen.logistics.computer.tab.triggers.item_comparison"));
        b.enumDropdown(0, ItemCountTrigger.Comparison.values(), trigger.getComparison(),
                cmp -> Component.translatable("screen.logistics.computer.tab.triggers.comparison." + cmp.name().toLowerCase()),
                trigger::setComparison);
        b.endRow();

        ItemFilterRows.build(trigger.getFilter(), b);
    }
}
