package de.cech12.woodenshears.gametest;

import de.cech12.woodenshears.platform.ForgeConfigHelper;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

import java.lang.reflect.Field;

/**
 * Thin Forge adapter for the common game tests. Only part of the gametest source set, which is loaded as separate
 * test mod (ForgeGradle does not merge it into the main mod).
 */
@Mod(ForgeGameTestAdapter.MOD_ID)
public final class ForgeGameTestAdapter implements IGameTestPlatform {

    /** mod id of the test mod */
    public static final String MOD_ID = "woodenshears_gametest";

    /** Only used by the ServiceLoader. */
    public ForgeGameTestAdapter() {}

    /**
     * Constructor of the test mod.
     * @param context mod loading context
     */
    public ForgeGameTestAdapter(FMLJavaModLoadingContext context) {
        RegisterEvent.getBus(context.getModBusGroup()).addListener(ForgeGameTestAdapter::register);
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> WoodenShearsGameTests.registerTestFunctions(helper::register));
        event.register(Registries.TEST_ENVIRONMENT_DEFINITION_TYPE, helper -> WoodenShearsGameTests.registerEnvironmentTypes(helper::register));
    }

    @Override
    public void setDurability(int durability) {
        try {
            ForgeConfigSpec spec = (ForgeConfigSpec) staticField("SERVER_CONFIG");
            ForgeConfigSpec.IntValue value = (ForgeConfigSpec.IntValue) staticField("DURABILITY");
            ForgeConfigSpec.ValueSpec valueSpec = spec.getSpec().get(value.getPath());
            value.set((Integer) valueSpec.correct(durability));
            value.clearCache();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot change durability config", e);
        }
    }

    private static Object staticField(String name) throws ReflectiveOperationException {
        Field field = ForgeConfigHelper.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

}
