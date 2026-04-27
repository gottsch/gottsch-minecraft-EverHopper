/**
 * @author Mark Gottschling on April 14, 2026
 */
package mod.gottsch.forge.everhopper.core.hopper;


public interface ModHopperBlockEntityInterface {

    long everHopper_1_20_1$getLastGameTime();
    void everHopper_1_20_1$setLastGameTime(long gameTime);

    boolean everHopper_1_20_1$isPendingCue();
    void everHopper_1_20_1$setPendingCue(boolean pending);
}