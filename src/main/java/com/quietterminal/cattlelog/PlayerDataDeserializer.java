package com.quietterminal.cattlelog;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlayerDataDeserializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataDeserializer.class);

    private PlayerDataDeserializer() {}

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
    }

    private static void deserializeBrandingIron(Player player, CompoundBinaryTag branding) {
        player.setFood(branding.getInt(CowSchema.FOOD, 20));
        player.setFoodSaturation(branding.getFloat(CowSchema.SATURATION, 5.0f));
        player.setLevel(branding.getInt(CowSchema.LEVEL, 0));
        player.setExp(branding.getFloat(CowSchema.EXP, 0.0f));

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
        player.teleport(pos);
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
}
