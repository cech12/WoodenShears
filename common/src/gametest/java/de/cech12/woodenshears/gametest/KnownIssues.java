package de.cech12.woodenshears.gametest;

import de.cech12.woodenshears.Constants;
import de.cech12.woodenshears.platform.Services;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Set;

/**
 * Marks tests that are known to fail on some loaders because of a real bug or missing loader support.
 * On these loaders the test is still executed: it passes while the issue is present and FAILS as soon as the issue is
 * gone, so the marker cannot be forgotten. On all other loaders the test runs normally.
 */
public final class KnownIssues {

    /** platform name of Fabric (see IPlatformHelper#getPlatformName) */
    public static final String FABRIC = "Fabric";
    /** platform name of Forge */
    public static final String FORGE = "Forge";
    /** platform name of NeoForge */
    public static final String NEOFORGE = "NeoForge";

    private KnownIssues() {}

    /**
     * Runs a synchronous test body and succeeds the test.
     *
     * @param helper game test helper
     * @param issue link or short description of the known issue
     * @param affectedPlatforms platforms where the issue is known
     * @param test test body with assertions
     */
    public static void run(GameTestHelper helper, String issue, Set<String> affectedPlatforms, Runnable test) {
        String platform = Services.PLATFORM.getPlatformName();
        if (!affectedPlatforms.contains(platform)) {
            test.run();
            helper.succeed();
            return;
        }
        try {
            test.run();
        } catch (GameTestAssertException e) {
            Constants.LOG.warn("Known issue still present on {} ({}): {}", platform, issue, e.getMessage());
            helper.succeed();
            return;
        }
        helper.fail("Known issue seems to be fixed on " + platform + " (" + issue + "). Remove the KnownIssues marker of this test.");
    }

}
