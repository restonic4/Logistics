package com.restonic4.logistics.blocks.computer.automation.triggers.actions;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.ActionExecutionContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.ExecuteResult;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.util.NetworkNodeTarget;
import com.restonic4.logistics.blocks.network_switch.NetworkSwitchBlock;
import com.restonic4.logistics.blocks.network_switch.NetworkSwitchNode;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Enables, disables, or toggles faces of the targeted network switch(es), splitting or merging the
 * energy network there. Faces are world directions (the switch has no facing), so the same action
 * means the same physical side on every targeted switch.
 * <p>
 * The classic pairing is two triggers on one redstone reader: "powered → enable" and "unpowered →
 * disable" the same face, which makes a real light switch.
 */
public class SwitchAction extends TriggerAction {
    private static final String TAG_OPERATION = "operation";
    private static final String TAG_ALL_FACES = "allFaces";
    private static final String TAG_FACE = "face";

    /** What to do to each selected face. */
    public enum Operation {
        /** Make the face conduct (merge the network across it). */
        ENABLE,
        /** Make the face stop conducting (split the network there). */
        DISABLE,
        /** Flip the face's current state. */
        TOGGLE
    }

    private final NetworkNodeTarget<NetworkSwitchNode> target = new NetworkNodeTarget<>();
    private Operation operation = Operation.TOGGLE;

    /** When true, the operation applies to all six faces; otherwise just {@link #face}. */
    private boolean allFaces = true;
    private Direction face = Direction.NORTH;

    public SwitchAction() {
        super(ActionRegistry.SWITCH);
    }

    public NetworkNodeTarget<NetworkSwitchNode> getTarget() { return target; }

    public Operation getOperation() { return operation; }
    public void setOperation(Operation operation) { this.operation = operation; }

    public boolean isAllFaces() { return allFaces; }
    public void setAllFaces(boolean allFaces) { this.allFaces = allFaces; }

    public Direction getFace() { return face; }
    public void setFace(Direction face) { this.face = face; }

    @Override
    public ExecuteResult execute(TriggerContext ctx, ActionExecutionContext runCtx) {
        ServerLevel level = ctx.getLevel();

        for (NetworkSwitchNode node : target.resolve(ctx.getNetworkSwitches())) {
            if (allFaces) {
                for (Direction dir : Direction.values()) {
                    apply(level, node, dir);
                }
            } else {
                apply(level, node, face);
            }
        }

        return ExecuteResult.SUCCESS;
    }

    private void apply(ServerLevel level, NetworkSwitchNode node, Direction dir) {
        boolean enabled = switch (operation) {
            case ENABLE -> true;
            case DISABLE -> false;
            case TOGGLE -> !NetworkSwitchBlock.isFaceEnabled(level, node.getBlockPos(), dir);
        };
        NetworkSwitchBlock.setFaceEnabled(level, node.getBlockPos(), dir, enabled);
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        target.save(tag);
        tag.putString(TAG_OPERATION, operation.name());
        tag.putBoolean(TAG_ALL_FACES, allFaces);
        tag.putString(TAG_FACE, face.getName());
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        target.load(tag);
        this.operation = Operation.valueOf(tag.getString(TAG_OPERATION));
        this.allFaces = tag.getBoolean(TAG_ALL_FACES);
        Direction parsed = Direction.byName(tag.getString(TAG_FACE));
        this.face = parsed != null ? parsed : Direction.NORTH;
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        target.write(buf);
        buf.writeEnum(operation);
        buf.writeBoolean(allFaces);
        buf.writeEnum(face);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        target.read(buf);
        this.operation = buf.readEnum(Operation.class);
        this.allFaces = buf.readBoolean();
        this.face = buf.readEnum(Direction.class);
    }
}
