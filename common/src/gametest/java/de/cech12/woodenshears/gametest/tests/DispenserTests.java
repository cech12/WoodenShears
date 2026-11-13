package de.cech12.woodenshears.gametest.tests;

import de.cech12.woodenshears.gametest.TestUtils;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

/**
 * Dispensers use wooden shears like vanilla shears.
 */
public final class DispenserTests {

    private static final BlockPos DISPENSER = new BlockPos(2, 1, 1);
    private static final BlockPos TARGET = new BlockPos(2, 1, 2);

    private DispenserTests() {}

    public static void register(WoodenShearsGameTests.TestCollector tests) {
        tests.add("dispenser/shears_sheep", DispenserTests::shearsSheep);
        tests.add("dispenser/harvests_full_beehive", DispenserTests::harvestsFullBeehive);
        tests.add("dispenser/cuts_leash", DispenserTests::cutsLeash);
    }

    /** Places a dispenser with wooden shears that faces the target position and powers it. */
    private static DispenserBlockEntity dispense(GameTestHelper helper) {
        helper.setBlock(DISPENSER, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
        DispenserBlockEntity dispenser = helper.getBlockEntity(DISPENSER, DispenserBlockEntity.class);
        dispenser.setItem(0, TestUtils.woodenShears());
        helper.setBlock(DISPENSER.west(), Blocks.REDSTONE_BLOCK);
        return dispenser;
    }

    public static void shearsSheep(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, TARGET);
        DispenserBlockEntity dispenser = dispense(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(sheep.isSheared(), "sheep was not sheared by the dispenser");
            TestUtils.assertItemEntityPresent(helper, ItemTags.WOOL);
            TestUtils.assertDamage(helper, dispenser.getItem(0), 1, "in dispenser after shearing a sheep");
        });
    }

    public static void harvestsFullBeehive(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS));
        DispenserBlockEntity dispenser = dispense(helper);
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(TARGET, BeehiveBlock.HONEY_LEVEL, 0);
            helper.assertItemEntityPresent(Items.HONEYCOMB);
            TestUtils.assertDamage(helper, dispenser.getItem(0), 1, "in dispenser after harvesting a beehive");
        });
    }

    public static void cutsLeash(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityTypes.PIG, TARGET);
        BlockPos fence = TARGET.east(2);
        helper.setBlock(fence, Blocks.OAK_FENCE);
        pig.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(helper.getLevel(), helper.absolutePos(fence)), true);
        DispenserBlockEntity dispenser = dispense(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(!pig.isLeashed(), "leash was not cut by the dispenser");
            TestUtils.assertDamage(helper, dispenser.getItem(0), 1, "in dispenser after cutting a leash");
        });
    }

}
