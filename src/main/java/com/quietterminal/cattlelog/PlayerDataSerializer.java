package com.quietterminal.cattlelog;

import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.TimedPotion;

/**
 * Serializes a {@link Player}'s state into a {@link CompoundBinaryTag} for
 * storage in a {@code .cow} file.
 *
 * <p>The resulting tag includes health, player stats, position, and inventory
 * data keyed using the names defined in {@link CowSchema}.</p>
 */
public final class PlayerDataSerializer {

    private PlayerDataSerializer() {}

    /**
     * Serializes the given player's data into a compound binary tag.
     *
     * @param player the player to serialize
     * @return a compound tag containing all persisted player data
     */
    public static CompoundBinaryTag serialize(Player player) {
        return CompoundBinaryTag.builder()
                .putString(CowSchema.ID, CowSchema.ID_VALUE)
                .putString(CowSchema.CUSTOM_NAME, player.getUsername())
                .putFloat(CowSchema.HEALTH, player.getHealth())
                .putInt(CowSchema.SCHEMA_VERSION, CowSchema.CURRENT_VERSION)
                .put(CowSchema.BRANDING_IRON, serializeBrandingIron(player))
                .put(CowSchema.PASTURE, serializePasture(player))
                .put(CowSchema.UDDER, serializeUdder(player))
                .put(CowSchema.BRANDS, serializeEffects(player))
                .build();
    }

    private static CompoundBinaryTag serializeBrandingIron(Player player) {
        return CompoundBinaryTag.builder()
                .putInt(CowSchema.FOOD, player.getFood())
                .putFloat(CowSchema.SATURATION, player.getFoodSaturation())
                .putInt(CowSchema.LEVEL, player.getLevel())
                .putFloat(CowSchema.EXP, player.getExp())
                .putString(CowSchema.GAME_MODE, player.getGameMode().name())
                .putByte(CowSchema.GRAZING_SLOT, player.getHeldSlot())
                .putInt(CowSchema.AIR, player.getEntityMeta().getAirTicks())
                .putBoolean(CowSchema.LEAPING, player.getEntityMeta().isFlyingWithElytra())
                .putInt(CowSchema.SCORCHING, player.getFireTicks())
                .build();
    }

    private static CompoundBinaryTag serializePasture(Player player) {
        var pos = player.getPosition();
        var respawn = player.getRespawnPoint();
        return CompoundBinaryTag.builder()
                .putDouble(CowSchema.POS_X, pos.x())
                .putDouble(CowSchema.POS_Y, pos.y())
                .putDouble(CowSchema.POS_Z, pos.z())
                .putFloat(CowSchema.POS_YAW, pos.yaw())
                .putFloat(CowSchema.POS_PITCH, pos.pitch())
                .put(CowSchema.RESPAWN_PASTURE, CompoundBinaryTag.builder()
                        .putDouble(CowSchema.RESPAWN_X, respawn.x())
                        .putDouble(CowSchema.RESPAWN_Y, respawn.y())
                        .putDouble(CowSchema.RESPAWN_Z, respawn.z())
                        .putFloat(CowSchema.RESPAWN_YAW, respawn.yaw())
                        .putFloat(CowSchema.RESPAWN_PITCH, respawn.pitch())
                        .build())
                .build();
    }

    private static ListBinaryTag serializeUdder(Player player) {
        var inventory = player.getInventory();
        var builder = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItemStack(slot);
            if (item.isAir()) continue;

            CompoundBinaryTag itemNbt = item.toItemNBT();
            CompoundBinaryTag.Builder entryBuilder = CompoundBinaryTag.builder()
                    .putByte(CowSchema.SLOT, (byte) slot);
            for (String key : itemNbt.keySet()) {
                entryBuilder.put(key, itemNbt.get(key));
            }
            builder.add(entryBuilder.build());
        }

        return builder.build();
    }

    private static ListBinaryTag serializeEffects(Player player) {
        var effects = player.getActiveEffects();
        var builder = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);

        for (TimedPotion tp : effects) {
            Potion potion = tp.potion();
            CompoundBinaryTag nbt = CompoundBinaryTag.builder()
                    .putInt(CowSchema.EFFECT, potion.effect().id())
                    .putInt(CowSchema.AMPLIFIER, potion.amplifier())
                    .putInt(CowSchema.DURATION, potion.duration())
                    .putBoolean(CowSchema.HAS_BLEND, potion.hasBlend())
                    .putBoolean(CowSchema.HAS_ICON, potion.hasIcon())
                    .putBoolean(CowSchema.HAS_PARTICLES, potion.hasParticles())
                    .putBoolean(CowSchema.IS_AMBIENT, potion.isAmbient())
                    .build();
            builder.add(nbt);
        }

        return builder.build();
    }
}
