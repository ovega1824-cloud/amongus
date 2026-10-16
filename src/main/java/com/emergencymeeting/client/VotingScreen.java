package com.emergencymeeting.client;

import com.emergencymeeting.network.Packets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Menú de la tablet: "Who Is The Impostor?". Lista a los jugadores de la reunión, permite votar
 * (clic en el jugador y luego en la palomita verde) o saltar el voto, y muestra el resultado.
 */
public class VotingScreen extends Screen {
    private int px;
    private int py;
    private int pw;
    private int ph;
    private int cols;
    private int colW;
    private int rowH;
    private int listTop;

    private final List<UUID> shown = new ArrayList<>();
    private final Map<UUID, String> localNames = new HashMap<>();
    private UUID selected;
    private Button skipButton;

    public VotingScreen() {
        super(Component.literal("Who Is The Impostor?"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        pw = Math.min(width - 16, 400);
        ph = Math.min(height - 16, 270);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        buildList();

        int n = Math.max(1, shown.size());
        cols = Math.max(2, Math.min(5, (n + 7) / 8));
        int rows = (n + cols - 1) / cols;
        colW = (pw - 24 - (cols - 1) * 6) / cols;
        listTop = py + 30;
        int availH = ph - 30 - 38;
        rowH = Math.max(14, Math.min(26, availH / rows - 4));

        skipButton = addRenderableWidget(Button.builder(Component.literal("SKIP VOTE"), b -> sendSkip())
                .bounds(px + 12, py + ph - 28, 84, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Quitar tablet"), b -> removeTablet())
                .bounds(px + pw - 12 - 78, py + 7, 78, 14).build());
    }

    private void buildList() {
        shown.clear();
        localNames.clear();
        if (ClientState.active) {
            shown.addAll(ClientState.ids);
        } else {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                    shown.add(info.getProfile().getId());
                    localNames.put(info.getProfile().getId(), info.getProfile().getName());
                }
            }
        }
    }

    private String nameOf(UUID id) {
        String n = ClientState.names.get(id);
        if (n == null) {
            n = localNames.get(id);
        }
        return n == null ? "?" : n;
    }

    private int[] rowRect(int index) {
        int col = index % cols;
        int row = index / cols;
        int x = px + 12 + col * (colW + 6);
        int y = listTop + row * (rowH + 4);
        return new int[] {x, y, x + colW, y + rowH};
    }

    private boolean canVote() {
        UUID me = ClientHelper.myId();
        return ClientState.active && ClientState.phase == 1 && !ClientState.iVoted
                && me != null && ClientState.ids.contains(me);
    }

    // ------------------------------------------------------------------ acciones

    private void sendVote(UUID target) {
        if (!canVote()) {
            return;
        }
        Packets.CHANNEL.sendToServer(new Packets.Vote(false, target));
        ClientState.iVoted = true;
        selected = null;
    }

    private void sendSkip() {
        if (!canVote()) {
            return;
        }
        Packets.CHANNEL.sendToServer(new Packets.Vote(true, null));
        ClientState.iVoted = true;
        selected = null;
    }

    private void removeTablet() {
        ClientConfig.setShowTablet(false);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("Tablet quitada. Presiona [" + ModBusEvents.TABLET_KEY.getTranslatedKeyMessage().getString()
                            + "] para recuperarla."), true);
        }
        onClose();
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && canVote()) {
            for (int i = 0; i < shown.size(); i++) {
                UUID id = shown.get(i);
                int[] r = rowRect(i);
                int bs = rowH - 4;
                if (id.equals(selected)) {
                    int cancelX = r[2] - bs - 2;
                    int okX = cancelX - bs - 3;
                    if (inside(mx, my, okX, r[1] + 2, bs, bs)) {
                        sendVote(id);
                        return true;
                    }
                    if (inside(mx, my, cancelX, r[1] + 2, bs, bs)) {
                        selected = null;
                        return true;
                    }
                }
                if (mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3]) {
                    selected = id.equals(selected) ? null : id;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        int frame = ClientHelper.myColorRgb();
        g.fill(px - 5, py - 5, px + pw + 5, py + ph + 5, 0xFF000000 | frame);
        g.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xFF10161D);
        g.fill(px, py, px + pw, py + ph, 0xFF1B2733);

        g.drawString(font, Component.literal("Who Is The Impostor?").withStyle(ChatFormatting.BOLD),
                px + 12, py + 10, 0xFFFFFFFF, false);

        for (int i = 0; i < shown.size(); i++) {
            drawRow(g, i, shown.get(i));
        }

        skipButton.active = canVote();
        skipButton.visible = ClientState.phase != 2;

        drawFooter(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawRow(GuiGraphics g, int index, UUID id) {
        int[] r = rowRect(index);
        int rgb = ClientHelper.colorOf(id);
        boolean isSelected = id.equals(selected);
        boolean lightRow = rgb >= 0 && GuiUtil.luminance(rgb) < 100;
        int bg = lightRow ? 0xFFC9D3DD : 0xFF2E3E50;
        if (isSelected) {
            bg = lightRow ? 0xFFE6ECF2 : 0xFF46617F;
        }
        g.fill(r[0], r[1], r[2], r[3], bg);
        if (isSelected) {
            GuiUtil.outline(g, r[0], r[1], r[2], r[3], 0xFFFFD34D);
        }

        int head = rowH - 6;
        drawHead(g, id, r[0] + 3, r[1] + 3, head);

        int textColor = 0xFF000000 | (rgb < 0 ? 0xFFFFFF : rgb);
        int reserved = (isSelected && canVote()) ? (rowH - 4) * 2 + 10 : 26;
        int maxW = Math.max(8, colW - head - 10 - reserved);
        String name = font.plainSubstrByWidth(nameOf(id), maxW);
        g.drawString(font, name, r[0] + head + 8, r[1] + (rowH - 8) / 2, textColor, !lightRow);

        int bs = rowH - 4;
        if (isSelected && canVote()) {
            int cancelX = r[2] - bs - 2;
            int okX = cancelX - bs - 3;
            g.fill(okX, r[1] + 2, okX + bs, r[1] + 2 + bs, 0xFF2EA043);
            GuiUtil.check(g, okX + 2, r[1] + 4, bs - 4, 0xFFFFFFFF);
            g.fill(cancelX, r[1] + 2, cancelX + bs, r[1] + 2 + bs, 0xFFD13438);
            GuiUtil.cross(g, cancelX + 2, r[1] + 4, bs - 4, 0xFFFFFFFF);
        } else if (ClientState.phase == 2) {
            int c = ClientState.counts.getOrDefault(id, 0);
            String t = "x" + c;
            g.drawString(font, t, r[2] - font.width(t) - 5, r[1] + (rowH - 8) / 2, lightRow ? 0xFF222222 : 0xFFFFFFFF, false);
        } else if (ClientState.voted.contains(id)) {
            int s = Math.min(12, rowH - 6);
            g.fill(r[2] - s - 4, r[1] + (rowH - s) / 2, r[2] - 4, r[1] + (rowH - s) / 2 + s, 0xFF2EA043);
            GuiUtil.check(g, r[2] - s - 3, r[1] + (rowH - s) / 2 + 1, s - 2, 0xFFFFFFFF);
        }

        if (ClientState.phase == 2 && id.equals(ClientState.ejected)) {
            GuiUtil.cross(g, r[0] + 3, r[1] + 3, head, 0xFFE02020);
            GuiUtil.outline(g, r[0], r[1], r[2], r[3], 0xFFE02020);
        }
    }

    private void drawHead(GuiGraphics g, UUID id, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
        ResourceLocation skin = info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
        PlayerFaceRenderer.draw(g, skin, x, y, size);
    }

    private void drawFooter(GuiGraphics g) {
        int baseY = py + ph - 24;
        if (!ClientState.active) {
            g.drawCenteredString(font, Component.literal("No hay una reunión activa"), px + pw / 2, baseY, 0xFFAAAAAA);
            return;
        }
        if (ClientState.phase == 2) {
            Component msg;
            if (ClientState.ejected != null) {
                int rgb = ClientHelper.colorOf(ClientState.ejected);
                msg = Component.literal(ClientState.ejectedName).withStyle(s -> s.withColor(rgb < 0 ? 0xFFFFFF : rgb))
                        .append(Component.literal(" fue expulsado de la nave.").withStyle(ChatFormatting.WHITE));
            } else {
                msg = Component.literal("Nadie fue expulsado.");
            }
            g.drawCenteredString(font, msg, px + pw / 2, baseY, 0xFFFFFFFF);
            g.drawCenteredString(font, Component.literal("Saltaron el voto: " + ClientState.skipCount),
                    px + pw / 2, baseY + 11, 0xFFAAAAAA);
            return;
        }

        int secs = Math.max(0, (ClientState.ticksLeft + 19) / 20);
        String timer = "Voting Ends In: " + secs + "s";
        g.drawString(font, timer, px + pw - 12 - font.width(timer), baseY + 5, 0xFFFF5555, false);

        String hint;
        if (ClientState.iVoted) {
            hint = "Voto enviado";
        } else if (!canVote()) {
            hint = "No participas en esta reunión";
        } else if (selected != null) {
            hint = "Confirma tu voto";
        } else {
            hint = "Elige a quién expulsar";
        }
        g.drawString(font, hint, px + 104, baseY + 5, 0xFFCCCCCC, false);
    }
}
