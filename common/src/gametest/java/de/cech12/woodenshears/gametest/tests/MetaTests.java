package de.cech12.woodenshears.gametest.tests;

import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

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
     * A test function without a test_instance json would silently never run.
     */
    public static void everyTestFunctionHasAnInstance(GameTestHelper helper) {
        var instances = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
        List<Identifier> missing = WoodenShearsGameTests.getTests().keySet().stream()
                .filter(id -> instances.get(ResourceKey.create(Registries.TEST_INSTANCE, id)).isEmpty())
                .toList();
        helper.assertTrue(missing.isEmpty(), "Test functions without test_instance json: " + missing);
        helper.succeed();
    }

}
