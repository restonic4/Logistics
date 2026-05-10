package com.restonic4.logistics.networks.types;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.BuiltInNetworks;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.nodes.ItemNode;
import com.restonic4.logistics.networks.pathfinding.Parcel;
import com.restonic4.logistics.networks.pathfinding.ParcelRenderSyncPacket;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NetworkTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ItemNetwork extends Network {
    public static final double TIME_PER_BLOCK = 1000;

    private List<Parcel> parcels = new ArrayList<>();

    public ItemNetwork(NetworkTypeRegistry.NetworkType<?> type, ServerLevel serverLevel) {
        super(type, serverLevel);
    }

    @Override
    public void mergeDataFrom(Network other) {
        if (other instanceof ItemNetwork itemOther) {
            this.parcels.addAll(itemOther.parcels);
            recalculateAll();
        }
    }

    @Override
    public void onSplit(Collection<Network> children) {
        if (children.isEmpty() || parcels.isEmpty()) return;

        Set<ItemNetwork> updatedNetworks = new HashSet<>();

        for (Parcel parcel : parcels) {
            Vec3 currentPos = parcel.getPosition();
            BlockPos currentBlock = BlockPos.containing(currentPos);
            boolean assigned = false;

            for (Network child : children) {
                if (child instanceof ItemNetwork itemChild) {
                    if (itemChild.getNodeIndex().findByBlockPos(currentBlock) != null) {
                        itemChild.parcels.add(parcel);
                        assigned = true;
                        updatedNetworks.add(itemChild);
                        break;
                    }
                }
            }

            if (!assigned) {
                Constants.LOG.info("Parcel dropped since it lost its network on split");
                ItemEntity itemEntity = new ItemEntity(
                        getServerLevel(),
                        currentPos.x, currentPos.y, currentPos.z,
                        parcel.getItemStackClone()
                );
                getServerLevel().addFreshEntity(itemEntity);
            }

            for (ItemNetwork updated : updatedNetworks) {
                updated.recalculateAll();
            }
        }

        parcels.clear();
    }

    @Override
    public void tick() {
        super.tick();

        Iterator<Parcel> iterator = parcels.iterator();
        while (iterator.hasNext()) {
            Parcel parcel = iterator.next();

            if (parcel.isFinished()) {
                handleParcelArrival(parcel);
                iterator.remove();
            }
        }
    }

    public Parcel requestParcel(ItemStack itemStack, BlockPos start, BlockPos end) {
        if (itemStack.getCount() > 64) {
            return null;
        }

        Parcel parcel = new Parcel(itemStack, start, end, TIME_PER_BLOCK);
        parcel.recalculate(getNodeIndex().getAllNodePositionsAsLongs());
        parcels.add(parcel);
        return parcel;
    }

    private void handleParcelArrival(Parcel parcel) {
        BlockPos endPos = parcel.getEndPos();
        NetworkNode node = getNodeIndex().findByBlockPos(endPos);

        if (node instanceof ItemNode itemNode) {
            itemNode.onParcelArrived(parcel);
        } else {
            Vec3 pos = parcel.getPosition();
            ItemEntity entity = new ItemEntity(getServerLevel(), pos.x, pos.y, pos.z, parcel.getItemStackClone());
            getServerLevel().addFreshEntity(entity);
        }
    }

    public void recalculateAll() {
        System.out.println("Recalculating all");
        parcels.forEach(parcel -> parcel.recalculate(getNodeIndex().getAllNodePositionsAsLongs(), BlockPos.containing(parcel.getPosition()), parcel.getTimePerBlock()));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        ListTag parcelsTag = new ListTag();
        for (Parcel parcel : parcels) {
            parcelsTag.add(parcel.save());
        }
        tag.put("parcels", parcelsTag);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        if (tag.contains("parcels", Tag.TAG_LIST)) {
            ListTag parcelsTag = tag.getList("parcels", Tag.TAG_COMPOUND);
            this.parcels = new ArrayList<>();
            for (int i = 0; i < parcelsTag.size(); i++) {
                this.parcels.add(Parcel.fromCompoundTag(parcelsTag.getCompound(i)));
            }
        }
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.text("Parcels (" + this.parcels.size() + "):", ChatFormatting.YELLOW);
        for (Parcel parcel : parcels) {
            builder.bullet(parcel.getItemStackClone().toString() + ": " + TooltipBuilder.formatTime(parcel.getTimeLeft()));
        }

        return true;
    }

    @Deprecated
    public List<Parcel> getParcels() {
        return parcels;
    }
}
