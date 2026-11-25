package dev.laydzr.client.module;

import dev.laydzr.client.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public final class SpecialModules {
    private SpecialModules() {}

    public static class Keystrokes extends HudModule {
        public Keystrokes() { super("Keystrokes", "WASD + mouse buttons", 6, 110); w = 58; h = 58; }

        @Override
        public void render(DrawContext c, MinecraftClient mc) {
            w = 58; h = 58; place(mc);
            key(c, mc, rx + 20, ry, 18, "W", mc.options.forwardKey.isPressed());
            key(c, mc, rx, ry + 20, 18, "A", mc.options.leftKey.isPressed());
            key(c, mc, rx + 20, ry + 20, 18, "S", mc.options.backKey.isPressed());
            key(c, mc, rx + 40, ry + 20, 18, "D", mc.options.rightKey.isPressed());
            key(c, mc, rx, ry + 40, 28, "LMB", mc.options.attackKey.isPressed());
            key(c, mc, rx + 30, ry + 40, 28, "RMB", mc.options.useKey.isPressed());
        }

        private void key(DrawContext c, MinecraftClient mc, int x, int y, int w, String label, boolean down) {
            Theme.box(c, x, y, w, 18, down ? 0xCC3D9BFF : Theme.HUD_BG);
            c.drawCenteredTextWithShadow(mc.textRenderer, label, x + w / 2, y + 5, down ? 0xFFFFFFFF : Theme.TEXT);
        }
    }

    public static class Armor extends HudModule {
        private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

        public Armor() { super("Armor Status", "Worn armor durability", 6, 176); w = 62; h = 72; }

        @Override
        public void render(DrawContext c, MinecraftClient mc) {
            w = 62; h = 72; place(mc);
            for (int i = 0; i < 4; i++) {
                int y = ry + i * 18;
                Theme.box(c, rx, y, w, 16, Theme.HUD_BG);
                c.fill(rx, y + 3, rx + 2, y + 13, Theme.ACCENT);
                if (mc.player == null) continue;
                ItemStack s = mc.player.getEquippedStack(SLOTS[i]);
                if (s.isEmpty()) continue;
                c.drawItem(s, rx + 4, y);
                if (s.isDamageable()) {
                    int left = s.getMaxDamage() - s.getDamage();
                    float f = left / (float) s.getMaxDamage();
                    int col = f > 0.5f ? Theme.TEXT : f > 0.2f ? 0xFFFFD25A : 0xFFFF5A5A;
                    c.drawText(mc.textRenderer, String.valueOf(left), rx + 24, y + 4, col, true);
                }
            }
        }
    }
}
