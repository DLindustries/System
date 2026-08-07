package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.mixin.MinecraftClientAccessor;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class AutoCart extends Module implements TickListener {

	private final BooleanSetting autoSwitch = new BooleanSetting(EncryptedString.of("Auto Switch"), true);
	private final BooleanSetting safetyBlock = new BooleanSetting(EncryptedString.of("Safety Block"), false);

	private boolean isActive;
	private int originalSlot = -1;
	private int tickCounter;
	private boolean hasRail;
	private boolean hasTntCart;
	private BlockPos placedRailPos;

	public AutoCart() {
		super(EncryptedString.of("Auto Cart"),
				EncryptedString.of("Places TNT minecarts on rails when shooting arrows"),
				-1,
				Category.sword);
		addSettings(autoSwitch, safetyBlock);
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
		if (isActive) {
			stopPlacing();
		}
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null)
			return;

		if (mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.BOW)) {
			if (!isActive) {
				startPlacing();
			}
		} else if (isActive && !mc.player.isUsingItem()) {
			if (tickCounter == 0) {
				tickCounter = 1;
			}
		}

		if (!isActive)
			return;

		HitResult hitResult = mc.crosshairTarget;
		if (!(hitResult instanceof BlockHitResult bhr)) {
			return;
		}

		if (tickCounter == 1) {
			if (placeRail(bhr)) {
				placedRailPos = bhr.getBlockPos().offset(bhr.getSide());
				tickCounter = 2;
			}
		} else if (tickCounter == 2) {
			placeTntCart();
			tickCounter = 3;
		} else if (tickCounter == 3) {
			if (safetyBlock.getValue() && placedRailPos != null) {
				placeSafetyBlock(placedRailPos);
			}
			stopPlacing();
		}
	}

	private void startPlacing() {
		if (isActive || mc.player.getMainHandStack().getItem() != Items.BOW)
			return;

		isActive = true;
		tickCounter = 0;
		originalSlot = mc.player.getInventory().getSelectedSlot();
	}

	private void stopPlacing() {
		if (!isActive)
			return;

		if (autoSwitch.getValue() && originalSlot != -1) {
			InventoryUtils.setInvSlot(originalSlot);
		}

		resetState();
	}

	private void resetState() {
		isActive = false;
		originalSlot = -1;
		tickCounter = 0;
		hasRail = false;
		hasTntCart = false;
		placedRailPos = null;
	}

	private boolean placeRail(BlockHitResult bhr) {
		if (hasRail)
			return true;

		int railSlot = findAnyRailInHotbar();
		if (railSlot == -1)
			return false;

		InventoryUtils.setInvSlot(railSlot);
		net.minecraft.util.ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
		if (result.isAccepted()) {
			mc.player.swingHand(Hand.MAIN_HAND);
			hasRail = true;
			return true;
		}
		return false;
	}

	private void placeTntCart() {
		if (hasTntCart)
			return;

		boolean offhandTnt = mc.player.getOffHandStack().isOf(Items.TNT_MINECART);
		int tntCartSlot = findTntCartInHotbar();

		if (!offhandTnt && tntCartSlot == -1) {
			return;
		}

		if (offhandTnt) {
			int usableSlot = findNonUsableSlot();
			if (usableSlot != -1) {
				InventoryUtils.setInvSlot(usableSlot);
			}
		} else {
			InventoryUtils.setInvSlot(tntCartSlot);
		}

		((MinecraftClientAccessor) mc).invokeDoItemUse();
		hasTntCart = true;
	}

	private void placeSafetyBlock(BlockPos railPos) {
		int safeSlot = -1;
		for (int i = 0; i < 9; i++) {
			Item item = mc.player.getInventory().getStack(i).getItem();
			if (item == Items.OBSIDIAN || item == Items.OAK_PLANKS) {
				safeSlot = i;
				break;
			}
		}
		if (safeSlot == -1)
			return;

		int deltaX = railPos.getX() - mc.player.getBlockX();
		int deltaZ = railPos.getZ() - mc.player.getBlockZ();
		Direction dir;
		if (Math.abs(deltaX) > Math.abs(deltaZ)) {
			dir = deltaX > 0 ? Direction.EAST : Direction.WEST;
		} else {
			dir = deltaZ > 0 ? Direction.SOUTH : Direction.NORTH;
		}
		if (dir == null)
			dir = mc.player.getHorizontalFacing();

		BlockPos placePos = mc.player.getBlockPos().offset(dir);
		if (mc.world.getBlockState(placePos).isReplaceable()) {
			BlockPos support = placePos.down();
			if (mc.world.getBlockState(support).isOpaque()) {
				InventoryUtils.setInvSlot(safeSlot);
				BlockHitResult safeBhr = new BlockHitResult(
						new Vec3d(support.getX() + 0.5, support.getY() + 1.0, support.getZ() + 0.5),
						Direction.UP,
						support,
						false);
				mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, safeBhr);
				mc.player.swingHand(Hand.MAIN_HAND);
			}
		}
	}

	private int findTntCartInHotbar() {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (!stack.isEmpty() && stack.isOf(Items.TNT_MINECART)) {
				return i;
			}
		}
		return -1;
	}

	private int findAnyRailInHotbar() {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (!stack.isEmpty() && isRail(stack.getItem())) {
				return i;
			}
		}
		return -1;
	}

	private int findNonUsableSlot() {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (stack.isEmpty())
				return i;
			Item item = stack.getItem();
			if (item.getUseAction(stack) == UseAction.NONE && !(item instanceof BlockItem)
					&& item != Items.TNT_MINECART) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isRail(Item item) {
		return item == Items.RAIL
				|| item == Items.POWERED_RAIL
				|| item == Items.DETECTOR_RAIL
				|| item == Items.ACTIVATOR_RAIL;
	}
}
