package dev.laydzr.client.ui;

import dev.laydzr.client.Theme;
import dev.laydzr.client.module.HudModule;
import dev.laydzr.client.module.ModuleManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {
    private HudModule drag;
    private int ox, oy;

    public HudEditScreen() { super(Text.literal("Edit HUD")); }

    @Override public boolean shouldPause() { return false; }

    // Everything is drawn in render() so it works regardless of who calls renderBackground.
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}

    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        c.fill(0, 0, width, height, 0x70050A14);
        super.render(c, mx, my, d);
        for (HudModule m : ModuleManager.ALL) {
            if (!m.enabled) continue;
            m.render(c, client);
            outline(c, m.rx - 2, m.ry - 2, m.w + 4, m.h + 4, m == drag ? Theme.ACCENT : 0x803D9BFF);
        }
        c.drawCenteredTextWithShadow(textRenderer, "Drag modules to move them - ESC to save and close",
            width / 2, height - 20, Theme.TEXT);
    }

    private static void outline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col);
        c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y + 1, x + 1, y + h - 1, col);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        for (int i = ModuleManager.ALL.size() - 1; i >= 0; i--) {
            HudModule m = ModuleManager.ALL.get(i);
            if (m.enabled && Theme.in(mx, my, m.rx, m.ry, m.w, m.h)) {
                drag = m; ox = mx - m.rx; oy = my - m.ry;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (drag == null) return super.mouseDragged(click, offsetX, offsetY);
        drag.x = Math.max(0, Math.min(width - drag.w, (int) click.x() - ox));
        drag.y = Math.max(0, Math.min(height - drag.h, (int) click.y() - oy));
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        drag = null;
        ModuleManager.save();
        return super.mouseReleased(click);
    }

    @Override public void removed() { ModuleManager.save(); }
}
