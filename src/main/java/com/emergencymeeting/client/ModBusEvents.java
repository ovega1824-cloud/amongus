package com.emergencymeeting.client;

import com.emergencymeeting.EmergencyMeetingMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Registros del cliente en el bus del mod: tecla y overlay. */
@Mod.EventBusSubscriber(modid = EmergencyMeetingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModBusEvents {
    private ModBusEvents() {}

    public static final KeyMapping TABLET_KEY = new KeyMapping(
            "key.emergencymeeting.tablet", GLFW.GLFW_KEY_V, "key.categories.emergencymeeting");

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TABLET_KEY);
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("emergency_meeting",
                (gui, graphics, partialTick, width, height) -> EmergencyOverlay.render(graphics, partialTick, width, height));
    }
}
