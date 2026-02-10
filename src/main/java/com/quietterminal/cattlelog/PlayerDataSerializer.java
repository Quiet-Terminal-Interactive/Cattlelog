package com.quietterminal.cattlelog;

import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

public final class PlayerDataSerializer {

    private PlayerDataSerializer() {}

    public static CompoundBinaryTag serialize(Player player) {
        return CompoundBinaryTag.builder()
                .putString(CowSchema.ID, CowSchema.ID_VALUE)
                .putString(CowSchema.CUSTOM_NAME, player.getUsername())
                .putFloat(CowSchema.HEALTH, player.getHealth())
                .putInt(CowSchema.SCHEMA_VERSION, CowSchema.CURRENT_VERSION)
                .put(CowSchema.BRANDING_IRON, serializeBrandingIron(player))
                .put(CowSchema.PASTURE, serializePasture(player))
                .put(CowSchema.UDDER, serializeUdder(player))
                .build();
    }

    private static CompoundBinaryTag serializeBrandingIron(Player player) {
        return CompoundBinaryTag.builder()
                .putInt(CowSchema.FOOD, player.getFood())
                .putFloat(CowSchema.SATURATION, player.getFoodSaturation())
                .putInt(CowSchema.LEVEL, player.getLevel())
                .putFloat(CowSchema.EXP, player.getExp())
                .putString(CowSchema.GAME_MODE, player.getGameMode().name())
                .build();
    }

    private static CompoundBinaryTag serializePasture(Player player) {
        var pos = player.getPosition();
        return CompoundBinaryTag.builder()
                .putDouble(CowSchema.POS_X, pos.x())
                .putDouble(CowSchema.POS_Y, pos.y())
                .putDouble(CowSchema.POS_Z, pos.z())
                .putFloat(CowSchema.POS_YAW, pos.yaw())
                .putFloat(CowSchema.POS_PITCH, pos.pitch())
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
}
