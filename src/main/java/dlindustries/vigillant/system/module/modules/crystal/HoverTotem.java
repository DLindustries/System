package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.mixin.HandledScreenMixin;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.concurrent.ThreadLocalRandom;

public final class HoverTotem extends Module implements TickListener {
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 20, 1, 1);
	private final BooleanSetting hotbar = new BooleanSetting(EncryptedString.of("Hotbar"), true)
			.setDescription(EncryptedString.of("Puts a totem in your hotbar as well"));
	private final NumberSetting slot = new NumberSetting(EncryptedString.of("Totem Slot"), 1, 9, 9, 1)
			.setDescription(EncryptedString.of("Your preferred hotbar slot for totems"));
	private final BooleanSetting dynamicDelay = new BooleanSetting(EncryptedString.of("Dynamic Delay"), true)
			.setDescription(EncryptedString.of("Adds random timing variations to avoid detection"));

	private int clock;
	private boolean safeMode;

	public HoverTotem() {
		super(EncryptedString.of("Autototem - legit"),
				EncryptedString.of("Equips totems when hovered - optimized and perfected with DhandMod module"),
				-1,
				Category.CRYSTAL);
		addSettings(delay, hotbar, slot, dynamicDelay);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		clock = 0;
		safeMode = false;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (!(mc.currentScreen instanceof InventoryScreen inv)) {
			clock = delay.getValueInt();
			safeMode = false; // Reset safe mode when exiting inventory
			return;
		}

		// Update safe mode based on current totem status
		boolean offhandTotem = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
		int hotbarSlotIndex = slot.getValueInt() - 1;
		boolean hotbarTotem = mc.player.getInventory().getStack(hotbarSlotIndex).isOf(Items.TOTEM_OF_UNDYING);
		safeMode = offhandTotem && hotbarTotem;

		Slot hoveredSlot = ((HandledScreenMixin) inv).getFocusedSlot();
		if (hoveredSlot == null || hoveredSlot.getIndex() > 35) return;

		if (hoveredSlot.getStack().getItem() == Items.TOTEM_OF_UNDYING) {
			if (clock > 0) {
				clock--;
				return;
			}

			executeTotemSwap(inv, hoveredSlot);
			clock = getDynamicDelay();
		}
	}

	private int getDynamicDelay() {
		int base = delay.getValueInt();
		if (safeMode) {
			base += 1; // Add 50ms (1 tick) delay in safe mode
		}
		if (dynamicDelay.getValue()) {
			return Math.max(0, base + ThreadLocalRandom.current().nextInt(-2, 3));
		} else {
			return Math.max(0, base);
		}
	}

	private void performSwap(InventoryScreen inv, int from, int to) {
		mc.interactionManager.clickSlot(
				inv.getScreenHandler().syncId,
				from,
				to,
				SlotActionType.SWAP,
				mc.player
		);
	}

	private void executeTotemSwap(InventoryScreen inv, Slot hoveredSlot) {
		int hotbarSlot = slot.getValueInt() - 1;


		if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
			performSwap(inv, hoveredSlot.getIndex(), 40);
		} else if (hotbar.getValue() && mc.player.getInventory().getStack(hotbarSlot).getItem() != Items.TOTEM_OF_UNDYING) {
			performSwap(inv, hoveredSlot.getIndex(), hotbarSlot);
		}
	}
}