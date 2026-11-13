package de.cech12.woodenshears.gametest.tests;

import de.cech12.woodenshears.gametest.GameTestPlatform;
import de.cech12.woodenshears.gametest.TestUtils;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import de.cech12.woodenshears.platform.Services;
import de.cech12.woodenshears.platform.services.IConfigHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Config option "durability". Every test runs in a test environment that sets the config value for its batch
 * (see test_environment jsons and DurabilityConfigEnvironment).
 */
public final class DurabilityTests {

    private DurabilityTests() {}

    public static void register(WoodenShearsGameTests.TestCollector tests) {
        tests.add("durability/default_value", DurabilityTests::defaultValue);
        tests.add("durability/custom_value", DurabilityTests::customValue);
        tests.add("durability/maximum_value", DurabilityTests::maximumValue);
        tests.add("durability/above_maximum_is_limited", DurabilityTests::aboveMaximumIsLimited);
        tests.add("durability/zero_makes_shears_unbreakable", DurabilityTests::zeroMakesShearsUnbreakable);
        tests.add("durability/below_zero_makes_shears_unbreakable", DurabilityTests::zeroMakesShearsUnbreakable);
        tests.add("durability/shears_break_when_used_up", DurabilityTests::shearsBreakWhenUsedUp);
        tests.add("durability/config_change_applies_to_existing_shears", DurabilityTests::configChangeAppliesToExistingShears);
    }

    private static void assertMaxDamage(GameTestHelper helper, ItemStack stack, int expected) {
        helper.assertTrue(stack.isDamageableItem(), "wooden shears are not damageable");
        helper.assertValueEqual(stack.getMaxDamage(), expected, "max damage of wooden shears");
    }

    private static void assertUnbreakable(GameTestHelper helper, ItemStack stack) {
        helper.assertTrue(!stack.isDamageableItem(), "wooden shears are damageable although durability is 0");
        helper.assertTrue(!stack.has(DataComponents.MAX_DAMAGE), "wooden shears have a max damage although durability is 0");
    }

    /** Environment "default": durability 50 (default config value). */
    public static void defaultValue(GameTestHelper helper) {
        helper.assertValueEqual(Services.CONFIG.getDurability(), IConfigHelper.DURABILITY_DEFAULT, "configured durability");
        assertMaxDamage(helper, TestUtils.woodenShears(), IConfigHelper.DURABILITY_DEFAULT);
        helper.succeed();
    }

    /** Environment "durability_200": a custom durability is used as max damage. */
    public static void customValue(GameTestHelper helper) {
        assertMaxDamage(helper, TestUtils.woodenShears(), 200);
        helper.succeed();
    }

    /** Environment "durability_10000": the maximal allowed durability. */
    public static void maximumValue(GameTestHelper helper) {
        assertMaxDamage(helper, TestUtils.woodenShears(), IConfigHelper.DURABILITY_MAX);
        helper.succeed();
    }

    /** Environment "durability_20000": values above the maximum are limited to 10000. */
    public static void aboveMaximumIsLimited(GameTestHelper helper) {
        helper.assertValueEqual(Services.CONFIG.getDurability(), IConfigHelper.DURABILITY_MAX, "configured durability");
        assertMaxDamage(helper, TestUtils.woodenShears(), IConfigHelper.DURABILITY_MAX);
        helper.succeed();
    }

    /**
     * Environments "durability_0" and "durability_-1": durability 0 (or less) deactivates the durability.
     * The shears are not damaged by usage.
     */
    public static void zeroMakesShearsUnbreakable(GameTestHelper helper) {
        helper.assertValueEqual(Services.CONFIG.getDurability(), 0, "configured durability");
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.woodenShears());
        ItemStack stack = player.getMainHandItem();
        assertUnbreakable(helper, stack);
        stack.hurtAndBreak(10, player, EquipmentSlot.MAINHAND);
        helper.assertTrue(!player.getMainHandItem().isEmpty(), "unbreakable wooden shears broke");
        TestUtils.assertDamage(helper, player.getMainHandItem(), 0, "after usage");
        helper.succeed();
    }

    /** Environment "default": shears break after 50 uses. */
    public static void shearsBreakWhenUsedUp(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.woodenShears());
        player.getMainHandItem().hurtAndBreak(IConfigHelper.DURABILITY_DEFAULT - 1, player, EquipmentSlot.MAINHAND);
        helper.assertTrue(!player.getMainHandItem().isEmpty(), "wooden shears broke too early");
        player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "wooden shears did not break after " + IConfigHelper.DURABILITY_DEFAULT + " uses");
        helper.succeed();
    }

    /**
     * Environment "durability_dynamic" (own batch, because the test changes the config while running):
     * The durability is not stored in the item stack, so config changes apply to already existing shears.
     */
    public static void configChangeAppliesToExistingShears(GameTestHelper helper) {
        ItemStack stack = TestUtils.woodenShears();
        stack.setDamageValue(10);
        GameTestPlatform.INSTANCE.setDurability(100);
        assertMaxDamage(helper, stack, 100);
        TestUtils.assertDamage(helper, stack, 10, "after config change");
        GameTestPlatform.INSTANCE.setDurability(0);
        helper.assertTrue(!stack.isDamageableItem(), "existing wooden shears are still damageable after durability was set to 0");
        GameTestPlatform.INSTANCE.setDurability(IConfigHelper.DURABILITY_DEFAULT);
        assertMaxDamage(helper, stack, IConfigHelper.DURABILITY_DEFAULT);
        helper.succeed();
    }

}
