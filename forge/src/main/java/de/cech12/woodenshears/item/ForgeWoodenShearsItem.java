package de.cech12.woodenshears.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

public class ForgeWoodenShearsItem extends WoodenShearsItem {

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return this.getShearsBurnTime();
    }

}
