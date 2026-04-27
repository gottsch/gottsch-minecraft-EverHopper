package mod.gottsch.forge.everhopper.core.network;

import mod.gottsch.forge.everhopper.core.EverHopper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * @author Mark Gottschling on 4/14/2026
 */
public class ModNetwork {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::onRegisterPayloads);
    }

    /**
     * Server-side optional: vanilla clients can connect to EverHopper-equipped servers.
     * The catch-up runs entirely server-side; the packet only delivers the optional
     * particle/sound cue, which gracefully degrades for vanilla players.
     */
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(EverHopper.MOD_ID).optional();
        registrar.playToClient(
                CatchupParticlePacket.TYPE,
                CatchupParticlePacket.STREAM_CODEC,
                CatchupParticlePacket::handle
        );
    }
}
