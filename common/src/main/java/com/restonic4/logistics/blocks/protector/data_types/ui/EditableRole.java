package com.restonic4.logistics.blocks.protector.data_types.ui;

import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.PlayerData;
import com.restonic4.logistics.blocks.protector.data_types.RoleData;

import java.util.*;

public class EditableRole {
    public UUID id;
    public String name;
    public int order;
    public String iconRl;
    public RoleData.RoleType type;
    public final List<PlayerData> players = new ArrayList<>();
    public final Map<String, FlagData> flags = new HashMap<>();

    public EditableRole(RoleData src) {
        this.id = src.id();
        this.name = src.name();
        this.order = src.order();
        this.iconRl = src.iconRl();
        this.type = src.roleType();
        this.players.addAll(src.players());
        this.flags.putAll(src.flags());
    }

    public RoleData toData() {
        return new RoleData(id, name, order, iconRl, type, List.copyOf(players), Map.copyOf(flags));
    }
}