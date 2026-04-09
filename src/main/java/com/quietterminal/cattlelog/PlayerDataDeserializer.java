package com.quietterminal.cattlelog;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restores a {@link Player}'s state from a {@link CompoundBinaryTag} loaded
 * from a {@code .cow} file.
 *
 * <p>Applies health, player stats, position, and inventory data using the
 * schema defined in {@link CowSchema}. Files with an unrecognized schema
 * version are skipped with a warning.</p>
 */
public final class PlayerDataDeserializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataDeserializer.class);

    private PlayerDataDeserializer() {}

    /**
     * Deserializes the given compound tag and applies it to the player.
     *
     * @param player the player to restore data to
     * @param cow    the compound tag read from the player's {@code .cow} file
     */
    public static void deserialize(Player player, CompoundBinaryTag cow) {
        int version = cow.getInt(CowSchema.SCHEMA_VERSION, 0);
        if (version < 1) {
            LOGGER.warn("Cow file for {} has unknown schema version {}, skipping load",
                    player.getUsername(), version);
            return;
        }

        player.setHealth(cow.getFloat(CowSchema.HEALTH, 20.0f));

        CompoundBinaryTag branding = cow.getCompound(CowSchema.BRANDING_IRON);
        deserializeBrandingIron(player, branding);

        CompoundBinaryTag pasture = cow.getCompound(CowSchema.PASTURE);
        deserializePasture(player, pasture);

        ListBinaryTag udder = cow.getList(CowSchema.UDDER);
        deserializeUdder(player, udder);

        ListBinaryTag brands = cow.getList(CowSchema.BRANDS);
        deserializeBrands(player, brands);
    }

    private static void deserializeBrandingIron(Player player, CompoundBinaryTag branding) {
        player.setFood(branding.getInt(CowSchema.FOOD, 20));
        player.setFoodSaturation(branding.getFloat(CowSchema.SATURATION, 5.0f));
        player.setLevel(branding.getInt(CowSchema.LEVEL, 0));
        player.setExp(branding.getFloat(CowSchema.EXP, 0.0f));
        player.setHeldItemSlot(branding.getByte(CowSchema.GRAZING_SLOT, (byte) 1));
        player.setFireTicks(branding.getInt(CowSchema.SCORCHING, 0));
        player.getEntityMeta().setAirTicks(branding.getInt(CowSchema.AIR, 300));
        player.getEntityMeta().setFlyingWithElytra(branding.getBoolean(CowSchema.LEAPING, false));

        String gameModeName = branding.getString(CowSchema.GAME_MODE, "SURVIVAL");
        try {
            player.setGameMode(GameMode.valueOf(gameModeName));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown game mode '{}' in cow file for {}, defaulting to SURVIVAL",
                    gameModeName, player.getUsername());
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private static void deserializePasture(Player player, CompoundBinaryTag pasture) {
        if (pasture.keySet().isEmpty()) return;

        Pos pos = new Pos(
                pasture.getDouble(CowSchema.POS_X, 0.0),
                pasture.getDouble(CowSchema.POS_Y, 64.0),
                pasture.getDouble(CowSchema.POS_Z, 0.0),
                pasture.getFloat(CowSchema.POS_YAW, 0.0f),
                pasture.getFloat(CowSchema.POS_PITCH, 0.0f)
        );
        CompoundBinaryTag respawnTag = pasture.getCompound(CowSchema.RESPAWN_PASTURE);
        Pos respawn = new Pos(
                respawnTag.getDouble(CowSchema.RESPAWN_X, 0),
                respawnTag.getDouble(CowSchema.RESPAWN_Y, 64),
                respawnTag.getDouble(CowSchema.RESPAWN_Z, 0),
                respawnTag.getFloat(CowSchema.RESPAWN_YAW, 0),
                respawnTag.getFloat(CowSchema.RESPAWN_PITCH, 0)
        );
        player.teleport(pos);
        player.setRespawnPoint(respawn);
    }

    private static void deserializeUdder(Player player, ListBinaryTag udder) {
        var inventory = player.getInventory();
        inventory.clear();

        for (BinaryTag entry : udder) {
            if (!(entry instanceof CompoundBinaryTag slotCompound)) continue;

            byte slot = slotCompound.getByte(CowSchema.SLOT, (byte) -1);
            if (slot < 0 || slot >= inventory.getSize()) {
                LOGGER.warn("Invalid slot {} in cow file for {}, skipping item",
                        slot, player.getUsername());
                continue;
            }

            try {
                CompoundBinaryTag.Builder itemBuilder = CompoundBinaryTag.builder();
                for (String key : slotCompound.keySet()) {
                    if (!key.equals(CowSchema.SLOT)) {
                        itemBuilder.put(key, slotCompound.get(key));
                    }
                }
                ItemStack item = ItemStack.fromItemNBT(itemBuilder.build());
                inventory.setItemStack(slot, item);
            } catch (Exception e) {
                LOGGER.warn("Failed to deserialize item in slot {} from cow file, skipping", slot, e);
            }
        }
    }

    private static void deserializeBrands(Player player, ListBinaryTag brands) {
        for (BinaryTag entry : brands) {
            if (!(entry instanceof CompoundBinaryTag compound)) continue;
            PotionEffect effect = PotionEffect.fromId(compound.getInt(CowSchema.EFFECT, 0));
            if (effect == null) return;
            int amplifier = compound.getInt(CowSchema.AMPLIFIER, 0);
            int duration = compound.getInt(CowSchema.DURATION, 0);
            boolean hasBlend = compound.getBoolean(CowSchema.HAS_BLEND, false);
            boolean hasIcon = compound.getBoolean(CowSchema.HAS_ICON, false);
            boolean hasParticles = compound.getBoolean(CowSchema.HAS_PARTICLES, false);
            boolean isAmbient = compound.getBoolean(CowSchema.IS_AMBIENT, false);

            byte flags = 0;

            if (hasBlend) {
                flags = (byte) (flags | Potion.BLEND_FLAG);
            }
            if (hasIcon) {
                flags = (byte) (flags | Potion.ICON_FLAG);
            }
            if (hasParticles) {
                flags = (byte) (flags | Potion.PARTICLES_FLAG);
            }
            if (isAmbient) {
                flags = (byte) (flags | Potion.AMBIENT_FLAG);
            }

            Potion potion = flags != 0
                    ? new Potion(effect, amplifier, duration, flags)
                    : new Potion(effect, amplifier, duration);
            player.addEffect(potion);
        }
    }
}
