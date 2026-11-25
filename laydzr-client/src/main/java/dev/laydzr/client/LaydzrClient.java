package dev.laydzr.client;

import dev.laydzr.client.module.Cps;
import dev.laydzr.client.module.HudModule;
import dev.laydzr.client.module.ModuleManager;
import dev.laydzr.client.net.Spotify;
import dev.laydzr.client.ui.HudEditScreen;
import dev.laydzr.client.ui.LaydzrScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class LaydzrClient implements ClientModInitializer {
    // Translation key: key.category.laydzrclient.main
    private static final KeyBinding.Category CATEGORY =
        KeyBinding.Category.create(Identifier.of("laydzrclient", "main"));

    private static KeyBinding reg(String key, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(key, InputUtil.Type.KEYSYM, code, CATEGORY));
    }

    @Override
    public void onInitializeClient() {
        ModuleManager.load();
        Spotify.init();

        KeyBinding menu = reg("key.laydzrclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        KeyBinding prev = reg("key.laydzrclient.prev", GLFW.GLFW_KEY_LEFT_BRACKET);
        KeyBinding play = reg("key.laydzrclient.play", GLFW.GLFW_KEY_BACKSLASH);
        KeyBinding next = reg("key.laydzrclient.next", GLFW.GLFW_KEY_RIGHT_BRACKET);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menu.wasPressed()) mc.setScreen(new LaydzrScreen());
            while (prev.wasPressed()) Spotify.previous();
            while (play.wasPressed()) Spotify.toggle();
            while (next.wasPressed()) Spotify.next();
        });

        HudElementRegistry.addLast(Identifier.of("laydzrclient", "hud"), (ctx, tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            Cps.tick(mc); // polled per frame so fast clicking is counted accurately
            if (mc.options.hudHidden || mc.currentScreen instanceof HudEditScreen) return;
            for (HudModule m : ModuleManager.ALL) if (m.enabled) m.render(ctx, mc);
        });
    }
}
