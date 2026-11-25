package dev.laydzr.client.module;

import dev.laydzr.client.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.function.Function;

/** A draggable HUD element. x < 0 means "centered horizontally". */
public class HudModule {
    public final String name, desc;
    public boolean enabled = true;
    public int x, y, w = 60, h = 16;
    public int rx, ry; // resolved on-screen position
    private final Function<MinecraftClient, String> text;

    public HudModule(String name, String desc, int x, int y, Function<MinecraftClient, String> text) {
        this.name = name; this.desc = desc; this.x = x; this.y = y; this.text = text;
    }

    public HudModule(String name, String desc, int x, int y) {
        this(name, desc, x, y, null);
    }

    protected void place(MinecraftClient mc) {
        rx = x < 0 ? (mc.getWindow().getScaledWidth() - w) / 2 : x;
        ry = y;
    }

    public void render(DrawContext c, MinecraftClient mc) {
        String s = text.apply(mc);
        w = mc.textRenderer.getWidth(s) + 12;
        h = 16;
        place(mc);
        Theme.box(c, rx, ry, w, h, Theme.HUD_BG);
        c.fill(rx, ry + 3, rx + 2, ry + h - 3, Theme.ACCENT);
        c.drawText(mc.textRenderer, s, rx + 7, ry + 4, Theme.TEXT, true);
    }
}
