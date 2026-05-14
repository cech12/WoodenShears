package de.cech12.woodenshears.mixin;

import de.cech12.woodenshears.item.WoodenShearsItem;
import de.cech12.woodenshears.platform.Services;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public class RepairItemRecipeMixin {

    @Inject(at = @At("RETURN"), method = "assemble", cancellable = true)
    public void getProxy(CraftingInput input, HolderLookup.Provider provider, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack itemstack = cir.getReturnValue();
        if (!itemstack.isEmpty() && itemstack.getItem() instanceof WoodenShearsItem && Services.CONFIG.getDurability() > 0) {
            itemstack.remove(DataComponents.MAX_DAMAGE);
            Integer damage = itemstack.getOrDefault(DataComponents.DAMAGE, 0);
            if (damage == 0) {
                itemstack.remove(DataComponents.DAMAGE);
            }
            cir.setReturnValue(itemstack);
        }
    }

}
