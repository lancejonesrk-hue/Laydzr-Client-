package dev.laydzr.client.module;

import dev.laydzr.client.Theme;
import dev.laydzr.client.net.Spotify;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Spotify "now playing" bar. Sits at the top-center of the screen by default. */
public class MusicModule extends HudModule {
    private static final Identifier ART = Identifier.of("laydzrclient", "album_art");
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private volatile String loadedArt = "", loading = "";
    private volatile boolean hasArt;

    public MusicModule() { super("Music", "Spotify now-playing bar", -1, 4); w = 200; h = 40; }

    private void ensureArt(MinecraftClient mc) {
        String url = Spotify.artUrl;
        if (url.isEmpty() || url.equals(loadedArt) || url.equals(loading)) return;
        loading = url;
        Thread t = new Thread(() -> {
            try {
                HttpResponse<byte[]> res = HTTP.send(HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(res.body()));
                NativeImage img = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
                for (int py = 0; py < 64; py++)
                    for (int px = 0; px < 64; px++)
                        img.setColorArgb(px, py, bi.getRGB(px * bi.getWidth() / 64, py * bi.getHeight() / 64));
                mc.execute(() -> {
                    mc.getTextureManager().registerTexture(ART,
                        new NativeImageBackedTexture(() -> "laydzr_album_art", img));
                    loadedArt = url; hasArt = true;
                });
            } catch (Exception e) {
                loadedArt = url; hasArt = false;
            }
        }, "Laydzr-AlbumArt");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void render(DrawContext c, MinecraftClient mc) {
        w = 200; h = 40; place(mc);
        ensureArt(mc);
        TextRenderer tr = mc.textRenderer;
        boolean live = Spotify.connected() && !Spotify.title.isEmpty();

        Theme.box(c, rx, ry, w, h, Theme.HUD_BG);
        c.fill(rx + 1, ry, rx + w - 1, ry + 1, Theme.ACCENT);

        // album art (64px texture drawn at half scale for a crisp 32px cover)
        if (live && hasArt) {
            c.getMatrices().pushMatrix();
            c.getMatrices().translate(rx + 4, ry + 4);
            c.getMatrices().scale(0.5f, 0.5f);
            c.drawTexture(RenderPipelines.GUI_TEXTURED, ART, 0, 0, 0f, 0f, 64, 64, 64, 64);
            c.getMatrices().popMatrix();
        } else {
            Theme.box(c, rx + 4, ry + 4, 32, 32, Theme.CARD);
            c.drawCenteredTextWithShadow(tr, "L", rx + 20, ry + 16, Theme.ACCENT);
        }

        String t1 = live ? Spotify.title : "Laydzr Music";
        String t2 = live ? Spotify.artist
            : Spotify.connected() ? Spotify.status : "Connect Spotify: Right Shift > Music";
        c.drawText(tr, tr.trimToWidth(t1, 128), rx + 42, ry + 8, Theme.TEXT, true);
        c.drawText(tr, tr.trimToWidth(t2, 128), rx + 42, ry + 19, Theme.MUTED, false);

        // animated equalizer
        long now = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            int bh = (live && Spotify.playing) ? 3 + (int) ((Math.sin(now / 180.0 + i * 1.7) + 1) * 4) : 2;
            int bx = rx + w - 22 + i * 4;
            c.fill(bx, ry + 20 - bh, bx + 3, ry + 20, Theme.ACCENT);
        }

        // progress bar
        if (live && Spotify.duration > 0) {
            float f = Math.min(1f, Spotify.progressNow() / (float) Spotify.duration);
            int bx = rx + 42, bw = w - 50;
            c.fill(bx, ry + h - 8, bx + bw, ry + h - 6, 0xFF2A3550);
            c.fill(bx, ry + h - 8, bx + (int) (bw * f), ry + h - 6, Theme.ACCENT);
        }
    }
}
