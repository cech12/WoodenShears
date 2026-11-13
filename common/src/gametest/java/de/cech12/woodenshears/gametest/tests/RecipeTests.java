package de.cech12.woodenshears.gametest.tests;

import de.cech12.woodenshears.Constants;
import de.cech12.woodenshears.gametest.TestUtils;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import de.cech12.woodenshears.platform.services.IConfigHelper;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

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
        tests.add("recipe/is_in_equipment_category", RecipeTests::isInEquipmentCategory);
        tests.add("recipe/unlocked_by_having_planks", RecipeTests::unlockedByHavingPlanks);
        tests.add("repair/combines_two_damaged_shears", RecipeTests::combinesTwoDamagedShears);
        tests.add("repair/fully_repaired_shears_equal_new_shears", RecipeTests::fullyRepairedShearsEqualNewShears);
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
        helper.succeed();
    }

    /**
     * #38: two damaged wooden shears can be combined in the crafting grid. The result has the remaining durability of
     * both plus a 5% bonus. The max damage is not stored in the result, so it still follows the config.
     */
    public static void combinesTwoDamagedShears(GameTestHelper helper) {
        int max = IConfigHelper.DURABILITY_DEFAULT;
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

    /** Environment "durability_0": unbreakable wooden shears cannot be damaged, so the repair recipe does not apply. */
    public static void notPossibleWhenUnbreakable(GameTestHelper helper) {
        ItemStack result = craft(helper, CraftingInput.of(2, 1, List.of(TestUtils.woodenShears(), TestUtils.woodenShears())));
        helper.assertTrue(result.isEmpty(), "unbreakable wooden shears can be combined: " + result);
        helper.succeed();
    }

}
