package com.restonic4.logistics.blocks.protector.data_types;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;

public class ProtectorData {
    private int radius;
    private boolean creative;
    private List<RoleData> roles;

    public ProtectorData(int radius, boolean creative, List<RoleData> roles) {
        this.radius = radius;
        this.creative = creative;
        this.roles = roles;
    }

    public void netWrite(FriendlyByteBuf buf) {
        buf.writeInt(this.radius);
        buf.writeBoolean(this.creative);
        buf.writeCollection(this.roles, (b, r) -> r.netWrite(b));
    }

    public static ProtectorData netRead(FriendlyByteBuf buf) {
        int radius = buf.readInt();
        boolean creative = buf.readBoolean();
        List<RoleData> roles = buf.readList(RoleData::netRead);
        return new ProtectorData(radius, creative, roles);
    }

    public void nbtWrite(CompoundTag tag) {
        tag.putInt("radius", this.radius);
        tag.putBoolean("creative", this.creative);
        ListTag rolesTag = new ListTag();
        for (RoleData role : this.roles) {
            CompoundTag roleCompound = new CompoundTag();
            role.nbtWrite(roleCompound);
            rolesTag.add(roleCompound);
        }
        tag.put("roles", rolesTag);
    }

    public static ProtectorData nbtRead(CompoundTag tag) {
        int radius = tag.getInt("radius");
        boolean creative = tag.getBoolean("creative");
        List<RoleData> roles = new ArrayList<>();
        ListTag rolesTag = tag.getList("roles", Tag.TAG_COMPOUND);
        for (int i = 0; i < rolesTag.size(); i++) {
            roles.add(RoleData.nbtRead(rolesTag.getCompound(i)));
        }
        return new ProtectorData(radius, creative, roles);
    }

    public int getRadius() { return radius; }
    public void setRadius(int radius) { this.radius = radius; }
    public boolean isCreative() { return creative; }
    public void setCreative(boolean creative) { this.creative = creative; }
    public List<RoleData> getRoles() { return roles; }
    public void setRoles(List<RoleData> roles) { this.roles = roles; }

    public void validate() {
        long defaultCount = roles.stream().filter(RoleData::isDefault).count();
        if (defaultCount == 0) {
            Map<String, FlagData> defaultFlags = new HashMap<>();
            for (FlagDefinition def : FlagRegistry.all()) {
                defaultFlags.put(def.id(), new FlagData(false, ActionType.DENY.name(), 0, ""));
            }
            List<RoleData> healed = new ArrayList<>(this.roles);
            healed.add(new RoleData(
                    UUID.randomUUID(),
                    "default",
                    Integer.MAX_VALUE,
                    "logistics:textures/item/parcel.png",
                    RoleData.RoleType.DEFAULT,
                    List.of(),
                    defaultFlags
            ));
            this.roles = healed;
        } else if (defaultCount > 1) {
            throw new IllegalStateException("ProtectorData must have exactly one DEFAULT role, found: " + defaultCount);
        }

        RoleData def = getDefaultRole();
        if (!def.players().isEmpty()) {
            throw new IllegalStateException("DEFAULT role cannot have assigned players");
        }

        // HEAL: every role must contain every registered flag.
        List<RoleData> healedRoles = new ArrayList<>();
        for (RoleData role : this.roles) {
            Map<String, FlagData> completeFlags = new HashMap<>(role.flags());
            for (FlagDefinition flagDef : FlagRegistry.all()) {
                completeFlags.putIfAbsent(flagDef.id(), new FlagData(false, ActionType.DENY.name(), 0, ""));
            }
            healedRoles.add(new RoleData(
                    role.id(), role.name(), role.order(), role.iconRl(), role.roleType(),
                    role.players(), completeFlags
            ));
        }
        this.roles = healedRoles;
    }

    public RoleData getDefaultRole() {
        return roles.stream()
                .filter(RoleData::isDefault)
                .findFirst()
                .orElseThrow();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProtectorData that = (ProtectorData) o;
        return radius == that.radius && creative == that.creative && Objects.equals(roles, that.roles);
    }

    @Override
    public int hashCode() { return Objects.hash(radius, creative, roles); }

    @Override
    public String toString() {
        return "ProtectorData{radius=" + radius + ", creative=" + creative + ", roles=" + roles + '}';
    }
}