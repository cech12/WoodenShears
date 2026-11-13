package de.cech12.woodenshears.gametest;

import de.cech12.woodenshears.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Small helpers shared by all game tests. Only uses vanilla API, so it works on every loader.
 */
public final class TestUtils {

    /** Position in the middle of the "woodenshears:platform" structure, one block above the floor. */
    public static final BlockPos CENTER = new BlockPos(2, 1, 2);

    private TestUtils() {}

    /**
     * @return the registered wooden shears item
     */
    public static Item woodenShearsItem() {
        return BuiltInRegistries.ITEM.getValue(Constants.id("wooden_shears"));
    }

    /**
     * @return a new wooden shears stack
     */
    public static ItemStack woodenShears() {
        return new ItemStack(woodenShearsItem());
    }

    /**
     * Creates a mock player (not added to the level) that holds the given stack in its main hand.
     */
    public static ServerPlayer player(GameTestHelper helper, GameType gameType, ItemStack mainHand) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(gameType);
        Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(CENTER.north(2)));
        player.snapTo(pos.x, pos.y, pos.z, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        return player;
    }

    /**
     * Right-clicks an entity like a real player would do.
     */
    public static InteractionResult interact(ServerPlayer player, Entity entity) {
        try {
            return player.interactOn(entity, InteractionHand.MAIN_HAND, entity.position());
        } finally {
            if (player.isPassenger()) {
                // e.g. a saddled pig was mounted instead of sheared: the mock player has no connection, so mounting throws
                // and a riding mock player would crash the whole test server on the next tick
                try {
                    player.stopRiding();
                } catch (RuntimeException ignored) {
                    // the vehicle is already removed when the missing connection is accessed
                }
            }
        }
    }

    /**
     * Breaks a block like a real survival player would do (drops, tool damage, block callbacks).
     */
    public static boolean destroyBlock(GameTestHelper helper, ServerPlayer player, BlockPos relativePos) {
        return player.gameMode.destroyBlock(helper.absolutePos(relativePos));
    }

    /**
     * Asserts the damage value of a stack.
     */
    public static void assertDamage(GameTestHelper helper, ItemStack stack, int expected, String context) {
        helper.assertValueEqual(stack.getDamageValue(), expected, "damage of wooden shears " + context);
    }

    /**
     * Asserts that an item entity of the given item tag lies inside the test area.
     */
    public static void assertItemEntityPresent(GameTestHelper helper, TagKey<Item> tag) {
        boolean present = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).stream()
                .anyMatch(entity -> entity.getItem().is(tag));
        helper.assertTrue(present, "no item of #" + tag.location() + " was dropped");
    }

}
