package com.restonic4.logistics.blocks.computer.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class RoleEntryWidget extends AbstractWidget {
    private final ResourceLocation icon;
    private String roleName;
    private final Consumer<String> onNameChanged;
    private final Runnable onUp;
    private final Runnable onDown;
    private final Runnable onDelete;          // NEW
    private final Consumer<Boolean> onSelect;

    private boolean selected = false;
    private boolean editing = false;
    private EditBox nameField;
    private long lastClickTime = 0;

    private static final int SELECTED_BG = 0x33FFFFFF;
    private static final int HOVER_BG = 0x22FFFFFF;

    public RoleEntryWidget(int x, int y, int width, int height, ResourceLocation icon, String name,
                           Consumer<String> onNameChanged, Runnable onUp, Runnable onDown,
                           Runnable onDelete,
                           Consumer<Boolean> onSelect) {
        super(x, y, width, height, Component.literal(name));
        this.icon = icon;
        this.roleName = name;
        this.onNameChanged = onNameChanged;
        this.onUp = onUp;
        this.onDown = onDown;
        this.onDelete = onDelete;
        this.onSelect = onSelect;

        Font font = Minecraft.getInstance().font;
        this.nameField = new EditBox(font, x + 24, y + (height - 10) / 2, width - 64, 12, Component.empty());
        this.nameField.setValue(name);
        this.nameField.setResponder(this::onTextChanged);
        this.nameField.setTextColor(0xFFFFFFFF);
        this.nameField.setBordered(false);
        this.nameField.setVisible(false);
        this.nameField.setEditable(false);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (!selected && editing) {
            stopEditing(true);
        }
    }

    public void setName(String name) {
        this.roleName = name;
        this.nameField.setValue(name);
        this.setMessage(Component.literal(name));
    }

    /*  LIVE COMMIT: every keystroke updates the underlying role immediately  */
    private void onTextChanged(String text) {
        if (!editing) return;
        this.roleName = text;
        if (onNameChanged != null) {
            onNameChanged.accept(roleName);
        }
    }

    private void startEditing() {
        this.editing = true;
        this.nameField.setVisible(true);
        this.nameField.setEditable(true);
        this.nameField.setFocused(true);
        this.setFocused(true);   // ensure we receive setFocused(false) when clicked away
    }

    private void stopEditing(boolean confirm) {
        if (!editing) return;
        this.editing = false;
        this.nameField.setVisible(false);
        this.nameField.setEditable(false);
        this.nameField.setFocused(false);

        if (!confirm || roleName.isBlank()) {
            /*  Escape / blank = revert both visual and underlying data  */
            String oldName = getMessage().getString();
            roleName = oldName;
            nameField.setValue(oldName);
            if (onNameChanged != null) {
                onNameChanged.accept(oldName);
            }
        } else {
            if (!roleName.equals(getMessage().getString())) {
                this.setMessage(Component.literal(roleName));
                // onNameChanged was already fired by onTextChanged, no need again
            }
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused && editing) {
            stopEditing(true);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hovered = isMouseOver(mouseX, mouseY);

        // Background
        if (selected) {
            graphics.fill(x, y, x + w, y + h, SELECTED_BG);
        } else if (hovered) {
            graphics.fill(x, y, x + w, y + h, HOVER_BG);
        }

        // Icon
        if (icon != null) {
            graphics.blit(icon, x + 4, y + (h - 16) / 2, 0, 0, 16, 16, 16, 16);
        }

        // Name
        if (editing) {
            nameField.setX(x + 24);
            nameField.setY(y + (h - 10) / 2);
            nameField.setWidth(w - 64);
            nameField.render(graphics, mouseX, mouseY, partialTick);
        } else {
            Font font = Minecraft.getInstance().font;
            String text = roleName;
            int maxWidth = w - 64;
            if (font.width(text) > maxWidth) {
                text = font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
            }
            graphics.drawString(font, text, x + 24, y + (h - 8) / 2, 0xFFFFFFFF, false);
        }

        // Arrows + Delete (visible on hover or selected)
        if (hovered || selected) {
            boolean canUp = onUp != null;
            boolean canDown = onDown != null;
            int arrowX = x + w - 28;
            int arrowY = y + (h - 12) / 2;

            if (canUp) {
                boolean arrowHover = mouseX >= arrowX && mouseX < arrowX + 12 && mouseY >= arrowY && mouseY < arrowY + 5;
                int color = arrowHover ? 0xFFFFFF55 : 0xFFAAAAAA;
                renderArrow(graphics, arrowX, arrowY, 12, 5, true, color);
            }

            if (canDown) {
                int downY = arrowY + 5 + 2;
                boolean arrowHover = mouseX >= arrowX && mouseX < arrowX + 12 && mouseY >= downY && mouseY < downY + 5;
                int color = arrowHover ? 0xFFFFFF55 : 0xFFAAAAAA;
                renderArrow(graphics, arrowX, downY, 12, 5, false, color);
            }

            /*  DELETE BUTTON  */
            if (onDelete != null) {
                int delX = x + w - 14;
                int delY = y + (h - 10) / 2;
                boolean delHover = mouseX >= delX && mouseX < delX + 12 && mouseY >= delY && mouseY < delY + 10;
                int delColor = delHover ? 0xFFFF5555 : 0xFFAAAAAA;
                graphics.drawString(Minecraft.getInstance().font, "×", delX, delY, delColor, false);
            }
        }
    }

    private void renderArrow(GuiGraphics graphics, int x, int y, int w, int h, boolean up, int color) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        int size = Math.min(w, h);
        int half = size / 2;
        int startY = cy - half;

        for (int i = 0; i < size; i++) {
            int halfWidth = up ? i : (size - 1 - i);
            int rowY = startY + i;
            graphics.fill(cx - halfWidth, rowY, cx + halfWidth + 1, rowY + 1, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.clicked(mouseX, mouseY)) return false;

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        /*  DELETE CHECK  */
        if (onDelete != null && mouseX >= x + w - 14 && mouseX < x + w - 2
                && mouseY >= y + (h - 10) / 2 && mouseY < y + (h - 10) / 2 + 10) {
            onDelete.run();
            return true;
        }

        int arrowX = x + w - 28;
        int arrowY = y + (h - 12) / 2;

        // Check up arrow
        if (onUp != null && mouseX >= arrowX && mouseX < arrowX + 12 && mouseY >= arrowY && mouseY < arrowY + 5) {
            onUp.run();
            return true;
        }

        // Check down arrow
        if (onDown != null && mouseX >= arrowX && mouseX < arrowX + 12 && mouseY >= arrowY + 5 + 2 && mouseY < arrowY + 5 + 2 + 5) {
            onDown.run();
            return true;
        }

        // Select
        onSelect.accept(true);

        // Double-click or click on name area to edit
        if (button == 0 && mouseX >= x + 24 && mouseX < x + w - 32) {
            long now = System.currentTimeMillis();
            boolean isDoubleClick = (now - lastClickTime) < 250;
            lastClickTime = now;

            if (selected && isDoubleClick && onNameChanged != null) {
                startEditing();
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                stopEditing(true);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                stopEditing(false);
                return true;
            }
            return nameField.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (editing) {
            return nameField.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}