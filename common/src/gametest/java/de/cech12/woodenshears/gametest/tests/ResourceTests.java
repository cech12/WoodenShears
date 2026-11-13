package de.cech12.woodenshears.gametest.tests;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import de.cech12.woodenshears.platform.Services;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Client resources (translations, item model, texture). A dedicated server does not load them, so the tests read them
 * directly from the mod resources. How they look in game must be checked manually (see TESTING.md).
 */
public final class ResourceTests {

    /** Translation key of the item name. */
    private static final String ITEM_KEY = "item.woodenshears.wooden_shears";
    /** All shipped translations. Add new languages here. */
    private static final List<String> LANGUAGES = List.of("de_de", "en_us", "es_es", "hu_hu", "ja_jp", "pt_br", "pt_pt", "ru_ru", "uk_ua", "zh_cn");

    private ResourceTests() {}

    public static void register(WoodenShearsGameTests.TestCollector tests) {
        tests.add("resources/item_name_is_translated", ResourceTests::itemNameIsTranslated);
        tests.add("resources/item_model_and_texture_exist", ResourceTests::itemModelAndTextureExist);
    }

    private static InputStream open(String path) {
        return Services.class.getClassLoader().getResourceAsStream(path);
    }

    private static JsonObject readJson(GameTestHelper helper, String path) {
        try (InputStream stream = open(path)) {
            helper.assertTrue(stream != null, "resource " + path + " is missing");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            helper.fail("resource " + path + " cannot be read: " + e.getMessage());
            return null;
        }
    }

    /** Every shipped language translates the item name, English is "Wooden Shears". */
    public static void itemNameIsTranslated(GameTestHelper helper) {
        for (String language : LANGUAGES) {
            JsonObject lang = readJson(helper, "assets/woodenshears/lang/" + language + ".json");
            helper.assertTrue(lang.has(ITEM_KEY) && !lang.get(ITEM_KEY).getAsString().isBlank(), language + " does not translate " + ITEM_KEY);
        }
        helper.assertValueEqual(readJson(helper, "assets/woodenshears/lang/en_us.json").get(ITEM_KEY).getAsString(), "Wooden Shears", "English item name");
        helper.succeed();
    }

    /** The item model definition points to an existing model, which points to an existing texture. */
    public static void itemModelAndTextureExist(GameTestHelper helper) {
        JsonObject definition = readJson(helper, "assets/woodenshears/items/wooden_shears.json");
        String model = definition.getAsJsonObject("model").get("model").getAsString();
        helper.assertValueEqual(model, "woodenshears:item/wooden_shears", "model of item definition");
        JsonObject modelJson = readJson(helper, "assets/woodenshears/models/item/wooden_shears.json");
        String texture = modelJson.getAsJsonObject("textures").get("layer0").getAsString();
        helper.assertValueEqual(texture, "woodenshears:item/wooden_shears", "texture of item model");
        try (InputStream stream = open("assets/woodenshears/textures/item/wooden_shears.png")) {
            helper.assertTrue(stream != null && stream.read() != -1, "texture is missing or empty");
        } catch (IOException e) {
            helper.fail("texture cannot be read: " + e.getMessage());
        }
        helper.succeed();
    }

}
