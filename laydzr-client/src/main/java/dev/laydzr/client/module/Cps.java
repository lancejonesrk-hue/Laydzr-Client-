package dev.laydzr.client.module;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayDeque;

public final class Cps {
    private static final ArrayDeque<Long> L = new ArrayDeque<>(), R = new ArrayDeque<>();
    private static boolean lw, rw;

    private Cps() {}

    public static void tick(MinecraftClient mc) {
        long win = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (mc.currentScreen == null) {
            if (l && !lw) L.addLast(now);
            if (r && !rw) R.addLast(now);
        }
        lw = l; rw = r;
        while (!L.isEmpty() && now - L.peekFirst() > 1000) L.pollFirst();
        while (!R.isEmpty() && now - R.peekFirst() > 1000) R.pollFirst();
    }

    public static int left() { return L.size(); }
    public static int right() { return R.size(); }
}
