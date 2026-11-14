package de.cech12.woodenshears.item;

import de.cech12.woodenshears.platform.Services;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Extends the ShearsItem to have a unique class and simplify the construction.
 */
public class WoodenShearsItem extends ShearsItem {

    /**
     * Constructs a WoodenShearsItem by configure it to stack to one item.
     */
    public WoodenShearsItem(Properties properties) {
        super(properties.stacksTo(1)
                .component(DataComponents.TOOL, ShearsItem.createToolProperties())
                .cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_ITEMS_LARGE) //should burn as long as a wooden tool
        );
    }

    /**
     * Repairing (crafting grid, grindstone) stores the current max damage as a fixed component in the result. Wooden
     * shears read their max damage from the config (DataComponentHolderMixin), so the stored value is removed again
     * to let config changes still apply. Damage 0 is removed as well, so fully repaired shears equal new shears.
     *
     * @param stack result of the repair, changed in place (other items are ignored)
     */
    public static void removeStoredMaxDamage(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof WoodenShearsItem && Services.CONFIG.getDurability() > 0) {
            stack.remove(DataComponents.MAX_DAMAGE);
            if (stack.getOrDefault(DataComponents.DAMAGE, 0) == 0) {
                stack.remove(DataComponents.DAMAGE);
            }
        }
    }

}
