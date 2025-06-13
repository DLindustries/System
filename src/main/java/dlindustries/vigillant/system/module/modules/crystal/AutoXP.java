package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.ItemUseListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.MathUtils;
import dlindustries.vigillant.system.utils.MouseSimulation;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public final class AutoXP extends Module implements TickListener, ItemUseListener {
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 20, 1, 1);
	private final NumberSetting chance = new NumberSetting(EncryptedString.of("Chance"), 0, 100, 100, 1)
			.setDescription(EncryptedString.of("Randomization"));
	private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), true)
			.setDescription(EncryptedString.of("Makes the CPS hud think you're legit"));
	int clock;

	public AutoXP() {
		super(EncryptedString.of("Faster XP"),
				EncryptedString.of("Automatically throws XP bottles for you"),
				-1,
				Category.CRYSTAL);
		addSettings(delay, chance, clickSimulation);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(ItemUseListener.class, this);

		clock = 0;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(ItemUseListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.currentScreen != null)
			return;

		boolean dontThrow = clock != 0;

		int randomInt = MathUtils.randomInt(1, 100);

		if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE)
			return;

		if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS)
			return;

		if (dontThrow)
			clock--;

		if (!dontThrow && randomInt <= chance.getValueInt()) {
			if (clickSimulation.getValue())
				MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

			ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
			if (result.isAccepted() && result.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);

			clock = delay.getValueInt();
		}
	}

	@Override
	public void onItemUse(ItemUseEvent event) {
		if (mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE) {
			event.cancel();
		}
	}

}
