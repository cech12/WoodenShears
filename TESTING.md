# Testing

Wooden Shears has two automated test levels. Both run in CI on every push and pull request for every loader
(Fabric, Forge, NeoForge) and turn the build red on failure.

| Level | What it checks | Where it runs |
|---|---|---|
| **GameTests** | Every feature of the mod (see table below) in a real game test server | Gradle (`runGameTestServer`), development environment |
| **Server start test** | The built release jar starts cleanly on a real dedicated server, with and without the optional integrations | Docker image [itzg/minecraft-server](https://github.com/itzg/docker-minecraft-server) |

In CI (`.github/workflows/cicd-workflow.yml`) the game tests run in the build job right after `./gradlew build`, so
they reuse the Minecraft setup and the compiled classes of the build. The server start test runs after the build (it
needs the release jars), and publishing on a tag only starts when build, game tests and server start test succeeded.

## Running the tests locally

GameTests (Java 25 toolchain is provisioned by Gradle):

```bash
./gradlew runGameTestServer              # all loaders
./gradlew :fabric:runGameTestServer      # one loader
./gradlew :forge:runGameTestServer
./gradlew :neoforge:runGameTestServer
```

The task fails if a test fails or if no test was found. A JUnit report is written to
`<loader>/build/gametest-report.xml`, logs are in `<loader>/runs/gametest/logs/`.

Server start test (needs bash, Docker, curl and jq; on Windows use WSL or Git Bash with Docker Desktop). The script is
shared by all mods and lives in [cech12/MinecraftModActions](https://github.com/cech12/MinecraftModActions)
(`scripts/server-starttest.sh`):

```bash
./gradlew build
S=https://raw.githubusercontent.com/cech12/MinecraftModActions/main/scripts/server-starttest.sh
curl -fsSL $S | bash -s -- fabric                        # mod + required mods
curl -fsSL $S | bash -s -- fabric --with-integrations    # + optional integrations
curl -fsSL $S | bash -s -- forge
curl -fsSL $S | bash -s -- neoforge
```

The server runs with the Minecraft version of `gradle.properties`, but with the newest loader version and the newest
versions of the dependencies, so that the test notices when a new loader or dependency release breaks the mod. The
dependencies are read from the metadata of the jar (`fabric.mod.json`, `mods.toml`, `neoforge.mods.toml`): required
dependencies are always installed, optional ones (on Fabric `recommends`/`suggests`, e.g. ModMenu) only with
`--with-integrations`. Mod ids with another name on Modrinth are mapped centrally in `scripts/server-starttest-mods.txt`
of MinecraftModActions. The test fails if the server does not log "Done" within 10 minutes, stops again right after
the start, writes a crash report, logs exceptions, mixin errors or errors mentioning the mod id, or if the mod is not
in the mod list of the loader. Known harmless log lines can be excluded in `scripts/server-starttest.allowlist` (one
regex per line, in addition to the built-in ones of the script). Logs and a summary are written to
`.server-starttest/<loader>-<mode>/`. `--with-integrations` is skipped (exit 0) for loaders without optional
dependencies (currently Forge and NeoForge), because it would equal the run without integrations. To reproduce a
failure with an older loader, set `STARTTEST_LOADER_VERSION`.

## How the GameTests are organized

```
common/src/gametest/java/.../gametest/          test logic, only vanilla API (shared by all loaders)
  WoodenShearsGameTests                         central list of all test classes
  tests/*Tests                                  the tests, grouped by feature
  DurabilityConfigEnvironment                   test environment that sets the durability config per batch
  KnownIssues                                   marks tests that are known to fail on some loaders
common/src/gametest/resources/data/woodenshears/
  test_instance/**.json                         one json per test (structure, environment, max ticks)
  test_environment/*.json                       config values (durability) used by the tests
  structure/platform.nbt                        5x4x5 test area with a stone floor
  loot_table/gametest/*.json                    data pack loot tables used by the loot tests
<loader>/src/gametest/                          thin adapter: registers the test functions, changes the config
```

Since Minecraft 1.21.5 test functions are a registry and test instances are data pack entries. The adapters only
register the functions; everything else is plain data, so a port to a new Minecraft version usually only needs
changes in the test code where vanilla API changed. On Fabric and Forge the gametest source set is a separate
test mod (`woodenshears_gametest`), on NeoForge it is part of the main mod. It is never part of the release jars.

### Writing a new test

1. Add a method `public static void myTest(GameTestHelper helper)` to the matching class in `tests/` and register it
   in its `register` method: `tests.add("group/my_test", Class::myTest);`. Call `helper.succeed()` (or use
   `helper.succeedWhen(...)` for tests that need ticks).
2. Add `common/src/gametest/resources/data/woodenshears/test_instance/group/my_test.json` (copy an existing one).
   The meta test `meta/every_test_function_has_an_instance` fails if you forget it.
3. Tests that need another durability config value use another `environment` (`test_environment/*.json`).
   Tests of one environment run together, different environments never run at the same time.
4. Run `./gradlew runGameTestServer` and make sure the test fails if the feature is broken.
5. Add the test to the feature table below.

If a test reveals a bug that cannot be fixed in the same change, wrap the assertions in
`KnownIssues.run(helper, "<issue link>", Set.of(KnownIssues.FORGE), () -> ...)` and open an issue. On the listed
loaders the test passes while the bug exists and fails as soon as it is fixed, so the marker is not forgotten.

## Features and tests

All test names are relative to `woodenshears:`. Every test runs on Fabric, Forge and NeoForge.

### Item

| Feature | Test(s) |
|---|---|
| The item `woodenshears:wooden_shears` exists and behaves as shears | `item/is_registered_as_shears` |
| Wooden shears do not stack (regression 1.18.2-1.2.1.1) | `item/does_not_stack` |
| Listed in the creative tab "Tools & Utilities" | `item/is_in_tools_and_utilities_tab` |
| Part of the common tag `c:tools/shear` (#17) | `item/is_in_common_shears_tag` |
| Enchantable like vanilla shears with every enchantment (Efficiency, Unbreaking, Mending, Curse of Vanishing), only with an anvil, not at the enchanting table (regression 1.20.6-2.3.0.1) | `item/is_enchantable_like_vanilla_shears` |
| Same mining speed and correct-tool behavior as vanilla shears for every block (cobweb, leaves, wool, ...) | `item/mines_like_vanilla_shears` |
| Same durability loss as vanilla shears when breaking any block: 1 for blocks with hardness, nothing for blocks without hardness like fire (#40) | `item/loses_durability_per_block_like_vanilla_shears` |
| Furnace fuel with the burn time of wooden tools (#39) | `item/is_fuel_like_wooden_tools`, `item/burns_in_furnace` |

### Durability config option (`durability`, default 50, range 0 - 10000, 0 deactivates the durability)

| Feature | Test(s) |
|---|---|
| Default 50 | `durability/default_value` |
| Custom value (200) | `durability/custom_value` |
| Maximum value 10000 | `durability/maximum_value` |
| Values above 10000 are limited to 10000 | `durability/above_maximum_is_limited` |
| 0 makes the shears unbreakable (not damageable, no max damage) | `durability/zero_makes_shears_unbreakable` |
| Values below 0 behave like 0 | `durability/below_zero_makes_shears_unbreakable` |
| Shears break after 50 uses | `durability/shears_break_when_used_up` |
| Config changes apply to already existing shears | `durability/config_change_applies_to_existing_shears` |

### Crafting and repair

| Feature | Test(s) |
|---|---|
| Two planks of any kind placed diagonally (both directions, mixed kinds) craft wooden shears | `recipe/crafted_from_two_diagonal_planks` |
| Two planks in a row / column do not match the recipe | `recipe/not_crafted_from_two_planks_in_a_row` |
| Recipe is in the "equipment" recipe book category (#27) | `recipe/is_in_equipment_category` |
| Recipe is unlocked when the player has planks | `recipe/unlocked_by_having_planks` |
| Two damaged shears can be combined (remaining durability + 5%), result follows the config (#38) | `repair/combines_two_damaged_shears` |
| Fully repaired shears are identical to new shears (#38) | `repair/fully_repaired_shears_equal_new_shears` |
| Two damaged shears can be combined in a grindstone (remaining durability + 5%, enchantments removed) | `repair/grindstone_combines_two_damaged_shears` |
| Shears combined in a grindstone do not store a fixed max damage, so config changes still apply (known issue [#51](https://github.com/cech12/WoodenShears/issues/51)) | `repair/grindstone_result_follows_config` |
| Two damaged shears can be combined in an anvil (remaining durability + 12%, enchantments of both kept) | `repair/anvil_combines_two_damaged_shears` |
| Unbreakable shears (durability 0) cannot be combined | `repair/not_possible_when_unbreakable` |

### Shearing entities (right-click)

| Feature | Test(s) |
|---|---|
| Sheep: drops wool, sheared, shears lose one durability | `entity/shears_sheep` |
| Creative mode (sheep, mooshroom, snow golem) works and does not damage the shears | `entity/shears_in_creative_without_damage` |
| Mooshroom turns into a cow and drops mushrooms | `entity/shears_mooshroom` |
| Snow golem loses its pumpkin | `entity/shears_snow_golem` |
| Bogged can be sheared (fixed known issue 1.21-3.1.0.0) | `entity/shears_bogged` |
| Copper golem loses its flower (1.21.9-3.6.0.1) | `entity/shears_copper_golem` |
| Sulfur cube ejects its swallowed item | `entity/shears_sulfur_cube` |
| Equipment like a saddle can be cut off, with the same durability loss as vanilla shears | `entity/removes_saddle` |
| Wolf armor can be removed by the owner, with the same durability loss as vanilla shears, and grants the advancement "Shear Brilliance" (fixed known issue 1.21-3.1.0.0) | `entity/removes_wolf_armor` |
| Saddles, horse and nautilus armor, llama carpets and happy ghast harnesses are removed from every mob that can wear them, exactly like with vanilla shears (pig, strider, horses, donkey, mule, camels, llamas, happy ghast, nautiluses) | `entity/removes_equipment_like_vanilla_shears` |
| Leash of an entity can be cut (1.21.9-3.6.0.1) | `entity/cuts_leash_of_entity` |
| Leash knot on a fence can be removed (1.21.9-3.6.0.1) | `entity/cuts_leash_knot` |

### Blocks

| Feature | Test(s) |
|---|---|
| Pumpkins can be carved (fixed 1.17.1-1.1.0.1) | `block/carves_pumpkin` |
| Full beehives and bee nests can be harvested (fixed 1.17.1-1.1.0.1) | `block/harvests_full_beehives` |
| Beehives and bee nests that are not full (honey level 0 - 4) are not harvested and the shears are not damaged | `block/does_not_harvest_beehives_without_honey` |
| Cutting a tripwire disarms it (fixed 1.17.1-1.1.0.1) | `block/disarms_tripwire`, control test `block/tripwire_triggers_without_shears` |
| Using shears on a plant tip stops its growth like vanilla shears, for every growing plant (twisting and weeping vines, cave vines, kelp, ...) | `block/stops_growth_of_plant_tips` |
| For every block state of every registered block, the drops with wooden shears are the same as with vanilla shears (leaves, grass, vines, cobweb, tall grass, ...). This keeps all shears specific loot tables identical on all loaders. The test fails if well-known shears blocks are not detected as shears specific, and it logs the list of detected blocks (about 33 in 26.3) | `block/drops_like_vanilla_shears` |
| Breaking leaves, cobweb, grass, fern, dead bush and glow lichen (#11) drops them and damages the shears | `block/breaking_drops_shearable_blocks` |

### Dispenser

| Feature | Test(s) |
|---|---|
| Dispensers shear sheep, mooshrooms, snow golems, bogged, copper golems and sulfur cubes | `dispenser/shears_sheep`, `dispenser/shears_mooshroom`, `dispenser/shears_snow_golem`, `dispenser/shears_bogged`, `dispenser/shears_copper_golem`, `dispenser/shears_sulfur_cube` |
| Dispensers harvest full beehives | `dispenser/harvests_full_beehive` |
| Dispensers cut leashes | `dispenser/cuts_leash` |

### Data packs

Recommended for data packs: check for shears with the vanilla predicate `minecraft:tool/can_shear` (like vanilla
loot tables do) or with the tag `c:tools/shear`. Both accept wooden shears on all loaders.

| Feature | Test(s) |
|---|---|
| Loot conditions that reference `minecraft:tool/can_shear` accept wooden shears | `loot/can_shear_predicate_accepts_wooden_shears` |
| `match_tool` conditions with the tag `c:tools/shear` accept wooden shears; such data packs do not crash the world loading ([CeramicShears#28](https://github.com/cech12/CeramicShears/issues/28)) | `loot/match_tool_with_shears_tag_accepts_wooden_shears` |
| Fabric only: `match_tool` conditions that name `minecraft:shears` directly accept wooden shears | `loot/fabric_match_tool_with_vanilla_shears_accepts_wooden_shears` (only asserts on Fabric) |

**Intended loader difference:** Forge and NeoForge replace the vanilla predicate `minecraft:tool/can_shear` with
their shears tool action (`forge:can_tool_perform_action` / `neoforge:can_item_perform_ability`, `shears_dig`),
which wooden shears support as a shears item. Fabric has no such tool action and keeps the vanilla predicate, so the
mod adds wooden shears to every `match_tool` condition that contains vanilla shears (`MatchToolMixin`). That is why a
data pack that names `minecraft:shears` directly also accepts wooden shears on Fabric, but not on Forge and NeoForge.
This is not a bug: such data packs should use the predicate or the tag, which work everywhere.

### Resources and test setup

| Feature | Test(s) |
|---|---|
| Item name translated in all shipped languages (all files in `assets/woodenshears/lang/`, found automatically), English "Wooden Shears" | `resources/item_name_is_translated` |
| Item model definition, model and texture exist and reference each other | `resources/item_model_and_texture_exist` |
| Every test function has a test instance | `meta/every_test_function_has_an_instance` |
| The release jar starts on a dedicated server, with and without integrations (Cloth Config and ModMenu on Fabric) | server start test |

### Known issues (marked with `KnownIssues`)

| Issue | Test | Loaders |
|---|---|---|
| [#51](https://github.com/cech12/WoodenShears/issues/51) Shears combined in a grindstone store a fixed max damage | `repair/grindstone_result_follows_config` | Fabric, Forge, NeoForge |

### Checked against the Minecraft Wiki

The feature list was checked against the [Minecraft Wiki page about shears](https://minecraft.wiki/w/Shears) (all
uses and the complete version history up to 26.3). Vanilla behavior that does not depend on the kind of shears is not
tested separately: pumpkin face direction, bees getting angry, sounds, loot of chests and villager trades of vanilla
shears.

## Not tested automatically

These features cannot be checked on a dedicated test server. Check them manually before a release:

| Feature | Why | Manual check |
|---|---|---|
| Item texture and model in the inventory and in the hand | Client rendering | Start `runClient`, look at the item in the inventory, hotbar and in first/third person |
| Item name and tooltip in the game | Client language loading (the files are tested) | Switch the language once, check the item name |
| Config screen via ModMenu (Fabric) | Client GUI | Fabric client: Mods > Wooden Shears > config, change durability, reopen |
| Config file creation and editing (`config/woodenshears.toml` on Fabric, server config `woodenshears-server.toml` on Forge/NeoForge) | The tests change the config at runtime, not via the file | Start a server, change `durability` in the file, restart, check the max damage of new shears |
| Creative tab in the client | Client GUI (the tab contents are tested) | Open the "Tools & Utilities" tab |
