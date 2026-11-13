package de.cech12.woodenshears.gametest;

import de.cech12.woodenshears.platform.Services;

/**
 * Holds the loader specific {@link IGameTestPlatform} implementation.
 */
public final class GameTestPlatform {

    /** loader specific game test platform */
    public static final IGameTestPlatform INSTANCE = Services.load(IGameTestPlatform.class);

    private GameTestPlatform() {}

}
