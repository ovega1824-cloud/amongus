package com.emergencymeeting.client;

import com.emergencymeeting.data.PlayerColors;
import com.emergencymeeting.network.Packets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Pantalla de la laptop: elegir el color del jugador. */
public class LaptopScreen extends Screen {
    private static final int COLUMNS = 6;
    private static final int CELL = 34;
    private static final int GAP = 6;

    private int selected;
    private int gridX;
    private int gridY;

    public LaptopScreen(int currentColor) {
        super(Component.literal("Laptop de colores"));
        this.selected = currentColor;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int rows = (PlayerColors.ALL.size() + COLUMNS - 1) / COLUMNS;
        int gridW = COLUMNS * CELL + (COLUMNS - 1) * GAP;
        int gridH = rows * CELL + (rows - 1) * GAP;
        gridX = (width - gridW) / 2;
        gridY = (height - gridH) / 2 - 4;

        int by = gridY + gridH + 34;
        addRenderableWidget(Button.builder(Component.literal("Quitar color"), b -> choose(-1))
                .bounds(width / 2 - 104, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(width / 2 + 4, by, 100, 20).build());
    }

    private void choose(int id) {
        selected = id;
        Packets.CHANNEL.sendToServer(new Packets.SetColor(id));
    }

    private int cellAt(double mx, double my) {
        for (int i = 0; i < PlayerColors.ALL.size(); i++) {
            int x = gridX + (i % COLUMNS) * (CELL + GAP);
            int y = gridY + (i / COLUMNS) * (CELL + GAP);
            if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int i = cellAt(mx, my);
            if (i >= 0) {
                choose(i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        int rows = (PlayerColors.ALL.size() + COLUMNS - 1) / COLUMNS;
        int gridW = COLUMNS * CELL + (COLUMNS - 1) * GAP;
        int gridH = rows * CELL + (rows - 1) * GAP;
        int frameColor = selected >= 0 ? PlayerColors.rgb(selected) : ClientHelper.NEUTRAL_RGB;

        int x0 = gridX - 16;
        int y0 = gridY - 42;
        int x1 = gridX + gridW + 16;
        int y1 = gridY + gridH + 62;
        g.fill(x0 - 4, y0 - 4, x1 + 4, y1 + 4, 0xFF000000 | frameColor);
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF10161D);
        g.fill(x0, y0, x1, y1, 0xFF1B2733);

        g.drawCenteredString(font, Component.literal("Elige tu color").withStyle(ChatFormatting.BOLD), width / 2, y0 + 8, 0xFFFFFFFF);

        Minecraft mc = Minecraft.getInstance();
        String playerName = mc.player == null ? "Jugador" : mc.player.getGameProfile().getName();
        int nameColor = selected >= 0 ? PlayerColors.rgb(selected) : 0xFFFFFF;
        g.drawCenteredString(font, Component.literal(playerName).withStyle(s -> s.withColor(nameColor)), width / 2, y0 + 24, 0xFFFFFFFF);

        int hovered = cellAt(mouseX, mouseY);
        for (int i = 0; i < PlayerColors.ALL.size(); i++) {
            int x = gridX + (i % COLUMNS) * (CELL + GAP);
            int y = gridY + (i / COLUMNS) * (CELL + GAP);
            g.fill(x, y, x + CELL, y + CELL, 0xFF000000 | PlayerColors.ALL.get(i).rgb());
            if (i == selected) {
                GuiUtil.outline(g, x - 2, y - 2, x + CELL + 2, y + CELL + 2, 0xFFFFFFFF);
                GuiUtil.outline(g, x - 1, y - 1, x + CELL + 1, y + CELL + 1, 0xFF000000);
            } else if (i == hovered) {
                GuiUtil.outline(g, x - 1, y - 1, x + CELL + 1, y + CELL + 1, 0xFFAAAAAA);
            }
        }

        String label = hovered >= 0 ? PlayerColors.ALL.get(hovered).name()
                : (selected >= 0 ? "Color actual: " + PlayerColors.ALL.get(selected).name() : "Sin color");
        g.drawCenteredString(font, Component.literal(label), width / 2, gridY + gridH + 10, 0xFFCCCCCC);

        super.render(g, mouseX, mouseY, partialTick);
    }
}
