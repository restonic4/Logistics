package com.restonic4.logistics.networks.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public final class TooltipBuilder {
    private final List<TooltipElement> elements = new ArrayList<>();

    public TooltipBuilder text(String text, ChatFormatting... formats) {
        MutableComponent c = Component.literal(text);
        if (formats.length > 0) c = c.withStyle(formats);
        elements.add(new TooltipElement(ElementType.TEXT, c));
        return this;
    }

    public TooltipBuilder text(Component component) {
        elements.add(new TooltipElement(ElementType.TEXT, component));
        return this;
    }

    public TooltipBuilder title(String text, ChatFormatting color) {
        MutableComponent c = Component.literal(text)
                .withStyle(color, ChatFormatting.BOLD);
        elements.add(new TooltipElement(ElementType.TEXT, c));
        return this;
    }

    public TooltipBuilder line() {
        elements.add(new TooltipElement(ElementType.LINE, null));
        return this;
    }

    public TooltipBuilder bullet(String text, ChatFormatting... formats) {
        MutableComponent prefix  = Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY);
        MutableComponent content = Component.literal(text);
        if (formats.length > 0) content = content.withStyle(formats);
        MutableComponent full = prefix.append(content);
        elements.add(new TooltipElement(ElementType.BULLET, full));
        return this;
    }

    public TooltipBuilder dash(String text, ChatFormatting... formats) {
        MutableComponent prefix  = Component.literal("- ").withStyle(ChatFormatting.DARK_GRAY);
        MutableComponent content = Component.literal(text);
        if (formats.length > 0) content = content.withStyle(formats);
        MutableComponent full = prefix.append(content);
        elements.add(new TooltipElement(ElementType.DASH, full));
        return this;
    }

    public TooltipBuilder keyValue(String key, String value, ChatFormatting keyColor, ChatFormatting valueColor) {
        MutableComponent c = Component.literal(key + ": ").withStyle(keyColor).append(Component.literal(value).withStyle(valueColor));
        elements.add(new TooltipElement(ElementType.KEY_VALUE, c));
        return this;
    }

    public TooltipBuilder keyValue(String key, String value, ChatFormatting keyColor) {
        return keyValue(key, value, keyColor, ChatFormatting.WHITE);
    }

    public TooltipBuilder spacer() {
        elements.add(new TooltipElement(ElementType.TEXT, Component.empty()));
        return this;
    }

    public List<TooltipElement> build() {
        return List.copyOf(elements);
    }

    public List<Component> toComponentList() {
        List<Component> out = new ArrayList<>();
        for (TooltipElement el : elements) {
            if (el.type == ElementType.LINE) {
                out.add(Component.literal("───────────────").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                out.add(el.component);
            }
        }
        return out;
    }

    public enum ElementType {
        TEXT,
        LINE,
        BULLET,
        DASH,
        KEY_VALUE
    }

    public static final class TooltipElement {
        private final ElementType type;
        private final Component component;

        private TooltipElement(ElementType type, Component component) {
            this.type = type;
            this.component = component;
        }

        public ElementType getType() { return type; }
        public Component getComponent() { return component; }
    }
}
