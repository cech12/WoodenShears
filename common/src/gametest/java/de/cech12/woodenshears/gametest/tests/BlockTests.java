package de.cech12.woodenshears.gametest.tests;

import de.cech12.woodenshears.Constants;
import de.cech12.woodenshears.gametest.TestUtils;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Using and breaking blocks with wooden shears does everything vanilla shears do.
 */
public final class BlockTests {

    private BlockTests() {}

    public static void register(WoodenShearsGameTests.TestCollector tests) {
        tests.add("block/carves_pumpkin", BlockTests::carvesPumpkin);
        tests.add("block/harvests_full_beehives", BlockTests::harvestsFullBeehives);
        tests.add("block/does_not_harvest_beehives_without_honey", BlockTests::doesNotHarvestBeehivesWithoutHoney);
        tests.add("block/disarms_tripwire", BlockTests::disarmsTripwire);
        tests.add("block/tripwire_triggers_without_shears", BlockTests::tripwireTriggersWithoutShears);
        tests.add("block/stops_growth_of_plant_tips", BlockTests::stopsGrowthOfPlantTips);
        tests.add("block/drops_like_vanilla_shears", BlockTests::dropsLikeVanillaShears);
        tests.add("block/breaking_drops_shearable_blocks", BlockTests::breakingDropsShearableBlocks);
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        return TestUtils.player(helper, GameType.SURVIVAL, TestUtils.woodenShears());
    }

    /** Fixed 1.17.1-1.1.0.1: using shears on a pumpkin carves it and drops pumpkin seeds. */
    public static void carvesPumpkin(GameTestHelper helper) {
        helper.setBlock(TestUtils.CENTER, Blocks.PUMPKIN);
        ServerPlayer player = survivalPlayer(helper);
        helper.useBlock(TestUtils.CENTER, player);
        helper.assertBlockPresent(Blocks.CARVED_PUMPKIN, TestUtils.CENTER);
        helper.assertItemEntityPresent(Items.PUMPKIN_SEEDS);
        TestUtils.assertDamage(helper, player.getMainHandItem(), 1, "after carving a pumpkin");
        helper.succeed();
    }

    /** Fixed 1.17.1-1.1.0.1: using shears on a full beehive or bee nest drops honeycombs and empties it. */
    public static void harvestsFullBeehives(GameTestHelper helper) {
        BlockPos hive = TestUtils.CENTER.west();
        BlockPos nest = TestUtils.CENTER.east();
        helper.setBlock(hive, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS));
        helper.setBlock(nest, Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS));
        ServerPlayer player = survivalPlayer(helper);
        helper.useBlock(hive, player);
        helper.useBlock(nest, player);
        helper.assertBlockProperty(hive, BeehiveBlock.HONEY_LEVEL, 0);
        helper.assertBlockProperty(nest, BeehiveBlock.HONEY_LEVEL, 0);
        helper.assertItemEntityPresent(Items.HONEYCOMB, hive, 2);
        helper.assertItemEntityPresent(Items.HONEYCOMB, nest, 2);
        TestUtils.assertDamage(helper, player.getMainHandItem(), 2, "after harvesting two beehives");
        helper.succeed();
    }

    /** Beehives and bee nests that are not full (honey level 0 - 4) are not harvested and the shears are not damaged. */
    public static void doesNotHarvestBeehivesWithoutHoney(GameTestHelper helper) {
        BlockPos hive = TestUtils.CENTER.west();
        BlockPos nest = TestUtils.CENTER.east();
        ServerPlayer player = survivalPlayer(helper);
        for (int level = 0; level < BeehiveBlock.MAX_HONEY_LEVELS; level++) {
            helper.setBlock(hive, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, level));
            helper.setBlock(nest, Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, level));
            helper.useBlock(hive, player);
            helper.useBlock(nest, player);
            helper.assertBlockProperty(hive, BeehiveBlock.HONEY_LEVEL, level);
            helper.assertBlockProperty(nest, BeehiveBlock.HONEY_LEVEL, level);
        }
        helper.assertItemEntityNotPresent(Items.HONEYCOMB);
        TestUtils.assertDamage(helper, player.getMainHandItem(), 0, "after using it on beehives without enough honey");
        helper.succeed();
    }

    // Tripwire setup: stone - hook (facing east) - tripwire - hook (facing west) - stone
    private static final BlockPos HOOK_WEST = new BlockPos(1, 1, 2);
    private static final BlockPos WIRE = new BlockPos(2, 1, 2);
    private static final BlockPos HOOK_EAST = new BlockPos(3, 1, 2);

    private static void buildTripwire(GameTestHelper helper) {
        helper.setBlock(HOOK_WEST.west(), Blocks.STONE);
        helper.setBlock(HOOK_EAST.east(), Blocks.STONE);
        helper.setBlock(WIRE, Blocks.TRIPWIRE.defaultBlockState()
                .setValue(TripWireBlock.ATTACHED, true)
                .setValue(TripWireBlock.EAST, true)
                .setValue(TripWireBlock.WEST, true));
        BlockState hook = Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.ATTACHED, true);
        helper.setBlock(HOOK_WEST, hook.setValue(TripWireHookBlock.FACING, Direction.EAST));
        helper.setBlock(HOOK_EAST, hook.setValue(TripWireHookBlock.FACING, Direction.WEST));
    }

    private static boolean anyHookPowered(GameTestHelper helper) {
        return helper.getBlockState(HOOK_WEST).getValue(TripWireHookBlock.POWERED) || helper.getBlockState(HOOK_EAST).getValue(TripWireHookBlock.POWERED);
    }

    /** Fixed 1.17.1-1.1.0.1: cutting a tripwire with shears disarms it, the hooks do not send a redstone signal. */
    public static void disarmsTripwire(GameTestHelper helper) {
        buildTripwire(helper);
        ServerPlayer player = survivalPlayer(helper);
        helper.assertTrue(TestUtils.destroyBlock(helper, player, WIRE), "tripwire could not be broken");
        helper.assertBlockNotPresent(Blocks.TRIPWIRE, WIRE);
        TestUtils.assertDamage(helper, player.getMainHandItem(), 1, "after cutting a tripwire");
        helper.startSequence()
                .thenExecuteFor(15, () -> helper.assertTrue(!anyHookPowered(helper), "tripwire hook was triggered although the tripwire was cut with shears"))
                .thenSucceed();
    }

    /** Control test for disarmsTripwire: without shears the same setup triggers the hooks. */
    public static void tripwireTriggersWithoutShears(GameTestHelper helper) {
        buildTripwire(helper);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        helper.assertTrue(TestUtils.destroyBlock(helper, player, WIRE), "tripwire could not be broken");
        helper.assertTrue(anyHookPowered(helper), "tripwire test setup is broken: hooks are not triggered without shears");
        helper.succeed();
    }

    /** Using shears on the tip of a growing plant (e.g. twisting vines) stops its growth. */
    public static void stopsGrowthOfPlantTips(GameTestHelper helper) {
        helper.setBlock(TestUtils.CENTER, Blocks.TWISTING_VINES);
        ServerPlayer player = survivalPlayer(helper);
        helper.useBlock(TestUtils.CENTER, player);
        BlockState state = helper.getBlockState(TestUtils.CENTER);
        helper.assertTrue(state.getBlock() instanceof GrowingPlantHeadBlock head && head.isMaxAge(state), "twisting vines tip still grows: " + state);
        TestUtils.assertDamage(helper, player.getMainHandItem(), 1, "after trimming a plant tip");
        helper.succeed();
    }

    /** Position where both halves of double plants are placed for the loot comparison. */
    private static final BlockPos DOUBLE_PLANT_POS = TestUtils.CENTER;

    /**
     * Blocks whose drops are known to depend on shears. They must be detected as shears specific, otherwise the
     * comparison in {@link #dropsLikeVanillaShears} would silently check nothing.
     */
    private static final List<Block> KNOWN_SHEARS_BLOCKS = List.of(
            Blocks.OAK_LEAVES, Blocks.COBWEB, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
            Blocks.DEAD_BUSH, Blocks.VINE, Blocks.GLOW_LICHEN, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS, Blocks.HANGING_ROOTS,
            Blocks.NETHER_SPROUTS, Blocks.TWISTING_VINES, Blocks.WEEPING_VINES
    );

    /**
     * For every block state of every registered block (also blocks of other loaded mods), the loot table produces the
     * same drops with wooden shears as with vanilla shears (leaves, grass, vines, cobwebs, ... drop themselves, tall
     * grass drops grass, ...). This is what keeps shears specific drops identical on all loaders: Fabric extends the
     * vanilla shears conditions (MatchToolMixin), Forge/NeoForge use their shears tool action.
     */
    public static void dropsLikeVanillaShears(GameTestHelper helper) {
        var level = helper.getLevel();
        var lootTables = level.getServer().reloadableRegistries();
        ItemStack wooden = TestUtils.woodenShears();
        ItemStack vanilla = new ItemStack(Items.SHEARS);
        Vec3 origin = helper.absoluteVec(Vec3.atCenterOf(TestUtils.CENTER));
        List<String> differences = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<Block> shearsSpecificBlocks = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            var key = block.getLootTable();
            if (key.isEmpty()) {
                continue;
            }
            LootTable table = lootTables.getLootTable(key.get());
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                long seed = (long) blockId.hashCode() * 31 + state.hashCode();
                Vec3 lootOrigin = origin;
                boolean doublePlant = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
                if (doublePlant) {
                    // loot tables of double plants (tall grass, large fern, ...) check that the other half exists
                    BlockState lower = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
                    placeWithoutUpdates(helper, DOUBLE_PLANT_POS, lower);
                    placeWithoutUpdates(helper, DOUBLE_PLANT_POS.above(), lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
                    BlockPos lootPos = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? DOUBLE_PLANT_POS : DOUBLE_PLANT_POS.above();
                    lootOrigin = helper.absoluteVec(Vec3.atCenterOf(lootPos));
                }
                try {
                    List<ItemStack> woodenDrops = table.getRandomItems(params(level, lootOrigin, state, wooden), seed);
                    List<ItemStack> vanillaDrops = table.getRandomItems(params(level, lootOrigin, state, vanilla), seed);
                    List<ItemStack> handDrops = table.getRandomItems(params(level, lootOrigin, state, ItemStack.EMPTY), seed);
                    if (!sameStacks(woodenDrops, vanillaDrops)) {
                        differences.add(state + " (wooden: " + woodenDrops + ", vanilla: " + vanillaDrops + ")");
                    }
                    if (!sameStacks(vanillaDrops, handDrops)) {
                        shearsSpecificBlocks.add(block);
                    }
                } catch (RuntimeException e) {
                    errors.add(state + ": " + e);
                } finally {
                    if (doublePlant) {
                        placeWithoutUpdates(helper, DOUBLE_PLANT_POS.above(), Blocks.AIR.defaultBlockState());
                        placeWithoutUpdates(helper, DOUBLE_PLANT_POS, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        List<String> undetected = KNOWN_SHEARS_BLOCKS.stream().filter(block -> !shearsSpecificBlocks.contains(block))
                .map(block -> BuiltInRegistries.BLOCK.getKey(block).toString()).toList();
        helper.assertTrue(undetected.isEmpty(), "test is broken: drops of " + undetected + " do not depend on shears");
        helper.assertTrue(errors.isEmpty(), "loot tables could not be evaluated: " + errors.stream().limit(10).toList());
        helper.assertTrue(differences.isEmpty(), differences.size() + " block states drop differently than with vanilla shears: "
                + differences.stream().limit(10).toList());
        Constants.LOG.info("Compared drops of {} shears specific blocks with vanilla shears: {}", shearsSpecificBlocks.size(),
                shearsSpecificBlocks.stream().map(block -> BuiltInRegistries.BLOCK.getKey(block).toString()).sorted().toList());
        helper.succeed();
    }

    /** Sets a block without neighbor updates, so plants do not break on the stone floor. */
    private static void placeWithoutUpdates(GameTestHelper helper, BlockPos pos, BlockState state) {
        helper.getLevel().setBlock(helper.absolutePos(pos), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
    }

    private static LootParams params(net.minecraft.server.level.ServerLevel level, Vec3 origin, BlockState state, ItemStack tool) {
        return new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
    }

    private static boolean sameStacks(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            if (!ItemStack.matches(first.get(i), second.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fixed #11 (glow lichen) and 1.18.2 breaking behavior: really breaking shearable blocks in survival drops the
     * blocks themselves and damages the shears by one per block.
     */
    public static void breakingDropsShearableBlocks(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = Map.of(
                new BlockPos(0, 1, 0), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
                new BlockPos(2, 1, 0), Blocks.COBWEB.defaultBlockState(),
                new BlockPos(4, 1, 0), Blocks.SHORT_GRASS.defaultBlockState(),
                new BlockPos(0, 1, 2), Blocks.FERN.defaultBlockState(),
                new BlockPos(4, 1, 2), Blocks.DEAD_BUSH.defaultBlockState(),
                new BlockPos(0, 1, 4), Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true)
        );
        blocks.forEach((pos, state) -> {
            helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
            helper.setBlock(pos, state);
        });
        ServerPlayer player = survivalPlayer(helper);
        blocks.forEach((pos, state) -> {
            helper.assertTrue(TestUtils.destroyBlock(helper, player, pos), "could not break " + state);
            helper.assertItemEntityPresent(state.getBlock().asItem(), pos, 1.5);
        });
        TestUtils.assertDamage(helper, player.getMainHandItem(), blocks.size(), "after breaking " + blocks.size() + " blocks");
        helper.succeed();
    }

}
