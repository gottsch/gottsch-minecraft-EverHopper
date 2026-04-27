package mod.gottsch.forge.everhopper.core.network;

import mod.gottsch.forge.everhopper.core.EverHopper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent server → client when catch-up completes and at least one item was transferred.
 * The client handler spawns a brief particle/sound cue at the hopper position.
 *
 * @author Mark Gottschling on 4/26/2026
 */
public class CatchupParticlePacket implements CustomPacketPayload {

    public static final Type<CatchupParticlePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EverHopper.MOD_ID, "catchup_particle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CatchupParticlePacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeBlockPos(packet.pos),
                    buf -> new CatchupParticlePacket(buf.readBlockPos())
            );

    private final BlockPos pos;

    public CatchupParticlePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void handle(CatchupParticlePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                CatchupParticleHandler.handle(packet.pos);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
