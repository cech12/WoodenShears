package de.cech12.woodenshears.gametest.tests;

import de.cech12.woodenshears.gametest.TestUtils;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Right-clicking entities with wooden shears does everything vanilla shears do.
 */
public final class EntityTests {

    private EntityTests() {}

    public static void register(WoodenShearsGameTests.TestCollector tests) {
        tests.add("entity/shears_sheep", EntityTests::shearsSheep);
        tests.add("entity/shears_in_creative_without_damage", EntityTests::shearsInCreativeWithoutDamage);
        tests.add("entity/shears_mooshroom", EntityTests::shearsMooshroom);
        tests.add("entity/shears_snow_golem", EntityTests::shearsSnowGolem);
        tests.add("entity/shears_bogged", EntityTests::shearsBogged);
        tests.add("entity/shears_copper_golem", EntityTests::shearsCopperGolem);
        tests.add("entity/shears_sulfur_cube", EntityTests::shearsSulfurCube);
        tests.add("entity/removes_saddle", EntityTests::removesSaddle);
        tests.add("entity/removes_wolf_armor", EntityTests::removesWolfArmor);
        tests.add("entity/removes_equipment_like_vanilla_shears", EntityTests::removesEquipmentLikeVanillaShears);
        tests.add("entity/cuts_leash_of_entity", EntityTests::cutsLeashOfEntity);
        tests.add("entity/cuts_leash_knot", EntityTests::cutsLeashKnot);
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        return TestUtils.player(helper, GameType.SURVIVAL, TestUtils.woodenShears());
    }

    private static void assertConsumed(GameTestHelper helper, InteractionResult result, String target) {
        helper.assertTrue(result.consumesAction(), "interaction with " + target + " was not successful: " + result);
    }

    /** Shearing a sheep drops wool, marks it as sheared and damages the shears by one. */
    public static void shearsSheep(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, TestUtils.CENTER);
        ServerPlayer player = survivalPlayer(helper);
        assertConsumed(helper, TestUtils.interact(player, sheep), "sheep");
        helper.assertTrue(sheep.isSheared(), "sheep was not sheared");
        TestUtils.assertItemEntityPresent(helper, ItemTags.WOOL);
        TestUtils.assertDamage(helper, player.getMainHandItem(), 1, "after shearing a sheep");
        helper.succeed();
    }

    /** Shearing a sheep, a mooshroom and a snow golem in creative mode works and does not damage the shears. */
    public static void shearsInCreativeWithoutDamage(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, TestUtils.CENTER);
        MushroomCow mooshroom = helper.spawn(EntityTypes.MOOSHROOM, TestUtils.CENTER.west());
        SnowGolem golem = helper.spawn(EntityTypes.SNOW_GOLEM, TestUtils.CENTER.east());
        ServerPlayer player = TestUtils.player(helper, GameType.CREATIVE, TestUtils.woodenShears());
        assertConsumed(helper, TestUtils.interact(player, sheep), "sheep");
        assertConsumed(helper, TestUtils.interact(player, mooshroom), "mooshroom");
        assertConsumed(helper, TestUtils.interact(player, golem), "snow golem");
        helper.assertTrue(sheep.isSheared(), "sheep was not sheared");
        helper.assertTrue(!golem.hasPumpkin(), "snow golem still has its pumpkin");
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityTypes.COW);
            TestUtils.assertDamage(helper, player.getMainHandItem(), 0, "after shearing in creative mode");
        });
    }

    /** Shearing a mooshroom turns it into a cow and drops mushrooms. */
    public static void shearsMooshroom(GameTestHelper helper) {
        MushroomCow mooshroom = helper.spawn(EntityTypes.MOOSHROOM, TestUtils.CENTER);
        ServerPlayer player = survivalPlayer(helper);
        assertConsumed(helper, TestUtils.interact(player, mooshroom), "mooshroom");
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityTypes.COW);
            helper.assertItemEntityPresent(Items.RED_MUSHROOM);
            TestUtils.assertDamage(helper, player.getMainHandItem(), 1, "after shearing a mooshroom");
        });
    }

    /** Shearing a snow golem removes its pumpkin. */
    public static void shearsSnowGolem(GameTestHelper helper) {
        SnowGolem golem = helper.spawn(EntityTypes.SNOW_GOLEM, TestUtils.CENTER);
        ServerPlayer player = survivalPlayer(helper);
        assertConsumed(helper, TestUtils.interact(player, golem), "snow golem");
        helper.assertTrue(!golem.hasPumpkin(), "snow golem still has its pumpkin");
        helper.assertItemEntityPresent(Items.CARVED_PUMPKIN);
        TestUtils.assertDamage(helper, player.getMainHandItem(), 1, "after shearing a snow golem");
        helper.succeed();
    }

    /** Shearing a bogged drops mushrooms and marks it as sheared (Forge known issue fixed in 1.21-3.1.0.0). */
    public static void shearsBogged(GameTestHelper helper) {
        Bogged bogged = helper.spawn(EntityTypes.BOGGED, TestUtils.CENTER);
        ServerPlayer player = survivalPlayer(helper);
        assertConsumed(helper, TestUtils.interact(player, bogged), "bogged");
        helper.assertTrue(bogged.isSheared(), "bogged was not sheared");
        TestUtils.assertDamage(helper, player.getMainHandItem(), 1, "after shearing a bogged");
        helper.succeed();
    }

    /** 1.21.9-3.6.0.1: the flower of a copper golem can be sheared off. */
    public static void shearsCopperGolem(GameTestHelper helper) {
        CopperGolem golem = helper.spawn(EntityTypes.COPPER_GOLEM, TestUtils.CENTER);
        Item flower = TestUtils.firstOfTag(helper, ItemTags.SHEARABLE_FROM_COPPER_GOLEM);
        golem.setItemSlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA, new ItemStack(flower));
        ServerPlayer player = survivalPlayer(helper);
        assertConsumed(helper, TestUtils.interact(player, golem), "copper golem");
        helper.assertTrue(golem.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).isEmpty(), "copper golem still has its flower");
        helper.assertItemEntityPresent(flower);
        TestUtils.assertDamage(helper, player.getMainHandItem(), 1, "after shearing a copper golem");
        helper.succeed();
    }

    /** Shearing a sulfur cube ejects the swallowed item. */
    public static void shearsSulfurCube(GameTestHelper helper) {
        SulfurCube cube = helper.spawn(EntityTypes.SULFUR_CUBE, TestUtils.CENTER);
        Item swallowable = TestUtils.firstOfTag(helper, ItemTags.SULFUR_CUBE_SWALLOWABLE);
        cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(swallowable));
        helper.assertTrue(cube.readyForShearing(), "sulfur cube test setup failed: cube is not ready for shearing");
        ServerPlayer player = survivalPlayer(helper);
        assertConsumed(helper, TestUtils.interact(player, cube), "sulfur cube");
        helper.assertTrue(cube.getItemBySlot(EquipmentSlot.BODY).isEmpty(), "sulfur cube still contains its item");
        helper.assertItemEntityPresent(swallowable);
        TestUtils.assertDamage(helper, player.getMainHandItem(), 1, "after shearing a sulfur cube");
        helper.succeed();
    }

    // Positions of the wooden and the vanilla shears run of the equipment tests. They are 4 blocks apart and the drop is
    // searched within 1.5 blocks, so a run never finds the drop of the other one.
    private static final BlockPos WOODEN_POS = TestUtils.CENTER.west(2);
    private static final BlockPos VANILLA_POS = TestUtils.CENTER.east(2);
    private static final double DROP_RADIUS = 1.5;

    /** Shears cut off equipment like a saddle, with the same durability loss as vanilla shears. */
    public static void removesSaddle(GameTestHelper helper) {
        ItemStack wooden = removeSaddle(helper, WOODEN_POS, TestUtils.woodenShears());
        ItemStack vanilla = removeSaddle(helper, VANILLA_POS, new ItemStack(Items.SHEARS));
        TestUtils.assertDamage(helper, wooden, vanilla.getDamageValue(), "after removing a saddle (like vanilla shears)");
        helper.succeed();
    }

    private static ItemStack removeSaddle(GameTestHelper helper, BlockPos pos, ItemStack shears) {
        Pig pig = helper.spawn(EntityTypes.PIG, pos);
        pig.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, shears);
        assertConsumed(helper, TestUtils.interact(player, pig), "saddled pig with " + shears.getItem());
        helper.assertTrue(pig.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), "saddle was not removed by " + shears.getItem());
        helper.assertItemEntityPresent(Items.SADDLE, pos, DROP_RADIUS);
        return player.getMainHandItem();
    }

    /**
     * Fixed known issue 1.21-3.1.0.0: wolf armor can be removed by the owner, with the same durability loss as vanilla
     * shears.
     */
    public static void removesWolfArmor(GameTestHelper helper) {
        ItemStack wooden = removeWolfArmor(helper, WOODEN_POS, TestUtils.woodenShears());
        ItemStack vanilla = removeWolfArmor(helper, VANILLA_POS, new ItemStack(Items.SHEARS));
        TestUtils.assertDamage(helper, wooden, vanilla.getDamageValue(), "after removing wolf armor (like vanilla shears)");
        helper.succeed();
    }

    private static ItemStack removeWolfArmor(GameTestHelper helper, BlockPos pos, ItemStack shears) {
        Wolf wolf = helper.spawn(EntityTypes.WOLF, pos);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, shears);
        wolf.tame(player);
        wolf.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.WOLF_ARMOR));
        assertConsumed(helper, TestUtils.interact(player, wolf), "wolf with armor and " + shears.getItem());
        helper.assertTrue(wolf.getItemBySlot(EquipmentSlot.BODY).isEmpty(), "wolf armor was not removed by " + shears.getItem());
        helper.assertItemEntityPresent(Items.WOLF_ARMOR, pos, DROP_RADIUS);
        // advancement "Shear Brilliance" (minecraft:husbandry/remove_wolf_armor)
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(Identifier.withDefaultNamespace("husbandry/remove_wolf_armor"));
        helper.assertTrue(advancement != null, "test setup broken: advancement husbandry/remove_wolf_armor is missing");
        helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),
                "advancement \"Shear Brilliance\" was not granted after removing wolf armor with " + shears.getItem());
        return player.getMainHandItem();
    }

    /** Mob, equipment slot and equipment that shears can remove. */
    private record Equipment(EntityType<? extends Mob> type, EquipmentSlot slot, Item item) {}

    /**
     * Shears remove saddles, horse and nautilus armor, llama carpets and happy ghast harnesses of every mob that can
     * wear them (vanilla 1.21.6+), exactly like vanilla shears: same result, same durability loss. Wolf armor needs the
     * owner, see above.
     */
    public static void removesEquipmentLikeVanillaShears(GameTestHelper helper) {
        Item carpet = TestUtils.firstOfTag(helper, ItemTags.WOOL_CARPETS);
        Item harness = TestUtils.firstOfTag(helper, ItemTags.HARNESSES);
        List<Equipment> cases = List.of(
                new Equipment(EntityTypes.PIG, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.STRIDER, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.HORSE, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.HORSE, EquipmentSlot.BODY, Items.IRON_HORSE_ARMOR),
                new Equipment(EntityTypes.DONKEY, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.MULE, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.SKELETON_HORSE, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.ZOMBIE_HORSE, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.ZOMBIE_HORSE, EquipmentSlot.BODY, Items.IRON_HORSE_ARMOR),
                new Equipment(EntityTypes.CAMEL, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.CAMEL_HUSK, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.LLAMA, EquipmentSlot.BODY, carpet),
                new Equipment(EntityTypes.TRADER_LLAMA, EquipmentSlot.BODY, carpet),
                new Equipment(EntityTypes.HAPPY_GHAST, EquipmentSlot.BODY, harness),
                new Equipment(EntityTypes.NAUTILUS, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.NAUTILUS, EquipmentSlot.BODY, Items.IRON_NAUTILUS_ARMOR),
                new Equipment(EntityTypes.ZOMBIE_NAUTILUS, EquipmentSlot.SADDLE, Items.SADDLE),
                new Equipment(EntityTypes.ZOMBIE_NAUTILUS, EquipmentSlot.BODY, Items.IRON_NAUTILUS_ARMOR));
        List<String> problems = new ArrayList<>();
        for (Equipment equipment : cases) {
            String name = BuiltInRegistries.ENTITY_TYPE.getKey(equipment.type()) + " " + BuiltInRegistries.ITEM.getKey(equipment.item());
            int vanilla = shearEquipment(helper, equipment, new ItemStack(Items.SHEARS));
            int wooden = shearEquipment(helper, equipment, TestUtils.woodenShears());
            if (vanilla < 0) {
                problems.add(name + ": test setup broken, vanilla shears do not remove it");
            } else if (wooden != vanilla) {
                problems.add(name + (wooden < 0 ? ": not removed by wooden shears" : ": durability loss " + wooden + " instead of " + vanilla));
            }
        }
        helper.assertTrue(problems.isEmpty(), "equipment removal differs from vanilla shears: " + problems);
        helper.succeed();
    }

    /** Puts the equipment on a new mob and uses the shears on it. Returns the durability loss, or -1 if it was not removed. */
    private static int shearEquipment(GameTestHelper helper, Equipment equipment, ItemStack shears) {
        Mob mob = helper.spawn(equipment.type(), TestUtils.CENTER);
        mob.setItemSlot(equipment.slot(), new ItemStack(equipment.item()));
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, shears);
        TestUtils.interact(player, mob);
        boolean removed = mob.getItemBySlot(equipment.slot()).isEmpty();
        mob.discard();
        return removed ? player.getMainHandItem().getDamageValue() : -1;
    }

    private static LeashFenceKnotEntity leashToFence(GameTestHelper helper, Pig pig, BlockPos fencePos) {
        helper.setBlock(fencePos, Blocks.OAK_FENCE);
        LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot(helper.getLevel(), helper.absolutePos(fencePos));
        pig.setLeashedTo(knot, true);
        helper.assertTrue(pig.isLeashed(), "leash test setup failed: pig is not leashed");
        return knot;
    }

    /** 1.21.9-3.6.0.1: right-clicking a leashed entity with shears cuts the leash. */
    public static void cutsLeashOfEntity(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityTypes.PIG, TestUtils.CENTER);
        leashToFence(helper, pig, TestUtils.CENTER.east(2));
        ServerPlayer player = survivalPlayer(helper);
        assertConsumed(helper, TestUtils.interact(player, pig), "leashed pig");
        helper.assertTrue(!pig.isLeashed(), "leash was not cut");
        TestUtils.assertDamage(helper, player.getMainHandItem(), 1, "after cutting a leash");
        helper.succeed();
    }

    /** 1.21.9-3.6.0.1: right-clicking a leash knot on a fence with shears removes the knot and its leashes. */
    public static void cutsLeashKnot(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityTypes.PIG, TestUtils.CENTER);
        LeashFenceKnotEntity knot = leashToFence(helper, pig, TestUtils.CENTER.east(2));
        ServerPlayer player = survivalPlayer(helper);
        assertConsumed(helper, TestUtils.interact(player, knot), "leash knot");
        helper.assertTrue(!pig.isLeashed(), "leash was not cut");
        helper.assertTrue(knot.isRemoved(), "leash knot was not removed");
        helper.succeed();
    }

}
