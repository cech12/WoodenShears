package de.cech12.woodenshears.gametest.tests;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import de.cech12.woodenshears.Constants;
import de.cech12.woodenshears.gametest.TestUtils;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.LongStream;

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
        List<String> tested = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof GrowingPlantHeadBlock head)) {
                continue;
            }
            String name = BuiltInRegistries.BLOCK.getKey(block).toString();
            tested.add(name);
            BlockState vanilla = trimPlantTip(helper, block, new ItemStack(Items.SHEARS));
            BlockState wooden = trimPlantTip(helper, block, TestUtils.woodenShears());
            if (vanilla == null || !head.isMaxAge(vanilla)) {
                problems.add(name + ": test setup broken, vanilla shears do not stop the growth");
            } else if (!vanilla.equals(wooden)) {
                problems.add(name + ": " + wooden + " instead of " + vanilla);
            }
        }
        helper.assertTrue(tested.containsAll(List.of("minecraft:twisting_vines", "minecraft:weeping_vines", "minecraft:cave_vines", "minecraft:kelp")),
                "test setup broken: known plant tips not found, only " + tested);
        helper.assertTrue(problems.isEmpty(), "using wooden shears on plant tips differs from vanilla shears: " + problems);
        helper.succeed();
    }

    /** Places a young plant tip, uses the shears on it and returns the new state, or null if the shears were not damaged. */
    private static BlockState trimPlantTip(GameTestHelper helper, Block block, ItemStack shears) {
        BlockState young = block.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 0);
        if (young.hasProperty(BlockStateProperties.WATERLOGGED)) {
            young = young.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        helper.setBlock(TestUtils.CENTER, young);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, shears);
        helper.useBlock(TestUtils.CENTER, player);
        BlockState result = helper.getBlockState(TestUtils.CENTER);
        boolean damaged = player.getMainHandItem().getDamageValue() == 1;
        helper.setBlock(TestUtils.CENTER, Blocks.AIR);
        return damaged ? result : null;
    }

    /** Position where both halves of double plants are placed for the loot comparison. */
    private static final BlockPos DOUBLE_PLANT_POS = TestUtils.CENTER;

    /**
     * Fixed seeds for the loot rolls, so the result is the same in every run and on every loader. Every block state is
     * rolled with the first seed. Block states where the shears make a difference with it, all states of the blocks of
     * {@link #VANILLA_SHEARS_DROPS} and of blocks whose loot table mentions shears are rolled with every seed, so random
     * hand drops (e.g. twisting vines drop
     * themselves with 33% without shears) cannot hide the shears dependency. Seed 0 is not allowed: vanilla then uses the
     * random of the level.
     */
    private static final long[] LOOT_SEEDS = LongStream.rangeClosed(1, 16).toArray();

    /**
     * All vanilla blocks whose loot tables check for shears ("minecraft:tool/can_shear" in
     * data/minecraft/loot_table/blocks of 26.3) and the item they drop with shears. Exactly these vanilla blocks must be
     * detected as shears specific: a missing block means the comparison silently checks nothing, an additional block
     * means that a new Minecraft version added one and this list must be updated.
     */
    private static final Map<String, String> VANILLA_SHEARS_DROPS = Map.ofEntries(
            // leaves: the leaves themselves instead of random saplings, sticks and apples
            Map.entry("acacia_leaves", "acacia_leaves"), Map.entry("azalea_leaves", "azalea_leaves"),
            Map.entry("birch_leaves", "birch_leaves"), Map.entry("cherry_leaves", "cherry_leaves"),
            Map.entry("dark_oak_leaves", "dark_oak_leaves"), Map.entry("flowering_azalea_leaves", "flowering_azalea_leaves"),
            Map.entry("jungle_leaves", "jungle_leaves"), Map.entry("mangrove_leaves", "mangrove_leaves"),
            Map.entry("oak_leaves", "oak_leaves"), Map.entry("orange_poplar_leaves", "orange_poplar_leaves"),
            Map.entry("pale_oak_leaves", "pale_oak_leaves"), Map.entry("red_poplar_leaves", "red_poplar_leaves"),
            Map.entry("spruce_leaves", "spruce_leaves"), Map.entry("yellow_poplar_leaves", "yellow_poplar_leaves"),
            // grass and plants: the plant instead of nothing or random seeds / sticks
            Map.entry("short_grass", "short_grass"), Map.entry("tall_grass", "short_grass"),
            Map.entry("fern", "fern"), Map.entry("large_fern", "fern"),
            Map.entry("short_dry_grass", "short_dry_grass"), Map.entry("tall_dry_grass", "tall_dry_grass"),
            Map.entry("bush", "bush"), Map.entry("dead_bush", "dead_bush"),
            Map.entry("seagrass", "seagrass"), Map.entry("tall_seagrass", "seagrass"),
            Map.entry("nether_sprouts", "nether_sprouts"), Map.entry("hanging_roots", "hanging_roots"),
            Map.entry("pale_hanging_moss", "pale_hanging_moss"), Map.entry("small_dripleaf", "small_dripleaf"),
            Map.entry("glow_lichen", "glow_lichen"), Map.entry("vine", "vine"), Map.entry("cobweb", "cobweb"),
            // vines: always the vines instead of a random 33% chance
            Map.entry("twisting_vines", "twisting_vines"), Map.entry("twisting_vines_plant", "twisting_vines"),
            Map.entry("weeping_vines", "weeping_vines"), Map.entry("weeping_vines_plant", "weeping_vines")
    );

    /**
     * For every block state of every registered block (also blocks of other loaded mods), the loot table produces the
     * same drops with wooden shears as with vanilla shears, with the seeds of {@link #LOOT_SEEDS}. This is what keeps
     * shears specific drops identical on all loaders: Fabric extends the vanilla shears conditions (MatchToolMixin),
     * Forge/NeoForge use their shears tool action.
     * <p>
     * A block is shears specific if vanilla shears and an empty hand drop something different with at least one seed.
     * The shears specific vanilla blocks must be exactly {@link #VANILLA_SHEARS_DROPS}. For these blocks the drops with
     * shears must not depend on the seed (so the equality with vanilla shears is no lucky roll) and must be the listed
     * item.
     */
    public static void dropsLikeVanillaShears(GameTestHelper helper) {
        var level = helper.getLevel();
        var lootTables = level.getServer().reloadableRegistries();
        ItemStack wooden = TestUtils.woodenShears();
        ItemStack vanilla = new ItemStack(Items.SHEARS);
        Vec3 origin = helper.absoluteVec(Vec3.atCenterOf(TestUtils.CENTER));
        List<String> differences = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> randomShearsDrops = new ArrayList<>();
        Set<String> shearsSpecificBlocks = new TreeSet<>();
        Map<String, Set<String>> shearsDrops = new TreeMap<>();
        RegistryOps<JsonElement> ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        long startTime = System.nanoTime();
        int states = 0;
        int stateRolls = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            var key = block.getLootTable();
            if (key.isEmpty()) {
                continue;
            }
            LootTable table = lootTables.getLootTable(key.get());
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            boolean knownShearsBlock = blockId.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) && VANILLA_SHEARS_DROPS.containsKey(blockId.getPath());
            boolean allSeeds = knownShearsBlock || mentionsShears(table, ops);
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                states++;
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
                    List<ItemStack> firstVanillaDrops = null;
                    boolean shearsSpecificState = false;
                    for (long seed : LOOT_SEEDS) {
                        if (seed != LOOT_SEEDS[0] && !allSeeds && !shearsSpecificState) {
                            // the other seeds only for states where the shears make a difference (keeps the test fast)
                            break;
                        }
                        stateRolls++;
                        List<ItemStack> woodenDrops = table.getRandomItems(params(level, lootOrigin, state, wooden), seed);
                        List<ItemStack> vanillaDrops = table.getRandomItems(params(level, lootOrigin, state, vanilla), seed);
                        List<ItemStack> handDrops = table.getRandomItems(params(level, lootOrigin, state, ItemStack.EMPTY), seed);
                        if (!sameStacks(woodenDrops, vanillaDrops)) {
                            differences.add(state + " with seed " + seed + " (wooden: " + woodenDrops + ", vanilla: " + vanillaDrops + ")");
                            break;
                        }
                        if (!sameStacks(vanillaDrops, handDrops)) {
                            shearsSpecificBlocks.add(blockId.toString());
                            shearsSpecificState = true;
                        }
                        if (knownShearsBlock) {
                            if (firstVanillaDrops == null) {
                                firstVanillaDrops = vanillaDrops;
                            } else if (!sameStacks(firstVanillaDrops, vanillaDrops)) {
                                randomShearsDrops.add(state + " (seed " + LOOT_SEEDS[0] + ": " + firstVanillaDrops + ", seed " + seed + ": " + vanillaDrops + ")");
                            }
                            vanillaDrops.stream().filter(drop -> !drop.isEmpty()).forEach(drop -> shearsDrops.computeIfAbsent(blockId.getPath(), id -> new TreeSet<>())
                                    .add(BuiltInRegistries.ITEM.getKey(drop.getItem()).getPath()));
                        }
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
        helper.assertTrue(errors.isEmpty(), "loot tables could not be evaluated: " + errors.stream().limit(10).toList());
        helper.assertTrue(differences.isEmpty(), differences.size() + " block states drop differently than with vanilla shears: "
                + differences.stream().limit(10).toList());
        Set<String> expectedBlocks = new TreeSet<>();
        VANILLA_SHEARS_DROPS.keySet().forEach(path -> expectedBlocks.add(Identifier.withDefaultNamespace(path).toString()));
        List<String> undetected = expectedBlocks.stream().filter(id -> !shearsSpecificBlocks.contains(id)).toList();
        helper.assertTrue(undetected.isEmpty(), "test is broken: drops of " + undetected + " do not depend on shears");
        List<String> unknown = shearsSpecificBlocks.stream()
                .filter(id -> id.startsWith(Identifier.DEFAULT_NAMESPACE + ":") && !expectedBlocks.contains(id)).toList();
        helper.assertTrue(unknown.isEmpty(), "vanilla blocks with unknown shears specific drops, add them to VANILLA_SHEARS_DROPS: " + unknown);
        helper.assertTrue(randomShearsDrops.isEmpty(), "drops with shears depend on the seed: " + randomShearsDrops.stream().limit(10).toList());
        List<String> wrongDrops = VANILLA_SHEARS_DROPS.entrySet().stream()
                .filter(entry -> !Set.of(entry.getValue()).equals(shearsDrops.get(entry.getKey())))
                .map(entry -> entry.getKey() + " drops " + shearsDrops.get(entry.getKey()) + " instead of " + entry.getValue()).sorted().toList();
        helper.assertTrue(wrongDrops.isEmpty(), "unexpected drops with shears: " + wrongDrops);
        Constants.LOG.info("Compared drops of {} shears specific blocks with vanilla shears ({} seeds per shears specific block state, "
                        + "{} block states, {} seed rolls, {} ms): {}", shearsSpecificBlocks.size(), LOOT_SEEDS.length, states, stateRolls,
                (System.nanoTime() - startTime) / 1_000_000, shearsSpecificBlocks);
        helper.succeed();
    }

    /**
     * True if the loot table mentions shears anywhere (vanilla predicate "minecraft:tool/can_shear", the shears tool
     * action of Forge/NeoForge, shears items or tags), or if it cannot be serialized. Such tables are rolled with every
     * seed, even if the first seed shows no difference (e.g. a random hand drop that equals the shears drop).
     */
    private static boolean mentionsShears(LootTable table, RegistryOps<JsonElement> ops) {
        return LootTable.DIRECT_CODEC.encodeStart(ops, table).result()
                .map(json -> json.toString().contains("shear"))
                .orElse(true);
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
