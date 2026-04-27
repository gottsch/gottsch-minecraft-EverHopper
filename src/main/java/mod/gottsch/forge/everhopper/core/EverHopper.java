package mod.gottsch.forge.everhopper.core;

import mod.gottsch.forge.everhopper.core.config.EverHopperConfig;
import mod.gottsch.forge.everhopper.core.network.ModNetwork;
import net.minecraftforge.fml.common.Mod;

/**
 * @author Mark Gottschling on 4/14/2026
 */
@Mod(value = EverHopper.MOD_ID)
public class EverHopper {
    public static final String MOD_ID = "everhopper";

    public EverHopper() {
        EverHopperConfig.register();
        ModNetwork.register();
    }
}
