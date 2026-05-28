package com.restonic4.logistics.blocks.protector.data_types;

import java.util.List;

public record FlagDefinition(
        String id,
        String name,
        List<ActionType> supportedActions,
        FlagCategory category
) {
    public enum FlagCategory { STANDARD, CREATIVE }
}