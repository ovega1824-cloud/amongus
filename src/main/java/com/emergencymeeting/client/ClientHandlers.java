package com.emergencymeeting.client;

import com.emergencymeeting.ModRegistry;
import com.emergencymeeting.logic.ColorStore;
import com.emergencymeeting.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;

/** Recibe los paquetes del servidor en el cliente. Esta clase solo se carga en el cliente. */
public final class ClientHandlers {
    private ClientHandlers() {}

    public static void openLaptop(int currentColor) {
        Minecraft.getInstance().setScreen(new LaptopScreen(currentColor));
    }

    public static void openAdmin(int duration, boolean endEarly, int cooldown) {
        Minecraft.getInstance().setScreen(new AdminScreen(duration, endEarly, cooldown));
    }

    public static void syncColors(Map<UUID, Integer> colors) {
        ColorStore.CLIENT.clear();
        ColorStore.CLIENT.putAll(colors);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (Player p : mc.level.players()) {
                p.refreshDisplayName();
            }
        }
    }

    public static void meetingStart(Packets.MeetingStart m) {
        Minecraft mc = Minecraft.getInstance();
        ClientState.reset();
        ClientState.active = true;
        ClientState.phase = 1;
        ClientState.animTick = 0;
        ClientState.ticksLeft = m.ticks;
        ClientState.callerName = m.callerName;
        ClientState.callerRgb = m.callerRgb;
        for (int i = 0; i < m.ids.size(); i++) {
            ClientState.ids.add(m.ids.get(i));
            ClientState.names.put(m.ids.get(i), m.names.get(i));
        }
        mc.setScreen(null);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModRegistry.MEETING_SOUND.get(), 1.0F, 1.0F));
    }

    public static void meetingUpdate(Packets.MeetingUpdate m) {
        if (!ClientState.active) {
            return;
        }
        ClientState.voted.clear();
        ClientState.voted.addAll(m.voted);
        ClientState.ticksLeft = m.ticksLeft;
        UUID me = ClientHelper.myId();
        if (me != null && ClientState.voted.contains(me)) {
            ClientState.iVoted = true;
        }
    }

    public static void meetingResult(Packets.MeetingResult m) {
        if (!ClientState.active) {
            return;
        }
        ClientState.phase = 2;
        ClientState.ticksLeft = 100;
        ClientState.counts.clear();
        for (int i = 0; i < m.ids.size(); i++) {
            ClientState.counts.put(m.ids.get(i), m.counts.get(i));
        }
        ClientState.skipCount = m.skipCount;
        ClientState.ejected = m.ejected;
        ClientState.ejectedName = m.ejectedName;
        Minecraft mc = Minecraft.getInstance();
        if (ClientState.animTick >= ClientState.ANIM_TOTAL && !(mc.screen instanceof VotingScreen)) {
            mc.setScreen(new VotingScreen());
        }
    }

    public static void meetingEnd() {
        Minecraft mc = Minecraft.getInstance();
        ClientState.reset();
        if (mc.screen instanceof VotingScreen) {
            mc.setScreen(null);
        }
    }
}
