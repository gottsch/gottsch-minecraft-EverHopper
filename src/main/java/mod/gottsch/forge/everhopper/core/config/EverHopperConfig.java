package mod.gottsch.forge.everhopper.core.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author Mark Gottschling on 4/14/2026
 */
public final class EverHopperConfig {

    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<Common, ForgeConfigSpec> commonPair =
                new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON      = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();

        final Pair<Client, ForgeConfigSpec> clientPair =
                new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT      = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
    }

    private EverHopperConfig() {}

    public static void register() {
        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        ctx.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }


    public static final class Common {

        public final ForgeConfigSpec.BooleanValue catchupEnabled;
        public final ForgeConfigSpec.LongValue    maxCatchupTicks;
        public final ForgeConfigSpec.IntValue     minDeltaThreshold;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("EverHopper — Common (server-side) configuration")
                    .push("catchup");

            catchupEnabled = builder
                    .comment("Master toggle for the catch-up mechanic.",
                            "Set to false to disable EverHopper entirely and let vanilla handle all hopper transfers.")
                    .define("catchupEnabled", true);

            maxCatchupTicks = builder
                    .comment("Maximum ticks of offline time to simulate in one catch-up pass.",
                            "Default: 24000 (1 in-game day). Range: 1 – 192000.")
                    .defineInRange("maxCatchupTicks", 24_000L, 1L, 192_000L);

            minDeltaThreshold = builder
                    .comment("Minimum tick gap required before catch-up logic fires.",
                            "Default: 20 (1 second at normal TPS). Below this the hopper is considered",
                            "actively ticking and vanilla handles transfers. Raise to require a larger gap.")
                    .defineInRange("minDeltaThreshold", 20, 1, 72_000);

            builder.pop();
        }
    }

    public static final class Client {

        public final ForgeConfigSpec.BooleanValue particleBurstEnabled;
        public final ForgeConfigSpec.BooleanValue soundCueEnabled;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("EverHopper — Client-side configuration")
                    .push("visuals");

            particleBurstEnabled = builder
                    .comment("Spawn a brief particle burst at a hopper when catch-up completes",
                            "and at least one item was transferred. Client-side only — no effect on servers.")
                    .define("particleBurstEnabled", true);

            soundCueEnabled = builder
                    .comment("Play a quiet item-pickup sound at a hopper when catch-up completes",
                            "and at least one item was transferred.")
                    .define("soundCueEnabled", true);

            builder.pop();
        }
    }
}
