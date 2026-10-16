package com.emergencymeeting.logic;

import com.emergencymeeting.data.EmergencyData;
import com.emergencymeeting.data.PlayerColors;
import com.emergencymeeting.network.Packets;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Colores de los jugadores: en el servidor viven en EmergencyData; el cliente guarda una copia sincronizada. */
public final class ColorStore {
    private ColorStore() {}

    /** Copia del lado del cliente (se llena con el paquete SyncColors). */
    public static final Map<UUID, Integer> CLIENT = new HashMap<>();

    /** Id del color (0..n) o -1 si no tiene. Funciona en cliente y servidor. */
    public static int colorIdOf(Player player) {
        if (player.level().isClientSide) {
            return CLIENT.getOrDefault(player.getUUID(), -1);
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return -1;
        }
        return EmergencyData.get(server).getColorId(player.getUUID());
    }

    public static MutableComponent colorize(Component name, int colorId) {
        return name.copy().withStyle(style -> style.withColor(PlayerColors.rgb(colorId)));
    }

    public static void setColor(ServerPlayer player, int id) {
        if (id < -1 || id >= PlayerColors.ALL.size()) {
            return;
        }
        EmergencyData data = EmergencyData.get(player.server);
        data.setColorId(player.getUUID(), id);
        syncAll(player.server);
        if (id < 0) {
            player.displayClientMessage(Component.literal("Quitaste tu color."), true);
        } else {
            player.displayClientMessage(ColorStore.colorize(
                    Component.literal("Ahora eres " + PlayerColors.ALL.get(id).name()), id), true);
        }
    }

    /** Envía todos los colores a todos los jugadores y refresca nombres (cabeza, chat y lista de jugadores). */
    public static void syncAll(MinecraftServer server) {
        Map<UUID, Integer> map = EmergencyData.get(server).allColors();
        Packets.CHANNEL.send(PacketDistributor.ALL.noArg(), new Packets.SyncColors(map));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.refreshDisplayName();
            p.refreshTabListName();
        }
    }
}
