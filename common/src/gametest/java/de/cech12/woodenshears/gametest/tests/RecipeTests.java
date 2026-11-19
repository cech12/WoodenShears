package de.cech12.woodenshears.gametest.tests;

import de.cech12.woodenshears.Constants;
import de.cech12.woodenshears.gametest.TestUtils;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import de.cech12.woodenshears.platform.Services;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Optional;

/**
 * Crafting recipe, recipe unlock advancement and repairing by combining two wooden shears.
 */
public final class RecipeTests {

    private static final ResourceKey<Recipe<?>> RECIPE = ResourceKey.create(Registries.RECIPE, Constants.id("wooden_shears"));

    private RecipeTests() {}

    public static void register(WoodenShearsGameTests.TestCollector tests) {
        tests.add("recipe/crafted_from_two_diagonal_planks", RecipeTests::craftedFromTwoDiagonalPlanks);
        tests.add("recipe/not_crafted_from_two_planks_in_a_row", RecipeTests::notCraftedFromTwoPlanksInARow);
        tests.add("recipe/not_crafted_from_other_items", RecipeTests::notCraftedFromOtherItems);
        tests.add("recipe/is_in_equipment_category", RecipeTests::isInEquipmentCategory);
        tests.add("recipe/unlocked_by_having_planks", RecipeTests::unlockedByHavingPlanks);
        tests.add("repair/combines_two_damaged_shears", RecipeTests::combinesTwoDamagedShears);
        tests.add("repair/grindstone_combines_two_damaged_shears", RecipeTests::grindstoneCombinesTwoDamagedShears);
        tests.add("repair/grindstone_result_follows_config", RecipeTests::grindstoneResultFollowsConfig);
        tests.add("repair/anvil_combines_two_damaged_shears", RecipeTests::anvilCombinesTwoDamagedShears);
        tests.add("repair/fully_repaired_shears_equal_new_shears", RecipeTests::fullyRepairedShearsEqualNewShears);
        tests.add("repair/anvil_fully_repaired_shears_like_vanilla_shears", RecipeTests::anvilFullyRepairedShearsLikeVanillaShears);
        tests.add("repair/grindstone_disenchants_single_shears", RecipeTests::grindstoneDisenchantsSingleShears);
        tests.add("repair/no_repair_material_like_vanilla_shears", RecipeTests::noRepairMaterialLikeVanillaShears);
        tests.add("repair/not_possible_when_unbreakable", RecipeTests::notPossibleWhenUnbreakable);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> findRecipe(GameTestHelper helper, CraftingInput input) {
        return helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
    }

    private static ItemStack craft(GameTestHelper helper, CraftingInput input) {
        return findRecipe(helper, input).map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    /** Two planks of any kind placed diagonally (in both directions) craft one pair of wooden shears. */
    public static void craftedFromTwoDiagonalPlanks(GameTestHelper helper) {
        var planks = BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.PLANKS);
        int count = 0;
        for (var holder : planks) {
            ItemStack plank = new ItemStack(holder);
            for (List<ItemStack> grid : List.of(
                    List.of(ItemStack.EMPTY, plank, plank, ItemStack.EMPTY),
                    List.of(plank, ItemStack.EMPTY, ItemStack.EMPTY, plank))) {
                ItemStack result = craft(helper, CraftingInput.of(2, 2, grid));
                helper.assertTrue(result.is(TestUtils.woodenShearsItem()) && result.getCount() == 1,
                        "two diagonal " + holder.getRegisteredName() + " do not craft wooden shears but " + result);
            }
            count++;
        }
        helper.assertTrue(count > 0, "no planks found in #minecraft:planks");
        //different planks can be mixed
        ItemStack result = craft(helper, CraftingInput.of(2, 2, List.of(ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BIRCH_PLANKS), ItemStack.EMPTY)));
        helper.assertTrue(result.is(TestUtils.woodenShearsItem()), "mixed planks do not craft wooden shears");
        helper.succeed();
    }

    /**
     * Negative case: two planks next to or above each other do not match the wooden shears recipe (they are
     * pressure plates and sticks). Checks the recipe itself, because another recipe could win the recipe lookup.
     */
    public static void notCraftedFromTwoPlanksInARow(GameTestHelper helper) {
        var recipe = helper.getLevel().recipeAccess().byKey(RECIPE);
        helper.assertTrue(recipe.isPresent() && recipe.get().value() instanceof CraftingRecipe, "recipe " + RECIPE.identifier() + " is missing");
        @SuppressWarnings("unchecked")
        Recipe<CraftingInput> craftingRecipe = (Recipe<CraftingInput>) recipe.get().value();
        ItemStack plank = new ItemStack(Items.OAK_PLANKS);
        for (CraftingInput input : List.of(CraftingInput.of(2, 1, List.of(plank, plank)), CraftingInput.of(1, 2, List.of(plank, plank)))) {
            helper.assertTrue(!craftingRecipe.matches(input, helper.getLevel()),
                    "two planks in a " + input.width() + "x" + input.height() + " grid match the wooden shears recipe");
        }
        helper.succeed();
    }

    /**
     * Negative cases: one plank, three planks on a diagonal, and two diagonal sticks, logs or a plank with a stick do not
     * match the wooden shears recipe. Checks the recipe itself, because another recipe could win the recipe lookup.
     */
    public static void notCraftedFromOtherItems(GameTestHelper helper) {
        var recipe = helper.getLevel().recipeAccess().byKey(RECIPE);
        helper.assertTrue(recipe.isPresent() && recipe.get().value() instanceof CraftingRecipe, "recipe " + RECIPE.identifier() + " is missing");
        @SuppressWarnings("unchecked")
        Recipe<CraftingInput> craftingRecipe = (Recipe<CraftingInput>) recipe.get().value();
        ItemStack plank = new ItemStack(Items.OAK_PLANKS);
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack log = new ItemStack(Items.OAK_LOG);
        ItemStack empty = ItemStack.EMPTY;
        for (CraftingInput input : List.of(
                CraftingInput.of(1, 1, List.of(plank)),
                CraftingInput.of(3, 3, List.of(empty, empty, plank, empty, plank, empty, plank, empty, empty)),
                CraftingInput.of(2, 2, List.of(empty, stick, stick, empty)),
                CraftingInput.of(2, 2, List.of(empty, log, log, empty)),
                CraftingInput.of(2, 2, List.of(empty, plank, stick, empty)))) {
            helper.assertTrue(!craftingRecipe.matches(input, helper.getLevel()), "the wooden shears recipe matches " + input.items());
        }
        helper.succeed();
    }

    /** #27: the recipe is shown in the equipment category of the recipe book. */
    public static void isInEquipmentCategory(GameTestHelper helper) {
        var recipe = helper.getLevel().recipeAccess().byKey(RECIPE);
        helper.assertTrue(recipe.isPresent(), "recipe " + RECIPE.identifier() + " is missing");
        helper.assertTrue(recipe.get().value() instanceof CraftingRecipe, "recipe is no crafting recipe");
        helper.assertValueEqual(((CraftingRecipe) recipe.get().value()).category(), CraftingBookCategory.EQUIPMENT, "recipe category");
        helper.succeed();
    }

    /** The recipe is unlocked by an advancement as soon as a player has planks. */
    public static void unlockedByHavingPlanks(GameTestHelper helper) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(Constants.id("recipes/tools/wooden_shears"));
        helper.assertTrue(advancement != null, "recipe advancement is missing");
        helper.assertTrue(advancement.value().rewards().recipes().contains(RECIPE), "recipe advancement does not unlock the recipe");
        var criterion = advancement.value().criteria().get("has_planks");
        helper.assertTrue(criterion != null, "recipe advancement has no has_planks criterion");
        helper.assertTrue(criterion.trigger() == net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED, "has_planks is no inventory_changed trigger");
        helper.assertTrue(criterion.triggerInstance() instanceof net.minecraft.advancements.triggers.InventoryChangeTrigger.TriggerInstance inventory
                        && inventory.items().size() == 1 && inventory.items().getFirst().test(new ItemStack(Items.OAK_PLANKS))
                        && inventory.items().getFirst().test(new ItemStack(Items.BIRCH_PLANKS))
                        && !inventory.items().getFirst().test(new ItemStack(Items.STICK)),
                "has_planks does not check for planks");
        var hasTheRecipe = advancement.value().criteria().get("has_the_recipe");
        helper.assertTrue(hasTheRecipe != null, "recipe advancement has no has_the_recipe criterion");
        var ownRecipe = helper.getLevel().recipeAccess().byKey(RECIPE);
        helper.assertTrue(ownRecipe.isPresent(), "recipe " + RECIPE.identifier() + " is missing");
        helper.assertTrue(hasTheRecipe.triggerInstance() instanceof net.minecraft.advancements.triggers.RecipeUnlockedTrigger.TriggerInstance unlocked
                        && unlocked.matches(ownRecipe.get()),
                "has_the_recipe does not check for its own recipe: " + hasTheRecipe.triggerInstance());
        // either criterion alone completes the advancement (OR), so having planks is enough
        var requirements = advancement.value().requirements();
        helper.assertTrue(requirements.test("has_planks"::equals), "recipe advancement is not completed by has_planks alone: " + requirements);
        helper.assertTrue(requirements.test("has_the_recipe"::equals), "recipe advancement is not completed by has_the_recipe alone: " + requirements);
        helper.assertTrue(!requirements.test(criterion2 -> false), "recipe advancement is completed without any criterion: " + requirements);
        helper.succeed();
    }

    /**
     * #38: two damaged wooden shears can be combined in the crafting grid. The result has the remaining durability of
     * both plus a 5% bonus. The max damage is not stored in the result, so it still follows the config.
     */
    public static void combinesTwoDamagedShears(GameTestHelper helper) {
        int max = Services.CONFIG.getDurability(); // set by the test environment "default"
        ItemStack first = TestUtils.woodenShears();
        first.setDamageValue(40);
        ItemStack second = TestUtils.woodenShears();
        second.setDamageValue(30);
        ItemStack result = craft(helper, CraftingInput.of(2, 1, List.of(first, second)));
        helper.assertTrue(result.is(TestUtils.woodenShearsItem()), "two damaged wooden shears cannot be combined: " + result);
        int remaining = (max - 40) + (max - 30) + max * 5 / 100;
        TestUtils.assertDamage(helper, result, max - remaining, "after repair");
        helper.assertTrue(result.getComponentsPatch().forget(type -> type != DataComponents.MAX_DAMAGE).isEmpty(), "repaired wooden shears store a fixed max damage");
        helper.succeed();
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** Puts two items into the menu inputs (slots 0 and 1) and returns the output (slot 2). */
    private static ItemStack combine(AbstractContainerMenu menu, ItemStack first, ItemStack second) {
        menu.getSlot(0).set(first);
        menu.getSlot(1).set(second);
        if (menu instanceof AnvilMenu anvil) {
            anvil.createResult();
        }
        return menu.getSlot(2).getItem();
    }

    /**
     * Like vanilla shears, two damaged wooden shears can be combined in a grindstone: the remaining durability of both
     * plus a 5% bonus, enchantments (except curses) are removed.
     */
    public static void grindstoneCombinesTwoDamagedShears(GameTestHelper helper) {
        int max = Services.CONFIG.getDurability(); // set by the test environment "default"
        ItemStack result = combineInGrindstone(helper);
        helper.assertTrue(result.is(TestUtils.woodenShearsItem()), "two damaged wooden shears cannot be combined in a grindstone: " + result);
        int remaining = (max - 40) + (max - 30) + max * 5 / 100;
        TestUtils.assertDamage(helper, result, Math.max(0, max - remaining), "after combining in a grindstone");
        helper.assertTrue(result.getEnchantments().isEmpty(), "grindstone did not remove the enchantments: " + result.getEnchantments());
        helper.succeed();
    }

    /**
     * Shears combined in a grindstone do not store a fixed max damage, so config changes still apply to them (like the
     * crafting repair, #38). Fixed #51.
     */
    public static void grindstoneResultFollowsConfig(GameTestHelper helper) {
        ItemStack result = combineInGrindstone(helper);
        helper.assertTrue(result.is(TestUtils.woodenShearsItem()), "two damaged wooden shears cannot be combined in a grindstone: " + result);
        helper.assertTrue(result.getComponentsPatch().forget(type -> type != DataComponents.MAX_DAMAGE).isEmpty(),
                "wooden shears combined in a grindstone store a fixed max damage: " + result.getComponentsPatch());
        helper.assertValueEqual(result.getMaxDamage(), Services.CONFIG.getDurability(), "max damage of the combined shears");
        helper.succeed();
    }

    /** Combines two damaged wooden shears (40 and 30 damage, the first with Efficiency I) in a grindstone. */
    private static ItemStack combineInGrindstone(GameTestHelper helper) {
        ItemStack first = TestUtils.woodenShears();
        first.setDamageValue(40);
        first.enchant(enchantment(helper, Enchantments.EFFICIENCY), 1);
        ItemStack second = TestUtils.woodenShears();
        second.setDamageValue(30);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        GrindstoneMenu menu = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TestUtils.CENTER)));
        return combine(menu, first, second);
    }

    /**
     * Like vanilla shears, two damaged wooden shears can be combined in an anvil: the remaining durability of both plus
     * a 12% bonus, the enchantments of both are kept. The max damage still follows the config.
     */
    public static void anvilCombinesTwoDamagedShears(GameTestHelper helper) {
        int max = Services.CONFIG.getDurability(); // set by the test environment "default"
        ItemStack first = TestUtils.woodenShears();
        first.setDamageValue(40);
        first.enchant(enchantment(helper, Enchantments.EFFICIENCY), 1);
        ItemStack second = TestUtils.woodenShears();
        second.setDamageValue(30);
        second.enchant(enchantment(helper, Enchantments.UNBREAKING), 1);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TestUtils.CENTER)));
        ItemStack result = combine(menu, first, second);
        helper.assertTrue(result.is(TestUtils.woodenShearsItem()), "two damaged wooden shears cannot be combined in an anvil: " + result);
        int remaining = (max - 40) + (max - 30) + max * 12 / 100;
        TestUtils.assertDamage(helper, result, Math.max(0, max - remaining), "after combining in an anvil");
        helper.assertTrue(result.getEnchantments().getLevel(enchantment(helper, Enchantments.EFFICIENCY)) == 1
                && result.getEnchantments().getLevel(enchantment(helper, Enchantments.UNBREAKING)) == 1,
                "anvil did not keep the enchantments of both shears: " + result.getEnchantments());
        helper.assertTrue(menu.getCost() > 0, "combining in an anvil costs no experience");
        helper.assertTrue(result.getComponentsPatch().forget(type -> type != DataComponents.MAX_DAMAGE).isEmpty(), "wooden shears combined in an anvil store a fixed max damage");
        helper.succeed();
    }

    /** #38: shears repaired to full durability are identical to new shears (no leftover damage component). */
    public static void fullyRepairedShearsEqualNewShears(GameTestHelper helper) {
        ItemStack first = TestUtils.woodenShears();
        first.setDamageValue(1);
        ItemStack second = TestUtils.woodenShears();
        second.setDamageValue(1);
        ItemStack result = craft(helper, CraftingInput.of(2, 1, List.of(first, second)));
        helper.assertTrue(result.is(TestUtils.woodenShearsItem()), "two damaged wooden shears cannot be combined: " + result);
        helper.assertTrue(ItemStack.isSameItemSameComponents(result, TestUtils.woodenShears()),
                "fully repaired wooden shears differ from new ones: " + result.getComponentsPatch());
        helper.succeed();
    }

    private static AnvilMenu anvil(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        return new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TestUtils.CENTER)));
    }

    /**
     * Shears combined to full durability in an anvil have the same stored components as vanilla shears combined the
     * same way (the repair cost, but no leftover damage or max damage).
     */
    public static void anvilFullyRepairedShearsLikeVanillaShears(GameTestHelper helper) {
        List<ItemStack> results = new java.util.ArrayList<>();
        for (ItemStack prototype : List.of(TestUtils.woodenShears(), new ItemStack(Items.SHEARS))) {
            ItemStack first = prototype.copy();
            first.setDamageValue(1);
            ItemStack second = prototype.copy();
            second.setDamageValue(1);
            ItemStack result = combine(anvil(helper), first, second);
            helper.assertTrue(result.is(prototype.getItem()) && result.getDamageValue() == 0, "two slightly damaged " + prototype + " are not fully repaired in an anvil: " + result);
            results.add(result);
        }
        var mod = TestUtils.storedComponentTypes(results.get(0));
        var vanilla = TestUtils.storedComponentTypes(results.get(1));
        helper.assertValueEqual(mod, vanilla, "stored components of fully repaired wooden shears compared to vanilla shears");
        helper.succeed();
    }

    /**
     * Like vanilla shears, a single enchanted and damaged pair of shears is disenchanted in a grindstone: the damage is
     * kept, no fixed max damage is stored.
     */
    public static void grindstoneDisenchantsSingleShears(GameTestHelper helper) {
        ItemStack shears = TestUtils.woodenShears();
        shears.setDamageValue(10);
        shears.enchant(enchantment(helper, Enchantments.EFFICIENCY), 1);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        GrindstoneMenu menu = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TestUtils.CENTER)));
        ItemStack result = combine(menu, shears, ItemStack.EMPTY);
        helper.assertTrue(result.is(TestUtils.woodenShearsItem()), "a single wooden shears cannot be disenchanted in a grindstone: " + result);
        helper.assertTrue(result.getEnchantments().isEmpty(), "grindstone did not remove the enchantments: " + result.getEnchantments());
        TestUtils.assertDamage(helper, result, 10, "after disenchanting in a grindstone");
        helper.assertTrue(result.getComponentsPatch().forget(type -> type != DataComponents.MAX_DAMAGE).isEmpty(),
                "disenchanted wooden shears store a fixed max damage: " + result.getComponentsPatch());
        helper.succeed();
    }

    /**
     * Like vanilla shears, wooden shears have no repair material: an anvil does not repair them with any item (only by
     * combining two shears).
     */
    public static void noRepairMaterialLikeVanillaShears(GameTestHelper helper) {
        helper.assertTrue(java.util.Objects.equals(TestUtils.woodenShears().get(DataComponents.REPAIRABLE), new ItemStack(Items.SHEARS).get(DataComponents.REPAIRABLE)),
                "repair material differs from vanilla shears: " + TestUtils.woodenShears().get(DataComponents.REPAIRABLE));
        for (var material : List.of(Items.OAK_PLANKS, Items.STICK, Items.IRON_INGOT)) {
            ItemStack shears = TestUtils.woodenShears();
            shears.setDamageValue(20);
            ItemStack result = combine(anvil(helper), shears, new ItemStack(material, 4));
            helper.assertTrue(result.isEmpty(), "wooden shears are repaired with " + material + " in an anvil: " + result);
        }
        helper.succeed();
    }

    /** Environment "durability_0": unbreakable wooden shears cannot be damaged, so the repair recipe does not apply. */
    public static void notPossibleWhenUnbreakable(GameTestHelper helper) {
        ItemStack result = craft(helper, CraftingInput.of(2, 1, List.of(TestUtils.woodenShears(), TestUtils.woodenShears())));
        helper.assertTrue(result.isEmpty(), "unbreakable wooden shears can be combined: " + result);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        GrindstoneMenu grindstone = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TestUtils.CENTER)));
        ItemStack grindstoneResult = combine(grindstone, TestUtils.woodenShears(), TestUtils.woodenShears());
        helper.assertTrue(grindstoneResult.isEmpty(), "unbreakable wooden shears can be combined in a grindstone: " + grindstoneResult);
        // enchanted, so that the anvil would combine them if they were damageable (enchantments of both are merged)
        ItemStack first = TestUtils.woodenShears();
        first.enchant(enchantment(helper, Enchantments.EFFICIENCY), 1);
        ItemStack second = TestUtils.woodenShears();
        second.enchant(enchantment(helper, Enchantments.UNBREAKING), 1);
        ItemStack anvilResult = combine(anvil(helper), first, second);
        helper.assertTrue(anvilResult.isEmpty(), "unbreakable wooden shears can be combined in an anvil: " + anvilResult);
        helper.succeed();
    }

}
