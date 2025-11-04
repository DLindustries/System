package dlindustries.vigillant.system.utils;

import dlindustries.vigillant.system.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static dlindustries.vigillant.system.system.mc;

public final class InventoryUtils {

	public static void setInvSlot(int slot) {
		mc.player.getInventory().selectedSlot = slot;
		((ClientPlayerInteractionManagerAccessor) mc.interactionManager).syncSlot();
	}

	public static boolean selectItemFromHotbar(Predicate<Item> item) {
		PlayerInventory inv = mc.player.getInventory();
		for (int i = 0; i < 9; i++) {
			ItemStack stack = inv.getStack(i);
			if (item.test(stack.getItem())) {
				inv.selectedSlot = i;
				return true;
			}
		}
		return false;
	}

	public static boolean selectItemFromHotbar(Item item) {
		return selectItemFromHotbar(i -> i == item);
	}

	public static boolean hasItemInHotbar(Predicate<Item> item) {
		PlayerInventory inv = mc.player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (item.test(inv.getStack(i).getItem())) {
				return true;
			}
		}
		return false;
	}

	public static int countItem(Predicate<Item> item) {
		PlayerInventory inv = mc.player.getInventory();
		int count = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack stack = inv.getStack(i);
			if (item.test(stack.getItem())) {
				count += stack.getCount();
			}
		}
		return count;
	}

	public static int countItemExceptHotbar(Predicate<Item> item) {
		PlayerInventory inv = mc.player.getInventory();
		int count = 0;
		for (int i = 9; i < 36; i++) {
			ItemStack stack = inv.getStack(i);
			if (item.test(stack.getItem())) {
				count += stack.getCount();
			}
		}
		return count;
	}

	public static int getSwordSlot() {
		Inventory inv = mc.player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (inv.getStack(i).getItem() instanceof SwordItem) {
				return i;
			}
		}
		return -1;
	}

	public static boolean selectSword() {
		int slot = getSwordSlot();
		if (slot != -1) {
			setInvSlot(slot);
			return true;
		}
		return false;
	}

	public static int findSplash(StatusEffect type, int duration, int amplifier) {
		PlayerInventory inv = mc.player.getInventory();
		StatusEffectInstance want = new StatusEffectInstance(
				Registries.STATUS_EFFECT.getEntry(type),
				duration,
				amplifier
		);
		for (int i = 0; i < 9; i++) {
			ItemStack stack = inv.getStack(i);
			if (stack.getItem() instanceof SplashPotionItem) {
				String effects = stack.get(DataComponentTypes.POTION_CONTENTS)
						.getEffects().toString();
				if (effects.contains(want.toString())) {
					return i;
				}
			}
		}
		return -1;
	}

	public static boolean isThatSplash(StatusEffect type, int duration, int amplifier, ItemStack stack) {
		if (!(stack.getItem() instanceof SplashPotionItem)) return false;
		StatusEffectInstance want = new StatusEffectInstance(
				Registries.STATUS_EFFECT.getEntry(type),
				duration,
				amplifier
		);
		return stack.get(DataComponentTypes.POTION_CONTENTS)
				.getEffects().toString()
				.contains(want.toString());
	}

	public static int findTotemSlot() {
		PlayerInventory inv = mc.player.getInventory();
		for (int i = 9; i < 36; i++) {
			if (inv.main.get(i).getItem() == Items.TOTEM_OF_UNDYING) {
				return i;
			}
		}
		return -1;
	}

	public static boolean selectAxe() {
		int slot = getAxeSlot();
		if (slot != -1) {
			setInvSlot(slot);
			return true;
		}
		return false;
	}

	public static int findRandomTotemSlot() {
		PlayerInventory inv = mc.player.getInventory();
		List<Integer> totems = new ArrayList<>();
		for (int i = 9; i < 36; i++) {
			if (inv.main.get(i).getItem() == Items.TOTEM_OF_UNDYING) {
				totems.add(i);
			}
		}
		if (!totems.isEmpty()) {
			return totems.get(new Random().nextInt(totems.size()));
		}
		return -1;
	}

	public static int findRandomPot(String potion) {
		PlayerInventory inv = mc.player.getInventory();
		int start = new Random().nextInt(27) + 9;
		for (int i = 0; i < 27; i++) {
			int idx = (start + i) % 36;
			ItemStack stack = inv.main.get(idx);
			if (stack.getItem() instanceof SplashPotionItem) {
				if (stack.get(DataComponentTypes.POTION_CONTENTS)
						.getEffects().toString()
						.contains(potion)) {
					return idx;
				} else {
					return -1;
				}
			}
		}
		return -1;
	}

	public static int findPot(StatusEffect effect, int duration, int amplifier) {
		PlayerInventory inv = mc.player.getInventory();
		StatusEffectInstance want = new StatusEffectInstance(
				Registries.STATUS_EFFECT.getEntry(effect),
				duration,
				amplifier
		);
		for (int i = 9; i < 36; i++) {
			ItemStack stack = inv.main.get(i);
			if (stack.getItem() instanceof SplashPotionItem &&
					stack.get(DataComponentTypes.POTION_CONTENTS)
							.getEffects().toString()
							.contains(want.toString())) {
				return i;
			}
		}
		return -1;
	}

	public static List<Integer> getEmptyHotbarSlots() {
		PlayerInventory inv = mc.player.getInventory();
		List<Integer> slots = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			if (inv.main.get(i).isEmpty()) {
				slots.add(i);
			} else {
				slots.remove((Integer) i);
			}
		}
		return slots;
	}

	public static int getAxeSlot() {
		Inventory inv = mc.player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (inv.getStack(i).getItem() instanceof AxeItem) {
				return i;
			}
		}
		return -1;
	}

	public static int countItem(Item item) {
		return countItem(i -> i == item);
	}

	public static boolean hasItem(Item item) {
		if (mc.player == null) return false;
		if (mc.player.getOffHandStack().getItem() == item) return true;
		for (int i = 0; i < 36; i++) {
			if (mc.player.getInventory().getStack(i).getItem() == item) {
				return true;
			}
		}
		return false;
	}


	public static int findItemSlot(Item item) {
		for (int i = 0; i < 9; i++) {
			if (mc.player.getInventory().getStack(i).isOf(item)) {
				return i;
			}
		}
		return -1;
	}


	public static void swap(int slot) {
		mc.interactionManager.clickSlot(
				mc.player.currentScreenHandler.syncId,
				slot + 36,          // hotbar slots are 36–44
				0,
				SlotActionType.SWAP,
				mc.player
		);
	}

}
