package com.restonic4.logistics.blocks.computer.screen.triggers.editors;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.computer.automation.triggers.util.AccessorTarget;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.networks.filter.ItemFilter;
import com.restonic4.logistics.networks.filter.NbtProperty;
import com.restonic4.logistics.networks.filter.NbtPropertyRegistry;
import com.restonic4.logistics.networks.filter.NbtRule;
import com.restonic4.logistics.screens.widgets.SearchableDropdownWidget;
import com.restonic4.logistics.screens.widgets.SearchableDropdownWidget.DropdownEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared {@link EditorBuilder} rows for editing an {@link ItemFilter} plus the accessor
 * pickers item triggers/actions need. Editors using this stay as declarative as the audio
 * ones: one call lays out item + NBT mode + exact-variant + property-rule rows.
 * <p>
 * The right panel doesn't relayout on dropdown changes, so every row is always present;
 * rows that don't apply to the selected NBT mode are simply ignored at match time.
 */
public final class ItemFilterRows {
    /** Dropdown sentinel meaning "all accessors" / "auto-pick a source". */
    public static final UUID ANY_ACCESSOR = new UUID(0L, 0L);

    private ItemFilterRows() {}

    /** Lays out the full filter configuration: item + NBT mode, exact variant, one property rule. */
    public static void build(ItemFilter filter, EditorBuilder b) {
        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.item"),
                Component.translatable("screen.logistics.computer.tab.transfer.nbt_mode"));
        // The dropdown widget visually preselects its first entry, so keep the model in sync.
        List<DropdownEntry<ResourceLocation>> items = itemEntries();
        if (filter.getItemId() == null && !items.isEmpty()) {
            filter.setItemId(items.get(0).value());
        }
        b.dropdown(0, items, filter.getItemId(), filter::setItemId);
        b.enumDropdown(1, ItemFilter.NbtMode.values(), filter.getMode(),
                mode -> Component.translatable("screen.logistics.computer.tab.transfer.nbt_mode." + mode.name().toLowerCase()),
                filter::setMode);
        b.endRow();

        // Exact mode: pick one concrete NBT variant currently visible in the network.
        b.label(Component.translatable("screen.logistics.computer.tab.transfer.variant"));
        b.dropdown(0, variantEntries(filter), filter.getExactTag(),
                tag -> filter.setExactTag(tag == null ? null : tag.copy()));
        b.endRow();

        // Rules mode: a single property condition (the filter model supports more; the UI edits one).
        NbtRule rule;
        if (filter.getRules().isEmpty()) {
            rule = new NbtRule();
            filter.getRules().add(rule);
        } else {
            rule = filter.getRules().get(0);
        }

        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.property"),
                Component.translatable("screen.logistics.computer.tab.triggers.property_comparison"));
        b.dropdown(0, propertyEntries(), rule.getPropertyId(), rule::setPropertyId);
        b.enumDropdown(1, NbtRule.Comparison.values(), rule.getComparison(),
                cmp -> Component.literal(cmp.symbol()), rule::setComparison);
        b.endRow();

        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.property_value"),
                Component.translatable("screen.logistics.computer.tab.triggers.property_unit"));
        b.number(0, rule.getThreshold(), 0, 1_000_000_000, 0, rule::setThreshold);
        List<DropdownEntry<Boolean>> unitEntries = new ArrayList<>();
        unitEntries.add(new DropdownEntry<>(Boolean.TRUE,
                Component.translatable("screen.logistics.computer.tab.transfer.unit.percent"), null));
        unitEntries.add(new DropdownEntry<>(Boolean.FALSE,
                Component.translatable("screen.logistics.computer.tab.transfer.unit.absolute"), null));
        b.dropdown(1, unitEntries, rule.isPercent(), rule::setPercent);
        b.endRow();
    }

    /** An accessor picker bound to {@code target}; {@code anyLabel} captions the sentinel entry. */
    public static SearchableDropdownWidget<UUID> accessorTargetDropdown(EditorBuilder b, int col,
                                                                        AccessorTarget target, Component anyLabel) {
        List<DropdownEntry<UUID>> entries = new ArrayList<>();
        entries.add(new DropdownEntry<>(ANY_ACCESSOR, anyLabel, null));
        for (AccessorNode accessor : accessors()) {
            entries.add(new DropdownEntry<>(accessor.getUUID(), Component.literal(accessor.getSafeName()), null));
        }

        return b.dropdown(col, entries, target.isAny() ? ANY_ACCESSOR : target.getAccessorId(),
                uuid -> {
                    if (uuid == null || ANY_ACCESSOR.equals(uuid)) target.setAny();
                    else target.setAccessor(uuid);
                });
    }

    /** A picker over the network's accessors by UUID, without an "any" sentinel. */
    public static SearchableDropdownWidget<UUID> accessorDropdown(EditorBuilder b, int col,
                                                                  UUID selected, java.util.function.Consumer<UUID> onSelect) {
        List<DropdownEntry<UUID>> entries = new ArrayList<>();
        for (AccessorNode accessor : accessors()) {
            entries.add(new DropdownEntry<>(accessor.getUUID(), Component.literal(accessor.getSafeName()), null));
        }
        // The dropdown widget visually preselects its first entry, so keep the model in sync.
        if (selected == null && !entries.isEmpty()) {
            selected = entries.get(0).value();
            onSelect.accept(selected);
        }
        return b.dropdown(col, entries, selected, onSelect);
    }

    /** The accessors bridged to the computer's network, or empty if there is no network. */
    public static List<AccessorNode> accessors() {
        if (ComputerScreen.getEnergyNetwork() == null) return List.of();
        return ComputerScreen.getEnergyNetwork().getAccessors();
    }

    /** The display name for an accessor by UUID, or "?" if it is gone. */
    public static String accessorLabel(UUID accessorId) {
        for (AccessorNode accessor : accessors()) {
            if (accessor.getUUID().equals(accessorId)) return accessor.getSafeName();
        }
        return "?";
    }

    /** One entry per distinct item visible in the network's replicated inventories. */
    private static List<DropdownEntry<ResourceLocation>> itemEntries() {
        Map<ResourceLocation, ItemStack> seen = new LinkedHashMap<>();
        for (AccessorNode accessor : accessors()) {
            for (ItemStack stack : accessor.getReplicatedInventory()) {
                if (stack.isEmpty()) continue;
                seen.putIfAbsent(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack);
            }
        }

        List<DropdownEntry<ResourceLocation>> entries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ItemStack> entry : seen.entrySet()) {
            entries.add(new DropdownEntry<>(entry.getKey(),
                    Component.translatable(entry.getValue().getItem().getDescriptionId()),
                    SearchableDropdownWidget.DropdownIcon.of(entry.getValue())));
        }
        return entries;
    }

    /** The distinct NBT variants of the filter's item currently visible in the network. */
    private static List<DropdownEntry<CompoundTag>> variantEntries(ItemFilter filter) {
        List<DropdownEntry<CompoundTag>> entries = new ArrayList<>();
        entries.add(new DropdownEntry<>(null,
                Component.translatable("screen.logistics.computer.tab.transfer.variant.no_nbt"), null));
        if (filter.getItemId() == null) return entries;

        Map<CompoundTag, ItemStack> variants = new LinkedHashMap<>();
        for (AccessorNode accessor : accessors()) {
            for (ItemStack stack : accessor.getReplicatedInventory()) {
                if (stack.isEmpty() || stack.getTag() == null) continue;
                if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(filter.getItemId())) continue;
                variants.putIfAbsent(stack.getTag(), stack);
            }
        }

        int index = 1;
        for (Map.Entry<CompoundTag, ItemStack> entry : variants.entrySet()) {
            ItemStack stack = entry.getValue();
            String label;
            List<NbtProperty> properties = NbtPropertyRegistry.propertiesFor(stack);
            if (!properties.isEmpty()) {
                NbtProperty property = properties.get(0);
                label = property.getDisplayName().getString() + " " + property.describeValue(stack);
            } else {
                label = Component.translatable("screen.logistics.computer.tab.transfer.variant").getString() + " #" + index;
            }
            entries.add(new DropdownEntry<>(entry.getKey(), Component.literal(label),
                    SearchableDropdownWidget.DropdownIcon.of(stack)));
            index++;
        }
        return entries;
    }

    /** Every registered NBT property, for the rule's property picker. */
    private static List<DropdownEntry<ResourceLocation>> propertyEntries() {
        List<DropdownEntry<ResourceLocation>> entries = new ArrayList<>();
        for (NbtProperty property : NbtPropertyRegistry.getAll()) {
            entries.add(new DropdownEntry<>(property.getId(), property.getDisplayName(), null));
        }
        return entries;
    }
}
