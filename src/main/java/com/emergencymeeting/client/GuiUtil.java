package com.emergencymeeting.client;

import net.minecraft.client.gui.GuiGraphics;

/** Pequeñas utilidades de dibujo que no dependen de ninguna fuente ni textura. */
public final class GuiUtil {
    private GuiUtil() {}

    public static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int thick, int color) {
        int steps = Math.max(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)), 1);
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            g.fill(x, y, x + thick, y + thick, color);
        }
    }

    public static void check(GuiGraphics g, int x, int y, int size, int color) {
        int t = Math.max(1, size / 6);
        line(g, x + size * 12 / 100, y + size * 52 / 100, x + size * 38 / 100, y + size * 78 / 100, t, color);
        line(g, x + size * 38 / 100, y + size * 78 / 100, x + size * 88 / 100, y + size * 20 / 100, t, color);
    }

    public static void cross(GuiGraphics g, int x, int y, int size, int color) {
        int t = Math.max(1, size / 6);
        line(g, x + size * 15 / 100, y + size * 15 / 100, x + size * 85 / 100, y + size * 85 / 100, t, color);
        line(g, x + size * 85 / 100, y + size * 15 / 100, x + size * 15 / 100, y + size * 85 / 100, t, color);
    }

    public static void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    public static int luminance(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }
}
