package dlindustries.vigillant.system.module.setting;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class KeybindSetting extends Setting<KeybindSetting> {
	private int keyCode;
	private boolean listening;
	private final boolean moduleKey;
	private final int originalKey;

	public KeybindSetting(CharSequence name, int key, boolean moduleKey) {
		super(name);
		this.keyCode = key;
		this.originalKey = key;
		this.moduleKey = moduleKey;
	}

	public boolean isModuleKey() {
		return moduleKey;
	}

	public boolean isListening() {
		return listening;
	}

	public int getOriginalKey() {
		return originalKey;
	}

	public void setListening(boolean listening) {
		this.listening = listening;
	}

	public int getKey() {
		return keyCode;
	}

	public void setKey(int key) {
		if (!isMediaKey(key)) {
			this.keyCode = key;
		}
	}

	public void toggleListening() {
		this.listening = !listening;
	}

	public boolean isPressed() {
		if (isMediaKey(keyCode) || keyCode == GLFW.GLFW_KEY_UNKNOWN) {
			return false;
		}
		return GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
	}

	private boolean isMediaKey(int key) {
		return key >= 179 && key <= 183;
	}

	public boolean shouldIgnoreKey(int key) {
		return isMediaKey(key) || key == GLFW.GLFW_KEY_UNKNOWN;
	}
}