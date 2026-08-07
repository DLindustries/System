package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.GameRenderListener;
import dlindustries.vigillant.system.event.events.ItemUseListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.BlockUtils;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.WorldUtils;
import dlindustries.vigillant.system.VigillantSystem;
import dlindustries.vigillant.system.utils.rotation.Rotation;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public final class SafeAnchor extends Module
		implements TickListener, ItemUseListener, AttackListener, GameRenderListener {
	private final NumberSetting actionDelay = new NumberSetting(EncryptedString.of("Action Delay"), 0, 10, 0, 1);
	private final NumberSetting safeSlot = new NumberSetting(EncryptedString.of("Safe Slot"), 1, 9, 9, 1);
	private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 3, 6, 4.5, 0.1);
	private final BooleanSetting silentRotation = new BooleanSetting(EncryptedString.of("Silent Rotation"), true)
			.setDescription(EncryptedString.of("Rotates server-side only; your camera does not turn"));
	private final BooleanSetting smoothRotations = new BooleanSetting(EncryptedString.of("Smooth Rotations"), true);
	private final NumberSetting rotationSpeed = new NumberSetting(EncryptedString.of("Rotation Speed"), 30, 360, 140,
			5);
	private final BooleanSetting useEasing = new BooleanSetting(EncryptedString.of("Use Easing"), true);
	private final NumberSetting humanizeAmount = new NumberSetting(EncryptedString.of("Humanize Amount"), 0, 100, 15,
			1);
	private final BooleanSetting autoExplode = new BooleanSetting(EncryptedString.of("Auto Explode"), true);
	private final BooleanSetting lootProtect = new BooleanSetting(EncryptedString.of("Loot Protect"), true);

	private final BooleanSetting levelBasedTrigger = new BooleanSetting(EncryptedString.of("Level Based Trigger"), true)
			.setDescription(EncryptedString.of("Only trigger safe anchor when placed below or same level as player"));

	private BlockPos anchorPos;
	private BlockPos guardPos;
	private SequenceMode mode = SequenceMode.SAFE;

	private int step;
	private int clock;
	private int anchorWait;
	private boolean running;
	private boolean rotating;
	private Rotation targetRotation;
	private Runnable rotationAction;
	private float currentYaw;
	private float currentPitch;
	private float startYaw;
	private float startPitch;
	private float rotationProgress;
	private float totalRotationDistance;
	private long lastFrameNanos;

	public SafeAnchor() {
		super(EncryptedString.of("Safe Anchor"),
				EncryptedString.of("Left-click anchors normally, right-click anchors safely"),
				-1,
				Category.CRYSTAL);
		addSettings(actionDelay, safeSlot, range, silentRotation, smoothRotations, rotationSpeed, useEasing,
				humanizeAmount, autoExplode, lootProtect, levelBasedTrigger);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(ItemUseListener.class, this);
		eventManager.add(AttackListener.class, this);
		eventManager.add(GameRenderListener.class, this);
		lastFrameNanos = System.nanoTime();
		resetSequence();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(ItemUseListener.class, this);
		eventManager.remove(AttackListener.class, this);
		eventManager.remove(GameRenderListener.class, this);
		dlindustries.vigillant.system.VigillantSystem.INSTANCE.rotatorManager.disable();
		dlindustries.vigillant.system.VigillantSystem.INSTANCE.rotatorManager.disable();
		resetSequence();
	}

	@Override
	public void onAttack(AttackEvent event) {
		if (!canStartFromHeldAnchor())
			return;
		BlockHitResult hit = getBlockHit();
		if (hit == null)
			return;
		if (!BlockUtils.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR))
			return;

		BlockPos pos = hit.getBlockPos();
		if (!isInRange(pos))
			return;

		event.cancel();
		startSequence(pos, SequenceMode.NORMAL);
	}

	@Override
	public void onItemUse(ItemUseEvent event) {
		if (!canStartFromHeldAnchor())
			return;
		if (!hasRequiredItems())
			return;

		BlockHitResult hit = getBlockHit();
		if (hit == null)
			return;

		BlockPos pos = getPlacementPos(hit);
		if (!isInRange(pos))
			return;

		SequenceMode sequenceMode;
		if (levelBasedTrigger.getValue()) {
			double verticalDiff = pos.getY() - mc.player.getY();
			sequenceMode = verticalDiff > 0.5 ? SequenceMode.NORMAL : SequenceMode.SAFE;
		} else {
			sequenceMode = getModeForNearestEnemy();
		}

		if (sequenceMode == SequenceMode.SAFE) {
			event.cancel();
		}
		startSequence(pos, sequenceMode);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.currentScreen != null)
			return;
		if (!running)
			return;

		if (rotating && smoothRotations.getValue()) {
			return;

		}

		if (rotating) {
			finishRotation();
			return;
		}

		if (clock++ < actionDelay.getValueInt())
			return;
		clock = 0;

		if (!hasRequiredItems()) {
			resetSequence();

			return;
		}

		switch (step) {
			case 0 -> placeOrWaitForAnchor();
			case 1 -> chargePlacedAnchor();
			case 2 -> placeGuardIfSafeMode();
			case 3 -> switchToSafeSlot();
			case 4 -> detonatePlacedAnchor();
			default -> resetSequence();
		}
	}

	private void placeOrWaitForAnchor() {
		if (BlockUtils.isBlock(anchorPos, Blocks.RESPAWN_ANCHOR)) {
			advance();
			return;
		}

		if (mode == SequenceMode.NORMAL) {
			if (anchorWait++ > 30)
				resetSequence();
			return;
		}

		if (!canPlaceAt(anchorPos)) {
			resetSequence();
			return;
		}

		if (!InventoryUtils.selectItemFromHotbar(Items.RESPAWN_ANCHOR)) {
			resetSequence();
			return;
		}

		BlockHitResult placement = getPlacementHit(anchorPos);
		if (placement == null) {
			if (anchorWait++ > 15)
				resetSequence();
			return;
		}

		WorldUtils.placeBlock(placement, true);
		if (BlockUtils.isBlock(anchorPos, Blocks.RESPAWN_ANCHOR)) {
			advance();
		} else if (anchorWait++ > 15) {
			resetSequence();
		}
	}

	private void chargePlacedAnchor() {
		if (!BlockUtils.isBlock(anchorPos, Blocks.RESPAWN_ANCHOR)) {
			resetSequence();
			return;
		}

		if (BlockUtils.isAnchorCharged(anchorPos)) {
			advance();
			return;
		}

		if (!chargeAnchor())
			resetSequence();
		else
			advance();
	}

	private void placeGuardIfSafeMode() {
		if (mode == SequenceMode.NORMAL) {
			advance();
			return;
		}

		if (guardPos == null || !canPlaceAt(guardPos)) {
			advance();
			return;
		}

		if (canPlaceAt(guardPos.down())) {
			advance();
			return;
		}

		BlockHitResult placement = getPlacementHit(guardPos);
		if (placement == null) {
			advance();
			return;
		}

		rotateThen(placement.getPos(), () -> {
			if (!placeItem(guardPos, Items.GLOWSTONE))
				resetSequence();
			else
				advance();
		});
	}

	private void switchToSafeSlot() {
		int slot = safeSlot.getValueInt() - 1;
		if (slot >= 0 && slot < 9)
			InventoryUtils.setInvSlot(slot);
		advance();
	}

	private void detonatePlacedAnchor() {
		if (!autoExplode.getValue()) {
			resetSequence();
			return;
		}

		if (lootProtect.getValue() && (hasLootNearAnchor() || WorldUtils.isValuableLootNearby())) {
			resetSequence();
			return;
		}

		if (mode == SequenceMode.NORMAL) {
			if (!detonateAnchor())
				resetSequence();
			else
				resetSequence();
			return;
		}

		BlockHitResult hit = anchorHit();
		rotateThen(hit.getPos(), () -> {
			if (!detonateAnchor())
				resetSequence();
			else
				resetSequence();
		});
	}

	private boolean canStartFromHeldAnchor() {
		return mc.player != null
				&& mc.world != null
				&& !running
				&& mc.currentScreen == null
				&& mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR);
	}

	private BlockHitResult getBlockHit() {
		if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
			return hit;
		}
		return null;
	}

	private BlockPos getPlacementPos(BlockHitResult hit) {
		BlockPos clicked = hit.getBlockPos();
		return mc.world.getBlockState(clicked).isReplaceable() ? clicked : clicked.offset(hit.getSide());
	}

	private void startSequence(BlockPos pos, SequenceMode sequenceMode) {
		anchorPos = pos;
		mode = sequenceMode;
		guardPos = getGuardPos(pos);
		step = 0;
		clock = 0;
		anchorWait = 0;
		running = true;
		rotating = false;
		targetRotation = null;
		rotationAction = null;
	}

	private BlockPos getGuardPos(BlockPos pos) {
		Vec3d midpoint = Vec3d.ofCenter(mc.player.getBlockPos()).add(Vec3d.ofCenter(pos)).multiply(0.5);
		BlockPos protectionPos = BlockPos.ofFloored(midpoint.x, mc.player.getBlockY(), midpoint.z);

		if (protectionPos.equals(pos) || protectionPos.equals(mc.player.getBlockPos())) {
			Direction facing = mc.player.getHorizontalFacing();
			protectionPos = mc.player.getBlockPos().offset(facing);
			if (protectionPos.equals(pos)) {
				protectionPos = mc.player.getBlockPos();
			}
		}
		return protectionPos;
	}

	private boolean placeItem(BlockPos pos, Item item) {
		if (pos == null || !canPlaceAt(pos))
			return false;
		if (!InventoryUtils.selectItemFromHotbar(item))
			return false;

		BlockHitResult placement = getPlacementHit(pos);
		if (placement == null)
			return false;

		WorldUtils.placeBlock(placement, true);
		return true;
	}

	private boolean chargeAnchor() {
		if (anchorPos == null || !BlockUtils.isBlock(anchorPos, Blocks.RESPAWN_ANCHOR))
			return false;
		if (!InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE))
			return false;

		WorldUtils.placeBlock(anchorHit(), true);
		return true;

	}

	private boolean detonateAnchor() {
		if (anchorPos == null || !BlockUtils.isAnchorCharged(anchorPos))
			return false;

		WorldUtils.placeBlock(anchorHit(), true);
		return true;
	}

	@Override
	public void onGameRender(GameRenderEvent event) {
		if (!rotating || mc.player == null || !smoothRotations.getValue()) {
			lastFrameNanos = System.nanoTime();
			return;
		}

		long now = System.nanoTime();
		float delta = (now - lastFrameNanos) / 1_000_000_000.0f;
		lastFrameNanos = now;
		if (delta <= 0.0f || delta > 0.5f)
			delta = 0.05f;

		if (useEasing.getValue()) {
			float speed = (float) rotationSpeed.getValue();
			float increment = (speed * delta)
					/ Math.max(0.1f, totalRotationDistance);
			rotationProgress = Math.min(1.0f, rotationProgress + increment);
			float eased = easeOut(rotationProgress);

			float yawDiff = MathHelper.wrapDegrees((float) targetRotation.yaw() - startYaw);
			currentYaw = MathHelper.wrapDegrees(startYaw + yawDiff * eased);
			currentPitch = MathHelper.clamp(startPitch + (float) (targetRotation.pitch() - startPitch) * eased, -90.0f,
					90.0f);
		} else {
			float maxStep = (float) rotationSpeed.getValue() * delta;
			float yawDiff = MathHelper.wrapDegrees((float) targetRotation.yaw() - currentYaw);
			if (Math.abs(yawDiff) <= maxStep)
				currentYaw = (float) targetRotation.yaw();

			else
				currentYaw = MathHelper.wrapDegrees(currentYaw + Math.copySign(maxStep, yawDiff));

			float pitchDiff = (float) targetRotation.pitch() - currentPitch;
			if (Math.abs(pitchDiff) <= maxStep)
				currentPitch = (float) targetRotation.pitch();
			else
				currentPitch = MathHelper.clamp(currentPitch + Math.copySign(maxStep, pitchDiff), -90.0f, 90.0f);
		}

		if (silentRotation.getValue()) {
			VigillantSystem.INSTANCE.rotatorManager.setRotation(new Rotation(currentYaw, currentPitch));
		} else {
			mc.player.setYaw(currentYaw);
			mc.player.setPitch(currentPitch);
		}

		float yawError = Math.abs(MathHelper.wrapDegrees((float) targetRotation.yaw() - currentYaw));
		float pitchError = Math.abs((float) targetRotation.pitch() - currentPitch);
		if (rotationProgress >= 0.98f || (yawError < 1.0f && pitchError < 1.0f)) {
			currentYaw = (float) targetRotation.yaw();
			currentPitch = (float) targetRotation.pitch();
			finishRotation();
		}
	}

	private float easeOut(float t) {
		return 1.0f - (float) Math.pow(1.0f - t, 2.2);
	}

	private void rotateThen(Vec3d pos, Runnable action) {
		Rotation base = getRotationTo(pos);
		double humanizeFactor = humanizeAmount.getValue() / 100.0;
		targetRotation = new Rotation(
				base.yaw() + randomOffset(humanizeFactor * 0.35),
				MathHelper.clamp(base.pitch() + randomOffset(humanizeFactor * 0.2), -90.0, 90.0));
		rotationAction = action;

		if (!smoothRotations.getValue()) {
			rotating = true;
			finishRotation();
			return;
		}

		if (silentRotation.getValue()) {
			currentYaw = mc.player.getYaw();
			currentPitch = mc.player.getPitch();
		} else {
			currentYaw = mc.player.getYaw();
			currentPitch = mc.player.getPitch();
		}

		startYaw = currentYaw;
		startPitch = currentPitch;
		rotationProgress = 0.0f;

		float yawDiff = Math.abs(MathHelper.wrapDegrees((float) targetRotation.yaw() - startYaw));
		float pitchDiff = Math.abs((float) targetRotation.pitch() - startPitch);
		totalRotationDistance = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
		if (totalRotationDistance < 0.1f)
			totalRotationDistance = 0.1f;

		rotating = true;
		lastFrameNanos = System.nanoTime();
	}

	private void finishRotation() {
		Runnable action = rotationAction;
		rotationAction = null;
		rotating = false;

		if (targetRotation == null) {
			if (action != null)
				action.run();
			return;
		}

		var rotator = VigillantSystem.INSTANCE.rotatorManager;
		if (silentRotation.getValue()) {
			rotator.setRotation(targetRotation);
			if (action != null)
				action.run();
		} else {
			mc.player.setYaw((float) targetRotation.yaw());
			mc.player.setPitch((float) targetRotation.pitch());
			if (action != null)
				action.run();
		}

		targetRotation = null;
		rotator.disable();
	}

	private BlockHitResult anchorHit() {
		return new BlockHitResult(Vec3d.ofCenter(anchorPos).add(0.0, 0.5, 0.0), Direction.UP, anchorPos, false);
	}

	private Rotation getRotationTo(Vec3d target) {
		Vec3d eyes = mc.player.getEyePos();
		double dx = target.x - eyes.x;
		double dy = target.y - eyes.y;
		double dz = target.z - eyes.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);

		double yaw = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		double pitch = MathHelper.clamp(-Math.toDegrees(Math.atan2(dy, horizontal)), -90.0, 90.0);
		return new Rotation(yaw, pitch);
	}

	private SequenceMode getModeForNearestEnemy() {
		PlayerEntity nearest = getNearestEnemy();
		if (nearest == null)
			return SequenceMode.SAFE;

		double vertical = nearest.getY() - mc.player.getY();
		return vertical > 0.45 ? SequenceMode.NORMAL : SequenceMode.SAFE;
	}

	private PlayerEntity getNearestEnemy() {
		PlayerEntity nearest = null;
		double nearestDistance = Double.MAX_VALUE;

		for (PlayerEntity player : mc.world.getPlayers()) {
			if (player == mc.player || player.isDead())
				continue;
			double horizontal = Math.hypot(player.getX() - mc.player.getX(), player.getZ() - mc.player.getZ());
			if (horizontal > range.getValue())
				continue;

			double distance = player.squaredDistanceTo(mc.player);
			if (distance < nearestDistance) {
				nearest = player;

				nearestDistance = distance;
			}
		}

		return nearest;
	}

	private double randomOffset(double amount) {
		if (amount <= 0.0)
			return 0.0;
		return ThreadLocalRandom.current().nextDouble(-amount, amount);
	}

	private void advance() {
		step++;
		clock = 0;
	}

	private BlockHitResult getPlacementHit(BlockPos pos) {
		Direction[] directions = {
				Direction.DOWN, Direction.UP, Direction.NORTH,
				Direction.SOUTH, Direction.WEST, Direction.EAST
		};
		for (Direction direction : directions) {
			BlockPos neighbor = pos.offset(direction);
			if (!mc.world.getBlockState(neighbor).isReplaceable()) {
				Direction side = direction.getOpposite();
				Vec3d hit = Vec3d.ofCenter(neighbor).add(Vec3d.of(side.getVector()).multiply(0.5));
				return new BlockHitResult(hit, side, neighbor, false);
			}
		}
		return null;
	}

	private boolean isInRange(BlockPos pos) {
		return mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) <= range.getValue();
	}

	private boolean canPlaceAt(BlockPos pos) {
		if (pos == null || mc.world == null)
			return false;
		var state = mc.world.getBlockState(pos);
		if (state.isAir())
			return true;

		Block block = state.getBlock();

		return block == Blocks.SHORT_GRASS
				|| block == Blocks.TALL_GRASS
				|| block == Blocks.FERN
				|| block == Blocks.LARGE_FERN
				|| block == Blocks.DEAD_BUSH
				|| block == Blocks.VINE
				|| block == Blocks.FIRE
				|| block == Blocks.SOUL_FIRE
				|| block == Blocks.WATER
				|| block == Blocks.LAVA
				|| block == Blocks.SNOW
				|| block == Blocks.SEAGRASS
				|| block == Blocks.TALL_SEAGRASS
				|| block == Blocks.KELP
				|| block == Blocks.KELP_PLANT
				|| state.isReplaceable();
	}

	private boolean hasRequiredItems() {
		if (mc.player == null)
			return false;
		boolean hasAnchor = false;
		boolean hasGlowstone = false;
		for (int i = 0; i < 9; i++) {
			var stack = mc.player.getInventory().getStack(i);
			if (stack.isOf(Items.RESPAWN_ANCHOR))
				hasAnchor = true;
			if (stack.isOf(Items.GLOWSTONE))
				hasGlowstone = true;
		}
		return hasAnchor && hasGlowstone;
	}

	private boolean hasLootNearAnchor() {
		if (mc.world == null || anchorPos == null)
			return false;

		double searchRadius = 10.0;
		Box searchBox = new Box(
				anchorPos.getX() - searchRadius, anchorPos.getY() - searchRadius, anchorPos.getZ() - searchRadius,
				anchorPos.getX() + searchRadius, anchorPos.getY() + searchRadius, anchorPos.getZ() + searchRadius);

		for (Entity entity : mc.world.getOtherEntities(null, searchBox)) {
			if (!(entity instanceof ItemEntity itemEntity))
				continue;
			ItemStack stack = itemEntity.getStack();
			if (stack.isEmpty())
				continue;
			if (stack.contains(DataComponentTypes.EQUIPPABLE)
					|| stack.isOf(Items.TOTEM_OF_UNDYING)) {
				return true;
			}
		}
		return false;
	}

	private void resetSequence() {
		anchorPos = null;
		guardPos = null;
		mode = SequenceMode.SAFE;
		step = 0;
		clock = 0;
		anchorWait = 0;
		running = false;
		rotating = false;
		rotationProgress = 0.0f;
		totalRotationDistance = 0.0f;
		targetRotation = null;
		rotationAction = null;
	}

	public boolean isRunningSequence() {
		return isEnabled() && (running || rotating);
	}

	private enum SequenceMode {
		NORMAL, SAFE
	}
}
