package mod.gottsch.forge.everhopper.core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server → client when catch-up completes and at least one item was transferred.
 * The client handler spawns a brief particle/sound cue at the hopper position.
 *
 * @author Mark Gottschling on 4/26/2026
 */
public class CatchupParticlePacket {

    private final BlockPos pos;

    public CatchupParticlePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(CatchupParticlePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
    }

    public static CatchupParticlePacket decode(FriendlyByteBuf buf) {
        return new CatchupParticlePacket(buf.readBlockPos());
    }

    public static void handle(CatchupParticlePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> CatchupParticleHandler.handle(packet.pos))
        );
        ctx.setPacketHandled(true);
    }
}
