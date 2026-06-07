package com.restonic4.logistics.ponder.util;

import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public class InputBuilder {
    private final PonderHelper ponderHelper;
    private final InputElementBuilder builder;

    public InputBuilder(PonderHelper ponderHelper, BlockPos pos, int ticks) {
        this.ponderHelper = ponderHelper;
        this.builder = this.ponderHelper.getScene().overlay().showControls(pos.getCenter(), Pointing.DOWN, ticks);
    }

    public InputBuilder withItem(ItemStack stack) {
        builder.withItem(stack);
        return this;
    }

    public InputBuilder leftClick() {
        builder.leftClick();
        return this;
    }

    public InputBuilder rightClick() {
        builder.rightClick();
        return this;
    }

    public InputBuilder scroll() {
        builder.scroll();
        return this;
    }

    public InputBuilder showing(ScreenElement icon) {
        builder.showing(icon);
        return this;
    }

    public InputBuilder whileSneaking() {
        builder.whileSneaking();
        return this;
    }

    public InputBuilder whileCTRL() {
        builder.whileCTRL();
        return this;
    }

    public PonderHelper build() {
        return ponderHelper;
    }
}
