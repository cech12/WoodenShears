package de.cech12.woodenshears.mixin;

import de.cech12.woodenshears.Constants;
import de.cech12.woodenshears.item.WoodenShearsItem;
import de.cech12.woodenshears.platform.Services;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * Mixin for the Item class to enable the possibility to configure the item's durability.
 */
@Mixin(Item.class)
public class MixinItem {

    @Final
    @Shadow
    private int maxDamage;

    /**
     * Updates the max damage value (by using reflection) if it is a WoodenShearsItem and the maxDamage is different.
     */
    @Unique
    private void woodenShears$updateMaxDamage() {
        if (!((Object)this instanceof WoodenShearsItem)) {
            return;
        }
        int durability = Services.CONFIG.getDurability();
        if (maxDamage == durability) {
            return;
        }
        try {
            Field maxDamageField = Item.class.getDeclaredField("maxDamage");
            maxDamageField.setAccessible(true);
            maxDamageField.set(this, durability);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Constants.LOG.error("Failed to set maxDamage of wooden Shears.", ex);
        }
    }

    /**
     * Injection method to initialize the maxDamage value.
     * @param cir CallbackInfoReturnable
     */
    @Inject(at = @At("HEAD"), method = "getMaxDamage")
    public void getMaxDamageProxy(CallbackInfoReturnable<Integer> cir) {
        woodenShears$updateMaxDamage();
    }

    /**
     * Injection method to initialize the maxDamage value.
     * @param cir CallbackInfoReturnable
     */
    @Inject(at = @At("HEAD"), method = "canBeDepleted")
    public void canBeDepletedProxy(CallbackInfoReturnable<Boolean> cir) {
        woodenShears$updateMaxDamage();
    }

    /**
     * Injection method to initialize the maxDamage value.
     * @param stack ItemStack
     * @param cir CallbackInfoReturnable
     */
    @Inject(at = @At("HEAD"), method = "getBarWidth")
    public void getBarWidthProxy(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        woodenShears$updateMaxDamage();
    }

    /**
     * Injection method to initialize the maxDamage value.
     * @param stack ItemStack
     * @param cir CallbackInfoReturnable
     */
    @Inject(at = @At("HEAD"), method = "getBarColor")
    public void getBarColorProxy(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        woodenShears$updateMaxDamage();
    }

}
