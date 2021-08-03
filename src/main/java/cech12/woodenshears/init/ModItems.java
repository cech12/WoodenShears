package cech12.woodenshears.init;

import cech12.woodenshears.api.item.WoodenShearsItems;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ShearsItem;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import static cech12.woodenshears.WoodenShearsMod.MOD_ID;

@Mod.EventBusSubscriber(modid= MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        WoodenShearsItems.WOODEN_SHEARS = registerItem("wooden_shears", new ShearsItem((new Item.Properties()).durability(50).tab(CreativeModeTab.TAB_TOOLS)));

        DispenserBlock.registerBehavior(WoodenShearsItems.WOODEN_SHEARS.asItem(), new ShearsDispenseItemBehavior());
    }

    private static Item registerItem(String name, Item item) {
        item.setRegistryName(name);
        ForgeRegistries.ITEMS.register(item);
        return item;
    }

}
