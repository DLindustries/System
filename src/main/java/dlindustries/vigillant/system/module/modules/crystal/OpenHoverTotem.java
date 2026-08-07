package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.mixin.HandledScreenMixin;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.concurrent.ThreadLocalRandom;

public final class OpenHoverTotem extends Module implements TickListener {
	public enum InventoryMode {
		None("None"),
		AutoOpen("Auto Open"),
		AutoClose("Auto Close");

		private final String name;

		InventoryMode(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 20, 0, 1);
	private final BooleanSetting hotbar = new BooleanSetting(EncryptedString.of("Hotbar"), true)
			.setDescription(EncryptedString.of("Puts a totem in your hotbar as well"));
	private final NumberSetting slot = new NumberSetting(EncryptedString.of("Totem Slot"), 1, 9, 9, 1)
			.setDescription(EncryptedString.of("Your preferred hotbar slot for totems"));
	private final BooleanSetting dynamicDelay = new BooleanSetting(EncryptedString.of("Dynamic Delay"), false)
			.setDescription(EncryptedString.of("Adds further random timing variations to avoid detection"));
	private final ModeSetting<InventoryMode> inventoryMode = new ModeSetting<>(EncryptedString.of("Inventory Mode"), InventoryMode.None, InventoryMode.class)
			.setDescription(EncryptedString.of("Auto open/close inventory behavior"));

	private int clock;
	private boolean safeMode;
	private boolean hadOffhandTotem;
	private boolean inventoryOpened = false;

	public OpenHoverTotem() {
		super(EncryptedString.of("Open Hover Totem"),
				EncryptedString.of("Opens your inventory and hover to put your totem"),
				-1,
				Category.CRYSTAL);
		addSettings(delay, hotbar, slot, dynamicDelay, inventoryMode);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		clock = 0;
		safeMode = false;
		hadOffhandTotem = mc.player != null && mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
		inventoryOpened = false;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		if (inventoryOpened && inventoryMode.getMode() == InventoryMode.AutoClose) {
			if (mc.player != null) {
				mc.player.closeScreen();
			}
			inventoryOpened = false;
		}
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;

		boolean offhandTotem = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
		if (hadOffhandTotem && !offhandTotem && mc.currentScreen == null && inventoryMode.getMode() != InventoryMode.None) {
			mc.setScreen(new InventoryScreen(mc.player));
			inventoryOpened = true;
		}

		if (inventoryOpened && inventoryMode.getMode() == InventoryMode.AutoClose && offhandTotem && mc.currentScreen != null) {
			mc.player.closeScreen();
			inventoryOpened = false;
		}

		hadOffhandTotem = offhandTotem;

		if (!(mc.currentScreen instanceof InventoryScreen inv)) {
			clock = delay.getValueInt();
			safeMode = false;
			return;
		}

		int hotbarSlotIndex = Math.max(0, Math.min(slot.getValueInt() - 1, 8));
		boolean hotbarTotem = mc.player.getInventory().getStack(hotbarSlotIndex).isOf(Items.TOTEM_OF_UNDYING);
		safeMode = offhandTotem && hotbarTotem;

		Slot hoveredSlot = ((HandledScreenMixin) inv).getFocusedSlot();
		if (hoveredSlot == null || hoveredSlot.getIndex() > 35 || hoveredSlot.getStack().isEmpty()) return;

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

	private void handleInventoryMode() {
		// Inventory opening and closing is handled by totem pop detection in onTick().
	}
}
