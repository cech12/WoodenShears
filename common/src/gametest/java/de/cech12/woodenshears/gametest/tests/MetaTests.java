package de.cech12.woodenshears.gametest.tests;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests that check the test setup itself.
 */
public final class MetaTests {

    private MetaTests() {}

    public static void register(WoodenShearsGameTests.TestCollector tests) {
        tests.add("meta/every_test_function_has_an_instance", MetaTests::everyTestFunctionHasAnInstance);
    }

    /**
     * Every test function has a test_instance json with the same id that runs exactly this function. A test function
     * without one would silently never run, and a copied json that names another function would run that one twice.
     */
    public static void everyTestFunctionHasAnInstance(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var instances = registries.lookupOrThrow(Registries.TEST_INSTANCE);
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        List<Identifier> missing = new ArrayList<>();
        List<String> wrong = new ArrayList<>();
        for (Identifier id : WoodenShearsGameTests.getTests().keySet()) {
            var instance = instances.get(ResourceKey.create(Registries.TEST_INSTANCE, id));
            if (instance.isEmpty()) {
                missing.add(id);
                continue;
            }
            GameTestInstance test = instance.get().value();
            String function = test instanceof FunctionGameTestInstance
                    ? GameTestInstance.DIRECT_CODEC.encodeStart(ops, test).result()
                            .filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject)
                            .map(json -> json.get("function")).map(JsonElement::getAsString).orElse("?")
                    : "(no function instance)";
            if (!id.toString().equals(function)) {
                wrong.add(id + " runs " + function);
            }
        }
        helper.assertTrue(missing.isEmpty(), "Test functions without test_instance json: " + missing);
        helper.assertTrue(wrong.isEmpty(), "test_instance jsons that do not run their own test function: " + wrong);
        helper.succeed();
    }

}
