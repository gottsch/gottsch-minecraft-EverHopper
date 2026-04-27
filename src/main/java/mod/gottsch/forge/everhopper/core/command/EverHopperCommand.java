package mod.gottsch.forge.everhopper.core.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import mod.gottsch.forge.everhopper.core.EverHopper;
import mod.gottsch.forge.everhopper.core.hopper.ModHopperBlockEntityInterface;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Admin debug commands for EverHopper.
 *
 * <ul>
 *   <li>{@code /everhopper inspect [x y z]} — show tracked state for a hopper (defaults to player feet).</li>
 *   <li>{@code /everhopper tick <radius>}    — force a {@code pushItemsTick} on every loaded hopper in radius.</li>
 *   <li>{@code /everhopper simulate <ticks> <radius>} — backdate every loaded hopper's
 *       {@code lastGameTime} by N ticks so the next tick triggers catch-up.</li>
 * </ul>
 */
@EventBusSubscriber(modid = EverHopper.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class EverHopperCommand {

    private static final int MAX_RADIUS = 64;

    private EverHopperCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("everhopper").requires(s -> s.hasPermission(2))
                .then(Commands.literal("inspect")
                    .executes(ctx -> inspect(ctx.getSource(), feet(ctx.getSource())))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> inspect(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos")))))
                .then(Commands.literal("tick")
                    .then(Commands.argument("radius", IntegerArgumentType.integer(0, MAX_RADIUS))
                        .executes(ctx -> tick(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("simulate")
                    .then(Commands.argument("ticks", LongArgumentType.longArg(0))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, MAX_RADIUS))
                            .executes(ctx -> simulate(ctx.getSource(),
                                LongArgumentType.getLong(ctx, "ticks"),
                                IntegerArgumentType.getInteger(ctx, "radius"))))))
        );
    }

    // ----------------------------------------------------------------------------------------------------------------
    // sub-commands
    // ----------------------------------------------------------------------------------------------------------------

    private static int inspect(CommandSourceStack src, BlockPos pos) {
        ServerLevel level = src.getLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HopperBlockEntity)) {
            src.sendFailure(Component.literal("No hopper at " + posStr(pos)));
            return 0;
        }
        long now   = level.getGameTime();
        long last  = ((ModHopperBlockEntityInterface) be).everHopper_1_20_1$getLastGameTime();
        long delta = last == 0L ? 0L : now - last;
        src.sendSuccess(() -> Component.literal(String.format(
            "Hopper @ %s — lastGameTime=%d, now=%d, delta=%d",
            posStr(pos), last, now, delta)), false);
        return 1;
    }

    private static int tick(CommandSourceStack src, int radius) {
        ServerLevel level = src.getLevel();
        BlockPos center   = feet(src);
        int[] count = {0};
        forEachHopper(level, center, radius, (pos, hopper, state) -> {
            HopperBlockEntity.pushItemsTick(level, pos, state, hopper);
            count[0]++;
        });
        src.sendSuccess(() -> Component.literal(
            "Ticked " + count[0] + " hopper(s) within " + radius + " blocks"), false);
        return count[0];
    }

    private static int simulate(CommandSourceStack src, long ticks, int radius) {
        ServerLevel level = src.getLevel();
        BlockPos center   = feet(src);
        long now = level.getGameTime();
        int[] count = {0};
        forEachHopper(level, center, radius, (pos, hopper, state) -> {
            ModHopperBlockEntityInterface iface = (ModHopperBlockEntityInterface) hopper;
            long last = iface.everHopper_1_20_1$getLastGameTime();
            if (last == 0L) last = now;
            iface.everHopper_1_20_1$setLastGameTime(last - ticks);
            hopper.setChanged();
            count[0]++;
        });
        src.sendSuccess(() -> Component.literal(
            "Backdated " + count[0] + " hopper(s) by " + ticks + " ticks"), false);
        return count[0];
    }

    // ----------------------------------------------------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------------------------------------------------

    @FunctionalInterface
    private interface HopperVisitor {
        void visit(BlockPos pos, HopperBlockEntity hopper, BlockState state);
    }

    private static void forEachHopper(ServerLevel level, BlockPos center, int radius, HopperVisitor visitor) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (be instanceof HopperBlockEntity hopper) {
                        visitor.visit(cursor.immutable(), hopper, level.getBlockState(cursor));
                    }
                }
            }
        }
    }

    private static BlockPos feet(CommandSourceStack src) {
        return BlockPos.containing(src.getPosition());
    }

    private static String posStr(BlockPos p) {
        return p.getX() + " " + p.getY() + " " + p.getZ();
    }
}
