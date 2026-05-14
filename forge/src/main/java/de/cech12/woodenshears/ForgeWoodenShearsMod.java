package de.cech12.woodenshears;

import de.cech12.woodenshears.item.ForgeWoodenShearsItem;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Mod class for the Forge loader.
 */
@SuppressWarnings("unused")
@Mod(Constants.MOD_ID)
@Mod.EventBusSubscriber(modid= Constants.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class ForgeWoodenShearsMod {

    /** mod specific item registry */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);

    static {
        Constants.WOODEN_SHEARS = ITEMS.register("wooden_shears", ForgeWoodenShearsItem::new);
    }

    /**
     * Constructor of a mod instance.
     */
    public ForgeWoodenShearsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        CommonLoader.init();
    }

    /**
     * Registers the dispense behaviour of the shears.
     * @param event FMLCommonSetupEvent
     */
    @SubscribeEvent
    public static void registerDispenseBehavior(FMLCommonSetupEvent event) {
        DispenserBlock.registerBehavior(Constants.WOODEN_SHEARS.get(), new ShearsDispenseItemBehavior());
    }

    /**
     * Registers the creative tabs for all items.
     * @param event BuildCreativeModeTabContentsEvent
     */
    @SubscribeEvent
    public static void addItemsToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(Constants.WOODEN_SHEARS);
        }
    }

}
