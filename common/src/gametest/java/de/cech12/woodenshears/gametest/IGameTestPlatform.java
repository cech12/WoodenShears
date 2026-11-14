package de.cech12.woodenshears.gametest;

/**
 * Loader specific operations that the game tests need, but that cannot be done with vanilla code alone.
 * Each loader provides an implementation in its gametest source set (see META-INF/services).
 */
public interface IGameTestPlatform {

    /**
     * Changes the durability config value of the mod at runtime, as if a server admin changed the config file.
     * Loaders with a validating config system (Forge, NeoForge) must apply their range correction here.
     *
     * @param durability raw durability value
     */
    void setDurability(int durability);

}
