package mod.gottsch.forge.everhopper.core.network;

import mod.gottsch.forge.everhopper.core.EverHopper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * @author Mark Gottschling on 4/14/2026
 */
public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    /**
     * Channel accepts ABSENT on either side so vanilla clients can connect to
     * EverHopper-equipped servers (and vice versa). The catch-up logic itself
     * is server-only — the packet only carries the optional particle/sound cue.
     */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EverHopper.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION::equals),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION::equals)
    );

    public static void register() {
        CHANNEL.registerMessage(
                0,
                CatchupParticlePacket.class,
                CatchupParticlePacket::encode,
                CatchupParticlePacket::decode,
                CatchupParticlePacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }
}
