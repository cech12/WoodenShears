package de.cech12.woodenshears.gametest.tests;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.cech12.woodenshears.Constants;
import de.cech12.woodenshears.gametest.WoodenShearsGameTests;
import de.cech12.woodenshears.platform.Services;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Client resources (translations, item model, texture). A dedicated server does not load them, so the tests read them
 * directly from the mod resources. How they look in game must be checked manually (see TESTING.md).
 */
public final class ResourceTests {

    /** Translation key of the item name. */
    private static final String ITEM_KEY = "item.woodenshears.wooden_shears";
    private static final String LANG_DIR = "assets/woodenshears/lang/";
    /** Languages that must always be found, so that a broken directory listing does not check nothing. */
    private static final List<String> REQUIRED_LANGUAGES = List.of("en_us", "de_de");

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

    /** All shipped languages: the json files next to en_us.json (in a directory or a jar). */
    private static List<String> shippedLanguages(GameTestHelper helper) {
        URL english = Services.class.getClassLoader().getResource(LANG_DIR + "en_us.json");
        helper.assertTrue(english != null, "resource " + LANG_DIR + "en_us.json is missing");
        try {
            URI uri = english.toURI();
            try {
                return listLanguages(Path.of(uri).getParent());
            } catch (FileSystemNotFoundException e) {
                // resources in a jar that is not opened as file system yet
                try (FileSystem jar = FileSystems.newFileSystem(uri, Map.of())) {
                    return listLanguages(jar.provider().getPath(uri).getParent());
                }
            }
        } catch (URISyntaxException | IOException | RuntimeException e) {
            helper.fail("cannot list the language files in " + english + ": " + e);
            return List.of();
        }
    }

    private static List<String> listLanguages(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted().toList();
        }
    }

    /** Every shipped language translates the item name, English is "Wooden Shears". */
    public static void itemNameIsTranslated(GameTestHelper helper) {
        List<String> languages = shippedLanguages(helper);
        helper.assertTrue(languages.containsAll(REQUIRED_LANGUAGES), "language files " + REQUIRED_LANGUAGES + " not found, found only " + languages);
        for (String language : languages) {
            JsonObject lang = readJson(helper, LANG_DIR + language + ".json");
            helper.assertTrue(lang.has(ITEM_KEY) && !lang.get(ITEM_KEY).getAsString().isBlank(), language + " does not translate " + ITEM_KEY);
        }
        helper.assertValueEqual(readJson(helper, LANG_DIR + "en_us.json").get(ITEM_KEY).getAsString(), "Wooden Shears", "English item name");
        Constants.LOG.info("Checked the translations of {} languages: {}", languages.size(), languages);
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
