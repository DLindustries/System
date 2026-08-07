package dlindustries.vigillant.system.module.modules.client;

import dlindustries.vigillant.system.event.events.PacketReceiveListener;
import dlindustries.vigillant.system.gui.ClickGui;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.VigillantSystem;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import org.lwjgl.glfw.GLFW;

public final class ClickGUI extends Module implements PacketReceiveListener {
	public static final NumberSetting red = new NumberSetting(EncryptedString.of("Red"), 0, 255, 10, 1);
	public static final NumberSetting green = new NumberSetting(EncryptedString.of("Green"), 0, 255, 10, 1);
	public static final NumberSetting blue = new NumberSetting(EncryptedString.of("Blue"), 0, 255, 50, 1);

	public enum Theme {
		PURPLE("Purple"),
		RED("Red"),
		PINK("Pink"),
		BLUE("Light blue"),
		GREEN("Green"),
		YELLOW("Yellow"),
		WHITE("White"),
		RAINBOW("Rainbow");

		private final String name;

		Theme(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static final ModeSetting<Theme> theme = new ModeSetting<>(
			EncryptedString.of("Theme"),
			Theme.PURPLE,
			Theme.class).setDescription(EncryptedString.of("Color theme for the GUI and ESP"));
	public static final NumberSetting alphaWindow = new NumberSetting(EncryptedString.of("Window Alpha"), 0, 255, 255,
			1);
	public static final BooleanSetting breathing = new BooleanSetting(EncryptedString.of("Breathing"), true)
			.setDescription(EncryptedString.of("System breathing theme"));
	public static final BooleanSetting background = new BooleanSetting(EncryptedString.of("Background"), true)
			.setDescription(EncryptedString.of("Renders the background of the Click Gui"));
	public static final NumberSetting backgroundAlpha = new NumberSetting(EncryptedString.of("Background Alpha"), 0,
			255, 220, 1)
			.setDescription(EncryptedString.of("Alpha of the background overlay"));
	public static final BooleanSetting backgroundImage = new BooleanSetting(EncryptedString.of("Background Image"),
			true)
			.setDescription(EncryptedString.of("Show the background image in ClickGUI"));
	public static final BooleanSetting blur = new BooleanSetting(EncryptedString.of("Blur"), true)
			.setDescription(EncryptedString.of("Blurs the background behind the ClickGUI"));
	private final BooleanSetting preventClose = new BooleanSetting(EncryptedString.of("Prevent Close"), true)
			.setDescription(EncryptedString.of("For servers with freeze plugins"));
	public static final NumberSetting roundQuads = new NumberSetting(EncryptedString.of("Roundness"), 0, 15, 5, 1);
	public static final ModeSetting<AnimationMode> animationMode = new ModeSetting<>(EncryptedString.of("Animations"),
			AnimationMode.Normal, AnimationMode.class);
	public static final BooleanSetting antiAliasing = new BooleanSetting(EncryptedString.of("MSAA"), true)
			.setDescription(EncryptedString.of("Anti Aliasing | Smoother UI edges |"));

	public enum AnimationMode {
		Normal, Positive, Off;
	}

	public ClickGUI() {
		super(EncryptedString.of("Regen"),
				EncryptedString.of(
						"Improved fork from System Client, dedicated for Vanilla, Spear and Mace Pvp while bypassing all modern anticheat solutions"),
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				Category.CLIENT);
		red.setValue(10);
		green.setValue(10);
		blue.setValue(50);
		addSettings(alphaWindow, breathing, background, backgroundAlpha, backgroundImage, blur, theme,
				preventClose, roundQuads, animationMode, antiAliasing);
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketReceiveListener.class, this);
		VigillantSystem.INSTANCE.previousScreen = mc.currentScreen;
		if (VigillantSystem.INSTANCE.clickGui != null) {
			mc.setScreenAndRender(VigillantSystem.INSTANCE.clickGui);
		} else if (mc.currentScreen instanceof InventoryScreen) {
			VigillantSystem.INSTANCE.guiInitialized = true;
		}
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketReceiveListener.class, this);
		if (mc.currentScreen instanceof ClickGui) {
			VigillantSystem.INSTANCE.clickGui.onGuiClose();
		} else if (mc.currentScreen instanceof InventoryScreen) {
			VigillantSystem.INSTANCE.guiInitialized = false;
		}
		super.onDisable();
	}

	@Override
	public void onPacketReceive(PacketReceiveEvent event) {
		if (VigillantSystem.INSTANCE.guiInitialized && event.packet instanceof OpenScreenS2CPacket
				&& preventClose.getValue()) {
			event.cancel();
		}
	}

	private static int interpolateColor(int start, int end, float progress) {
		return (int) (start + (end - start) * progress);
	}
}