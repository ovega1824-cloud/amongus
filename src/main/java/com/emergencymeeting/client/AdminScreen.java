package com.emergencymeeting.client;

import com.emergencymeeting.network.Packets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Menú de la tablet de administrador: configura las reuniones. */
public class AdminScreen extends Screen {
    private int duration;
    private boolean endEarly;
    private int cooldown;

    private EditBox durationBox;
    private EditBox cooldownBox;
    private Button toggleButton;

    public AdminScreen(int duration, boolean endEarly, int cooldown) {
        super(Component.literal("Tablet de administrador"));
        this.duration = duration;
        this.endEarly = endEarly;
        this.cooldown = cooldown;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        durationBox = new EditBox(font, cx - 40, cy - 38, 80, 18, Component.literal("Duración"));
        durationBox.setFilter(s -> s.matches("\\d{0,3}"));
        durationBox.setValue(String.valueOf(duration));
        addRenderableWidget(durationBox);

        addRenderableWidget(Button.builder(Component.literal("-10"), b -> adjust(-10))
                .bounds(cx - 72, cy - 38, 28, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), b -> adjust(10))
                .bounds(cx + 44, cy - 38, 28, 18).build());

        toggleButton = addRenderableWidget(Button.builder(toggleText(), b -> {
            endEarly = !endEarly;
            b.setMessage(toggleText());
        }).bounds(cx - 100, cy - 10, 200, 20).build());

        cooldownBox = new EditBox(font, cx - 40, cy + 30, 80, 18, Component.literal("Enfriamiento"));
        cooldownBox.setFilter(s -> s.matches("\\d{0,4}"));
        cooldownBox.setValue(String.valueOf(cooldown));
        addRenderableWidget(cooldownBox);

        addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> save())
                .bounds(cx - 100, cy + 62, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(cx + 2, cy + 62, 98, 20).build());
    }

    private Component toggleText() {
        return Component.literal("Terminar al votar todos: " + (endEarly ? "SI" : "NO"));
    }

    private int parse(EditBox box, int fallback) {
        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void adjust(int delta) {
        int value = Math.max(5, Math.min(900, parse(durationBox, duration) + delta));
        durationBox.setValue(String.valueOf(value));
    }

    private void save() {
        int d = Math.max(5, Math.min(900, parse(durationBox, duration)));
        int c = Math.max(0, Math.min(3600, parse(cooldownBox, cooldown)));
        Packets.CHANNEL.sendToServer(new Packets.SaveConfig(d, endEarly, c));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int cx = width / 2;
        int cy = height / 2;
        g.fill(cx - 120, cy - 88, cx + 120, cy + 94, 0xFF3A4654);
        g.fill(cx - 117, cy - 85, cx + 117, cy + 91, 0xFF1B2733);

        g.drawCenteredString(font, Component.literal("Tablet de administrador").withStyle(ChatFormatting.BOLD),
                cx, cy - 78, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.literal("Duración de cada reunión (segundos)"), cx, cy - 52, 0xFFCCCCCC);
        g.drawCenteredString(font, Component.literal("Enfriamiento del botón (segundos)"), cx, cy + 18, 0xFFCCCCCC);

        super.render(g, mouseX, mouseY, partialTick);
    }
}
