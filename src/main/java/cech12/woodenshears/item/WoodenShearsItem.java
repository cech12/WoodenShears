package cech12.woodenshears.item;

import cech12.woodenshears.config.ServerConfig;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;

public class WoodenShearsItem extends ShearsItem {

    public WoodenShearsItem() {
        super((new Item.Properties()).stacksTo(1).tab(CreativeModeTab.TAB_TOOLS));
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        try {
            return ServerConfig.DURABILITY.get() > 0;
        } catch (IllegalStateException ex) {
            return true;
        }
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        try {
            return ServerConfig.DURABILITY.get();
        } catch (IllegalStateException ex) {
            return ServerConfig.DEFAULT_DURABILITY;
        }
    }

}
