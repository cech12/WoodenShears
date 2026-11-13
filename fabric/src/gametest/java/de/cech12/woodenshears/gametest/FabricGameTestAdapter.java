package de.cech12.woodenshears.gametest;

import de.cech12.woodenshears.platform.FabricConfigHelper;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Thin Fabric adapter for the common game tests. Only part of the gametest source set (separate test mod).
 */
public final class FabricGameTestAdapter implements ModInitializer, IGameTestPlatform {

    @Override
    public void onInitialize() {
        WoodenShearsGameTests.registerTestFunctions((id, test) -> Registry.register(BuiltInRegistries.TEST_FUNCTION, id, test));
        WoodenShearsGameTests.registerEnvironmentTypes((id, codec) -> Registry.register(BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE, id, codec));
    }

    @Override
    public void setDurability(int durability) {
        AutoConfig.getConfigHolder(FabricConfigHelper.class).getConfig().DURABILITY = durability;
    }

}
