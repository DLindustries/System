package dlindustries.vigillant.system.module.modules.pot;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.mixin.HandledScreenMixin;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.module.setting.MinMaxSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public final class AutoPotRefill extends Module implements TickListener {
	public enum Mode {
		Auto, Hover
	}

	private final ModeSetting<Mode> mode = new ModeSetting<>(EncryptedString.of("Mode"), Mode.Auto, Mode.class);
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 10, 0, 1);
	private final MinMaxSetting slots = new MinMaxSetting(
			EncryptedString.of("Pot Slots"),
			1, 9, 1, 4, 9
	).setDescription(EncryptedString.of("Range of hotbar slots to be refilled with pots (1-9)"));

	private int clock;

	public AutoPotRefill() {
		super(EncryptedString.of("Auto Pot Refill"),
				EncryptedString.of("Refills your hotbar with potions"),
				-1,
				Category.pot);
		addSettings(mode, delay, slots);
		clock = 0;
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		clock = 0;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (!(mc.currentScreen instanceof InventoryScreen inventoryScreen)) {
			return;
		}

		int minSlot = slots.getMinInt() - 1;
		int maxSlot = slots.getMaxInt() - 1;

		if (mode.isMode(Mode.Hover)) {
			Slot focusedSlot = ((HandledScreenMixin) inventoryScreen).getFocusedSlot();
			if (focusedSlot == null) return;

			PlayerInventory inventory = mc.player.getInventory();
			int emptySlot = -1;
			for (int i = minSlot; i <= maxSlot; i++) {
				if (inventory.getStack(i).isEmpty()) {
					emptySlot = i;
					break;
				}
			}
			if (emptySlot == -1) return;

			if (InventoryUtils.isThatSplash(StatusEffects.INSTANT_HEALTH.value(), 1, 1, focusedSlot.getStack())) {
				if (clock < delay.getValueInt()) {
					clock++;
					return;
				}

				mc.interactionManager.clickSlot(
						inventoryScreen.getScreenHandler().syncId,
						focusedSlot.getIndex(),
						emptySlot,
						SlotActionType.SWAP,
						mc.player
				);
				clock = 0;
			}
		}

		if (mode.isMode(Mode.Auto)) {
			int potSlot = InventoryUtils.findPot(StatusEffects.INSTANT_HEALTH.value(), 1, 1);
			if (potSlot == -1) return;

			PlayerInventory inventory = mc.player.getInventory();
			int emptySlot = -1;
			for (int i = minSlot; i <= maxSlot; i++) {
				if (inventory.getStack(i).isEmpty()) {
					emptySlot = i;
					break;
				}
			}
			if (emptySlot == -1) return;

			if (clock < delay.getValueInt()) {
				clock++;
				return;
			}

			mc.interactionManager.clickSlot(
					inventoryScreen.getScreenHandler().syncId,
					potSlot,
					emptySlot,
					SlotActionType.SWAP,
					mc.player
			);
			clock = 0;
		}
	}
}
