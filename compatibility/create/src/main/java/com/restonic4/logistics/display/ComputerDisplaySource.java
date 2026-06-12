package com.restonic4.logistics.display;

import com.restonic4.logistics.blocks.computer.ComputerNode;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComputerDisplaySource extends DisplaySource {

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        List<MutableComponent> result = new ArrayList<>();
        if (!(context.level() instanceof ServerLevel serverLevel)) return result;

        NetworkNode node = NetworkManager.get(serverLevel).getNodeByBlockPos(context.getSourcePos());
        if (!(node instanceof ComputerNode computerNode)) return result;

        // Read the saved selection index from the Display Link's NBT configuration
        int modeIndex = context.sourceConfig().getInt("DisplayMode");
        ComputerDisplayMode currentMode = ComputerDisplayMode.byIndex(modeIndex);

        // Change the displayed text depending on what mode the user selected
        String dataString = switch (currentMode) {
            case STORED_ENERGY -> "Energy: " + computerNode.getNetwork().getStoredEnergyBuffer();
            case TOTAL_ENERGY -> "Max E: " + computerNode.getNetwork().getTotalEnergyBuffer();
            case NODE_COUNT -> "Nodes: " + computerNode.getNetwork().getNodeIndex().size();
        };

        result.add(Component.literal(dataString));
        return result;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
        if (!isFirstLine) return; // Prevent duplicating UI elements if the source spans multiple lines

        // Gather display names as vanilla Components
        List<net.minecraft.network.chat.Component> UIoptions = new ArrayList<>();
        for (ComputerDisplayMode mode : ComputerDisplayMode.values()) {
            UIoptions.add(net.minecraft.network.chat.Component.literal(mode.getDisplayName()));
        }

        builder.addSelectionScrollInput(
                0, // x offset
                120, // width of the component area
                (scrollInput, label) -> {
                    // This BiConsumer configures the raw Create widget when it spawns
                    scrollInput.forOptions(UIoptions)
                            .titled(net.minecraft.network.chat.Component.literal("Display Type"));

                    // Get the current value from NBT to set the initial index (defaults to 0)
                    int currentSelection = context.sourceConfig().getInt("DisplayMode");
                    scrollInput.setState(currentSelection);

                    // Attach a callback that runs when the player scrolls/selects an option
                    scrollInput.calling(selectedIndex -> {
                        context.sourceConfig().putInt("DisplayMode", selectedIndex);
                    });
                },
                "DisplayMode" // This string key tells Create where to auto-track it in context.sourceConfig()
        );
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}