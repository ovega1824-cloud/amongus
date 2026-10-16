package com.emergencymeeting.client;

import com.emergencymeeting.EmergencyMeetingMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos del cliente en el bus de Forge. */
@Mod.EventBusSubscriber(modid = EmergencyMeetingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        while (ModBusEvents.TABLET_KEY.consumeClick()) {
            ClientConfig.setShowTablet(true);
            if (mc.screen == null) {
                mc.setScreen(new VotingScreen());
            }
        }

        if (ClientState.active) {
            if (ClientState.animTick >= 0 && ClientState.animTick < ClientState.ANIM_TOTAL) {
                ClientState.animTick++;
            }
            if (ClientState.ticksLeft > 0) {
                ClientState.ticksLeft--;
            }
            if (ClientState.animTick >= ClientState.ANIM_TOTAL && !ClientState.autoOpened && mc.screen == null) {
                ClientState.autoOpened = true;
                mc.setScreen(new VotingScreen());
            }
        }
    }

    /** Durante la reunión el jugador no puede caminar, saltar ni agacharse. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientState.active) {
            return;
        }
        Input input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
    }

    /** Agrega el botón de la tablet (y el de quitarla) en chat, inventarios y menú de pausa. */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        boolean valid = screen instanceof ChatScreen || screen instanceof PauseScreen
                || screen instanceof AbstractContainerScreen<?>;
        if (!valid || !ClientConfig.showTablet() || Minecraft.getInstance().player == null) {
            return;
        }
        int size = 24;
        TabletButton tablet = new TabletButton(screen.width - size - 6, 6, size);
        Button remove = Button.builder(Component.literal("Quitar"), b -> {
            ClientConfig.setShowTablet(false);
            b.visible = false;
            tablet.visible = false;
        }).bounds(screen.width - size - 14, 6 + size + 2, size + 16, 14).build();
        event.addListener(tablet);
        event.addListener(remove);
    }
}
