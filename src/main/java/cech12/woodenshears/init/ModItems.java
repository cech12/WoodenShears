package cech12.woodenshears.init;

import cech12.woodenshears.api.item.WoodenShearsItems;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.BeehiveDispenseBehavior;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ShearsItem;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import static cech12.woodenshears.WoodenShearsMod.MOD_ID;

@Mod.EventBusSubscriber(modid= MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        WoodenShearsItems.WOODEN_SHEARS = registerItem("wooden_shears", new ShearsItem((new Item.Properties()).durability(50).tab(ItemGroup.TAB_TOOLS)));

        DispenserBlock.registerBehavior(WoodenShearsItems.WOODEN_SHEARS.asItem(), new BeehiveDispenseBehavior());
    }

    private static Item registerItem(String name, Item item) {
        item.setRegistryName(name);
        ForgeRegistries.ITEMS.register(item);
        return item;
    }

}
