package de.cech12.woodenshears;

import de.cech12.woodenshears.item.WoodenShearsItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * Mod class for the Fabric loader.
 */
public class WoodenShearsMod implements ModInitializer {

    /** wooden shears item registry object */

    public static final Item WOODEN_SHEARS = Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Constants.MOD_ID, "wooden_shears"), new WoodenShearsItem());

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
        //register dispense behavior (is done by wsmlmb)
        //DispenserBlock.registerBehavior(WOODEN_SHEARS, new ShearsDispenseItemBehavior());
    }

}
