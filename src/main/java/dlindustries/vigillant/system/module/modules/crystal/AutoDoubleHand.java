package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.HudListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.utils.BlockUtils;
import dlindustries.vigillant.system.utils.EncryptedString;

import dlindustries.vigillant.system.utils.DamageUtils;

import dlindustries.vigillant.system.utils.CrystalUtils;
import dlindustries.vigillant.system.utils.RotationUtils;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class AutoDoubleHand extends Module implements HudListener {
	public enum AnchorMode {
		ALWAYS("Always"),
		CRITICAL("Critical");

		private final String name;

		AnchorMode(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private final BooleanSetting stopOnCrystal = new BooleanSetting(EncryptedString.of("Stop On Crystal"), true)
			.setDescription(EncryptedString.of("Stops while Auto Crystal is running"));
	private final BooleanSetting checkShield = new BooleanSetting(EncryptedString.of("Check Shield"), true)
			.setDescription(EncryptedString.of("Checks if you're blocking with a shield"));
	private final BooleanSetting onPop = new BooleanSetting(EncryptedString.of("On Pop"), false)
			.setDescription(EncryptedString.of("Switches to a totem if you pop"));
	private final BooleanSetting onHealth = new BooleanSetting(EncryptedString.of("On Health"), false)
			.setDescription(EncryptedString.of("Switches to totem if low on health"));
	private final BooleanSetting predict = new BooleanSetting(EncryptedString.of("Predict Damage"), true);
	private final NumberSetting health = new NumberSetting(EncryptedString.of("Health"), 1, 20, 2, 1)
			.setDescription(EncryptedString.of("Health to trigger at"));
	private final BooleanSetting onGround = new BooleanSetting(EncryptedString.of("On Ground"), false)
			.setDescription(EncryptedString.of("Whether crystal damage is checked on ground or not"));
	private final BooleanSetting checkPlayers = new BooleanSetting(EncryptedString.of("Check Players"), true)
			.setDescription(EncryptedString.of("Checks for nearby players"));
	private final NumberSetting distance = new NumberSetting(EncryptedString.of("Distance"), 1, 10, 5, 0.1)
			.setDescription(EncryptedString.of("Player distance"));
	private final BooleanSetting predictCrystals = new BooleanSetting(EncryptedString.of("Predict Crystals"), false);
	private final BooleanSetting checkAim = new BooleanSetting(EncryptedString.of("Check Aim"), true)
			.setDescription(EncryptedString.of("Checks if the opponent is aiming at obsidian"));
	private final BooleanSetting checkItems = new BooleanSetting(EncryptedString.of("Check Items"), true)
			.setDescription(EncryptedString.of("Checks if the opponent is holding crystals"));
	private final NumberSetting activatesAbove = new NumberSetting(EncryptedString.of("Activates Above"), 0, 4, 0.2, 0.1)
			.setDescription(EncryptedString.of("Height to trigger at"));
	private final BooleanSetting reduceStrictness = new BooleanSetting(EncryptedString.of("Reduce Strictness"), true)
			.setDescription(EncryptedString.of("Pause totem switch during elytra flight activation - still forces double hand if you do not have a offhand totem for safety"));
	private final BooleanSetting includeAnchor = new BooleanSetting(EncryptedString.of("Include Anchor"), true)
			.setDescription(EncryptedString.of("Detect fatal charged anchors"));
	private final ModeSetting<AnchorMode> anchorMode = new ModeSetting<>(
			EncryptedString.of("Anchor Mode"),
			AnchorMode.CRITICAL,
			AnchorMode.class
	).setDescription(EncryptedString.of("When to double hand for anchors"));

	private boolean belowHealth;
	private boolean offhandHasNoTotem;

	public AutoDoubleHand() {
		super(EncryptedString.of("Predict double hand"),
				EncryptedString.of("Automatically switches to your totem when you're about to pop"),
				-1,
				Category.CRYSTAL);
		addSettings(stopOnCrystal, checkShield, onPop, onHealth, predict, health, onGround, checkPlayers,
				distance, predictCrystals, checkAim, checkItems, activatesAbove, reduceStrictness,
				includeAnchor, anchorMode);
		belowHealth = false;
		offhandHasNoTotem = false;
	}

	@Override
	public void onEnable() {
		eventManager.add(HudListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(HudListener.class, this);
		super.onDisable();
	}

	@SuppressWarnings("all")
	@Override
	public void onRenderHud(HudEvent event) {
		if (mc.player == null) return;
		if (system.INSTANCE.getModuleManager().getModule(AutoCrystal.class).crystalling && stopOnCrystal.getValue())
			return;
		if (reduceStrictness.getValue() && isElytraEquipped()) {
			if (hasTotemInOffhand()) {
				return;
			} else {
				int totemSlot = findItemInHotbar(Items.TOTEM_OF_UNDYING);
				mc.player.getInventory().selectedSlot = totemSlot;
				return;
			}
		}

		double squaredDistance = distance.getValue() * distance.getValue();
		PlayerInventory inventory = mc.player.getInventory();

		if (checkShield.getValue() && mc.player.isBlocking()) return;

		if (inventory.offHand.get(0).getItem() != Items.TOTEM_OF_UNDYING && onPop.getValue() && !offhandHasNoTotem) {
			offhandHasNoTotem = true;
			int totemSlot = findItemInHotbar(Items.TOTEM_OF_UNDYING);
			mc.player.getInventory().selectedSlot = totemSlot;
		}


		if (inventory.offHand.get(0).getItem() == Items.TOTEM_OF_UNDYING) offhandHasNoTotem = false;

		if (mc.player.getHealth() <= health.getValue() && onHealth.getValue() && !belowHealth) {
			belowHealth = true;
			int totemSlot = findItemInHotbar(Items.TOTEM_OF_UNDYING);
			if (totemSlot != -1) mc.player.getInventory().selectedSlot = totemSlot;

		}

		if (mc.player.getHealth() > health.getValue()) belowHealth = false;

		if (!predict.getValue()) return;
		if (includeAnchor.getValue()) {
			List<BlockPos> chargedAnchors = BlockUtils.getAllInBoxStream(
							mc.player.getBlockPos().add(-6, -6, -6),
							mc.player.getBlockPos().add(6, 6, 6))
					.filter(pos -> {
						var state = mc.world.getBlockState(pos);
						return state.getBlock() == Blocks.RESPAWN_ANCHOR && state.get(RespawnAnchorBlock.CHARGES) > 0;
					})
					.toList();

			for (BlockPos anchorPos : chargedAnchors) {
				Vec3d anchorVec = Vec3d.ofCenter(anchorPos);
				float damage = DamageUtils.anchorDamage(mc.player, anchorVec);
				if (damage >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
					int totemSlot = findItemInHotbar(Items.TOTEM_OF_UNDYING);
					switch (anchorMode.getMode()) {
						case ALWAYS:
							if (totemSlot != -1) mc.player.getInventory().selectedSlot = totemSlot;
							return;
						case CRITICAL:
							if (mc.player.getInventory().offHand.get(0).getItem() != Items.TOTEM_OF_UNDYING) {
								if (totemSlot != -1) mc.player.getInventory().selectedSlot = totemSlot;
								return;
							}
							break;
					}
				}
			}
		}
		if (mc.player.getHealth() > 19) return;
		if (!onGround.getValue() && mc.player.isOnGround()) return;
		if (checkPlayers.getValue()) {
			boolean anyNearby = mc.world.getPlayers().stream()
					.filter(e -> e != mc.player)
					.anyMatch(p -> mc.player.squaredDistanceTo(p) <= squaredDistance);
			if (!anyNearby) return;
		}
		if (mc.player.getHealth() > 19) return;
		double above = activatesAbove.getValue();
		for (int floor = (int) Math.floor(above), i = 1; i <= floor; i++) {
			if (!mc.world.getBlockState(mc.player.getBlockPos().add(0, -i, 0)).isAir())
				return;
		}

		Vec3d playerPos = mc.player.getPos();
		BlockPos playerBlockPos = new BlockPos((int) playerPos.x, (int) playerPos.y - (int) above, (int) playerPos.z);
		if (!mc.world.getBlockState(new BlockPos(playerBlockPos)).isAir())
			return;

		List<EndCrystalEntity> crystals = nearbyCrystals();
		List<Vec3d> crystalPositions = new ArrayList<>();
		crystals.forEach(e -> crystalPositions.add(e.getPos()));

		if (predictCrystals.getValue()) {
			Stream<BlockPos> s = BlockUtils.getAllInBoxStream(mc.player.getBlockPos().add(-6, -8, -6), mc.player.getBlockPos().add(6, 2, 6))
					.filter(e -> mc.world.getBlockState(e).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(e).getBlock() == Blocks.BEDROCK)
					.filter(CrystalUtils::canPlaceCrystalClient);

			if (checkAim.getValue()) {
				if (checkItems.getValue())
					s = s.filter(this::arePeopleAimingAtBlockAndHoldingCrystals);
				else
					s = s.filter(this::arePeopleAimingAtBlock);
			}
			s.forEachOrdered(e -> crystalPositions.add(Vec3d.ofBottomCenter(e).add(0, 1, 0)));
		}

		for (Vec3d crys : crystalPositions) {
			if (Math.abs(mc.player.getY() - crys.y) > 3.0) continue;

			double damage = DamageUtils.crystalDamage(mc.player, crys);
			if (damage >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
				int totemSlot = findItemInHotbar(Items.TOTEM_OF_UNDYING);
				if (totemSlot != -1) mc.player.getInventory().selectedSlot = totemSlot;
				break;
			}
		}
	}


	private int findItemInHotbar(net.minecraft.item.Item item) {
		if (mc.player == null) return -1;
		for (int i = 0; i < 9; i++) {
			if (mc.player.getInventory().getStack(i).getItem() == item) return i;
		}
		return -1;
	}



	private boolean isElytraEquipped() {
		return mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA;
	}

	private boolean hasTotemInOffhand() {
		return mc.player.getInventory().offHand.get(0).getItem() == Items.TOTEM_OF_UNDYING;
	}

	private List<EndCrystalEntity> nearbyCrystals() {
		Vec3d pos = mc.player.getPos();
		return mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(pos.add(-6.0, -6.0, -6.0), pos.add(6.0, 6.0, 6.0)), e -> true);
	}

	private boolean arePeopleAimingAtBlock(final BlockPos block) {
		final Vec3d[] eyesPos = new Vec3d[1];
		final BlockHitResult[] hitResult = new BlockHitResult[1];

		return mc.world.getPlayers().parallelStream().filter(e -> e != mc.player).anyMatch(e -> {
			eyesPos[0] = RotationUtils.getEyesPos(e);
			hitResult[0] = mc.world.raycast(new RaycastContext(eyesPos[0], eyesPos[0].add(RotationUtils.getPlayerLookVec(e).multiply(4.5)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));
			return hitResult[0] != null && hitResult[0].getBlockPos().equals(block);
		});
	}

	private boolean arePeopleAimingAtBlockAndHoldingCrystals(final BlockPos block) {
		final Vec3d[] eyesPos = new Vec3d[1];
		final BlockHitResult[] hitResult = new BlockHitResult[1];

		return mc.world.getPlayers().parallelStream().filter(e -> e != mc.player).filter(e -> e.isHolding(Items.END_CRYSTAL)).anyMatch(e -> {
			eyesPos[0] = RotationUtils.getEyesPos(e);
			hitResult[0] = mc.world.raycast(new RaycastContext(eyesPos[0], eyesPos[0].add(RotationUtils.getPlayerLookVec(e).multiply(4.5)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));

			return hitResult[0] != null && hitResult[0].getBlockPos().equals(block);
		});
	}
}