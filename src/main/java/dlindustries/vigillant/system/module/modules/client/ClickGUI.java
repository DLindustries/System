package dlindustries.vigillant.system.module.modules.client;

import dlindustries.vigillant.system.event.events.PacketReceiveListener;
import dlindustries.vigillant.system.gui.ClickGui;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import org.lwjgl.glfw.GLFW;

public final class ClickGUI extends Module implements PacketReceiveListener {
	// Existing color settings
	public static final NumberSetting red = new NumberSetting(EncryptedString.of("Red"), 0, 255, 10, 1);
	public static final NumberSetting green = new NumberSetting(EncryptedString.of("Green"), 0, 255, 10, 1);
	public static final NumberSetting blue = new NumberSetting(EncryptedString.of("Blue"), 0, 255, 50, 1);

	public static final NumberSetting alphaWindow = new NumberSetting(EncryptedString.of("Window Alpha"), 0, 255, 180, 1);

	// Enable breathing by default
	public static final BooleanSetting breathing = new BooleanSetting(EncryptedString.of("Breathing"), true)
			.setDescription(EncryptedString.of("System breathing theme"));

	public static final BooleanSetting background = new BooleanSetting(EncryptedString.of("Background"), true)
			.setDescription(EncryptedString.of("Renders the background of the Click Gui"));

	// NEW: Setting for background image
	public static final BooleanSetting backgroundImage = new BooleanSetting(EncryptedString.of("Background Image"), false)
			.setDescription(EncryptedString.of("Show the background image in ClickGUI"));

	public static final BooleanSetting customFont = new BooleanSetting(EncryptedString.of("Custom Font"), true);

	private final BooleanSetting preventClose = new BooleanSetting(EncryptedString.of("Prevent Close"), true)
			.setDescription(EncryptedString.of("For servers with freeze plugins"));

	public static final NumberSetting roundQuads = new NumberSetting(EncryptedString.of("Roundness"), 1, 10, 5, 1);
	public static final ModeSetting<AnimationMode> animationMode = new ModeSetting<>(EncryptedString.of("Animations"), AnimationMode.Normal, AnimationMode.class);
	public static final BooleanSetting antiAliasing = new BooleanSetting(EncryptedString.of("MSAA"), true)
			.setDescription(EncryptedString.of("Anti Aliasing | Smoother UI edges |"));

	public enum AnimationMode {
		Normal, Positive, Off;
	}

	public ClickGUI() {
		super(EncryptedString.of("System"),
				EncryptedString.of("Client developed by DL-industries"),
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				Category.CLIENT);

		// Force initial shadow blue values
		red.setValue(10);
		green.setValue(10);
		blue.setValue(50);

		// Add the new setting to the settings list
		addSettings(alphaWindow, breathing, background, backgroundImage, preventClose,
				roundQuads, animationMode, antiAliasing, customFont);
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketReceiveListener.class, this);
		system.INSTANCE.previousScreen = mc.currentScreen;

		if (system.INSTANCE.clickGui != null) {
			mc.setScreenAndRender(system.INSTANCE.clickGui);
		} else if (mc.currentScreen instanceof InventoryScreen) {
			system.INSTANCE.guiInitialized = true;
		}
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketReceiveListener.class, this);

		if (mc.currentScreen instanceof ClickGui) {
			system.INSTANCE.clickGui.close();
			mc.setScreenAndRender(system.INSTANCE.previousScreen);
			system.INSTANCE.clickGui.onGuiClose();
		} else if (mc.currentScreen instanceof InventoryScreen) {
			system.INSTANCE.guiInitialized = false;
		}
		super.onDisable();
	}

	@Override
	public void onPacketReceive(PacketReceiveEvent event) {
		if (system.INSTANCE.guiInitialized && event.packet instanceof OpenScreenS2CPacket && preventClose.getValue()) {
			event.cancel();
		}
	}

	private static int interpolateColor(int start, int end, float progress) {
		return (int)(start + (end - start) * progress);
	}
}