package com.emergencymeeting.network;

import com.emergencymeeting.EmergencyMeetingMod;
import com.emergencymeeting.client.ClientHandlers;
import com.emergencymeeting.data.EmergencyData;
import com.emergencymeeting.item.AdminTabletItem;
import com.emergencymeeting.logic.ColorStore;
import com.emergencymeeting.logic.MeetingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Todos los paquetes de red del mod. */
public final class Packets {
    private Packets() {}

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EmergencyMeetingMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        // Cliente -> servidor
        CHANNEL.registerMessage(id++, SetColor.class, SetColor::encode, SetColor::decode, SetColor::handle);
        CHANNEL.registerMessage(id++, Vote.class, Vote::encode, Vote::decode, Vote::handle);
        CHANNEL.registerMessage(id++, SaveConfig.class, SaveConfig::encode, SaveConfig::decode, SaveConfig::handle);
        // Servidor -> cliente
        CHANNEL.registerMessage(id++, OpenLaptop.class, OpenLaptop::encode, OpenLaptop::decode, OpenLaptop::handle);
        CHANNEL.registerMessage(id++, OpenAdmin.class, OpenAdmin::encode, OpenAdmin::decode, OpenAdmin::handle);
        CHANNEL.registerMessage(id++, SyncColors.class, SyncColors::encode, SyncColors::decode, SyncColors::handle);
        CHANNEL.registerMessage(id++, MeetingStart.class, MeetingStart::encode, MeetingStart::decode, MeetingStart::handle);
        CHANNEL.registerMessage(id++, MeetingUpdate.class, MeetingUpdate::encode, MeetingUpdate::decode, MeetingUpdate::handle);
        CHANNEL.registerMessage(id++, MeetingResult.class, MeetingResult::encode, MeetingResult::decode, MeetingResult::handle);
        CHANNEL.registerMessage(id++, MeetingEnd.class, MeetingEnd::encode, MeetingEnd::decode, MeetingEnd::handle);
    }

    // =====================================================================
    // Cliente -> servidor
    // =====================================================================

    public static class SetColor {
        public final int colorId;

        public SetColor(int colorId) {
            this.colorId = colorId;
        }

        public static void encode(SetColor m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.colorId + 1);
        }

        public static SetColor decode(FriendlyByteBuf buf) {
            return new SetColor(buf.readVarInt() - 1);
        }

        public static void handle(SetColor m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    ColorStore.setColor(player, m.colorId);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class Vote {
        public final boolean skip;
        public final UUID target;

        public Vote(boolean skip, UUID target) {
            this.skip = skip;
            this.target = target;
        }

        public static void encode(Vote m, FriendlyByteBuf buf) {
            buf.writeBoolean(m.skip);
            if (!m.skip) {
                buf.writeUUID(m.target);
            }
        }

        public static Vote decode(FriendlyByteBuf buf) {
            boolean skip = buf.readBoolean();
            return new Vote(skip, skip ? null : buf.readUUID());
        }

        public static void handle(Vote m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    MeetingManager.vote(player, m.skip, m.target);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class SaveConfig {
        public final int duration;
        public final boolean endEarly;
        public final int cooldown;

        public SaveConfig(int duration, boolean endEarly, int cooldown) {
            this.duration = duration;
            this.endEarly = endEarly;
            this.cooldown = cooldown;
        }

        public static void encode(SaveConfig m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.duration);
            buf.writeBoolean(m.endEarly);
            buf.writeVarInt(m.cooldown);
        }

        public static SaveConfig decode(FriendlyByteBuf buf) {
            return new SaveConfig(buf.readVarInt(), buf.readBoolean(), buf.readVarInt());
        }

        public static void handle(SaveConfig m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) {
                    return;
                }
                if (!AdminTabletItem.isAdmin(player)) {
                    player.displayClientMessage(Component.literal("§cNo tienes permiso."), true);
                    return;
                }
                EmergencyData data = EmergencyData.get(player.server);
                data.setConfig(m.duration, m.endEarly, m.cooldown);
                player.displayClientMessage(Component.literal("§aConfiguración guardada: reunión de "
                        + data.durationSeconds + "s"), true);
            });
            ctx.setPacketHandled(true);
        }
    }

    // =====================================================================
    // Servidor -> cliente
    // =====================================================================

    public static class OpenLaptop {
        public final int currentColor;

        public OpenLaptop(int currentColor) {
            this.currentColor = currentColor;
        }

        public static void encode(OpenLaptop m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.currentColor + 1);
        }

        public static OpenLaptop decode(FriendlyByteBuf buf) {
            return new OpenLaptop(buf.readVarInt() - 1);
        }

        public static void handle(OpenLaptop m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientHandlers.openLaptop(m.currentColor)));
            ctx.setPacketHandled(true);
        }
    }

    public static class OpenAdmin {
        public final int duration;
        public final boolean endEarly;
        public final int cooldown;

        public OpenAdmin(int duration, boolean endEarly, int cooldown) {
            this.duration = duration;
            this.endEarly = endEarly;
            this.cooldown = cooldown;
        }

        public static void encode(OpenAdmin m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.duration);
            buf.writeBoolean(m.endEarly);
            buf.writeVarInt(m.cooldown);
        }

        public static OpenAdmin decode(FriendlyByteBuf buf) {
            return new OpenAdmin(buf.readVarInt(), buf.readBoolean(), buf.readVarInt());
        }

        public static void handle(OpenAdmin m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientHandlers.openAdmin(m.duration, m.endEarly, m.cooldown)));
            ctx.setPacketHandled(true);
        }
    }

    public static class SyncColors {
        public final Map<UUID, Integer> colors;

        public SyncColors(Map<UUID, Integer> colors) {
            this.colors = colors;
        }

        public static void encode(SyncColors m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.colors.size());
            for (Map.Entry<UUID, Integer> e : m.colors.entrySet()) {
                buf.writeUUID(e.getKey());
                buf.writeVarInt(e.getValue());
            }
        }

        public static SyncColors decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            Map<UUID, Integer> map = new HashMap<>();
            for (int i = 0; i < n; i++) {
                UUID id = buf.readUUID();
                map.put(id, buf.readVarInt());
            }
            return new SyncColors(map);
        }

        public static void handle(SyncColors m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientHandlers.syncColors(m.colors)));
            ctx.setPacketHandled(true);
        }
    }

    public static class MeetingStart {
        public final String callerName;
        public final int callerRgb;
        public final int ticks;
        public final List<UUID> ids;
        public final List<String> names;

        public MeetingStart(String callerName, int callerRgb, int ticks, List<UUID> ids, List<String> names) {
            this.callerName = callerName;
            this.callerRgb = callerRgb;
            this.ticks = ticks;
            this.ids = ids;
            this.names = names;
        }

        public static void encode(MeetingStart m, FriendlyByteBuf buf) {
            buf.writeUtf(m.callerName);
            buf.writeInt(m.callerRgb);
            buf.writeVarInt(m.ticks);
            buf.writeVarInt(m.ids.size());
            for (int i = 0; i < m.ids.size(); i++) {
                buf.writeUUID(m.ids.get(i));
                buf.writeUtf(m.names.get(i));
            }
        }

        public static MeetingStart decode(FriendlyByteBuf buf) {
            String caller = buf.readUtf();
            int rgb = buf.readInt();
            int ticks = buf.readVarInt();
            int n = buf.readVarInt();
            List<UUID> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                ids.add(buf.readUUID());
                names.add(buf.readUtf());
            }
            return new MeetingStart(caller, rgb, ticks, ids, names);
        }

        public static void handle(MeetingStart m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientHandlers.meetingStart(m)));
            ctx.setPacketHandled(true);
        }
    }

    public static class MeetingUpdate {
        public final List<UUID> voted;
        public final int ticksLeft;

        public MeetingUpdate(List<UUID> voted, int ticksLeft) {
            this.voted = voted;
            this.ticksLeft = ticksLeft;
        }

        public static void encode(MeetingUpdate m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.ticksLeft);
            buf.writeVarInt(m.voted.size());
            for (UUID id : m.voted) {
                buf.writeUUID(id);
            }
        }

        public static MeetingUpdate decode(FriendlyByteBuf buf) {
            int ticks = buf.readVarInt();
            int n = buf.readVarInt();
            List<UUID> voted = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                voted.add(buf.readUUID());
            }
            return new MeetingUpdate(voted, ticks);
        }

        public static void handle(MeetingUpdate m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientHandlers.meetingUpdate(m)));
            ctx.setPacketHandled(true);
        }
    }

    public static class MeetingResult {
        public final List<UUID> ids;
        public final List<Integer> counts;
        public final int skipCount;
        public final UUID ejected;
        public final String ejectedName;

        public MeetingResult(List<UUID> ids, List<Integer> counts, int skipCount, UUID ejected, String ejectedName) {
            this.ids = ids;
            this.counts = counts;
            this.skipCount = skipCount;
            this.ejected = ejected;
            this.ejectedName = ejectedName;
        }

        public static void encode(MeetingResult m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.ids.size());
            for (int i = 0; i < m.ids.size(); i++) {
                buf.writeUUID(m.ids.get(i));
                buf.writeVarInt(m.counts.get(i));
            }
            buf.writeVarInt(m.skipCount);
            buf.writeBoolean(m.ejected != null);
            if (m.ejected != null) {
                buf.writeUUID(m.ejected);
                buf.writeUtf(m.ejectedName);
            }
        }

        public static MeetingResult decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<UUID> ids = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                ids.add(buf.readUUID());
                counts.add(buf.readVarInt());
            }
            int skip = buf.readVarInt();
            UUID ejected = null;
            String ejectedName = "";
            if (buf.readBoolean()) {
                ejected = buf.readUUID();
                ejectedName = buf.readUtf();
            }
            return new MeetingResult(ids, counts, skip, ejected, ejectedName);
        }

        public static void handle(MeetingResult m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientHandlers.meetingResult(m)));
            ctx.setPacketHandled(true);
        }
    }

    public static class MeetingEnd {
        public MeetingEnd() {}

        public static void encode(MeetingEnd m, FriendlyByteBuf buf) {}

        public static MeetingEnd decode(FriendlyByteBuf buf) {
            return new MeetingEnd();
        }

        public static void handle(MeetingEnd m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientHandlers.meetingEnd()));
            ctx.setPacketHandled(true);
        }
    }
}
