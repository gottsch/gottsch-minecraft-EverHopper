/**
 * @author Mark Gottschling on April 14, 2026
 */
package mod.gottsch.forge.everhopper.core.mixin;

import mod.gottsch.forge.everhopper.core.config.EverHopperConfig;
import mod.gottsch.forge.everhopper.core.hopper.ModHopperBlockEntityInterface;
import mod.gottsch.forge.everhopper.core.network.CatchupParticlePacket;
import mod.gottsch.forge.everhopper.core.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Container;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(HopperBlockEntity.class)
public abstract class ModHopperBlockEntityMixin extends RandomizableContainerBlockEntity
        implements ModHopperBlockEntityInterface {

    // ----------------------------------------------------------------------------------------------------------------
    // constants
    // ----------------------------------------------------------------------------------------------------------------

    @Unique private static final int TRANSFER_COOLDOWN = HopperBlockEntity.MOVE_ITEM_SPEED;

    @Unique private static final String LAST_GAME_TIME_TAG = "everhopper_lastGameTime";
    @Unique private static final String PENDING_CUE_TAG    = "everhopper_pendingCue";
    @Unique private static final String NBT_VERSION_TAG    = "everhopper_version";

    @Unique private static final int CURRENT_NBT_VERSION  = 1;

    /** range at which a nearby player triggers the deferred cue. ~16 blocks keeps it close
     * enough that particles render at full detail and the pickup sound is audible. */
    @Unique private static final double CUE_TRIGGER_RANGE = 16.0;
    /** range of the packet itself, in case other players within view want the cue too. */
    @Unique private static final double CUE_PACKET_RANGE  = 32.0;

    // ----------------------------------------------------------------------------------------------------------------
    // fields
    // ----------------------------------------------------------------------------------------------------------------

    @Unique private long    everHopper_1_20_1$lastGameTime;
    @Unique private boolean everHopper_1_20_1$pendingCue;

    // ----------------------------------------------------------------------------------------------------------------
    // constructors
    // ----------------------------------------------------------------------------------------------------------------

    protected ModHopperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // nbt
    // ----------------------------------------------------------------------------------------------------------------

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        tag.putInt    (NBT_VERSION_TAG,    CURRENT_NBT_VERSION);
        tag.putLong   (LAST_GAME_TIME_TAG, this.everHopper_1_20_1$lastGameTime);
        tag.putBoolean(PENDING_CUE_TAG,    this.everHopper_1_20_1$pendingCue);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        this.everHopper_1_20_1$lastGameTime = tag.getLong   (LAST_GAME_TIME_TAG);
        // missing tag → returns false, which is the correct default for pre-existing hoppers
        this.everHopper_1_20_1$pendingCue   = tag.getBoolean(PENDING_CUE_TAG);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // tick
    // ----------------------------------------------------------------------------------------------------------------

    @Inject(method = "pushItemsTick", at = @At("HEAD"))
    private static void onTick(Level world, BlockPos pos, BlockState state,
                               HopperBlockEntity blockEntity, CallbackInfo ci) {

        ModHopperBlockEntityInterface mixin = (ModHopperBlockEntityInterface) blockEntity;

        // try to fire any cue that's been pending since a previous catch-up.
        // catch-up runs the moment the chunk loads — long before the player walks
        // close enough to see particles or hear the sound. queueing the cue and
        // releasing it when a player is actually in range fixes that.
        firePendingCueIfReady(world, pos, blockEntity);

        if (!EverHopperConfig.COMMON.catchupEnabled.get()) return;

        long currentGameTime   = world.getGameTime();
        long localLastGameTime = mixin.everHopper_1_20_1$getLastGameTime();

        // if hopper is redstone-disabled, pause lastGameTime updates and skip catch-up
        if (!(Boolean) state.getValue(HopperBlock.ENABLED)) {
            mixin.everHopper_1_20_1$setLastGameTime(localLastGameTime);
            return;
        }

        mixin.everHopper_1_20_1$setLastGameTime(currentGameTime);

        // first tick guard — stamp and return to avoid catch-up against world age
        if (localLastGameTime == 0L) return;

        long deltaTime = currentGameTime - localLastGameTime;
        if (deltaTime < EverHopperConfig.COMMON.minDeltaThreshold.get()) return;

        deltaTime = Math.min(deltaTime, EverHopperConfig.COMMON.maxCatchupTicks.get());

        // ------------------------------------------------
        // simulate transfers
        // ------------------------------------------------

        // how many transfer operations fit in deltaTime
        long transferOps = deltaTime / TRANSFER_COOLDOWN;
        if (transferOps <= 0) return;

        // extract first (container above → hopper), then insert (hopper → container below)
        long extracted = simulateExtract(world, pos, blockEntity, transferOps);
        long inserted  = simulateInsert(world, pos, blockEntity, transferOps);

        if (extracted > 0 || inserted > 0) {
            blockEntity.setChanged();
            // queue the visual cue. it'll fire on the next tick where a player is within
            // CUE_TRIGGER_RANGE of the hopper — usually when the player walks back into
            // their base. firing right now would spray the packet at empty range.
            mixin.everHopper_1_20_1$setPendingCue(true);
            firePendingCueIfReady(world, pos, blockEntity);
        }
    }

    /**
     * If a cue is pending and a player is close enough to actually see/hear it, broadcast
     * the catch-up packet to nearby clients and clear the flag. Safe to call every tick;
     * the pending check short-circuits when there's nothing to do.
     */
    @Unique
    private static void firePendingCueIfReady(Level world, BlockPos pos,
                                              HopperBlockEntity blockEntity) {
        ModHopperBlockEntityInterface mixin = (ModHopperBlockEntityInterface) blockEntity;
        if (!mixin.everHopper_1_20_1$isPendingCue()) return;
        if (!(world instanceof ServerLevel sLevel)) return;

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        if (!sLevel.hasNearbyAlivePlayer(cx, cy, cz, CUE_TRIGGER_RANGE)) return;

        ModNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        cx, cy, cz, CUE_PACKET_RANGE, sLevel.dimension())),
                new CatchupParticlePacket(pos));

        mixin.everHopper_1_20_1$setPendingCue(false);
        blockEntity.setChanged();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // simulation helpers
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Simulates pulling items from the container above into the hopper.
     * Returns the number of items actually extracted.
     */
    @Unique
    private static long simulateExtract(Level world, BlockPos pos,
                                        HopperBlockEntity blockEntity, long transferOps) {
        Container above = HopperBlockEntity.getContainerAt(world, pos.above());
        if (above == null) return 0;

        long extracted = 0;

        outer:
        for (long op = 0; op < transferOps; op++) {
            // find a slot in the source container with an item we can take
            for (int srcSlot = 0; srcSlot < above.getContainerSize(); srcSlot++) {
                ItemStack srcStack = above.getItem(srcSlot);
                if (srcStack.isEmpty()) continue;

                // find a slot in the hopper to put it
                for (int dstSlot = 0; dstSlot < blockEntity.getContainerSize(); dstSlot++) {
                    ItemStack dstStack = blockEntity.getItem(dstSlot);

                    if (dstStack.isEmpty()) {
                        // place one item into the empty slot
                        blockEntity.setItem(dstSlot, srcStack.split(1));
                        above.setItem(srcSlot, srcStack);
                        extracted++;
                        continue outer;
                    } else if (canMerge(dstStack, srcStack)) {
                        dstStack.grow(1);
                        srcStack.shrink(1);
                        if (srcStack.isEmpty()) above.setItem(srcSlot, ItemStack.EMPTY);
                        extracted++;
                        continue outer;
                    }
                }
                // hopper is full — no point checking further ops
                return extracted;
            }
            // nothing left in source to extract
            break;
        }

        if (extracted > 0) above.setChanged();
        return extracted;
    }

    /**
     * Simulates pushing items from the hopper into the container below.
     * Returns the number of items actually inserted.
     */
    @Unique
    private static long simulateInsert(Level world, BlockPos pos,
                                       HopperBlockEntity blockEntity, long transferOps) {
        Direction facing = world.getBlockState(pos).getValue(HopperBlock.FACING);
        Container below  = HopperBlockEntity.getContainerAt(world, pos.relative(facing));
        if (below == null) return 0;

        long inserted = 0;

        outer:
        for (long op = 0; op < transferOps; op++) {
            // find a slot in the hopper with an item to push
            for (int srcSlot = 0; srcSlot < blockEntity.getContainerSize(); srcSlot++) {
                ItemStack srcStack = blockEntity.getItem(srcSlot);
                if (srcStack.isEmpty()) continue;

                // find a slot in the destination to receive it
                for (int dstSlot = 0; dstSlot < below.getContainerSize(); dstSlot++) {
                    ItemStack dstStack = below.getItem(dstSlot);

                    if (dstStack.isEmpty()) {
                        below.setItem(dstSlot, srcStack.split(1));
                        blockEntity.setItem(srcSlot, srcStack);
                        inserted++;
                        continue outer;
                    } else if (canMerge(dstStack, srcStack)) {
                        dstStack.grow(1);
                        srcStack.shrink(1);
                        if (srcStack.isEmpty()) blockEntity.setItem(srcSlot, ItemStack.EMPTY);
                        inserted++;
                        continue outer;
                    }
                }
                // destination full — no point checking further ops
                return inserted;
            }
            // hopper is empty — nothing left to push
            break;
        }

        if (inserted > 0) below.setChanged();
        return inserted;
    }

    /**
     * Returns true if srcStack can merge into dstStack (same item, not at max count).
     */
    @Unique
    private static boolean canMerge(ItemStack dst, ItemStack src) {
        return !dst.isEmpty()
                && ItemStack.isSameItemSameTags(dst, src)
                && dst.getCount() < dst.getMaxStackSize();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // interface impl
    // ----------------------------------------------------------------------------------------------------------------

    @Unique
    public long everHopper_1_20_1$getLastGameTime() {
        return this.everHopper_1_20_1$lastGameTime;
    }

    @Unique
    public void everHopper_1_20_1$setLastGameTime(long gameTime) {
        this.everHopper_1_20_1$lastGameTime = gameTime;
    }

    @Unique
    public boolean everHopper_1_20_1$isPendingCue() {
        return this.everHopper_1_20_1$pendingCue;
    }

    @Unique
    public void everHopper_1_20_1$setPendingCue(boolean pending) {
        this.everHopper_1_20_1$pendingCue = pending;
    }
}