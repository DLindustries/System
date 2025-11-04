package dlindustries.vigillant.system.module.modules.sword;


import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.MouseSimulation;
import dlindustries.vigillant.system.utils.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShieldDisabler extends Module implements TickListener, AttackListener {
	private final NumberSetting hitDelay = new NumberSetting(EncryptedString.of("Hit Delay"), 0, 20, 0, 1);
	private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 20, 0, 1);
	private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
	private final BooleanSetting stun = new BooleanSetting(EncryptedString.of("Stun"), false);
	private final BooleanSetting stunSlam = new BooleanSetting(EncryptedString.of("Stun Slam"), true)
			.setDescription(EncryptedString.of("Use mace for second hit instead of sword"));
	private final NumberSetting maceSlot = new NumberSetting(EncryptedString.of("Mace Slot"), 1, 9, 1, 1)
			.setDescription(EncryptedString.of("Slot 1-9 for mace (for Stun Slam)"));
	private final BooleanSetting clickSimulate = new BooleanSetting(EncryptedString.of("Click Simulation"), true);
	private final BooleanSetting requireHoldAxe = new BooleanSetting(EncryptedString.of("Hold Axe"), false);
	private final NumberSetting minShieldHold = new NumberSetting(EncryptedString.of("Min Shield Hold"), 50, 300, 100, 5)
			.setDescription(EncryptedString.of("Minimum time opponent must hold shield (ms)"));
	private int originalSlot = -1;
	private int hitClock, switchClock;
	private boolean inStunSequence;
	private int stunStep;
	private final Map<UUID, Long> shieldStartTimes = new HashMap<>();
	private PlayerEntity currentTarget;
	private boolean isBreaking;

	public ShieldDisabler() {
		super(EncryptedString.of("Shield Disabler"),
				EncryptedString.of("Disables shield upon looking"),
				-1,
				Category.sword);

		addSettings(
				switchDelay, hitDelay, switchBack, stun, stunSlam, maceSlot,
				clickSimulate, requireHoldAxe, minShieldHold
		);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(AttackListener.class, this);

		resetState();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(AttackListener.class, this);
		restoreOriginalSlot();
		shieldStartTimes.clear();
		super.onDisable();
	}

	private void resetState() {
		hitClock = hitDelay.getValueInt();
		switchClock = switchDelay.getValueInt();
		originalSlot = -1;
		inStunSequence = false;
		stunStep = 0;
		currentTarget = null;
		isBreaking = false;
	}

	@Override
	public void onTick() {
		if (shouldSkipTick()) {
			if (isBreaking) abortBreaking();
			return;
		}

		if (inStunSequence) {
			handleStunSequence();
			return;
		}
		EntityHitResult entityHit = getValidTarget();
		PlayerEntity newTarget = (entityHit != null && entityHit.getEntity() instanceof PlayerEntity)
				? (PlayerEntity) entityHit.getEntity()
				: null;
		updateShieldTracking(newTarget);
		if (isBreaking && currentTarget != null) {
			handleBreakingSequence();
			return;
		}
		if (newTarget != null && shouldBreakShield(newTarget)) {
			startBreakingSequence(newTarget);
		}
	}

	private boolean shouldSkipTick() {
		return mc.currentScreen != null ||
				mc.player == null ||
				(requireHoldAxe.getValue() &&
						!(mc.player.getMainHandStack().getItem() instanceof AxeItem)) ||
				mc.player.isUsingItem();
	}

	private EntityHitResult getValidTarget() {
		if (!(mc.crosshairTarget instanceof EntityHitResult entityHit)) return null;
		Entity entity = entityHit.getEntity();

		return (entity instanceof PlayerEntity player &&
				entity != mc.player &&
				player.isAlive() &&
				!WorldUtils.isShieldFacingAway(player))
				? entityHit
				: null;
	}

	private void updateShieldTracking(PlayerEntity target) {
		currentTarget = target;
		for (PlayerEntity player : mc.world.getPlayers()) {
			if (player == mc.player) continue;

			UUID uuid = player.getUuid();
			boolean isBlocking = player.isHolding(Items.SHIELD) && player.isBlocking();

			if (isBlocking) {
				if (!shieldStartTimes.containsKey(uuid)) {
					shieldStartTimes.put(uuid, System.currentTimeMillis());
				}
			} else {
				shieldStartTimes.remove(uuid);
			}
		}
	}

	private boolean shouldBreakShield(PlayerEntity target) {
		if (target == null) return false;

		UUID uuid = target.getUuid();
		if (!shieldStartTimes.containsKey(uuid)) return false;

		long holdTime = System.currentTimeMillis() - shieldStartTimes.get(uuid);
		return holdTime >= minShieldHold.getValue();
	}

	private void startBreakingSequence(PlayerEntity target) {
		isBreaking = true;
		currentTarget = target;

		if (originalSlot == -1) {
			originalSlot = mc.player.getInventory().selectedSlot;
		}
		switchClock = switchDelay.getValueInt();
	}

	private void handleBreakingSequence() {
		if (currentTarget == null || !shouldBreakShield(currentTarget)) {
			abortBreaking();
			return;
		}

		if (switchClock > 0) {
			switchClock--;
			return;
		}

		if (!InventoryUtils.selectAxe()) {
			abortBreaking();
			return;
		}

		if (hitClock > 0) {
			hitClock--;
			return;
		}
		if (clickSimulate.getValue()) {
			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		}
		WorldUtils.hitEntity(currentTarget, true);
		shieldStartTimes.remove(currentTarget.getUuid());
		if (stun.getValue()) {
			inStunSequence = true;
			stunStep = 1;
			isBreaking = false;
		} else {
			restoreOriginalSlot();
			resetState();
		}
	}

	private void abortBreaking() {
		restoreOriginalSlot();
		resetState();
	}

	private void handleStunSequence() {
		if (mc.player == null || currentTarget == null) {
			resetStunSequence();
			return;
		}

		switch (stunStep) {
			case 1: // Prepare for second hit
				if (stunSlam.getValue()) {
					mc.player.getInventory().selectedSlot = maceSlot.getValueInt() - 1;
				}
				stunStep = 2;
				break;

			case 2: // Execute second hit
				if (mc.crosshairTarget instanceof EntityHitResult entityHit) {
					Entity entity = entityHit.getEntity();
					if (entity != null) {
						if (clickSimulate.getValue()) {
							MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
						}
						WorldUtils.hitEntity(entity, true);
					}
				}
				stunStep = 3;
				break;

			case 3: // Restore to original weapon
				restoreOriginalSlot();
				resetStunSequence();
				break;
		}
	}

	private void resetStunSequence() {
		inStunSequence = false;
		stunStep = 0;
		resetState();
	}

	private void restoreOriginalSlot() {
		if (switchBack.getValue() && originalSlot != -1) {
			if (switchDelay.getValueInt() > 0) {
				if (switchClock > 0) {
					switchClock--;
				} else {
					mc.player.getInventory().selectedSlot = originalSlot;
					originalSlot = -1;
				}
			} else {
				mc.player.getInventory().selectedSlot = originalSlot;
				originalSlot = -1;
			}
		}
	}

	@Override
	public void onAttack(AttackEvent event) {
		if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
			event.cancel();
		}
	}
}