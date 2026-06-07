package com.restonic4.logistics.blocks.protector.data_types.ui;

import com.restonic4.logistics.blocks.protector.data_types.*;
import net.minecraft.core.BlockPos;

import java.util.*;

public class EditableProtector {
    public final UUID nodeId;
    public final BlockPos pos;
    public int radius;
    public boolean creative;
    public final List<EditableRole> roles = new ArrayList<>();

    public EditableProtector(ProtectionZone zone) {
        this.nodeId = zone.nodeId();
        this.pos = zone.pos();
        this.radius = zone.radius();
        this.creative = zone.creative();
        for (RoleData r : zone.roles()) this.roles.add(new EditableRole(r));
    }

    public ProtectionZone toZone() {
        List<RoleData> list = new ArrayList<>();
        for (EditableRole r : roles) {
            Map<String, FlagData> completeFlags = new HashMap<>(r.flags);
            for (FlagDefinition def : FlagRegistry.forZone(this.creative)) {
                completeFlags.putIfAbsent(def.id(), new FlagData(false, ActionType.DENY.name(), 0, ""));
            }
            list.add(new RoleData(r.id, r.name, r.order, r.iconRl, r.type,
                    List.copyOf(r.players), Map.copyOf(completeFlags)));
        }
        return new ProtectionZone(nodeId, pos, radius, creative, list, false); // we don't care about power here, the server handles it
    }
}