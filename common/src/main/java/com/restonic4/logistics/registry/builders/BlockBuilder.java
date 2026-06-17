package com.restonic4.logistics.registry.builders;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.platform.Services;
import com.restonic4.logistics.registry.NetworkTypeRegistry;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.restonic4.logistics.registry.PlatformRegistry;
import com.restonic4.logistics.registry.entries.BlockEntry;
import com.restonic4.logistics.registry.variants.GenerationContext;
import com.restonic4.logistics.registry.variants.VariantArchetype;
import com.restonic4.logistics.registry.variants.archetypes.CubeAllArchetype;
import com.restonic4.logistics.registry.variants.archetypes.CubeBottomTopArchetype;
import com.restonic4.logistics.registry.variants.archetypes.LitArchetype;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class BlockBuilder<B extends Block, N extends NetworkNode> {
    private final ResourceLocation id;
    private final Supplier<B> blockFactory;

    private NetworkTypeRegistry.NetworkType<?> networkType;
    private NodeTypeRegistry.NodeFactory<N> nodeFactory;

    private Function<B, Item> itemFactory;

    private BlockEntityType.BlockEntitySupplier<? extends BlockEntity> blockEntitySupplier;

    private final List<ResourceKey<CreativeModeTab>> tabs = new ArrayList<>();

    private VariantArchetype variantArchetype;

    public BlockBuilder(ResourceLocation id, Supplier<B> blockFactory) {
        this.id = id;
        this.blockFactory = blockFactory;
    }

    @SuppressWarnings("unchecked")
    public <N2 extends NetworkNode> BlockBuilder<B, N2> network(
            NetworkTypeRegistry.NetworkType<?> networkType,
            NodeTypeRegistry.NodeFactory<N2> nodeFactory
    ) {
        this.networkType = networkType;
        this.nodeFactory = (NodeTypeRegistry.NodeFactory<N>) nodeFactory;
        return (BlockBuilder<B, N2>) this;
    }

    public BlockBuilder<B, N> withItem() {
        this.itemFactory = block -> new BlockItem(block, new Item.Properties());
        return this;
    }

    public BlockBuilder<B, N> withItem(Item.Properties properties) {
        this.itemFactory = block -> new BlockItem(block, properties);
        return this;
    }

    public BlockBuilder<B, N> withItem(Function<B, Item> factory) {
        this.itemFactory = factory;
        return this;
    }

    public BlockBuilder<B, N> withBlockEntity(
            BlockEntityType.BlockEntitySupplier<? extends BlockEntity> supplier
    ) {
        this.blockEntitySupplier = supplier;
        return this;
    }

    public final BlockBuilder<B, N> addToTab(ResourceKey<CreativeModeTab> tabKey) {
        this.tabs.add(tabKey);
        return this;
    }

    public BlockBuilder<B, N> mineWithPickaxe() {
        return scheduleTag("mineable/pickaxe");
    }

    public BlockBuilder<B, N> mineWithAxe() {
        return scheduleTag("mineable/axe");
    }

    public BlockBuilder<B, N> mineWithShovel() {
        return scheduleTag("mineable/shovel");
    }

    public BlockBuilder<B, N> mineWithHoe() {
        return scheduleTag("mineable/hoe");
    }

    public BlockBuilder<B, N> mineWithSword() {
        return scheduleTag("sword_efficient");
    }

    public BlockBuilder<B, N> needsStoneTool() {
        return scheduleTag("needs_stone_tool");
    }

    public BlockBuilder<B, N> needsIronTool() {
        return scheduleTag("needs_iron_tool");
    }

    public BlockBuilder<B, N> needsDiamondTool() {
        return scheduleTag("needs_diamond_tool");
    }

    private BlockBuilder<B, N> scheduleTag(String path) {
        PlatformRegistry.scheduleBlockTagInjection(new ResourceLocation("minecraft", path), this.id);
        return this;
    }

    public BlockBuilder<B, N> dropSelf() {
        return dropSelf(true);
    }

    public BlockBuilder<B, N> dropSelf(boolean survivesExplosion) {
        PlatformRegistry.scheduleSelfDropLootInjection(this.id, survivesExplosion);
        return this;
    }

    /**
     * Auto-generates this block's assets at runtime from the given archetype (blockstate, models,
     * item model), instead of shipping hand-written JSON. Generated resources sit at the bottom of
     * the pack stack, so dropping a real JSON file with the same id overrides any of them.
     */
    public BlockBuilder<B, N> variant(VariantArchetype archetype) {
        this.variantArchetype = archetype;
        return this;
    }

    /** Convenience: non-directional full cube, same texture on all faces (wallpapers, tiles...). */
    public BlockBuilder<B, N> cubeAll() {
        return variant(new CubeAllArchetype(null));
    }

    /** Convenience: cube_all with an explicit texture reference (e.g. "logistics:block/wallpaper/red"). */
    public BlockBuilder<B, N> cubeAll(String texture) {
        return variant(new CubeAllArchetype(texture));
    }

    /** Convenience: cube with distinct side vs top/bottom textures (wood-trim wallpapers, floors...). */
    public BlockBuilder<B, N> cubeBottomTop(String sideTexture, String endTexture) {
        return variant(new CubeBottomTopArchetype(sideTexture, endTexture));
    }

    /** Convenience: full cube driven by the vanilla {@code lit} property (lamps). */
    public BlockBuilder<B, N> litCubeAll(String offTexture, String onTexture) {
        return variant(new LitArchetype(offTexture, onTexture));
    }

    @SuppressWarnings("unchecked")
    public BlockEntry<B, N> register() {
        if (this.variantArchetype != null) {
            this.variantArchetype.emit(new GenerationContext(this.id));
        }

        NodeTypeRegistry.NetworkNodeType<N> nodeType = null;
        if (this.networkType != null && this.nodeFactory != null) {
            nodeType = new NodeTypeRegistry.NetworkNodeType<>(this.networkType, this.nodeFactory);
        }
        NodeTypeRegistry.register(id, nodeType);

        return (BlockEntry<B, N>) Services.PLATFORM_REGISTRY.fromBlockBuilder(
                this.id,
                this.blockFactory,
                (BlockEntityType.BlockEntitySupplier<BlockEntity>) this.blockEntitySupplier,
                (Function<Block, Item>) this.itemFactory,
                (NodeTypeRegistry.NetworkNodeType<NetworkNode>) nodeType,
                tabs
        );
    }
}
