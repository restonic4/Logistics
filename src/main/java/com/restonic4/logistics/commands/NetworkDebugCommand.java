package com.restonic4.logistics.commands;


public class NetworkDebugCommand {
/*
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("energynet")
                        .requires(source -> source.hasPermission(2)) // OP only

                        .then(Commands.literal("list")
                                .executes(NetworkDebugCommand::listNetworks))

                        .then(Commands.literal("info")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(ctx -> networkInfo(ctx,
                                                IntegerArgumentType.getInteger(ctx, "index")))))

                        .then(Commands.literal("here")
                                .executes(NetworkDebugCommand::networkHere))

                        .then(Commands.literal("stats")
                                .executes(NetworkDebugCommand::globalStats))
        );
    }

    // -------------------------------------------------------------------------
    // /energynet list
    // -------------------------------------------------------------------------

    private static int listNetworks(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        EnergyNetworkManager manager = EnergyNetworkManager.get(level);
        Collection<EnergyNetwork> networks = manager.getAllNetworks();

        if (networks.isEmpty()) {
            source.sendSuccess(() -> error("No energy networks found in this dimension."), false);
            return 0;
        }

        source.sendSuccess(() -> header("=== Energy Networks (" + networks.size() + " total) ==="), false);

        List<EnergyNetwork> list = new ArrayList<>(networks);
        for (int i = 0; i < list.size(); i++) {
            EnergyNetwork net = list.get(i);
            long loaded = net.getLoadedNodes().stream().filter(n -> !(n instanceof EnergyNode)).count();

            final int index = i;
            source.sendSuccess(() -> line(
                    ChatFormatting.YELLOW, "[" + index + "] ",
                    ChatFormatting.WHITE,  "UUID: ",
                    ChatFormatting.AQUA,   shortId(net) + "  ",
                    ChatFormatting.WHITE,  "Members: ",
                    ChatFormatting.GREEN,  String.valueOf(net.getMemberCount()) + "  ",
                    ChatFormatting.WHITE,  "Buffer: ",
                    energyColor(net.getEnergyBuffer(), net.getMaxBuffer()),
                    net.getEnergyBuffer() + "/" + net.getMaxBuffer() + " EU  ",
                    ChatFormatting.WHITE,  "Loaded nodes: ",
                    ChatFormatting.GOLD,   String.valueOf(net.getLoadedNodes().size())
            ), false);
        }

        source.sendSuccess(() -> dim("Use /energynet info <index> for details."), false);
        return list.size();
    }

    // -------------------------------------------------------------------------
    // /energynet info <index>
    // -------------------------------------------------------------------------

    private static int networkInfo(CommandContext<CommandSourceStack> ctx, int index) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        EnergyNetworkManager manager = EnergyNetworkManager.get(level);

        List<EnergyNetwork> list = new ArrayList<>(manager.getAllNetworks());
        if (index < 0 || index >= list.size()) {
            source.sendSuccess(() -> error("No network at index " + index + ". Use /energynet list first."), false);
            return 0;
        }

        EnergyNetwork net = list.get(index);
        printNetworkDetail(source, level, net);
        return 1;
    }

    // -------------------------------------------------------------------------
    // /energynet here
    // -------------------------------------------------------------------------

    private static int networkHere(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();

        // Check the block the player is standing on and the 5 blocks around their feet
        BlockPos center = BlockPos.containing(source.getPosition());
        EnergyNetworkManager manager = EnergyNetworkManager.get(level);

        EnergyNetwork found = null;
        outer:
        for (int dy = -1; dy <= 1 && found == null; dy++) {
            for (int dx = -1; dx <= 1 && found == null; dx++) {
                for (int dz = -1; dz <= 1 && found == null; dz++) {
                    found = manager.getNetworkAt(center.offset(dx, dy, dz));
                }
            }
        }

        if (found == null) {
            source.sendSuccess(() -> error("No energy network found near your position."), false);
            return 0;
        }

        final EnergyNetwork net = found;
        printNetworkDetail(source, level, net);
        return 1;
    }

    // -------------------------------------------------------------------------
    // /energynet stats
    // -------------------------------------------------------------------------

    private static int globalStats(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        EnergyNetworkManager manager = EnergyNetworkManager.get(level);
        Collection<EnergyNetwork> networks = manager.getAllNetworks();

        long totalNetworks   = networks.size();
        long totalMembers    = networks.stream().mapToLong(EnergyNetwork::getMemberCount).sum();
        long totalBuffer     = networks.stream().mapToLong(EnergyNetwork::getEnergyBuffer).sum();
        long totalMaxBuffer  = networks.stream().mapToLong(EnergyNetwork::getMaxBuffer).sum();
        long totalProduction = networks.stream().mapToLong(EnergyNetwork::getProductionPerTick).sum();
        long totalConsumption= networks.stream().mapToLong(EnergyNetwork::getConsumptionPerTick).sum();
        long loadedNetworks  = networks.stream().filter(n -> !n.getLoadedNodes().isEmpty()).count();

        source.sendSuccess(() -> header("=== Global Energy Stats ==="), false);
        source.sendSuccess(() -> kv("Networks total",   String.valueOf(totalNetworks)), false);
        source.sendSuccess(() -> kv("Networks loaded",  loadedNetworks + "/" + totalNetworks), false);
        source.sendSuccess(() -> kv("Total members",    String.valueOf(totalMembers)), false);
        source.sendSuccess(() -> kv("Total buffer",     totalBuffer + "/" + totalMaxBuffer + " EU"), false);
        source.sendSuccess(() -> kv("Total production", totalProduction + " EU/t"), false);
        source.sendSuccess(() -> kv("Total consumption",totalConsumption + " EU/t"), false);

        long net = totalProduction - totalConsumption;
        String sign = net >= 0 ? "+" : "";
        final String netStr = sign + net + " EU/t";
        source.sendSuccess(() -> kv("Net balance",
                totalProduction >= totalConsumption ? netStr : netStr), false);

        return 1;
    }

    // -------------------------------------------------------------------------
    // Shared detail printer
    // -------------------------------------------------------------------------

    private static void printNetworkDetail(CommandSourceStack source, ServerLevel level, EnergyNetwork net) {
        source.sendSuccess(() -> header("=== Network " + shortId(net) + " ==="), false);
        source.sendSuccess(() -> kv("Full UUID",        net.getId().toString()), false);
        source.sendSuccess(() -> kv("Member positions", String.valueOf(net.getMemberCount())), false);
        source.sendSuccess(() -> kv("Loaded nodes",     String.valueOf(net.getLoadedNodes().size())), false);
        source.sendSuccess(() -> kv("Buffer",           net.getEnergyBuffer() + " / " + net.getMaxBuffer() + " EU"
                + "  (" + pct(net.getEnergyBuffer(), net.getMaxBuffer()) + "%)"), false);
        source.sendSuccess(() -> kv("Production/t",     net.getProductionPerTick() + " EU/t"), false);
        source.sendSuccess(() -> kv("Consumption/t",    net.getConsumptionPerTick() + " EU/t"), false);

        long netBalance = net.getProductionPerTick() - net.getConsumptionPerTick();
        String sign = netBalance >= 0 ? "+" : "";
        source.sendSuccess(() -> kv("Net balance",      sign + netBalance + " EU/t"), false);
        source.sendSuccess(() -> kv("Last simulated",   "tick " + net.getLastSimulatedTick()), false);

        // --- Member detail ---
        source.sendSuccess(() -> subheader("-- Members --"), false);
        for (BlockPos pos : net.getMemberPositions()) {
            BlockEntity be = level.getBlockEntity(pos);
            String posStr = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();

            if (be instanceof GeneratorBlockEntity gen) {
                source.sendSuccess(() -> memberLine(posStr, "GENERATOR",
                        ChatFormatting.GREEN,
                        "produces " + gen.getEnergyProductionPerTick() + " EU/t" +
                                "  |  last extracted: " + gen.getLastExtracted() + " EU"), false);

            } else if (be instanceof BatteryBlockEntity bat) {
                source.sendSuccess(() -> memberLine(posStr, "BATTERY",
                        ChatFormatting.AQUA,
                        "stored: " + bat.getStoredEnergy() + "/" + bat.getMaxStoredEnergy() + " EU" +
                                "  (" + pct(bat.getStoredEnergy(), bat.getMaxStoredEnergy()) + "%)" +
                                "  charge: " + bat.getEnergyConsumptionPerTick() + " EU/t" +
                                "  discharge: " + bat.getEnergyProductionPerTick() + " EU/t"), false);

            } else if (be instanceof MachineBlockEntity machine) {
                String status = machine.isPoweredLastTick() ? "RUNNING" : "STALLED";
                ChatFormatting statusColor = machine.isPoweredLastTick() ? ChatFormatting.GREEN : ChatFormatting.RED;
                source.sendSuccess(() -> memberLine(posStr, "MACHINE",
                        statusColor,
                        status +
                                "  |  consumes " + machine.getEnergyConsumptionPerTick() + " EU/t" +
                                "  |  internal buffer: " + machine.getInternalBuffer() + "/" + MachineBlockEntity.INTERNAL_BUFFER_MAX + " EU"), false);

            } else if (be instanceof EnergyNode) {
                // Unknown node type — generic display
                source.sendSuccess(() -> memberLine(posStr, "NODE",
                        ChatFormatting.WHITE, be.getClass().getSimpleName()), false);
            } else {
                // Pure pipe / unloaded chunk
                String chunkStatus = level.isLoaded(pos) ? "pipe" : "pipe (chunk unloaded)";
                source.sendSuccess(() -> memberLine(posStr, "PIPE", ChatFormatting.DARK_GRAY, chunkStatus), false);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Formatting helpers
    // -------------------------------------------------------------------------

    private static Component header(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static Component subheader(String text) {
        return Component.literal(text).withStyle(ChatFormatting.YELLOW);
    }

    private static Component error(String text) {
        return Component.literal("✗ " + text).withStyle(ChatFormatting.RED);
    }

    private static Component dim(String text) {
        return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static Component kv(String key, String value) {
        return Component.literal("  " + key + ": ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static Component memberLine(String pos, String type, ChatFormatting typeColor, String detail) {
        return Component.literal("  [")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(type).withStyle(typeColor, ChatFormatting.BOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(pos + "  ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(detail).withStyle(ChatFormatting.WHITE));
    }


    private static Component line(Object... parts) {
        MutableComponent result = Component.literal("");
        for (int i = 0; i + 1 < parts.length; i += 2) {
            ChatFormatting color = (ChatFormatting) parts[i];
            String text = (String) parts[i + 1];
            result = result.append(Component.literal(text).withStyle(color));
        }
        return result;
    }

    private static ChatFormatting energyColor(long current, long max) {
        if (max == 0) return ChatFormatting.GRAY;
        double ratio = (double) current / max;
        if (ratio > 0.6) return ChatFormatting.GREEN;
        if (ratio > 0.25) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private static String shortId(EnergyNetwork net) {
        // First 8 chars of UUID — readable but not overwhelming
        return net.getId().toString().substring(0, 8);
    }

    private static long pct(long current, long max) {
        if (max == 0) return 0;
        return (current * 100) / max;
    }

 */
}