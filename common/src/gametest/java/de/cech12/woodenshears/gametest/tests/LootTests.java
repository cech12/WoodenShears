package de.cech12.woodenshears.gametest.tests;

import de.cech12.woodenshears.Constants;
import de.cech12.woodenshears.gametest.KnownIssues;
import de.cech12.woodenshears.gametest.TestUtils;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import de.cech12.woodenshears.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Loot tables of data packs that check for shears. The test loot tables are part of the gametest resources
 * (data/woodenshears/loot_table/gametest/*.json) and drop a diamond if their shears condition matches.
 * <p>
 * Recommended for data packs (works on all loaders): reference the vanilla predicate "minecraft:tool/can_shear" (like
 * vanilla loot tables do) or use the tag "c:tools/shear". Forge and NeoForge replace "minecraft:tool/can_shear" with
 * their shears tool action, Fabric keeps the vanilla predicate and adds wooden shears to it (MatchToolMixin).
 */
public final class LootTests {

    private LootTests() {}

    public static void register(WoodenShearsGameTests.TestCollector tests) {
        tests.add("loot/can_shear_predicate_accepts_wooden_shears", LootTests::canShearPredicate);
        tests.add("loot/match_tool_with_shears_tag_accepts_wooden_shears", LootTests::matchToolWithShearsTag);
        tests.add("loot/fabric_match_tool_with_vanilla_shears_accepts_wooden_shears", LootTests::fabricMatchToolWithVanillaShears);
    }

    private static List<ItemStack> roll(GameTestHelper helper, String table, ItemStack tool) {
        var level = helper.getLevel();
        LootTable lootTable = level.getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Constants.id("gametest/" + table)));
        helper.assertTrue(lootTable != LootTable.EMPTY, "test loot table " + table + " is missing");
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.atCenterOf(TestUtils.CENTER)))
                .withParameter(LootContextParams.BLOCK_STATE, Blocks.STONE.defaultBlockState())
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        return lootTable.getRandomItems(params, 0L);
    }

    private static void assertMatches(GameTestHelper helper, String table) {
        helper.assertTrue(roll(helper, table, new ItemStack(Items.SHEARS)).stream().anyMatch(s -> s.is(Items.DIAMOND)),
                "test loot table " + table + " is broken: vanilla shears do not match");
        helper.assertTrue(roll(helper, table, ItemStack.EMPTY).isEmpty(), "test loot table " + table + " is broken: an empty hand matches");
        helper.assertTrue(roll(helper, table, TestUtils.woodenShears()).stream().anyMatch(s -> s.is(Items.DIAMOND)),
                "wooden shears do not match the shears condition of loot table " + table);
    }

    /** Data packs that reference the vanilla predicate "minecraft:tool/can_shear" accept wooden shears (all loaders). */
    public static void canShearPredicate(GameTestHelper helper) {
        assertMatches(helper, "predicate_can_shear");
        helper.succeed();
    }

    /**
     * Regression https://github.com/cech12/CeramicShears/issues/28: data packs with a shears tag as "match_tool"
     * condition crashed the world loading (this test would not even start) and must accept wooden shears.
     */
    public static void matchToolWithShearsTag(GameTestHelper helper) {
        assertMatches(helper, "match_tool_shears_tag");
        helper.succeed();
    }

    /**
     * Fabric only: "match_tool" conditions that name vanilla shears directly ("items": "minecraft:shears") also accept
     * wooden shears, because Fabric has no shears tool action and the MatchToolMixin extends these conditions.
     * Forge and NeoForge intentionally keep such explicit item checks as they are (data packs should use the predicate or
     * the tag, see above), so this test does nothing there.
     */
    public static void fabricMatchToolWithVanillaShears(GameTestHelper helper) {
        if (KnownIssues.FABRIC.equals(Services.PLATFORM.getPlatformName())) {
            assertMatches(helper, "match_tool_vanilla_shears");
        }
        helper.succeed();
    }

}
