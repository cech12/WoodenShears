package de.cech12.woodenshears.gametest.tests;

import de.cech12.woodenshears.gametest.GameTestPlatform;
import de.cech12.woodenshears.gametest.TestUtils;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
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
        tests.add("item/has_the_components_of_vanilla_shears", ItemTests::hasTheComponentsOfVanillaShears);
        tests.add("item/loses_durability_per_block_like_vanilla_shears", ItemTests::losesDurabilityPerBlockLikeVanillaShears);
        tests.add("item/mending_repairs_like_vanilla_shears", ItemTests::mendingRepairsLikeVanillaShears);
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
        // own key: CreativeModeTabs.TOOLS_AND_UTILITIES is private in vanilla (only the loaders widen it)
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(
                ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities")));
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
        // like vanilla shears only enchantable with an anvil, not at the enchanting table
        helper.assertTrue(Objects.equals(wooden.get(DataComponents.ENCHANTABLE), vanilla.get(DataComponents.ENCHANTABLE)),
                "enchanting table enchantability differs from vanilla shears: " + wooden.get(DataComponents.ENCHANTABLE));
        helper.succeed();
    }

    /**
     * Components that are allowed to differ from vanilla shears: name, model and the durability (config). The fuel is no
     * component in this version (see isFuelLikeWoodenTools).
     */
    private static final java.util.Set<net.minecraft.core.component.DataComponentType<?>> OWN_COMPONENTS = java.util.Set.of(
            DataComponents.ITEM_NAME, DataComponents.ITEM_MODEL, DataComponents.MAX_DAMAGE, DataComponents.DAMAGE);

    /**
     * Wooden shears have all default components of vanilla shears with the same values (tool rules incl. breaking
     * blocks in creative, stack size, enchantability, repair cost, rarity, ...), except name, model and durability. A
     * new property of vanilla shears in a future Minecraft version is noticed here.
     */
    public static void hasTheComponentsOfVanillaShears(GameTestHelper helper) {
        var mod = TestUtils.woodenShearsItem().components();
        var vanilla = Items.SHEARS.components();
        java.util.Set<net.minecraft.core.component.DataComponentType<?>> types = new java.util.HashSet<>(mod.keySet());
        types.addAll(vanilla.keySet());
        types.removeAll(OWN_COMPONENTS);
        List<String> differences = new ArrayList<>();
        for (var type : types) {
            if (!Objects.equals(mod.get(type), vanilla.get(type))) {
                differences.add(type + ": " + mod.get(type) + " instead of " + vanilla.get(type));
            }
        }
        helper.assertTrue(differences.isEmpty(), "components differ from vanilla shears: " + differences);
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

    /**
     * Breaking a block costs the same durability as with vanilla shears for every block: 1 for every block except fire,
     * also for blocks without hardness like grass (#40 asked for no loss on such blocks and was closed in favor of the
     * vanilla behavior).
     */
    public static void losesDurabilityPerBlockLikeVanillaShears(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        BlockPos pos = helper.absolutePos(TestUtils.CENTER);
        List<String> differences = new ArrayList<>();
        int damaging = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            ItemStack wooden = TestUtils.woodenShears();
            ItemStack vanilla = new ItemStack(Items.SHEARS);
            wooden.mineBlock(helper.getLevel(), state, pos, player);
            vanilla.mineBlock(helper.getLevel(), state, pos, player);
            if (wooden.getDamageValue() != vanilla.getDamageValue()) {
                differences.add(BuiltInRegistries.BLOCK.getKey(block) + " (" + wooden.getDamageValue() + " instead of " + vanilla.getDamageValue() + ")");
            }
            if (vanilla.getDamageValue() > 0) {
                damaging++;
            }
        }
        helper.assertTrue(damaging > 0, "test setup broken: vanilla shears lose no durability on any block");
        helper.assertTrue(differences.isEmpty(), "durability loss when breaking blocks differs from vanilla shears: " + differences);
        helper.succeed();
    }

    /**
     * Mending repairs damaged wooden shears with experience exactly like vanilla shears (the max damage comes from the
     * config, not from the item). Shears repaired to full durability store the same components as vanilla shears.
     */
    public static void mendingRepairsLikeVanillaShears(GameTestHelper helper) {
        var mending = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
        List<ItemStack> repaired = new ArrayList<>();
        for (int damage : new int[] {30, 2}) { // 10 xp repair 20 durability: 30 stays partially damaged
            for (ItemStack prototype : List.of(TestUtils.woodenShears(), new ItemStack(Items.SHEARS))) {
                ItemStack stack = prototype.copy();
                stack.setDamageValue(damage);
                stack.enchant(mending, 1);
                ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, stack);
                new ExperienceOrb(helper.getLevel(), player.getX(), player.getY(), player.getZ(), 10).playerTouch(player);
                repaired.add(player.getMainHandItem());
            }
        }
        helper.assertTrue(repaired.get(0).getDamageValue() > 0 && repaired.get(0).getDamageValue() < 30,
                "mending did not partially repair wooden shears: damage " + repaired.get(0).getDamageValue());
        helper.assertValueEqual(repaired.get(0).getDamageValue(), repaired.get(1).getDamageValue(), "damage of wooden shears after mending compared to vanilla shears");
        TestUtils.assertDamage(helper, repaired.get(2), 0, "after mending to full durability");
        helper.assertValueEqual(TestUtils.storedComponentTypes(repaired.get(2)), TestUtils.storedComponentTypes(repaired.get(3)),
                "stored components of wooden shears mended to full durability compared to vanilla shears");
        helper.succeed();
    }

    /** #39: wooden shears are a furnace fuel with the same burn time as wooden tools. */
    public static void isFuelLikeWoodenTools(GameTestHelper helper) {
        int burnTime = GameTestPlatform.INSTANCE.burnTime(helper.getLevel(), TestUtils.woodenShears());
        helper.assertTrue(burnTime > 0, "wooden shears are no fuel");
        helper.assertValueEqual(burnTime, GameTestPlatform.INSTANCE.burnTime(helper.getLevel(), new ItemStack(Items.WOODEN_SHOVEL)),
                "burn time of wooden shears compared to a wooden shovel");
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
