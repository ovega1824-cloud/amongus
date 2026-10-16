package com.emergencymeeting.client;

import com.emergencymeeting.EmergencyMeetingMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Overlay del cliente:
 *  - la animación de "EMERGENCY MEETING" (rayos rojos que aparecen de izquierda a derecha, alarma del color
 *    de quien presionó el botón y el título)
 *  - el icono de la tablet y el temporizador de la reunión.
 */
public final class EmergencyOverlay {
    private EmergencyOverlay() {}

    private static final ResourceLocation BURST = new ResourceLocation(EmergencyMeetingMod.MODID, "textures/gui/burst.png");
    private static final ResourceLocation ALARM_BASE = new ResourceLocation(EmergencyMeetingMod.MODID, "textures/gui/alarm_base.png");
    private static final ResourceLocation ALARM_DOME = new ResourceLocation(EmergencyMeetingMod.MODID, "textures/gui/alarm_dome.png");

    public static void render(GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (ClientState.active && ClientState.animTick >= 0 && ClientState.animTick < ClientState.ANIM_TOTAL) {
            renderAnimation(g, mc, partialTick, w, h);
            return;
        }
        if (mc.screen != null) {
            return;
        }
        renderHud(g, mc, w, h);
    }

    // ---------------------------------------------------------------------------------------------
    // Animación
    // ---------------------------------------------------------------------------------------------

    private static void renderAnimation(GuiGraphics g, Minecraft mc, float pt, int w, int h) {
        Font font = mc.font;
        float t = ClientState.animTick + pt;
        float total = ClientState.ANIM_TOTAL;

        // El barrido va de izquierda a derecha en ~0.5 s.
        float wipe = Mth.clamp(t / 10.0F, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - wipe) * (1.0F - wipe) * (1.0F - wipe);
        int revealX = Math.max(1, (int) (w * eased));
        float fade = Mth.clamp((total - t) / 10.0F, 0.0F, 1.0F);

        float shakeAmp = Math.max(0.0F, 1.0F - t / 14.0F) * 5.0F;
        float sx = Mth.sin(t * 2.7F) * shakeAmp;
        float sy = Mth.cos(t * 3.3F) * shakeAmp;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.enableScissor(0, 0, revealX, h);
        g.pose().pushPose();
        g.pose().translate(sx, sy, 0.0F);

        // Fondo oscuro rojizo
        int dimAlpha = (int) (150 * fade);
        g.fill(-10, -10, w + 10, h + 10, (dimAlpha << 24) | 0x140000);

        // Banda de rayos rojos
        int bh = Math.max(110, h / 2);
        int by = h / 2 - bh / 2;
        g.setColor(1.0F, 1.0F, 1.0F, fade);
        g.blit(BURST, 0, by, w, bh, 0.0F, 0.0F, 512, 128, 512, 128);
        int drift = (int) ((t * 7.0F) % 48.0F);
        g.setColor(1.0F, 1.0F, 1.0F, 0.55F * fade);
        g.blit(BURST, -48 + drift, by - 8, w + 96, bh + 16, 0.0F, 0.0F, 512, 128, 512, 128);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Alarma del color de quien presionó el botón
        float pulse = 1.0F + 0.06F * Mth.sin(t * 1.3F);
        int rgb = ClientState.callerRgb;
        float cr = ((rgb >> 16) & 255) / 255.0F;
        float cg = ((rgb >> 8) & 255) / 255.0F;
        float cb = (rgb & 255) / 255.0F;
        int alarmSize = (int) Math.min(120, Math.max(48, h / 3.2F));
        g.pose().pushPose();
        g.pose().translate(w / 2.0F, by - alarmSize * 0.20F, 0.0F);
        g.pose().scale(pulse, pulse, 1.0F);
        // resplandor
        float glow = 0.25F + 0.2F * (0.5F + 0.5F * Mth.sin(t * 1.8F));
        g.setColor(cr, cg, cb, glow * fade);
        g.blit(ALARM_DOME, -alarmSize, -alarmSize, alarmSize * 2, alarmSize * 2, 0.0F, 0.0F, 64, 64, 64, 64);
        g.setColor(1.0F, 1.0F, 1.0F, fade);
        g.blit(ALARM_BASE, -alarmSize / 2, -alarmSize / 2, alarmSize, alarmSize, 0.0F, 0.0F, 64, 64, 64, 64);
        g.setColor(cr, cg, cb, fade);
        g.blit(ALARM_DOME, -alarmSize / 2, -alarmSize / 2, alarmSize, alarmSize, 0.0F, 0.0F, 64, 64, 64, 64);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().popPose();

        int textAlpha = (int) (255 * fade);
        if (textAlpha > 8) {
            // Título
            Component title = Component.literal("EMERGENCY MEETING").withStyle(ChatFormatting.BOLD);
            int tw = font.width(title);
            float scale = Math.min(w * 0.82F / tw, h * 0.16F / 9.0F);
            float slide = (1.0F - eased) * -w * 0.25F;
            g.pose().pushPose();
            g.pose().translate(w / 2.0F + slide, h / 2.0F, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            int black = (textAlpha << 24);
            int white = (textAlpha << 24) | 0xFFFFFF;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        g.pose().pushPose();
                        g.pose().translate(dx * 0.7F, dy * 0.7F, 0.0F);
                        g.drawString(font, title, -tw / 2, -4, black, false);
                        g.pose().popPose();
                    }
                }
            }
            g.drawString(font, title, -tw / 2, -4, white, false);
            g.pose().popPose();

            // Quién llamó
            int callerColor = (textAlpha << 24) | (rgb & 0xFFFFFF);
            Component caller = Component.literal(ClientState.callerName).withStyle(ChatFormatting.BOLD);
            Component label = Component.literal("Llamada por ");
            int total2 = font.width(label) + font.width(caller);
            float cs = Math.max(1.0F, Math.min(3.0F, h / 130.0F));
            g.pose().pushPose();
            g.pose().translate(w / 2.0F, by + bh + 8.0F, 0.0F);
            g.pose().scale(cs, cs, 1.0F);
            g.drawString(font, label, -total2 / 2, 0, (textAlpha << 24) | 0xFFFFFF, true);
            g.drawString(font, caller, -total2 / 2 + font.width(label), 0, callerColor, true);
            g.pose().popPose();
        }

        g.pose().popPose();
        g.disableScissor();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // ---------------------------------------------------------------------------------------------
    // HUD: icono de tablet + temporizador
    // ---------------------------------------------------------------------------------------------

    private static void renderHud(GuiGraphics g, Minecraft mc, int w, int h) {
        Font font = mc.font;
        if (ClientConfig.showTablet()) {
            int size = 24;
            int x = w - size - 6;
            TabletIcon.draw(g, x, 6, size, ClientHelper.myColorRgb());
            Component key = ModBusEvents.TABLET_KEY.getTranslatedKeyMessage();
            Component hint = Component.literal("[").append(key).append("]");
            g.drawString(font, hint, x + size / 2 - font.width(hint) / 2, 6 + size + 2, 0xFFCCCCCC, true);
        }
        if (ClientState.active && ClientState.phase == 1) {
            int secs = Math.max(0, (ClientState.ticksLeft + 19) / 20);
            String text = "Reunión: " + secs / 60 + ":" + (secs % 60 < 10 ? "0" : "") + secs % 60;
            g.drawCenteredString(font, Component.literal(text), w / 2, 8, 0xFFFF5555);
            g.drawCenteredString(font, Component.literal("Presiona [" + ModBusEvents.TABLET_KEY.getTranslatedKeyMessage().getString() + "] para votar"),
                    w / 2, 20, 0xFFFFFFFF);
        }
    }
}
