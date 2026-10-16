package com.emergencymeeting.client;

import com.emergencymeeting.data.PlayerColors;
import com.emergencymeeting.logic.ColorStore;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/** Funciones auxiliares del cliente. */
public final class ClientHelper {
    private ClientHelper() {}

    public static final int NEUTRAL_RGB = 0x6B7A8C;

    public static UUID myId() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : mc.player.getUUID();
    }

    /** Color elegido por un jugador (RGB) o -1 si no eligió. */
    public static int colorOf(UUID id) {
        Integer colorId = ColorStore.CLIENT.get(id);
        return colorId == null || colorId < 0 ? -1 : PlayerColors.rgb(colorId);
    }

    public static int myColorRgb() {
        UUID id = myId();
        int rgb = id == null ? -1 : colorOf(id);
        return rgb < 0 ? NEUTRAL_RGB : rgb;
    }
}
