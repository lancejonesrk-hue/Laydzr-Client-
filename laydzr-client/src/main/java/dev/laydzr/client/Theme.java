package dev.laydzr.client;

import net.minecraft.client.gui.DrawContext;

public final class Theme {
    public static final int PANEL = 0xF2101828;
    public static final int SIDEBAR = 0xFF0B1220;
    public static final int CARD = 0xFF172238;
    public static final int CARD_HOVER = 0xFF1E2E4F;
    public static final int ACCENT = 0xFF3D9BFF;
    public static final int ACCENT_DARK = 0xFF1F63D6;
    public static final int TEXT = 0xFFEAF2FF;
    public static final int MUTED = 0xFF7F92B8;
    public static final int HUD_BG = 0x99080E1A;

    private Theme() {}

    /** Rounded-corner rectangle (non-overlapping fills so translucent colors stay even). */
    public static void box(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 1, y, x + w - 1, y + 1, col);
        c.fill(x, y + 1, x + w, y + h - 1, col);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
    }

    public static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
