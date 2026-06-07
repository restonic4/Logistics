package com.restonic4.logistics.blocks.protector.data_types;

public enum ActionType {
    DENY, DAMAGE, MESSAGE;

    /** Higher = more restrictive. */
    public int restrictiveness() {
        return switch (this) {
            case DENY -> 3;
            case DAMAGE -> 2;
            case MESSAGE -> 1;
        };
    }
}