package com.emergencymeeting.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Botón con forma de tablet que aparece en pantalla (chat, inventario, pausa). */
public class TabletButton extends Button {
    public TabletButton(int x, int y, int size) {
        super(x, y, size, size, Component.literal("Tablet"),
                b -> Minecraft.getInstance().setScreen(new VotingScreen()), DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int rgb = ClientHelper.myColorRgb();
        TabletIcon.draw(g, getX(), getY(), getWidth(), rgb);
        if (isHoveredOrFocused()) {
            GuiUtil.outline(g, getX() + 4, getY(), getX() + getWidth() - 4, getY() + getHeight(), 0xFFFFFFFF);
            g.renderTooltip(Minecraft.getInstance().font, Component.literal("Abrir tablet de votación"), mouseX, mouseY);
        }
    }
}
