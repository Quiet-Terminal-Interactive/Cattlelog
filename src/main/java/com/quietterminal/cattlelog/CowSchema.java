package com.quietterminal.cattlelog;

/**
 * NBT tag names and constants used in {@code .cow} files.
 *
 * <p>All player data is stored using cow-themed key names. This class defines
 * the schema so that serializers and deserializers stay in sync.</p>
 */
public final class CowSchema {

    private CowSchema() {}

    /** Entity type identifier. */
    public static final String ID = "id";
    /** The entity type value ({@code minecraft:cow}). */
    public static final String ID_VALUE = "minecraft:cow";
    /** The player's username. */
    public static final String CUSTOM_NAME = "CustomName";
    /** The player's health. */
    public static final String HEALTH = "Health";
    /** Schema version tag for forward-compatibility checks. */
    public static final String SCHEMA_VERSION = "CattlelogVersion";
    /** The current schema version. */
    public static final int CURRENT_VERSION = 2;
    /** Compound tag containing player stats (food, saturation, level, XP, game mode). */
    public static final String BRANDING_IRON = "BrandingIron";
    /** Food level within the {@link #BRANDING_IRON} compound. */
    public static final String FOOD = "FeedLevel";
    /** Food saturation within the {@link #BRANDING_IRON} compound. */
    public static final String SATURATION = "FeedSaturation";
    /** XP level within the {@link #BRANDING_IRON} compound. */
    public static final String LEVEL = "HerdRank";
    /** XP progress within the {@link #BRANDING_IRON} compound. */
    public static final String EXP = "GrazingProgress";
    /** Game mode name within the {@link #BRANDING_IRON} compound. */
    public static final String GAME_MODE = "Temperament";
    /** Compound tag containing position data. */
    public static final String PASTURE = "Pasture";
    /** X coordinate within the {@link #PASTURE} compound. */
    public static final String POS_X = "x";
    /** Y coordinate within the {@link #PASTURE} compound. */
    public static final String POS_Y = "y";
    /** Z coordinate within the {@link #PASTURE} compound. */
    public static final String POS_Z = "z";
    /** Yaw rotation within the {@link #PASTURE} compound. */
    public static final String POS_YAW = "yaw";
    /** Pitch rotation within the {@link #PASTURE} compound. */
    public static final String POS_PITCH = "pitch";
    /** List tag containing inventory items. */
    public static final String UDDER = "Udder";
    /** Inventory slot index within each item entry. */
    public static final String SLOT = "Slot";
    /** List tag containing active potion effects. */
    public static final String BRANDS = "Brands";
    /** Effect type identifier within each entry of the {@link #BRANDS} list. */
    public static final String EFFECT = "effect";
    /** Amplifier (level - 1) within each entry of the {@link #BRANDS} list. */
    public static final String AMPLIFIER = "amplifier";
    /** Remaining duration in ticks within each entry of the {@link #BRANDS} list. */
    public static final String DURATION = "duration";
    /** Whether the effect uses a blend within each entry of the {@link #BRANDS} list. */
    public static final String HAS_BLEND = "hasBlend";
    /** Whether the effect shows an icon within each entry of the {@link #BRANDS} list. */
    public static final String HAS_ICON = "hasIcon";
    /** Whether the effect shows particles within each entry of the {@link #BRANDS} list. */
    public static final String HAS_PARTICLES = "hasParticles";
    /** Whether the effect is ambient (beacon-sourced) within each entry of the {@link #BRANDS} list. */
    public static final String IS_AMBIENT = "isAmbient";
    /** The selected hotbar slot index. */
    public static final String GRAZING_SLOT = "GrazingSlot";
    /** Remaining air supply in ticks. */
    public static final String AIR = "Air";
    /** Whether the player is currently using an elytra (gliding). */
    public static final String LEAPING = "Leaping";
    /** Whether the player is currently on fire. */
    public static final String SCORCHING = "Scorching";
    /** Compound tag containing the player's respawn position data. */
    public static final String RESPAWN_PASTURE = "RespawnPasture";
    /** X coordinate within the {@link #RESPAWN_PASTURE} compound. */
    public static final String RESPAWN_X = "x";
    /** Y coordinate within the {@link #RESPAWN_PASTURE} compound. */
    public static final String RESPAWN_Y = "y";
    /** Z coordinate within the {@link #RESPAWN_PASTURE} compound. */
    public static final String RESPAWN_Z = "z";
    /** Yaw rotation within the {@link #RESPAWN_PASTURE} compound. */
    public static final String RESPAWN_YAW = "yaw";
    /** Pitch rotation within the {@link #RESPAWN_PASTURE} compound. */
    public static final String RESPAWN_PITCH = "pitch";
}
