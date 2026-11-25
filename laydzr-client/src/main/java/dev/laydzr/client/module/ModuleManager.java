package dev.laydzr.client.module;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<HudModule> ALL = new ArrayList<>();

    static {
        ALL.add(new MusicModule());
        ALL.add(new HudModule("FPS", "Frames per second", 6, 6, mc -> mc.getCurrentFps() + " FPS"));
        ALL.add(new HudModule("CPS", "Clicks per second", 6, 26, mc -> Cps.left() + " | " + Cps.right() + " CPS"));
        ALL.add(new HudModule("Ping", "Server latency", 6, 46, ModuleManager::ping));
        ALL.add(new HudModule("Coordinates", "Your XYZ position", 6, 66, ModuleManager::coords));
        ALL.add(new HudModule("Clock", "Real-world time", 6, 86,
            mc -> LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))));
        ALL.add(new SpecialModules.Keystrokes());
        ALL.add(new SpecialModules.Armor());
    }

    private ModuleManager() {}

    private static String ping(MinecraftClient mc) {
        if (mc.player == null || mc.getNetworkHandler() == null) return "-- ms";
        var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return (e == null ? 0 : e.getLatency()) + " ms";
    }

    private static String coords(MinecraftClient mc) {
        if (mc.player == null) return "XYZ: 0 / 0 / 0";
        return String.format("XYZ: %.0f / %.0f / %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("laydzrclient.json");
    }

    public static void load() {
        try {
            if (!Files.exists(file())) return;
            JsonObject o = JsonParser.parseString(Files.readString(file())).getAsJsonObject();
            for (HudModule m : ALL) {
                if (!o.has(m.name)) continue;
                JsonObject j = o.getAsJsonObject(m.name);
                m.enabled = j.get("on").getAsBoolean();
                m.x = j.get("x").getAsInt();
                m.y = j.get("y").getAsInt();
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            JsonObject o = new JsonObject();
            for (HudModule m : ALL) {
                JsonObject j = new JsonObject();
                j.addProperty("on", m.enabled);
                j.addProperty("x", m.x);
                j.addProperty("y", m.y);
                o.add(m.name, j);
            }
            Files.writeString(file(), o.toString());
        } catch (Exception ignored) {}
    }
}
