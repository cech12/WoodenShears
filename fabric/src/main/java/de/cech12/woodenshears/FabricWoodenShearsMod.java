package de.cech12.woodenshears;

import de.cech12.woodenshears.item.WoodenShearsItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * Mod class for the Fabric loader.
 */
@SuppressWarnings("unused")
public class FabricWoodenShearsMod implements ModInitializer {

    /** wooden shears item registry object */

    public static final Item WOODEN_SHEARS = Registry.register(BuiltInRegistries.ITEM, Constants.id("wooden_shears"), new WoodenShearsItem());

    static {
        Constants.WOODEN_SHEARS = () -> WOODEN_SHEARS;
    }

    /**
     * Initialization of a mod instance.
     */
    @Override
    public void onInitialize() {
        CommonLoader.init();
        //Register item in the creative tab.
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.accept(WOODEN_SHEARS);
        });
        //register dispense behavior
        DispenserBlock.registerBehavior(WOODEN_SHEARS, new ShearsDispenseItemBehavior());
    }

}
