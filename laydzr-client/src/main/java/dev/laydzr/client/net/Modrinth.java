package dev.laydzr.client.net;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class Modrinth {
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    public record Project(String id, String title, String author, String desc, int downloads) {}

    private Modrinth() {}

    public static String mcVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft").get()
            .getMetadata().getVersion().getFriendlyString();
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    // Modrinth requires a descriptive User-Agent
    private static HttpRequest.Builder req(String url) {
        return HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "laydzr/laydzr-client/1.0.0");
    }

    public static CompletableFuture<List<Project>> search(String q, String mc) {
        String facets = "[[\"project_type:mod\"],[\"categories:fabric\"],[\"versions:" + mc + "\"]]";
        String url = "https://api.modrinth.com/v2/search?limit=30&index=" + (q.isBlank() ? "downloads" : "relevance")
            + "&query=" + enc(q) + "&facets=" + enc(facets);
        return HTTP.sendAsync(req(url).build(), BodyHandlers.ofString()).thenApply(r -> {
            List<Project> out = new ArrayList<>();
            for (JsonElement e : JsonParser.parseString(r.body()).getAsJsonObject().getAsJsonArray("hits")) {
                JsonObject o = e.getAsJsonObject();
                out.add(new Project(o.get("project_id").getAsString(), o.get("title").getAsString(),
                    o.get("author").getAsString(), o.get("description").getAsString(), o.get("downloads").getAsInt()));
            }
            return out;
        });
    }

    /** Downloads the newest Fabric build for this Minecraft version into /mods and verifies its SHA-512. */
    public static CompletableFuture<String> install(Project p, String mc) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = "https://api.modrinth.com/v2/project/" + p.id() + "/version?loaders="
                    + enc("[\"fabric\"]") + "&game_versions=" + enc("[\"" + mc + "\"]");
                JsonArray versions = JsonParser.parseString(HTTP.send(req(url).build(), BodyHandlers.ofString()).body()).getAsJsonArray();
                if (versions.isEmpty()) return "No Fabric build for " + mc;
                JsonArray files = versions.get(0).getAsJsonObject().getAsJsonArray("files");
                JsonObject f = files.get(0).getAsJsonObject();
                for (JsonElement e : files) if (e.getAsJsonObject().get("primary").getAsBoolean()) f = e.getAsJsonObject();

                Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
                Files.createDirectories(mods);
                Path dest = mods.resolve(f.get("filename").getAsString());
                HTTP.send(req(f.get("url").getAsString()).build(), BodyHandlers.ofFile(dest));

                String want = f.getAsJsonObject("hashes").get("sha512").getAsString();
                String got = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(Files.readAllBytes(dest)));
                if (!got.equalsIgnoreCase(want)) { Files.deleteIfExists(dest); return "Hash mismatch - download removed"; }
                return "Installed " + p.title() + " - restart the game (check its required dependencies)";
            } catch (Exception ex) {
                return "Install failed: " + ex.getMessage();
            }
        });
    }
}
