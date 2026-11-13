package de.cech12.woodenshears.gametest.tests;

import de.cech12.woodenshears.gametest.TestUtils;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Basic properties of the wooden shears item.
 */
public final class ItemTests {

    private static final TagKey<Item> COMMON_SHEARS_TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/shear"));

    private ItemTests() {}

    public static void register(WoodenShearsGameTests.TestCollector tests) {
        tests.add("item/is_registered_as_shears", ItemTests::isRegisteredAsShears);
        tests.add("item/does_not_stack", ItemTests::doesNotStack);
        tests.add("item/is_in_tools_and_utilities_tab", ItemTests::isInToolsAndUtilitiesTab);
        tests.add("item/is_in_common_shears_tag", ItemTests::isInCommonShearsTag);
        tests.add("item/is_enchantable_like_vanilla_shears", ItemTests::isEnchantableLikeVanillaShears);
        tests.add("item/mines_like_vanilla_shears", ItemTests::minesLikeVanillaShears);
        tests.add("item/is_fuel_like_wooden_tools", ItemTests::isFuelLikeWoodenTools);
        tests.add("item/burns_in_furnace", ItemTests::burnsInFurnace);
    }

    /** "woodenshears:wooden_shears" exists and is a shears item (vanilla and loader shears logic applies). */
    public static void isRegisteredAsShears(GameTestHelper helper) {
        Item item = TestUtils.woodenShearsItem();
        helper.assertTrue(item != Items.AIR, "woodenshears:wooden_shears is not registered");
        helper.assertTrue(item instanceof ShearsItem, "wooden shears are no ShearsItem");
        helper.succeed();
    }

    /** Regression 1.18.2-1.2.1.1: wooden shears could be stacked. */
    public static void doesNotStack(GameTestHelper helper) {
        helper.assertValueEqual(TestUtils.woodenShears().getMaxStackSize(), 1, "max stack size");
        helper.succeed();
    }

    /** Wooden shears are listed in the creative tab "Tools and Utilities". */
    public static void isInToolsAndUtilitiesTab(GameTestHelper helper) {
        var level = helper.getLevel();
        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.TOOLS_AND_UTILITIES);
        boolean contained = tab.getDisplayItems().stream().anyMatch(stack -> stack.is(TestUtils.woodenShearsItem()));
        helper.assertTrue(contained, "wooden shears are missing in the tools and utilities creative tab");
        helper.succeed();
    }

    /** #17: wooden shears are part of the common "c:tools/shear" tag, so other mods treat them as shears. */
    public static void isInCommonShearsTag(GameTestHelper helper) {
        helper.assertTrue(TestUtils.woodenShears().is(COMMON_SHEARS_TAG), "wooden shears are not in #c:tools/shear");
        helper.succeed();
    }

    /**
     * Regression 1.20.6-2.3.0.1: wooden shears could not be enchanted.
     * Wooden shears accept exactly the enchantments vanilla shears accept, at least Efficiency, Unbreaking and Mending.
     */
    public static void isEnchantableLikeVanillaShears(GameTestHelper helper) {
        ItemStack wooden = TestUtils.woodenShears();
        ItemStack vanilla = new ItemStack(Items.SHEARS);
        helper.assertTrue(wooden.is(ItemTags.DURABILITY_ENCHANTABLE), "wooden shears are not in #minecraft:enchantable/durability");
        helper.assertTrue(wooden.is(ItemTags.MINING_ENCHANTABLE), "wooden shears are not in #minecraft:enchantable/mining");
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var key : List.of(Enchantments.EFFICIENCY, Enchantments.UNBREAKING, Enchantments.MENDING)) {
            helper.assertTrue(enchantments.getOrThrow(key).value().canEnchant(wooden), "wooden shears cannot be enchanted with " + key.identifier());
        }
        List<String> differences = new ArrayList<>();
        enchantments.listElements().forEach(holder -> {
            Enchantment enchantment = holder.value();
            if (enchantment.canEnchant(wooden) != enchantment.canEnchant(vanilla)) {
                differences.add(holder.key().identifier().toString());
            }
        });
        helper.assertTrue(differences.isEmpty(), "enchantability differs from vanilla shears for " + differences);
        helper.succeed();
    }

    /** Wooden shears have the same mining speed and "correct tool" behavior as vanilla shears for every block. */
    public static void minesLikeVanillaShears(GameTestHelper helper) {
        ItemStack wooden = TestUtils.woodenShears();
        ItemStack vanilla = new ItemStack(Items.SHEARS);
        helper.assertValueEqual(wooden.getDestroySpeed(Blocks.COBWEB.defaultBlockState()), 15.0F, "destroy speed on cobweb");
        helper.assertTrue(wooden.isCorrectToolForDrops(Blocks.COBWEB.defaultBlockState()), "wooden shears are not the correct tool for cobweb");
        List<String> differences = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            if (wooden.getDestroySpeed(state) != vanilla.getDestroySpeed(state)
                    || wooden.isCorrectToolForDrops(state) != vanilla.isCorrectToolForDrops(state)) {
                differences.add(BuiltInRegistries.BLOCK.getKey(block).toString());
            }
        }
        helper.assertTrue(differences.isEmpty(), "mining behavior differs from vanilla shears for " + differences);
        helper.succeed();
    }

    /** #39: wooden shears are a furnace fuel with the same burn time as wooden tools. */
    public static void isFuelLikeWoodenTools(GameTestHelper helper) {
        var fuel = TestUtils.woodenShears().get(DataComponents.COOKING_FUEL);
        helper.assertTrue(fuel != null, "wooden shears are no fuel");
        helper.assertTrue(Objects.equals(fuel, new ItemStack(Items.WOODEN_SHOVEL).get(DataComponents.COOKING_FUEL)),
                "fuel of wooden shears differs from wooden shovel: " + fuel);
        helper.succeed();
    }

    /** #39: a furnace with wooden shears as fuel starts burning and consumes them. */
    public static void burnsInFurnace(GameTestHelper helper) {
        BlockPos pos = TestUtils.CENTER;
        helper.setBlock(pos, Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, TestUtils.woodenShears());
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(pos, AbstractFurnaceBlock.LIT, true);
            helper.assertTrue(furnace.getItem(1).isEmpty(), "wooden shears were not consumed as fuel");
        });
    }

}
