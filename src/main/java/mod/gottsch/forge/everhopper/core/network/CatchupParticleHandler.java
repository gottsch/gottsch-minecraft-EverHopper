package mod.gottsch.forge.everhopper.core.network;

import mod.gottsch.forge.everhopper.core.config.EverHopperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * Client-only handler for {@link CatchupParticlePacket}.
 *
 * <p>Spawns a brief sparkle + dust burst at the hopper to signal that items
 * were transferred during catch-up, and plays a quiet item-pickup sound.
 *
 * @author Mark Gottschling on 4/26/2026
 */
@OnlyIn(Dist.CLIENT)
public class CatchupParticleHandler {

    public static void handle(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        spawnParticles(level, pos);
        playSound(level, pos);
    }

    private static void spawnParticles(Level level, BlockPos pos) {
        if (!EverHopperConfig.CLIENT.particleBurstEnabled.get()) return;

        Random rand = new Random();
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        // green sparkles around the hopper top — "transactions completed"
        for (int i = 0; i < 10; i++) {
            double ox = (rand.nextDouble() - 0.5) * 0.7;
            double oy = 0.4 + rand.nextDouble() * 0.4;
            double oz = (rand.nextDouble() - 0.5) * 0.7;
            level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                    cx + ox, cy + oy, cz + oz, 0, 0.01, 0);
        }

        // small dust puffs at the output direction (front for side-facing, below for downward)
        Direction facing = Direction.DOWN;
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(HopperBlock.FACING)) {
            facing = state.getValue(HopperBlock.FACING);
        }
        double fx = cx + facing.getStepX() * 0.55;
        double fy = cy + facing.getStepY() * 0.55;
        double fz = cz + facing.getStepZ() * 0.55;
        for (int i = 0; i < 6; i++) {
            double ox = (rand.nextDouble() - 0.5) * 0.3;
            double oy = (rand.nextDouble() - 0.5) * 0.3;
            double oz = (rand.nextDouble() - 0.5) * 0.3;
            level.addParticle(ParticleTypes.POOF,
                    fx + ox, fy + oy, fz + oz, 0, 0.0, 0);
        }
    }

    private static void playSound(Level level, BlockPos pos) {
        if (!EverHopperConfig.CLIENT.soundCueEnabled.get()) return;

        level.playLocalSound(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ITEM_PICKUP,
                SoundSource.BLOCKS,
                0.4f, 1.0f, false);
    }
}
