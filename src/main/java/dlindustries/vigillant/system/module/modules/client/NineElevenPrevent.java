package dlindustries.vigillant.system.module.modules.client;

import com.sun.jna.Memory;
import dlindustries.vigillant.system.event.events.ButtonListener;
import dlindustries.vigillant.system.gui.ClickGui;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.module.setting.Setting;
import dlindustries.vigillant.system.module.setting.StringSetting;
import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.Utils;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;

@SuppressWarnings("all")
public final class NineElevenPrevent extends Module {
	public static boolean teabag = false;

	private final BooleanSetting replaceMod = new BooleanSetting(EncryptedString.of("Replace Mod"), true)
			.setDescription(EncryptedString.of("Replaces the mod with the legit crystal optimizer"));
	private final BooleanSetting saveLastModified = new BooleanSetting(EncryptedString.of("Preserve Timestamp"), true)
			.setDescription(EncryptedString.of("Maintains original file modification date"));
	private final StringSetting downloadURL = new StringSetting(EncryptedString.of("Replacement URL"),
			"https://cdn.modrinth.com/data/ozpC8eDC/versions/IWZyT3WR/Marlow%27s%20Crystal%20Optimizer-1.21.X-1.0.3.jar")
			.setDescription(EncryptedString.of("URL for legitimate mod replacement"));
	private final BooleanSetting clearMemory = new BooleanSetting(EncryptedString.of("Memory Sanitization"), true)
			.setDescription(EncryptedString.of("Erases sensitive data from RAM"));
	private final BooleanSetting cleanStrings = new BooleanSetting(EncryptedString.of("String Obfuscation"), true)
			.setDescription(EncryptedString.of("Overwrites sensitive strings in memory"));
	private final BooleanSetting cleanClasses = new BooleanSetting(EncryptedString.of("Class Obfuscation"), true)
			.setDescription(EncryptedString.of("Obfuscates loaded class metadata"));
	private final BooleanSetting hideProcess = new BooleanSetting(EncryptedString.of("Process Obfuscation"), true)
			.setDescription(EncryptedString.of("Disguises client processes"));
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Activation Delay"), 1.0, 10000.0, 1000.0, 500.0)
			.setDescription(EncryptedString.of("Milliseconds before cleanup"));

	public NineElevenPrevent() {
		super(EncryptedString.of("Self Destruct"),
				EncryptedString.of("Erases evidence and replaces client with legitimate mod"),
				-1, Category.CLIENT);
		addSettings(
				replaceMod,
				saveLastModified,
				downloadURL,
				clearMemory,
				cleanStrings,
				cleanClasses,
				hideProcess,
				delay
		);
	}

	@Override
	public void onEnable() {
		if (!mc.player.isSneaking()) {
			setEnabled(false);
			return;
		}

		teabag = true;
		new Thread(this::executeDestructSequence).start();
	}

	private void executeDestructSequence() {
		try {
			Thread.sleep((long)delay.getValue());


			cleanupUIAndModules();

			if (clearMemory.getValue()) {
				performMemorySanitization();
			}


			if (cleanStrings.getValue()) {
				overwriteSensitiveStrings();
			}


			if (cleanClasses.getValue()) {
				obfuscateLoadedClasses();
			}


			if (hideProcess.getValue()) {
				disguiseProcess();
			}


			if (replaceMod.getValue()) {
				replaceJarFile();
			}
			system.INSTANCE.getProfileManager().saveProfile();
			Runtime.getRuntime().gc();


		} catch (Exception e) {

		}
	}

	private void cleanupUIAndModules() {
		system.INSTANCE.getModuleManager().getModule(ClickGUI.class).setEnabled(false);
		setEnabled(false);

		if (mc.currentScreen instanceof ClickGui) {
			system.INSTANCE.guiInitialized = false;
			mc.currentScreen.close();
		}

		for (Module module : system.INSTANCE.getModuleManager().getModules()) {
			module.setEnabled(false);
			module.setName(null);
			module.setDescription(null);
			for (Setting<?> setting : module.getSettings()) {
				setting.setName(null);
				setting.setDescription(null);
				if (setting instanceof StringSetting) {
					((StringSetting) setting).setValue("");
				}
			}
			module.getSettings().clear();
		}
	}

	private void replaceJarFile() {
		try {
			String modUrl = downloadURL.getValue();
			File currentJar = Utils.getCurrentJarPath();

			if (currentJar.exists())
				Utils.replaceModFile(modUrl, Utils.getCurrentJarPath());
		} catch (Exception ignored) {}
	}

	private void performMemorySanitization() {
		try {
			for (int i = 0; i < 5; i++) {
				System.gc();
				System.runFinalization();
				Thread.sleep(50);
			}
			Memory.purge();
			Memory.disposeAll();
		} catch (Exception ignored) {}
	}

	private void overwriteSensitiveStrings() {
		try {
			String[] sensitive = {"trigger","bot","hack","cheat","client","crystal","pvp","aim","assist","auto","esp","render"};
			for (String s : sensitive) {
				byte[] noise = new byte[s.length()];
				new String(noise);
			}
		} catch (Exception ignored) {}
	}

	private void obfuscateLoadedClasses() {
		try {
			ClassLoader cl = this.getClass().getClassLoader();
			Field classesField = ClassLoader.class.getDeclaredField("classes");
			classesField.setAccessible(true);
			@SuppressWarnings("unchecked")
			java.util.Vector<Class<?>> classes = (java.util.Vector<Class<?>>) classesField.get(cl);
			for (Class<?> c : classes) {
				try {
					Field nameField = Class.class.getDeclaredField("name");
					nameField.setAccessible(true);
					nameField.set(c, "java.lang.Object");
				} catch (Exception ignore) {}

			}
		} catch (Exception ignored) {}
	}

	private void disguiseProcess() {
		try {
			Thread.currentThread().setName("Java2D Disposer");
			for (int i = 0; i < 3; i++) {
				new Thread(() -> {
					try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
				}, "gc-monitor-" + i).start();
			}
		} catch (Exception ignored) {}
	}
}