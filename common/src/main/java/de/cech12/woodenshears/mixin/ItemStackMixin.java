package de.cech12.woodenshears.mixin;

import de.cech12.woodenshears.item.WoodenShearsItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wooden shears have no default damage component (the durability comes from the config, see
 * {@link DataComponentHolderMixin}), so setting the damage to 0 (anvil, mending) would store a damage component that
 * new shears do not have. Like vanilla shears, fully repaired wooden shears must equal new ones.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(at = @At("RETURN"), method = "setDamageValue")
    private void woodenshears$removeZeroDamage(int damage, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (damage <= 0 && stack.getItem() instanceof WoodenShearsItem) {
            stack.remove(DataComponents.DAMAGE);
        }
    }

}
