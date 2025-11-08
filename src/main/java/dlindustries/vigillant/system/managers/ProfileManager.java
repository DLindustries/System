package dlindustries.vigillant.system.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.*;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;

public final class ProfileManager {
	private final Gson g = new GsonBuilder().setPrettyPrinting().create();
	private final Path profileFolderPath;
	private final Path profilePath;
	private JsonObject profile;

	public ProfileManager() {
		String os = System.getProperty("os.name", "").toLowerCase();
		String base = os.contains("win") ? System.getenv("APPDATA") : System.getProperty("user.home");
		if (base == null || base.isBlank()) base = System.getProperty("user.home");
		profileFolderPath = Paths.get(base, ".minecraft", "systemconfig");
		profilePath = profileFolderPath.resolve("config.json");
	}

	public void loadProfile() {
		try {
			if (!Files.isRegularFile(profilePath)) return;
			String content = Files.readString(profilePath);
			if (content == null || content.isBlank()) return;
			profile = g.fromJson(content, JsonObject.class);
			if (profile == null) return;

			List<Module> modules = system.INSTANCE.getModuleManager().getModules();
			for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
				Module module = modules.get(moduleIndex);
				String moduleNameKey = GetModuleName(module);
				JsonElement moduleJson = null;
				if (moduleNameKey != null) moduleJson = profile.get(moduleNameKey);
				if ((moduleJson == null || !moduleJson.isJsonObject()) && profile.has(String.valueOf(moduleIndex)))
					moduleJson = profile.get(String.valueOf(moduleIndex));
				if (moduleJson == null || !moduleJson.isJsonObject()) continue;
				JsonObject moduleConfig = moduleJson.getAsJsonObject();

				JsonElement enabledJson = moduleConfig.get("enabled");
				if (enabledJson != null && enabledJson.isJsonPrimitive())
					module.setEnabled(enabledJson.getAsBoolean());

				List<Setting<?>> settings = module.getSettings();
				for (int settingIndex = 0; settingIndex < settings.size(); settingIndex++) {
					Setting<?> setting = settings.get(settingIndex);
					String settingNameKey = GetSettingName(setting);
					JsonElement settingJson = null;
					if (settingNameKey != null) settingJson = moduleConfig.get(settingNameKey);
					if (settingJson == null && moduleConfig.has(String.valueOf(settingIndex)))
						settingJson = moduleConfig.get(String.valueOf(settingIndex));
					if (settingJson == null) continue;

					if (setting instanceof BooleanSetting s && settingJson.isJsonPrimitive())
						s.setValue(settingJson.getAsBoolean());
					else if (setting instanceof ModeSetting<?> s && settingJson.isJsonPrimitive())
						s.setModeIndex(settingJson.getAsInt());
					else if (setting instanceof NumberSetting s && settingJson.isJsonPrimitive())
						s.setValue(settingJson.getAsDouble());
					else if (setting instanceof KeybindSetting s && settingJson.isJsonPrimitive()) {
						int key = settingJson.getAsInt();
						s.setKey(key);
						if (s.isModuleKey()) module.setKey(key);
					} else if (setting instanceof StringSetting s && settingJson.isJsonPrimitive())
						s.setValue(settingJson.getAsString());
					else if (setting instanceof MinMaxSetting s && settingJson.isJsonObject()) {
						JsonObject o = settingJson.getAsJsonObject();
						Double min = null, max = null;
						if (o.has("min") && o.get("min").isJsonPrimitive()) min = o.get("min").getAsDouble();
						if (o.has("max") && o.get("max").isJsonPrimitive()) max = o.get("max").getAsDouble();
						if (min == null && o.has("1") && o.get("1").isJsonPrimitive()) min = o.get("1").getAsDouble();
						if (max == null && o.has("2") && o.get("2").isJsonPrimitive()) max = o.get("2").getAsDouble();
						if (min != null) s.setMinValue(min);
						if (max != null) s.setMaxValue(max);
					} else if (settingJson.isJsonPrimitive()) {
						try {
							Object val = settingJson.getAsString();
							try { setting.getClass().getMethod("setValue", String.class).invoke(setting, val); } catch (Throwable ignored) {}
						} catch (Throwable ignored) {}
					}
				}
			}
		} catch (Exception ignored) {}
	}

	public void saveProfile() {
		try {
			Files.createDirectories(profileFolderPath);
			if (Files.isRegularFile(profilePath))
				try { Files.copy(profilePath, profileFolderPath.resolve("config.json.bak"), StandardCopyOption.REPLACE_EXISTING); } catch (Exception ignored) {}

			profile = new JsonObject();
			List<Module> modules = system.INSTANCE.getModuleManager().getModules();

			for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
				Module module = modules.get(moduleIndex);
				JsonObject moduleConfig = new JsonObject();
				moduleConfig.addProperty("enabled", module.isEnabled());

				List<Setting<?>> settings = module.getSettings();
				for (int settingIndex = 0; settingIndex < settings.size(); settingIndex++) {
					Setting<?> setting = settings.get(settingIndex);
					String settingKey = GetSettingName(setting);
					String indexKey = String.valueOf(settingIndex);

					if (setting instanceof BooleanSetting s) {
						if (settingKey != null) moduleConfig.addProperty(settingKey, s.getValue());
						moduleConfig.addProperty(indexKey, s.getValue());
					} else if (setting instanceof ModeSetting<?> s) {
						if (settingKey != null) moduleConfig.addProperty(settingKey, s.getModeIndex());
						moduleConfig.addProperty(indexKey, s.getModeIndex());
					} else if (setting instanceof NumberSetting s) {
						if (settingKey != null) moduleConfig.addProperty(settingKey, s.getValue());
						moduleConfig.addProperty(indexKey, s.getValue());
					} else if (setting instanceof KeybindSetting s) {
						if (settingKey != null) moduleConfig.addProperty(settingKey, s.getKey());
						moduleConfig.addProperty(indexKey, s.getKey());
					} else if (setting instanceof StringSetting s) {
						if (settingKey != null) moduleConfig.addProperty(settingKey, s.getValue());
						moduleConfig.addProperty(indexKey, s.getValue());
					} else if (setting instanceof MinMaxSetting s) {
						JsonObject o = new JsonObject();
						o.addProperty("min", s.getMinValue());
						o.addProperty("max", s.getMaxValue());
						o.addProperty("1", s.getMinValue());
						o.addProperty("2", s.getMaxValue());
						if (settingKey != null) moduleConfig.add(settingKey, o);
						moduleConfig.add(indexKey, o);
					} else {
						try {
							Object val = setting.getClass().getMethod("getValue").invoke(setting);
							if (val != null) {
								if (settingKey != null) moduleConfig.addProperty(settingKey, Objects.toString(val));
								moduleConfig.addProperty(indexKey, Objects.toString(val));
							}
						} catch (Throwable ignored) {}
					}
				}

				String moduleKey = GetModuleName(module);
				if (moduleKey != null) profile.add(moduleKey, moduleConfig);
				profile.add(String.valueOf(moduleIndex), moduleConfig);
			}

			Files.writeString(profilePath, g.toJson(profile), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (Exception ignored) {}
	}

	private String GetModuleName(Module module) {
		if (module == null) return null;
		try {
			CharSequence name = module.getName();
			if (name != null) return name.toString();
		} catch (Throwable ignored) {}
		try { return module.getClass().getSimpleName(); } catch (Throwable ignored) {}
		return null;
	}

	private String GetSettingName(Setting<?> setting) {
		if (setting == null) return null;
		try {
			CharSequence name = setting.getName();
			if (name != null) return name.toString();
		} catch (Throwable ignored) {}
		return null;
	}
}
