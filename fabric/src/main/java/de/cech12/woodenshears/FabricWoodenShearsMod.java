package de.cech12.woodenshears;

import de.cech12.woodenshears.item.WoodenShearsItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;

import java.util.function.Function;

/**
 * Mod class for the Fabric loader.
 */
@SuppressWarnings("unused")
public class FabricWoodenShearsMod implements ModInitializer {

    /** wooden shears item registry object */

    public static final Item WOODEN_SHEARS = registerItem("wooden_shears", WoodenShearsItem::new);

    static {
        Constants.WOODEN_SHEARS = () -> WOODEN_SHEARS;
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> itemConstructor) {
        ResourceKey<Item> resourceKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), Constants.id(name));
        return Registry.register(BuiltInRegistries.ITEM, resourceKey, itemConstructor.apply(new Item.Properties().setId(resourceKey)));
    }

    /**
     * Initialization of a mod instance.
     */
    @Override
    public void onInitialize() {
        CommonLoader.init();
        //Register item in the creative tab.
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.accept(WOODEN_SHEARS);
        });
        //register dispense behavior
        DispenserBlock.registerBehavior(WOODEN_SHEARS, new ShearsDispenseItemBehavior());
    }

}
