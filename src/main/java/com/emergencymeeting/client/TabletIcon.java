package com.emergencymeeting.client;

import com.emergencymeeting.EmergencyMeetingMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Dibuja el icono de la tablet (el marco toma el color del jugador). */
public final class TabletIcon {
    private TabletIcon() {}

    private static final ResourceLocation FRAME = new ResourceLocation(EmergencyMeetingMod.MODID, "textures/gui/tablet_frame.png");
    private static final ResourceLocation SCREEN = new ResourceLocation(EmergencyMeetingMod.MODID, "textures/gui/tablet_screen.png");

    public static void draw(GuiGraphics g, int x, int y, int size, int rgb) {
        float r = ((rgb >> 16) & 255) / 255.0F;
        float gr = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;
        RenderSystem.enableBlend();
        g.setColor(r, gr, b, 1.0F);
        g.blit(FRAME, x, y, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(SCREEN, x, y, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
    }
}
