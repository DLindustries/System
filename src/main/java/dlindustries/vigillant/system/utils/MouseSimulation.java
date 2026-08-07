package dlindustries.vigillant.system.utils;

import dlindustries.vigillant.system.mixin.MinecraftClientAccessor;
import dlindustries.vigillant.system.mixin.MouseHandlerAccessor;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dlindustries.vigillant.system.VigillantSystem.mc;


public final class MouseSimulation {
	public static HashMap<Integer, Boolean> mouseButtons = new HashMap<>();
	public static ExecutorService clickExecutor = Executors.newFixedThreadPool(100);

	public static MouseHandlerAccessor getMouseHandler() {
		return (MouseHandlerAccessor) ((MinecraftClientAccessor) mc).getMouse();
	}

	public static boolean isMouseButtonPressed(int keyCode) {
		Boolean key = mouseButtons.get(keyCode);
		return key != null ? key : false;
	}

	public static void mousePress(int keyCode) {
		mouseButtons.put(keyCode, true);
		getMouseHandler().press(mc.getWindow().getHandle(), new MouseInput(keyCode, 0), GLFW.GLFW_PRESS);
	}

	public static void mouseRelease(int keyCode) {
		mouseButtons.put(keyCode, false);
		getMouseHandler().press(mc.getWindow().getHandle(), new MouseInput(keyCode, 0), GLFW.GLFW_RELEASE);
	}

	public static void mouseClick(int keyCode, int millis) {
		clickExecutor.submit(() -> {
			try {
				MouseSimulation.mousePress(keyCode);
				Thread.sleep(millis);
				MouseSimulation.mouseRelease(keyCode);
			} catch (InterruptedException ignored) {

			}
		});
	}

	public static void mouseClick(int keyCode) {
		mouseClick(keyCode, 35);
	}
}
