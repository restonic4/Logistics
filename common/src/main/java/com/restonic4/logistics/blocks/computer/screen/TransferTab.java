package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.computer.ComputerTransferPacket;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.networks.filter.ItemFilter;
import com.restonic4.logistics.networks.filter.NbtProperty;
import com.restonic4.logistics.networks.filter.NbtPropertyRegistry;
import com.restonic4.logistics.networks.filter.NbtRule;
import com.restonic4.logistics.screens.tabs.Tab;
import com.restonic4.logistics.screens.widgets.NumberPickerWidget;
import com.restonic4.logistics.screens.widgets.SearchableDropdownWidget;
import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TransferTab extends Tab {
    private NumberPickerWidget quantityBox;
    private SearchableDropdownWidget<String> textBox;
    private SearchableDropdownWidget<BlockPos> leftAccessor;
    private SearchableDropdownWidget<BlockPos> rightAccessor;
    private StyledButton transferButton;

    // NBT filter controls (shown below the quantity box, depending on the selected mode)
    private SearchableDropdownWidget<ItemFilter.NbtMode> nbtModeDropdown;
    private SearchableDropdownWidget<CompoundTag> variantDropdown;
    private SearchableDropdownWidget<ResourceLocation> propertyDropdown;
    private SearchableDropdownWidget<NbtRule.Comparison> comparisonDropdown;
    private NumberPickerWidget ruleValueBox;
    private SearchableDropdownWidget<Boolean> unitDropdown;

    // Persisted state across tab switches / resizes
    private int savedQuantity = 1;
    private String savedItem = "minecraft:chest";
    private BlockPos savedLeft = null;
    private BlockPos savedRight = null;
    private ItemFilter.NbtMode savedNbtMode = ItemFilter.NbtMode.ANY;
    @Nullable private CompoundTag savedVariantTag = null;
    private ResourceLocation savedPropertyId = NbtPropertyRegistry.ENERGY.getId();
    private NbtRule.Comparison savedComparison = NbtRule.Comparison.GREATER_EQUAL;
    private double savedRuleValue = 50;
    private boolean savedRulePercent = true;

    private final List<AccessorNode> accessors;

    public TransferTab() {
        super(Component.translatable("screen.logistics.computer.tab.transfer.title"));
        this.accessors = ComputerScreen.getEnergyNetwork().getAccessors();
    }

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {
        int midW = 50;
        int midX = x + width / 2 - midW / 2;
        int midY = y + height / 2 - midW / 2 - 10;
        int centerX = x + width / 2;

        // Quantity
        this.quantityBox = new NumberPickerWidget(midX, midY + midW + 8, midW, 20, Component.empty(), 1, val -> {});
        this.quantityBox.setRange(1, ComputerTransferPacket.MAX_QUANTITY);
        this.quantityBox.setValue(savedQuantity);
        parent.addRenderableWidget(this.quantityBox);

        // Item selector
        this.textBox = new SearchableDropdownWidget<>(midX, midY, midW, midW, Component.empty(), new ArrayList<>(), val -> onItemSelected());
        this.textBox.setSelectionRenderer(new SearchableDropdownWidget.CompactItemSelectorRenderer<>(4, 0.5f));
        if (savedItem != null) this.textBox.setSelectedValue(savedItem);
        parent.addRenderableWidget(this.textBox);

        // FROM
        this.leftAccessor = new SearchableDropdownWidget<>(x + 20, midY, midW, midW, Component.empty(), new ArrayList<>(), val -> {});
        this.leftAccessor.setSelectionRenderer(new SearchableDropdownWidget.CompactItemSelectorRenderer<>(4, 0.5f));
        parent.addRenderableWidget(this.leftAccessor);

        // TARGET
        this.rightAccessor = new SearchableDropdownWidget<>(x + width - midW - 20, midY, midW, midW, Component.empty(), new ArrayList<>(), val -> {});
        this.rightAccessor.setSelectionRenderer(new SearchableDropdownWidget.CompactItemSelectorRenderer<>(4, 0.5f));
        parent.addRenderableWidget(this.rightAccessor);

        // NBT mode + exact-variant row
        int nbtY = midY + midW + 8 + 20 + 6;
        this.nbtModeDropdown = new SearchableDropdownWidget<>(centerX - 72, nbtY, 70, 16, Component.empty(),
                buildNbtModeEntries(), mode -> updateNbtControls());
        this.nbtModeDropdown.setSelectedValueSilently(savedNbtMode);
        parent.addRenderableWidget(this.nbtModeDropdown);

        this.variantDropdown = new SearchableDropdownWidget<>(centerX + 2, nbtY, 70, 16, Component.empty(),
                new ArrayList<>(), val -> {});
        parent.addRenderableWidget(this.variantDropdown);

        // Property rule row: <property> <comparison> <value> <unit>
        int rulesY = nbtY + 20;
        int ruleX = centerX - 100;
        this.propertyDropdown = new SearchableDropdownWidget<>(ruleX, rulesY, 70, 16, Component.empty(),
                new ArrayList<>(), val -> updateUnitOptions());
        parent.addRenderableWidget(this.propertyDropdown);

        List<SearchableDropdownWidget.DropdownEntry<NbtRule.Comparison>> cmpEntries = new ArrayList<>();
        for (NbtRule.Comparison cmp : NbtRule.Comparison.values()) {
            cmpEntries.add(new SearchableDropdownWidget.DropdownEntry<>(cmp, Component.literal(cmp.symbol()), null));
        }
        this.comparisonDropdown = new SearchableDropdownWidget<>(ruleX + 74, rulesY, 34, 16, Component.empty(),
                cmpEntries, val -> {});
        this.comparisonDropdown.setSelectedValueSilently(savedComparison);
        parent.addRenderableWidget(this.comparisonDropdown);

        this.ruleValueBox = new NumberPickerWidget(ruleX + 112, rulesY, 50, 16, Component.empty(), savedRuleValue, val -> {});
        this.ruleValueBox.setRange(0, Double.MAX_VALUE);
        parent.addRenderableWidget(this.ruleValueBox);

        List<SearchableDropdownWidget.DropdownEntry<Boolean>> unitEntries = new ArrayList<>();
        unitEntries.add(new SearchableDropdownWidget.DropdownEntry<>(Boolean.TRUE,
                Component.translatable("screen.logistics.computer.tab.transfer.unit.percent"), null));
        unitEntries.add(new SearchableDropdownWidget.DropdownEntry<>(Boolean.FALSE,
                Component.translatable("screen.logistics.computer.tab.transfer.unit.absolute"), null));
        this.unitDropdown = new SearchableDropdownWidget<>(ruleX + 166, rulesY, 34, 16, Component.empty(),
                unitEntries, val -> {});
        this.unitDropdown.setSelectedValueSilently(savedRulePercent);
        parent.addRenderableWidget(this.unitDropdown);

        // Transfer button
        this.transferButton = new StyledButton(
                x + width / 2 - 50,
                y + height - 30,
                100,
                20,
                Component.translatable("screen.logistics.computer.tab.transfer.title"),
                this::executeTransfer
        );
        parent.addRenderableWidget(this.transferButton);

        refreshAccessorDropdowns();
        onItemSelected();
    }

    private List<SearchableDropdownWidget.DropdownEntry<ItemFilter.NbtMode>> buildNbtModeEntries() {
        List<SearchableDropdownWidget.DropdownEntry<ItemFilter.NbtMode>> entries = new ArrayList<>();
        for (ItemFilter.NbtMode mode : ItemFilter.NbtMode.values()) {
            entries.add(new SearchableDropdownWidget.DropdownEntry<>(mode,
                    Component.translatable("screen.logistics.computer.tab.transfer.nbt_mode." + mode.name().toLowerCase()),
                    null));
        }
        return entries;
    }

    public void refreshAccessorDropdowns() {
        List<SearchableDropdownWidget.DropdownEntry<BlockPos>> leftEntries = new ArrayList<>();
        List<SearchableDropdownWidget.DropdownEntry<BlockPos>> rightEntries = new ArrayList<>();
        List<SearchableDropdownWidget.DropdownEntry<String>> itemEntries = new ArrayList<>();

        leftEntries.add(new SearchableDropdownWidget.DropdownEntry<>(null,
                Component.translatable("screen.logistics.computer.tab.transfer.auto"),
                SearchableDropdownWidget.DropdownIcon.of(com.restonic4.logistics.experiment.Items.CHIP.getItem())));

        Set<String> addedItemIds = new HashSet<>();

        for (AccessorNode accessorNode : accessors) {
            BlockPos pos = accessorNode.getBlockPos();

            leftEntries.add(new SearchableDropdownWidget.DropdownEntry<>(pos,
                    Component.literal(accessorNode.getSafeName()),
                    SearchableDropdownWidget.DropdownIcon.of(BlockRegistry.ACCESSOR_BLOCK.getBlock())));

            rightEntries.add(new SearchableDropdownWidget.DropdownEntry<>(pos,
                    Component.literal(accessorNode.getSafeName()),
                    SearchableDropdownWidget.DropdownIcon.of(BlockRegistry.ACCESSOR_BLOCK.getBlock())));

            for (ItemStack itemStack : accessorNode.getReplicatedInventory()) {
                if (itemStack.getItem() == Items.AIR) continue;
                ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
                String itemId = registryName.toString();

                if (!addedItemIds.contains(itemId)) {
                    addedItemIds.add(itemId);
                    itemEntries.add(new SearchableDropdownWidget.DropdownEntry<>(itemId,
                            Component.translatable(itemStack.getItem().getDescriptionId()),
                            SearchableDropdownWidget.DropdownIcon.of(itemStack)));
                }
            }
        }

        if (leftAccessor != null) {
            leftAccessor.setOptions(leftEntries);
            leftAccessor.setSelectedValue(savedLeft);
        }

        if (rightAccessor != null) {
            rightAccessor.setOptions(rightEntries);
            if (savedRight != null) rightAccessor.setSelectedValue(savedRight);
        }

        if (textBox != null) {
            textBox.setOptions(itemEntries);
        }
    }

    // =====================================================================
    // NBT filter controls
    // =====================================================================

    /** Every stack of the currently selected item visible in the network's replicated inventories. */
    private List<ItemStack> getSelectedItemStacks() {
        String itemId = textBox != null ? textBox.getSelectedValue() : null;
        List<ItemStack> stacks = new ArrayList<>();
        if (itemId == null) return stacks;

        for (AccessorNode accessorNode : accessors) {
            for (ItemStack stack : accessorNode.getReplicatedInventory()) {
                if (stack.isEmpty()) continue;
                if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) {
                    stacks.add(stack);
                }
            }
        }
        return stacks;
    }

    /** Rebuilds the variant and property dropdowns for the newly selected item. */
    private void onItemSelected() {
        if (variantDropdown == null || propertyDropdown == null) return;

        List<ItemStack> stacks = getSelectedItemStacks();

        // Distinct NBT variants of this item currently in the network
        Set<CompoundTag> seenTags = new LinkedHashSet<>();
        boolean hasUntagged = false;
        for (ItemStack stack : stacks) {
            if (stack.getTag() == null) hasUntagged = true;
            else seenTags.add(stack.getTag());
        }

        List<SearchableDropdownWidget.DropdownEntry<CompoundTag>> variantEntries = new ArrayList<>();
        if (hasUntagged || seenTags.isEmpty()) {
            variantEntries.add(new SearchableDropdownWidget.DropdownEntry<>(null,
                    Component.translatable("screen.logistics.computer.tab.transfer.variant.no_nbt"), null));
        }
        int variantIndex = 1;
        for (CompoundTag tag : seenTags) {
            ItemStack display = stacks.stream()
                    .filter(s -> Objects.equals(s.getTag(), tag))
                    .findFirst().map(ItemStack::copy).orElse(ItemStack.EMPTY);
            variantEntries.add(new SearchableDropdownWidget.DropdownEntry<>(tag,
                    Component.literal(describeVariant(display, variantIndex)),
                    SearchableDropdownWidget.DropdownIcon.of(display)));
            variantIndex++;
        }
        variantDropdown.setOptions(variantEntries);
        if (savedVariantTag != null) variantDropdown.setSelectedValueSilently(savedVariantTag);

        // Properties that make sense for this item; fall back to all registered ones
        Set<NbtProperty> applicable = new LinkedHashSet<>();
        String itemId = textBox != null ? textBox.getSelectedValue() : null;
        if (itemId != null) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
            if (item != Items.AIR) applicable.addAll(NbtPropertyRegistry.propertiesFor(new ItemStack(item)));
        }
        for (ItemStack stack : stacks) {
            applicable.addAll(NbtPropertyRegistry.propertiesFor(stack));
        }
        Iterable<NbtProperty> shown = applicable.isEmpty() ? NbtPropertyRegistry.getAll() : applicable;

        List<SearchableDropdownWidget.DropdownEntry<ResourceLocation>> propertyEntries = new ArrayList<>();
        for (NbtProperty property : shown) {
            propertyEntries.add(new SearchableDropdownWidget.DropdownEntry<>(property.getId(),
                    property.getDisplayName(), null));
        }
        propertyDropdown.setOptions(propertyEntries);
        propertyDropdown.setSelectedValueSilently(savedPropertyId);
        if (propertyDropdown.getSelectedValue() == null && !propertyEntries.isEmpty()) {
            propertyDropdown.setSelectedIndex(0);
        }

        updateNbtControls();
    }

    /** A short label for one NBT variant, using a registered property's value when possible. */
    private String describeVariant(ItemStack stack, int index) {
        List<NbtProperty> properties = NbtPropertyRegistry.propertiesFor(stack);
        if (!properties.isEmpty()) {
            NbtProperty property = properties.get(0);
            return property.getDisplayName().getString() + " " + property.describeValue(stack);
        }
        return Component.translatable("screen.logistics.computer.tab.transfer.variant").getString() + " #" + index;
    }

    /** Shows/hides the mode-dependent widgets. */
    private void updateNbtControls() {
        ItemFilter.NbtMode mode = nbtModeDropdown != null ? nbtModeDropdown.getSelectedValue() : ItemFilter.NbtMode.ANY;
        if (mode == null) mode = ItemFilter.NbtMode.ANY;

        boolean exact = mode == ItemFilter.NbtMode.EXACT;
        boolean rules = mode == ItemFilter.NbtMode.RULES;

        if (variantDropdown != null) variantDropdown.visible = exact;
        if (propertyDropdown != null) propertyDropdown.visible = rules;
        if (comparisonDropdown != null) comparisonDropdown.visible = rules;
        if (ruleValueBox != null) ruleValueBox.visible = rules;
        if (unitDropdown != null) unitDropdown.visible = rules;

        updateUnitOptions();
    }

    /** Clamps the rule value range when the unit is percent. */
    private void updateUnitOptions() {
        if (ruleValueBox == null || unitDropdown == null) return;
        boolean percent = !Boolean.FALSE.equals(unitDropdown.getSelectedValue());
        ruleValueBox.setRange(0, percent ? 100 : Double.MAX_VALUE);
    }

    private ItemFilter buildFilter(String itemId) {
        ItemFilter filter = new ItemFilter(new ResourceLocation(itemId));

        ItemFilter.NbtMode mode = nbtModeDropdown != null ? nbtModeDropdown.getSelectedValue() : ItemFilter.NbtMode.ANY;
        if (mode == null) mode = ItemFilter.NbtMode.ANY;
        filter.setMode(mode);

        if (mode == ItemFilter.NbtMode.EXACT && variantDropdown != null) {
            CompoundTag tag = variantDropdown.getSelectedValue();
            filter.setExactTag(tag != null ? tag.copy() : null);
        }

        if (mode == ItemFilter.NbtMode.RULES && propertyDropdown != null && propertyDropdown.getSelectedValue() != null) {
            NbtRule rule = new NbtRule();
            rule.setPropertyId(propertyDropdown.getSelectedValue());
            if (comparisonDropdown != null && comparisonDropdown.getSelectedValue() != null) {
                rule.setComparison(comparisonDropdown.getSelectedValue());
            }
            if (ruleValueBox != null) rule.setThreshold(ruleValueBox.getValue());
            if (unitDropdown != null) rule.setPercent(!Boolean.FALSE.equals(unitDropdown.getSelectedValue()));
            filter.getRules().add(rule);
        }

        return filter;
    }

    // =====================================================================
    // Transfer
    // =====================================================================

    private void executeTransfer() {
        BlockPos from = leftAccessor != null ? leftAccessor.getSelectedValue() : null;
        BlockPos target = rightAccessor != null ? rightAccessor.getSelectedValue() : null;
        if (target == null) return;

        String itemId = textBox != null ? textBox.getSelectedValue() : null;
        if (itemId == null) return;

        int qty = quantityBox != null ? (int) quantityBox.getValue() : 1;

        saveState();

        ClientNetworking.sendToServer(new ComputerTransferPacket(
                ComputerScreen.getComputerNode().getBlockPos(), from, target, qty, buildFilter(itemId)));
    }

    private void saveState() {
        if (quantityBox != null) savedQuantity = (int) quantityBox.getValue();
        if (textBox != null) savedItem = textBox.getSelectedValue();
        if (leftAccessor != null) savedLeft = leftAccessor.getSelectedValue();
        if (rightAccessor != null) savedRight = rightAccessor.getSelectedValue();
        if (nbtModeDropdown != null && nbtModeDropdown.getSelectedValue() != null) savedNbtMode = nbtModeDropdown.getSelectedValue();
        if (variantDropdown != null) savedVariantTag = variantDropdown.getSelectedValue();
        if (propertyDropdown != null && propertyDropdown.getSelectedValue() != null) savedPropertyId = propertyDropdown.getSelectedValue();
        if (comparisonDropdown != null && comparisonDropdown.getSelectedValue() != null) savedComparison = comparisonDropdown.getSelectedValue();
        if (ruleValueBox != null) savedRuleValue = ruleValueBox.getValue();
        if (unitDropdown != null) savedRulePercent = !Boolean.FALSE.equals(unitDropdown.getSelectedValue());
    }

    @Override
    public void onHide() {
        saveState();
    }

    @Override
    public void tick() {
        if (quantityBox != null) quantityBox.tick();
        if (ruleValueBox != null && ruleValueBox.visible) ruleValueBox.tick();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta, int x, int y, int width, int height) {
        renderArrow(gfx, x, y, width, height);
    }

    private void renderArrow(GuiGraphics gfx, int x, int y, int width, int height) {
        int midW = 50;
        int midY = y + height / 2 - midW / 2 - 10;

        int startX = (x + 20) + midW + 8;
        int endX = (x + width - midW - 20) - 8;
        int cy = midY + midW / 2;
        if (endX <= startX) return;

        int color = 0xFFFFFFFF;
        int shaftThick = 6;
        int headLen = 12;
        int headHalf = 7;
        int shaftEnd = endX - headLen;

        gfx.fill(startX, cy - shaftThick / 2, shaftEnd, cy + shaftThick / 2, color);

        for (int i = 0; i < headLen; i++) {
            int px = shaftEnd + i;
            int halfH = (headHalf * (headLen - i) + headLen / 2) / headLen;
            if (halfH < 1) halfH = 1;
            gfx.fill(px, cy - halfH, px + 1, cy + halfH, color);
        }
    }
}
