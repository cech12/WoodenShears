package de.cech12.woodenshears.gametest;

import de.cech12.woodenshears.Constants;
import de.cech12.woodenshears.platform.NeoforgeConfigHelper;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.lang.reflect.Field;

/**
 * Thin NeoForge adapter for the common game tests. Only part of the gametest source set.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class NeoforgeGameTestAdapter implements IGameTestPlatform {

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> WoodenShearsGameTests.registerTestFunctions(helper::register));
        event.register(Registries.TEST_ENVIRONMENT_DEFINITION_TYPE, helper -> WoodenShearsGameTests.registerEnvironmentTypes(helper::register));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setDurability(int durability) {
        try {
            Field field = NeoforgeConfigHelper.class.getDeclaredField("DURABILITY");
            field.setAccessible(true);
            ModConfigSpec.IntValue value = (ModConfigSpec.IntValue) field.get(null);
            value.set((Integer) value.getSpec().correct(durability));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot change durability config", e);
        }
    }

}
