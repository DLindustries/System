package dlindustries.vigillant.system.module.modules.pot;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.KeyUtils;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class AutoPot extends Module implements TickListener {
	private final KeybindSetting activateKey = new KeybindSetting(
			EncryptedString.of("Activate Key"),
			-1,
			false
	).setDescription(EncryptedString.of("Key that activates auto-potting when held"));
	private final NumberSetting minHealth = new NumberSetting(EncryptedString.of("Min Health"), 1, 20, 10, 1);
	private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 10, 0, 1);
	private final NumberSetting throwDelay = new NumberSetting(EncryptedString.of("Throw Delay"), 0, 10, 0, 1);
	private final BooleanSetting goToPrevSlot = new BooleanSetting(EncryptedString.of("Switch Back"), true);
	private final BooleanSetting lookDown = new BooleanSetting(EncryptedString.of("Look Down"), true);

	private int switchClock, throwClock, prevSlot;
	private float prevPitch;
	private boolean potting;

	public AutoPot() {
		super(
				EncryptedString.of("Auto Pot"),
				EncryptedString.of("Automatically throws health potions when low on health while key is held"),
				-1,
				Category.pot
		);
		addSettings(activateKey, minHealth, switchDelay, throwDelay, goToPrevSlot, lookDown);
		resetState();
	}

	private void resetState() {
		switchClock = 0;
		throwClock = 0;
		prevSlot = -1;
		prevPitch = -1;
		potting = false;
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		resetState();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (activateKey.getKey() != -1 && !KeyUtils.isKeyPressed(activateKey.getKey())) {
			potting = false;
			return;
		}
		potting = true;

		if (mc.currentScreen != null) return;
		if (mc.player.getHealth() <= minHealth.getValueFloat() || potting) {
			if (potting && mc.player.getHealth() >= mc.player.getMaxHealth()) {
				potting = false;
				return;
			}
			if (!InventoryUtils.isThatSplash(StatusEffects.INSTANT_HEALTH.value(), 1, 1, mc.player.getMainHandStack())) {
				if (switchClock < switchDelay.getValueInt()) {
					switchClock++;
					return;
				}

				if (goToPrevSlot.getValue() && prevSlot == -1) prevSlot = mc.player.getInventory().selectedSlot;
				if (lookDown.getValue() && prevPitch < 0) prevPitch = mc.player.getPitch();

				int potSlot = InventoryUtils.findSplash(StatusEffects.INSTANT_HEALTH.value(), 1, 1);
				if (potSlot != -1) {
					InventoryUtils.setInvSlot(potSlot);
					switchClock = 0;
				}
			}
			if (InventoryUtils.isThatSplash(StatusEffects.INSTANT_HEALTH.value(), 1, 1, mc.player.getMainHandStack())) {
				if (throwClock < throwDelay.getValueInt()) {
					throwClock++;
					return;
				}

				if (lookDown.getValue()) mc.player.setPitch(90F);

				ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
				if (result.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
				throwClock = 0;
			}
		} else if (prevSlot != -1 || prevPitch >= 0) {
			InventoryUtils.setInvSlot(prevSlot);
			prevSlot = -1;
			mc.player.setPitch(prevPitch);
			prevPitch = -1;
		}
	}
}
