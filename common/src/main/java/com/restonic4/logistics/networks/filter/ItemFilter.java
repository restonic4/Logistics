package com.restonic4.logistics.networks.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Describes which item stacks an item-network operation (transfer, count, restock) applies to:
 * an item ID plus how the stack's NBT is matched.
 * <p>
 * The three NBT modes cover the whole spectrum of "does the player care about NBT?":
 * <ul>
 *   <li>{@link NbtMode#ANY} — pick whatever; NBT is ignored entirely.</li>
 *   <li>{@link NbtMode#EXACT} — only stacks whose tag equals one captured tag
 *       ({@code null} = "no NBT"). Works for arbitrary, unregistered NBT.</li>
 *   <li>{@link NbtMode#RULES} — only stacks satisfying every {@link NbtRule}
 *       (e.g. "energy ≥ 50%"). Requires the relevant {@link NbtProperty} to be registered.</li>
 * </ul>
 * A filter with no item selected matches nothing. Stack counts are never considered.
 */
public final class ItemFilter {
    private static final String TAG_ITEM = "item";
    private static final String TAG_MODE = "mode";
    private static final String TAG_EXACT = "exactTag";
    private static final String TAG_HAS_EXACT = "hasExactTag";
    private static final String TAG_RULES = "rules";

    public enum NbtMode { ANY, EXACT, RULES }

    @Nullable private ResourceLocation itemId;
    private NbtMode mode = NbtMode.ANY;
    @Nullable private CompoundTag exactTag;
    private final List<NbtRule> rules = new ArrayList<>();

    public ItemFilter() {}

    public ItemFilter(@Nullable ResourceLocation itemId) {
        this.itemId = itemId;
    }

    /** Convenience for the common "this item, any NBT" case. */
    public static ItemFilter any(ResourceLocation itemId) {
        return new ItemFilter(itemId);
    }

    @Nullable public ResourceLocation getItemId() { return itemId; }
    public void setItemId(@Nullable ResourceLocation itemId) { this.itemId = itemId; }

    public NbtMode getMode() { return mode; }
    public void setMode(NbtMode mode) { this.mode = mode; }

    /** The tag stacks must equal in {@link NbtMode#EXACT}; {@code null} means "no NBT". */
    @Nullable public CompoundTag getExactTag() { return exactTag; }
    public void setExactTag(@Nullable CompoundTag exactTag) { this.exactTag = exactTag; }

    /** The conditions ANDed together in {@link NbtMode#RULES}; mutable. */
    public List<NbtRule> getRules() { return rules; }

    /** The filtered item, or {@link Items#AIR} if none/unknown is selected. */
    public Item getItem() {
        if (itemId == null) return Items.AIR;
        return BuiltInRegistries.ITEM.get(itemId);
    }

    /** Whether an actual item is selected (the filter can match anything at all). */
    public boolean isValid() {
        return itemId != null && (getItem() != Items.AIR || itemId.equals(BuiltInRegistries.ITEM.getKey(Items.AIR)));
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || itemId == null) return false;
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) return false;

        return switch (mode) {
            case ANY -> true;
            case EXACT -> Objects.equals(stack.getTag(), exactTag);
            case RULES -> {
                for (NbtRule rule : rules) {
                    if (!rule.matches(stack)) yield false;
                }
                yield true;
            }
        };
    }

    /** A representative stack for icons/tooltips (carries the exact tag when in EXACT mode). */
    public ItemStack createDisplayStack() {
        ItemStack stack = new ItemStack(getItem());
        if (mode == NbtMode.EXACT && exactTag != null && !stack.isEmpty()) {
            stack.setTag(exactTag.copy());
        }
        return stack;
    }

    /** Short text for logs/summaries, e.g. {@code "logistics:kinetic_crystal_shard [energy ≥ 50%]"}. */
    public String describe() {
        String item = itemId == null ? "?" : itemId.toString();
        return switch (mode) {
            case ANY -> item;
            case EXACT -> item + " [exact NBT]";
            case RULES -> rules.isEmpty() ? item : item + " [" +
                    rules.stream().map(NbtRule::describe).collect(Collectors.joining(", ")) + "]";
        };
    }

    public void save(CompoundTag tag) {
        if (itemId != null) tag.putString(TAG_ITEM, itemId.toString());
        tag.putString(TAG_MODE, mode.name());
        tag.putBoolean(TAG_HAS_EXACT, exactTag != null);
        if (exactTag != null) tag.put(TAG_EXACT, exactTag.copy());

        ListTag rulesTag = new ListTag();
        for (NbtRule rule : rules) {
            CompoundTag ruleTag = new CompoundTag();
            rule.save(ruleTag);
            rulesTag.add(ruleTag);
        }
        tag.put(TAG_RULES, rulesTag);
    }

    public void load(CompoundTag tag) {
        this.itemId = tag.contains(TAG_ITEM, Tag.TAG_STRING) ? new ResourceLocation(tag.getString(TAG_ITEM)) : null;
        this.mode = NbtMode.valueOf(tag.getString(TAG_MODE));
        this.exactTag = tag.getBoolean(TAG_HAS_EXACT) ? tag.getCompound(TAG_EXACT) : null;

        rules.clear();
        ListTag rulesTag = tag.getList(TAG_RULES, Tag.TAG_COMPOUND);
        for (int i = 0; i < rulesTag.size(); i++) {
            rules.add(NbtRule.fromTag(rulesTag.getCompound(i)));
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(itemId != null);
        if (itemId != null) buf.writeResourceLocation(itemId);
        buf.writeEnum(mode);
        buf.writeNbt(exactTag);
        buf.writeVarInt(rules.size());
        for (NbtRule rule : rules) {
            rule.write(buf);
        }
    }

    public void read(FriendlyByteBuf buf) {
        this.itemId = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.mode = buf.readEnum(NbtMode.class);
        this.exactTag = buf.readNbt();
        rules.clear();
        int ruleCount = buf.readVarInt();
        for (int i = 0; i < ruleCount; i++) {
            rules.add(NbtRule.fromBuf(buf));
        }
    }

    public static ItemFilter fromTag(CompoundTag tag) {
        ItemFilter filter = new ItemFilter();
        filter.load(tag);
        return filter;
    }

    public static ItemFilter fromBuf(FriendlyByteBuf buf) {
        ItemFilter filter = new ItemFilter();
        filter.read(buf);
        return filter;
    }

    public ItemFilter copy() {
        CompoundTag tag = new CompoundTag();
        save(tag);
        return fromTag(tag);
    }
}
