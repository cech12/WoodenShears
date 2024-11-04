package de.cech12.woodenshears.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ShearsItem;

/**
 * Extends the ShearsItem to have a unique class and simplify the construction.
 */
public class WoodenShearsItem extends ShearsItem {

    /**
     * Constructs a WoodenShearsItem by configure it to stack to one item.
     */
    public WoodenShearsItem(Properties properties) {
        super(properties.stacksTo(1).component(DataComponents.TOOL, ShearsItem.createToolProperties()));
    }

}
