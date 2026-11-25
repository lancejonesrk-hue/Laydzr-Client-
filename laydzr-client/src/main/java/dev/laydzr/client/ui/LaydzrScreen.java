package dev.laydzr.client.ui;

import dev.laydzr.client.Theme;
import dev.laydzr.client.module.HudModule;
import dev.laydzr.client.module.ModuleManager;
import dev.laydzr.client.net.Modrinth;
import dev.laydzr.client.net.Spotify;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Main Laydzr menu: sidebar with Mods / Music / Modrinth tabs. */
public class LaydzrScreen extends Screen {
    private static final int SIDE = 112;
    private static final String MC = Modrinth.mcVersion();
    private static int tab = 0;

    private record Hit(int x, int y, int w, int h, Runnable r) {}

    private final List<Hit> hits = new ArrayList<>();
    private TextFieldWidget search, clientId;
    private List<Modrinth.Project> results = List.of();
    private String mrStatus = "Fabric mods for Minecraft " + MC;
    private int scroll, px, py, pw, ph;

    public LaydzrScreen() { super(Text.literal("Laydzr Client")); }

    @Override public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        pw = Math.min(width - 24, 520);
        ph = Math.min(height - 24, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        int cx = px + SIDE + 14, cw = pw - SIDE - 28;

        search = new TextFieldWidget(textRenderer, cx, py + 40, cw - 60, 18, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search mods on Modrinth..."));
        addDrawableChild(search);

        clientId = new TextFieldWidget(textRenderer, cx, py + 78, cw, 18, Text.literal("Client ID"));
        clientId.setMaxLength(64);
        clientId.setText(Spotify.clientId);
        clientId.setPlaceholder(Text.literal("Spotify Client ID"));
        addDrawableChild(clientId);

        applyTab();
        if (results.isEmpty()) doSearch();
    }

    private void applyTab() {
        search.setVisible(tab == 2);
        clientId.setVisible(tab == 1);
        setFocused(tab == 2 ? search : null);
    }

    private void doSearch() {
        mrStatus = "Searching...";
        scroll = 0;
        Modrinth.search(search.getText(), MC)
            .thenAccept(r -> client.execute(() -> { results = r; mrStatus = r.size() + " results for Minecraft " + MC; }))
            .exceptionally(e -> { client.execute(() -> mrStatus = "Search failed - check your connection"); return null; });
    }

    private void hit(int x, int y, int w, int h, Runnable r) { hits.add(new Hit(x, y, w, h, r)); }

    private void button(DrawContext c, int x, int y, int w, int h, String label, int mx, int my, Runnable r) {
        Theme.box(c, x, y, w, h, Theme.in(mx, my, x, y, w, h) ? Theme.ACCENT : Theme.ACCENT_DARK);
        c.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
        hit(x, y, w, h, r);
    }

    private void head(DrawContext c, String t, String sub, int x, int y) {
        c.drawText(textRenderer, t, x, y, Theme.TEXT, true);
        c.drawText(textRenderer, textRenderer.trimToWidth(sub, pw - SIDE - 28), x, y + 12, Theme.MUTED, false);
    }

    // The UI is painted in render() before the vanilla text fields so the fields sit on top of it.
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}

    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        drawUi(c, mx, my);
        super.render(c, mx, my, d);
    }

    private void drawUi(DrawContext c, int mx, int my) {
        hits.clear();
        c.fillGradient(0, 0, width, height, 0xD0060B16, 0xD00A1830);
        Theme.box(c, px, py, pw, ph, Theme.PANEL);
        c.fill(px + 1, py + 1, px + SIDE, py + ph - 1, Theme.SIDEBAR);
        c.fill(px + SIDE, py + 1, px + SIDE + 1, py + ph - 1, Theme.ACCENT_DARK);
        c.fill(px + 1, py, px + pw - 1, py + 2, Theme.ACCENT);

        c.getMatrices().pushMatrix();
        c.getMatrices().translate(px + 12, py + 12);
        c.getMatrices().scale(1.5f, 1.5f);
        c.drawText(textRenderer, "LAYDZR", 0, 0, Theme.ACCENT, true);
        c.getMatrices().popMatrix();
        c.drawText(textRenderer, "CLIENT", px + 13, py + 27, Theme.MUTED, false);

        String[] tabs = {"Mods", "Music", "Modrinth"};
        for (int i = 0; i < tabs.length; i++) {
            int ty = py + 52 + i * 24;
            boolean on = tab == i, hov = Theme.in(mx, my, px + 6, ty, SIDE - 12, 20);
            if (on) {
                Theme.box(c, px + 6, ty, SIDE - 12, 20, 0x403D9BFF);
                c.fill(px + 6, ty + 3, px + 8, ty + 17, Theme.ACCENT);
            } else if (hov) {
                Theme.box(c, px + 6, ty, SIDE - 12, 20, 0x20FFFFFF);
            }
            c.drawText(textRenderer, tabs[i], px + 16, ty + 6, on ? Theme.ACCENT : Theme.TEXT, false);
            final int t = i;
            hit(px + 6, ty, SIDE - 12, 20, () -> { tab = t; applyTab(); });
        }

        c.drawText(textRenderer, "v1.0 by laydzr", px + 12, py + ph - 42, Theme.MUTED, false);
        button(c, px + 8, py + ph - 28, SIDE - 16, 20, "Edit HUD", mx, my, () -> client.setScreen(new HudEditScreen()));

        switch (tab) {
            case 0 -> drawMods(c, mx, my);
            case 1 -> drawMusic(c, mx, my);
            default -> drawModrinth(c, mx, my);
        }
    }

    private void drawMods(DrawContext c, int mx, int my) {
        int cx = px + SIDE + 14, cw = pw - SIDE - 28;
        head(c, "Mods", "Click a card to toggle it. Use Edit HUD to drag modules around.", cx, py + 12);
        int cols = 3, gap = 8, w = (cw - gap * 2) / cols, h = 52;
        for (int i = 0; i < ModuleManager.ALL.size(); i++) {
            HudModule m = ModuleManager.ALL.get(i);
            int x = cx + (i % cols) * (w + gap), y = py + 44 + (i / cols) * (h + gap);
            Theme.box(c, x, y, w, h, Theme.in(mx, my, x, y, w, h) ? Theme.CARD_HOVER : Theme.CARD);
            if (m.enabled) c.fill(x + 2, y, x + w - 2, y + 1, Theme.ACCENT);
            c.drawText(textRenderer, m.name, x + 8, y + 8, Theme.TEXT, false);
            c.drawText(textRenderer, textRenderer.trimToWidth(m.desc, w - 16), x + 8, y + 21, Theme.MUTED, false);
            int sx = x + 8, sy = y + h - 15;
            Theme.box(c, sx, sy, 24, 9, m.enabled ? Theme.ACCENT_DARK : 0xFF2A3550);
            int kx = m.enabled ? sx + 14 : sx + 2;
            c.fill(kx, sy + 2, kx + 8, sy + 7, 0xFFFFFFFF);
            hit(x, y, w, h, () -> { m.enabled = !m.enabled; ModuleManager.save(); });
        }
    }

    private void drawMusic(DrawContext c, int mx, int my) {
        int cx = px + SIDE + 14, cw = pw - SIDE - 28;
        head(c, "Music", "Spotify bar shown at the top of your screen.", cx, py + 12);
        c.drawText(textRenderer, "Spotify Client ID", cx, py + 66, Theme.MUTED, false);
        button(c, cx, py + 104, 100, 20, "Connect", mx, my, () -> Spotify.connect(clientId.getText()));
        button(c, cx + 108, py + 104, 100, 20, "Disconnect", mx, my, Spotify::disconnect);
        c.drawText(textRenderer, "Status: " + Spotify.status, cx, py + 134, Theme.ACCENT, false);
        String[] help = {
            "1. Create an app at developer.spotify.com/dashboard",
            "2. Add this redirect URI: http://127.0.0.1:8888/callback",
            "3. Paste its Client ID above and press Connect",
            "Keys: [ previous   \\ play/pause   ] next   (needs Premium)"
        };
        for (int i = 0; i < help.length; i++)
            c.drawText(textRenderer, textRenderer.trimToWidth(help[i], cw), cx, py + 156 + i * 12, Theme.MUTED, false);
    }

    private static String fmt(int n) {
        return n >= 1_000_000 ? String.format("%.1fM", n / 1e6) : n >= 1000 ? (n / 1000) + "K" : String.valueOf(n);
    }

    private void drawModrinth(DrawContext c, int mx, int my) {
        int cx = px + SIDE + 14, cw = pw - SIDE - 28;
        head(c, "Modrinth", mrStatus, cx, py + 12);
        button(c, cx + cw - 54, py + 39, 54, 20, "Search", mx, my, this::doSearch);
        int top = py + 66, rowH = 34;
        int vis = Math.max(1, (py + ph - 8 - top) / rowH);
        for (int i = 0; i < vis && scroll + i < results.size(); i++) {
            Modrinth.Project p = results.get(scroll + i);
            int y = top + i * rowH;
            Theme.box(c, cx, y, cw, rowH - 4, Theme.in(mx, my, cx, y, cw, rowH - 4) ? Theme.CARD_HOVER : Theme.CARD);
            c.drawText(textRenderer, p.title(), cx + 8, y + 5, Theme.TEXT, false);
            c.drawText(textRenderer, "by " + p.author() + " - " + fmt(p.downloads()),
                cx + 14 + textRenderer.getWidth(p.title()), y + 5, Theme.MUTED, false);
            c.drawText(textRenderer, textRenderer.trimToWidth(p.desc(), cw - 76), cx + 8, y + 17, Theme.MUTED, false);
            button(c, cx + cw - 52, y + 6, 46, 18, "Install", mx, my, () -> {
                mrStatus = "Installing " + p.title() + "...";
                Modrinth.install(p, MC).thenAccept(msg -> client.execute(() -> mrStatus = msg));
            });
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        int mx = (int) click.x(), my = (int) click.y();
        for (Hit h : new ArrayList<>(hits)) {
            if (Theme.in(mx, my, h.x(), h.y(), h.w(), h.h())) { h.r().run(); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (tab == 2) {
            int vis = Math.max(1, (ph - 74) / 34);
            scroll = Math.max(0, Math.min(Math.max(0, results.size() - vis), scroll - (int) Math.signum(vAmount)));
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.getKeycode();
        if (tab == 2 && search.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            doSearch();
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT && !search.isFocused() && !clientId.isFocused()) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }
}
