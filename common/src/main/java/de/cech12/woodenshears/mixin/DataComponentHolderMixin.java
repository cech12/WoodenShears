package de.cech12.woodenshears.mixin;

import de.cech12.woodenshears.item.WoodenShearsItem;
import de.cech12.woodenshears.platform.Services;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataComponentHolder.class)
public interface DataComponentHolderMixin {

    @Shadow
    DataComponentMap getComponents();

    @Unique
    default Item woodenshears$getItem() {
        if ((Object) this instanceof ItemStack stack) {
            return stack.getItem();
        }
        return null;
    }

    @Inject(at = @At("RETURN"), method = "get", cancellable = true)
    default <T> void getProxy(DataComponentType<? extends T> type, CallbackInfoReturnable<T> cir) {
        if (cir.getReturnValue() == null && this.woodenshears$getItem() instanceof WoodenShearsItem && Services.CONFIG.getDurability() > 0) {
            if (type == DataComponents.MAX_DAMAGE) {
                cir.setReturnValue((T) Integer.valueOf(Services.CONFIG.getDurability()));
            } else if (type == DataComponents.DAMAGE) {
                cir.setReturnValue((T) Integer.valueOf(0)); //avoid returning null if "has" method returns true
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "getOrDefault", cancellable = true)
    default <T> void getOrDefaultProxy(DataComponentType<? extends T> type, T defaultValue, CallbackInfoReturnable<T> cir) {
        if ((type == DataComponents.MAX_DAMAGE || type == DataComponents.DAMAGE) && this.woodenshears$getItem() instanceof WoodenShearsItem && Services.CONFIG.getDurability() > 0) {
            T value = getComponents().get(type);
            if (type == DataComponents.MAX_DAMAGE) {
                cir.setReturnValue(value != null ? value : (T) Integer.valueOf(Services.CONFIG.getDurability()));
            } else { //type == DataComponents.DAMAGE
                cir.setReturnValue(value != null ? value : defaultValue);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "has", cancellable = true)
    default void hasProxy(DataComponentType<?> type, CallbackInfoReturnable<Boolean> cir) {
        if ((type == DataComponents.MAX_DAMAGE || type == DataComponents.DAMAGE) && this.woodenshears$getItem() instanceof WoodenShearsItem) {
            cir.setReturnValue(Services.CONFIG.getDurability() > 0);
        }
    }

}
