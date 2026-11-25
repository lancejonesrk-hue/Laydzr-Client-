package dev.laydzr.client.net;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Spotify Web API client using the PKCE login flow (no client secret needed). */
public final class Spotify {
    public static volatile String clientId = "", refresh = "", access = "";
    public static volatile String title = "", artist = "", artUrl = "", status = "Not connected";
    public static volatile boolean playing;
    public static volatile long progress, duration, polledAt, accessExpiry;

    private static final String REDIRECT = "http://127.0.0.1:8888/callback";
    private static final String SCOPES = "user-read-playback-state user-modify-playback-state";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final ScheduledExecutorService POOL = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Laydzr-Spotify"); t.setDaemon(true); return t;
    });
    private static String verifier = "";

    private Spotify() {}

    public static void init() {
        load();
        if (connected()) status = "Connecting...";
        POOL.scheduleWithFixedDelay(Spotify::poll, 2, 3, TimeUnit.SECONDS);
    }

    public static boolean connected() { return !refresh.isEmpty(); }

    public static long progressNow() {
        return playing ? Math.min(duration, progress + (System.currentTimeMillis() - polledAt)) : progress;
    }

    // ---- storage ----
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("laydzrclient-spotify.json"); }

    private static void load() {
        try {
            if (!Files.exists(file())) return;
            JsonObject o = JsonParser.parseString(Files.readString(file())).getAsJsonObject();
            clientId = o.get("clientId").getAsString();
            refresh = o.get("refresh").getAsString();
        } catch (Exception ignored) {}
    }

    private static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("clientId", clientId);
            o.addProperty("refresh", refresh);
            Files.writeString(file(), o.toString());
        } catch (Exception ignored) {}
    }

    public static void disconnect() {
        refresh = ""; access = ""; title = ""; artist = ""; artUrl = ""; playing = false;
        status = "Not connected";
        save();
    }

    // ---- login ----
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    public static void connect(String id) {
        clientId = id.trim();
        if (clientId.isEmpty()) { status = "Paste your Client ID first"; return; }
        try {
            byte[] rnd = new byte[64];
            new SecureRandom().nextBytes(rnd);
            verifier = B64.encodeToString(rnd);
            String challenge = B64.encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            Thread t = new Thread(Spotify::listen, "Laydzr-SpotifyLogin");
            t.setDaemon(true);
            t.start();
            Thread.sleep(150);
            String url = "https://accounts.spotify.com/authorize?response_type=code&client_id=" + enc(clientId)
                + "&scope=" + enc(SCOPES) + "&redirect_uri=" + enc(REDIRECT)
                + "&code_challenge_method=S256&code_challenge=" + challenge;
            Util.getOperatingSystem().open(URI.create(url));
            status = "Waiting for browser login...";
        } catch (Exception e) {
            status = "Error: " + e.getMessage();
        }
    }

    /** Tiny one-shot loopback listener that catches Spotify's redirect (java.net only). */
    private static void listen() {
        try (ServerSocket ss = new ServerSocket(8888, 1, InetAddress.getLoopbackAddress())) {
            ss.setSoTimeout(180_000);
            try (Socket s = ss.accept()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                String line = in.readLine();
                String code = null;
                int i = line == null ? -1 : line.indexOf("code=");
                if (i > 0) {
                    String q = line.substring(i + 5);
                    int sp = q.indexOf(' ');
                    if (sp >= 0) q = q.substring(0, sp);
                    code = URLDecoder.decode(q.split("&")[0], StandardCharsets.UTF_8);
                }
                byte[] body = ("<html><body style='font-family:sans-serif;background:#0b1220;color:#3d9bff;text-align:center;padding-top:80px'>"
                    + "<h2>Laydzr Client: " + (code != null ? "Spotify connected. You can close this tab." : "login cancelled.")
                    + "</h2></body></html>").getBytes(StandardCharsets.UTF_8);
                OutputStream out = s.getOutputStream();
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "
                    + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(body);
                out.flush();
                if (code == null) { status = "Login cancelled"; return; }
                boolean ok = token("client_id=" + enc(clientId) + "&grant_type=authorization_code&code=" + enc(code)
                    + "&redirect_uri=" + enc(REDIRECT) + "&code_verifier=" + enc(verifier));
                if (ok) { status = "Connected"; poll(); }
            }
        } catch (Exception e) {
            status = "Login failed: " + e.getMessage();
        }
    }

    private static boolean token(String form) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(BodyPublishers.ofString(form)).build();
        HttpResponse<String> res = HTTP.send(req, BodyHandlers.ofString());
        if (res.statusCode() != 200) { status = "Auth failed (" + res.statusCode() + ")"; return false; }
        JsonObject j = JsonParser.parseString(res.body()).getAsJsonObject();
        access = j.get("access_token").getAsString();
        accessExpiry = System.currentTimeMillis() + j.get("expires_in").getAsLong() * 1000 - 30_000;
        if (j.has("refresh_token")) refresh = j.get("refresh_token").getAsString();
        save();
        return true;
    }

    private static boolean ensureToken() throws Exception {
        if (!access.isEmpty() && System.currentTimeMillis() < accessExpiry) return true;
        return token("grant_type=refresh_token&refresh_token=" + enc(refresh) + "&client_id=" + enc(clientId));
    }

    private static HttpResponse<String> api(String method, String path) throws Exception {
        if (!connected() || !ensureToken()) return null;
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1" + path))
            .header("Authorization", "Bearer " + access)
            .method(method, BodyPublishers.noBody()).build();
        return HTTP.send(req, BodyHandlers.ofString());
    }

    // ---- playback state ----
    private static void poll() {
        if (!connected()) return;
        try {
            HttpResponse<String> r = api("GET", "/me/player");
            if (r == null) return;
            if (r.statusCode() == 204) { playing = false; title = ""; status = "Nothing playing - start Spotify"; return; }
            if (r.statusCode() != 200) { status = "Spotify error " + r.statusCode(); return; }
            JsonObject j = JsonParser.parseString(r.body()).getAsJsonObject();
            JsonElement item = j.get("item");
            if (item == null || item.isJsonNull()) { title = ""; status = "Nothing playing"; return; }
            JsonObject it = item.getAsJsonObject();
            StringBuilder names = new StringBuilder();
            for (JsonElement a : it.getAsJsonArray("artists")) {
                if (names.length() > 0) names.append(", ");
                names.append(a.getAsJsonObject().get("name").getAsString());
            }
            JsonArray imgs = it.getAsJsonObject("album").getAsJsonArray("images");
            artUrl = imgs.size() > 0 ? imgs.get(imgs.size() - 1).getAsJsonObject().get("url").getAsString() : "";
            artist = names.toString();
            duration = it.get("duration_ms").getAsLong();
            progress = j.get("progress_ms").getAsLong();
            playing = j.get("is_playing").getAsBoolean();
            polledAt = System.currentTimeMillis();
            title = it.get("name").getAsString();
            status = "Connected";
        } catch (Exception e) {
            status = "Offline";
        }
    }

    // ---- controls (Spotify Premium required) ----
    private static void control(String method, String path) {
        if (!connected()) return;
        POOL.execute(() -> {
            try { api(method, path); Thread.sleep(500); poll(); } catch (Exception ignored) {}
        });
    }

    public static void toggle() { control("PUT", playing ? "/me/player/pause" : "/me/player/play"); }
    public static void next() { control("POST", "/me/player/next"); }
    public static void previous() { control("POST", "/me/player/previous"); }
}
