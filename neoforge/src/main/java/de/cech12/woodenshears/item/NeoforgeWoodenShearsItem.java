package de.cech12.woodenshears.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.FuelValues;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoforgeWoodenShearsItem extends WoodenShearsItem {

    public NeoforgeWoodenShearsItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack stack) {
        return stack.isDamageableItem(); //references to mixed in method
    }

    @Override
    public int getMaxDamage(@NotNull ItemStack stack) {
        return stack.getMaxDamage(); //references to mixed in method
    }

    @Override
    public int getBurnTime(@NotNull ItemStack itemStack, @Nullable RecipeType<?> recipeType, @NotNull FuelValues fuelValues) {
        return this.getShearsBurnTime();
    }

}
