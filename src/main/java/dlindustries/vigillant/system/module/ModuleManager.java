package dlindustries.vigillant.system.module;


import dlindustries.vigillant.system.event.events.ButtonListener;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;

import dlindustries.vigillant.system.module.modules.client.NameProtect;
import dlindustries.vigillant.system.module.modules.client.NineElevenPrevent;
import dlindustries.vigillant.system.module.modules.crystal.*;
import dlindustries.vigillant.system.module.modules.mace.*;
import dlindustries.vigillant.system.module.modules.mace.FireworkMacro;
import dlindustries.vigillant.system.module.modules.optimizer.*;
import dlindustries.vigillant.system.module.modules.pot.AutoPot;
import dlindustries.vigillant.system.module.modules.pot.AutoPotRefill;
import dlindustries.vigillant.system.module.modules.render.*;
import dlindustries.vigillant.system.module.modules.sword.*;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.utils.EncryptedString;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager implements ButtonListener {
	private final List<Module> modules = new ArrayList<>();

	public ModuleManager() {
		addModules();
		addKeybinds();
	}

	public void addModules() {
		add(new AimAssist());
		add(new TriggerBot());
		add(new AutoPot());
		add(new AutoPotRefill());
		add(new AutoWTap());

		add(new ShieldDisabler());
		add(new AutoJumpReset());
		add(new DoubleAnchor());
		add(new HoverTotem());
		add(new AnchorMacro());
		add(new AutoCrystal());
		add(new AutoDoubleHand());
		add(new dtapsetup());
		add(new AutoInventoryTotem());
		add(new TotemOffhand());
		add(new KeyPearl());
		add(new DhandMod());

		add(new BreachSwap());
		add(new DiveBomber());
		add(new FireworkMacro());
		add(new KeyWindCharge());
		add(new MisclickOptimizer());
		add(new AutoXP());
		add(new JumpOptimizer());
		add(new CrystalOptimizer());
		add(new NoMissDelay());
		add(new NoBreakDelay());
		add(new PackSpoof());
		add(new Sprint());
		add(new CameraOptimizer());
		add(new PlacementOptimizer());
		add(new HitOptimizer());
		add(new ShieldOptimizer());
		add(new HUD());
		add(new NoBounce());

		add(new TargetHud());
		add(new NameTags());
		add(new RenderBarrier());
		add(new SuperVision());
		add(new StorageEsp());
		add(new ClickGUI());
		add(new NameProtect());
		add(new NineElevenPrevent());

	}

	public List<Module> getEnabledModules() {
		return modules.stream()
				.filter(Module::isEnabled)
				.toList();
	}


	public List<Module> getModules() {
		return modules;
	}

	public void addKeybinds() {
		system.INSTANCE.getEventManager().add(ButtonListener.class, this);

		for (Module module : modules)
			module.addSetting(new KeybindSetting(EncryptedString.of("Keybind"), module.getKey(), true).setDescription(EncryptedString.of("Key to enabled the module")));
	}

	public List<Module> getModulesInCategory(Category category) {
		return modules.stream()
				.filter(module -> module.getCategory() == category)
				.toList();
	}

	@SuppressWarnings("unchecked")
	public <T extends Module> T getModule(Class<T> moduleClass) {
		return (T) modules.stream()
				.filter(moduleClass::isInstance)
				.findFirst()
				.orElse(null);
	}

	public void add(Module module) {
		modules.add(module);
	}

	@Override
	public void onButtonPress(ButtonEvent event) {
		if (event.button >= 179 && event.button <= 183 ||
				event.button == GLFW.GLFW_KEY_UNKNOWN ||
				NineElevenPrevent.teabag) {
			return;
		}

		modules.forEach(module -> {
			if (module.getKey() == event.button && event.action == GLFW.GLFW_PRESS) {
				module.toggle();
			}
		});
	}
}
