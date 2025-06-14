package dlindustries.vigillant.system.module.modules.client;

import dlindustries.vigillant.system.gui.ClickGui;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.Setting;
import dlindustries.vigillant.system.module.setting.StringSetting;
import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.Utils;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("all")
public final class SelfDestruct extends Module {
	public static boolean destruct = false;

	private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), GLFW.GLFW_KEY_DELETE, false);


	private final BooleanSetting replaceMod = new BooleanSetting(EncryptedString.of("Replace Mod"), true)
			.setDescription(EncryptedString.of("Replaces the mod with the original JAR file"));

	private final BooleanSetting saveLastModified = new BooleanSetting(EncryptedString.of("Save Last Modified"), true)
			.setDescription(EncryptedString.of("Saves the last modified date after self-destruct"));

	private final StringSetting downloadURL = new StringSetting(EncryptedString.of("Replace URL"),
			"https://cdn.modrinth.com/data/5ZwdcRci/versions/UDgEo3mK/ImmediatelyFast-Fabric-1.6.5%2B1.21.1.jar");

	public SelfDestruct() {
		super(EncryptedString.of("Self Destruct"),
				EncryptedString.of("Kills the system"),
				-1,  // Placeholder (real keybind is handled by destructKey)
				Category.CLIENT);
		addSettings(activateKey, replaceMod, saveLastModified, downloadURL);
	}

	@Override
	public void onEnable() {
		// Only trigger if player is sneaking (holding SHIFT)
		if (!mc.player.isSneaking()) {
			setEnabled(false);
			return;
		}

		destruct = true;
		system.INSTANCE.getModuleManager().getModule(ClickGUI.class).setEnabled(false);
		setEnabled(false);

		// Rest of your self-destruct logic...
		system.INSTANCE.getProfileManager().saveProfile();

		if (mc.currentScreen instanceof ClickGui) {
			system.INSTANCE.guiInitialized = false;
			mc.currentScreen.close();
		}

		if (replaceMod.getValue()) {
			try {
				Utils.replaceModFile(downloadURL.getValue(), Utils.getCurrentJarPath());
			} catch (Exception ignored) {}
		}

		// Clear modules
		for (Module module : system.INSTANCE.getModuleManager().getModules()) {
			module.setEnabled(false);
			module.setName(null);
			module.setDescription(null);
			for (Setting<?> setting : module.getSettings()) {
				setting.setName(null);
				setting.setDescription(null);
				if (setting instanceof StringSetting set) set.setValue(null);
			}
			module.getSettings().clear();
		}

		// Cleanup
		if (saveLastModified.getValue()) system.INSTANCE.resetModifiedDate();
		Runtime.getRuntime().gc();
	}
}