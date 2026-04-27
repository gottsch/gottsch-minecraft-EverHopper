package mod.gottsch.forge.everhopper.core;

import mod.gottsch.forge.everhopper.core.config.EverHopperConfig;
import mod.gottsch.forge.everhopper.core.network.ModNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * @author Mark Gottschling on 4/14/2026
 */
@Mod(value = EverHopper.MOD_ID)
public class EverHopper {
    public static final String MOD_ID = "everhopper";

    public EverHopper(IEventBus modEventBus, ModContainer modContainer) {
        EverHopperConfig.register(modContainer);
        ModNetwork.register(modEventBus);
    }
}
