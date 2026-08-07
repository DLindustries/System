package dlindustries.vigillant.system.utils;

import dlindustries.vigillant.system.VigillantSystem;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Objects;
import java.util.stream.Stream;

import static dlindustries.vigillant.system.VigillantSystem.mc;

public final class WorldUtils {
	public static boolean isDeadBodyNearby() {
		return mc.world.getPlayers().parallelStream()
				.filter(e -> e != mc.player)
				.filter(e -> e.squaredDistanceTo(mc.player) <= 36)
				.anyMatch(LivingEntity::isDead);
	}

	public static Entity findNearestEntity(PlayerEntity toPlayer, float radius, boolean seeOnly) {
		float mr = Float.MAX_VALUE;
		Entity entity = null;

		assert mc.world != null;
		for (Entity e : mc.world.getEntities()) {
			float d = e.distanceTo(toPlayer);

			if (e != toPlayer && d <= radius && mc.player.canSee(e) == seeOnly) {
				if (d < mr) {
					mr = d;
					entity = e;
				}
			}
		}
		return entity;
	}

	public static double distance(Vec3d fromVec, Vec3d toVec) {
		return Math.sqrt(Math.pow(toVec.x - fromVec.x, 2) + Math.pow(toVec.y - fromVec.y, 2) + Math.pow(toVec.z - fromVec.z, 2));
	}

	public static PlayerEntity findNearestPlayer(PlayerEntity toPlayer, float range, boolean seeOnly, boolean excludeFriends) {
		if (toPlayer == null || mc.world == null) return null;

		float minRange = Float.MAX_VALUE;
		PlayerEntity minPlayer = null;

		for (PlayerEntity player : mc.world.getPlayers()) {
			float distance = (float) distance(new Vec3d(toPlayer.getX(), toPlayer.getY(), toPlayer.getZ()), new Vec3d(player.getX(), player.getY(), player.getZ()));
			if (player == toPlayer) continue;
			if (excludeFriends && VigillantSystem.INSTANCE != null && VigillantSystem.INSTANCE.getFriendManager() != null && VigillantSystem.INSTANCE.getFriendManager().isFriend(player)) {
				continue;
			}
			if (distance <= range && toPlayer.canSee(player) == seeOnly) {
				if (distance < minRange) {
					minRange = distance;
					minPlayer = player;
				}
			}
		}

		return minPlayer;
	}

	public static boolean isValuableLootNearby() {
		if (mc.player == null || mc.world == null) return false;

		Box area = new Box(
				mc.player.getX() - 10, mc.player.getY() - 5, mc.player.getZ() - 10,
				mc.player.getX() + 10, mc.player.getY() + 5, mc.player.getZ() + 10
		);

		int valuableArmorCount = 0;
		for (Entity entity : mc.world.getEntities()) {
			if (!(entity instanceof ItemEntity itemEntity)) continue;
			if (!area.contains(new Vec3d(entity.getX(), entity.getY(), entity.getZ()))) continue;

			ItemStack stack = itemEntity.getStack();
			Item item = stack.getItem();
			int count = stack.getCount();
			if (isDiamondOrNetheriteArmor(item)) {
				valuableArmorCount++;
			}
			else if (count > 32 && (item == Items.END_CRYSTAL
					|| item == Items.OBSIDIAN
					|| item == Items.ENDER_PEARL
					|| item == Items.EXPERIENCE_BOTTLE)) {
				return true;
			}
		}
		return valuableArmorCount >= 2;
	}

	public static Vec3d getPlayerLookVec(float yaw, float pitch) {
		float f = pitch * 0.017453292F;
		float g = -yaw * 0.017453292F;

		float h = MathHelper.cos(g);
		float i = MathHelper.sin(g);
		float j = MathHelper.cos(f);
		float k = MathHelper.sin(f);

		return new Vec3d((i * j), (-k), (h * j));
	}

	public static Vec3d getPlayerLookVec(PlayerEntity player) {
		return getPlayerLookVec(player.getYaw(), player.getPitch());
	}

	public static HitResult getHitResult(double radius) {
		return getHitResult(mc.player, false, mc.player.getYaw(), mc.player.getPitch(), radius);
	}

	public static HitResult getHitResult(PlayerEntity entity, boolean ignoreInvisibles, float yaw, float pitch, double distance) {
		if (entity == null || mc.world == null) return null;

		double d = distance;
		Vec3d cameraPosVec = entity.getCameraPosVec(mc.getRenderTickCounter().getDynamicDeltaTicks());
		Vec3d rotationVec = getPlayerLookVec(yaw, pitch);
		Vec3d range = cameraPosVec.add(rotationVec.x * d, rotationVec.y * d, rotationVec.z * d);

		HitResult result = mc.world.raycast(new RaycastContext(cameraPosVec, range, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, entity));

		double e = d * d;
		d = distance;

		if (result != null) {
			e = result.getPos().squaredDistanceTo(cameraPosVec);
		}

		Vec3d vec3d3 = cameraPosVec.add(rotationVec.x * d, rotationVec.y * d, rotationVec.z * d);
		Box box = entity.getBoundingBox().stretch(rotationVec.multiply(d)).expand(1.0, 1.0, 1.0);

		EntityHitResult entityHitResult = ProjectileUtil.raycast(entity, cameraPosVec, vec3d3, box, (entityx) ->
				!entityx.isSpectator() && entityx.canHit() && (!entityx.isInvisible() || !ignoreInvisibles), e);

		if (entityHitResult != null) {
			Vec3d vec3d4 = entityHitResult.getPos();
			double g = cameraPosVec.squaredDistanceTo(vec3d4);

			if ((distance > 0.0 && g > Math.pow(distance, 2)) || (g < e || result == null)) {
				result = g > Math.pow(distance, 2)
						? BlockHitResult.createMissed(vec3d4, Direction.getFacing(rotationVec.x, rotationVec.y, rotationVec.z), BlockPos.ofFloored(vec3d4))
						: entityHitResult;
			}
		}

		return result;
	}


	public static void placeBlock(BlockHitResult blockHit, boolean swingHand) {
		ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
		if (result.isAccepted() && swingHand) mc.player.swingHand(Hand.MAIN_HAND);
	}

	public static Stream<WorldChunk> getLoadedChunks() {
		int radius = Math.max(2, mc.options.getClampedViewDistance()) + 3;
		int diameter = radius * 2 + 1;

		ChunkPos center = mc.player.getChunkPos();
		ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
		ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);

		return Stream.iterate(min, pos -> {
					int x = pos.x;
					int z = pos.z;
					x++;
					if (x > max.x) {
						x = min.x;
						z++;
					}
					if (z > max.z)
						throw new IllegalStateException("Stream limit didn't work.");

					return new ChunkPos(x, z);

				}).limit((long) diameter * diameter)
				.filter(c -> mc.world.isChunkLoaded(c.x, c.z))
				.map(c -> mc.world.getChunk(c.x, c.z)).filter(Objects::nonNull);
	}

    /*
                NORTH

        WEST      +      EAST

                SOUTH
     */

	public static boolean isShieldFacingAway(PlayerEntity player) {
		if (mc.player != null && player != null) {
			Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
			Vec3d targetPos = new Vec3d(player.getX(), player.getY(), player.getZ());

			Vec3d directionToPlayer = playerPos.subtract(targetPos).normalize();

			float yaw = player.getYaw();
			float pitch = player.getPitch();
			Vec3d facingDirection = new Vec3d(
					-Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
					-Math.sin(Math.toRadians(pitch)),
					Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))
			).normalize();

			double dotProduct = facingDirection.dotProduct(directionToPlayer);

			return dotProduct < 0;
		}
		return false;
	}

	public static boolean isTool(ItemStack itemStack) {
		Item item = itemStack.getItem();
		return item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD
				|| item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE
				|| item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE
				|| item == Items.DIAMOND_SHOVEL || item == Items.NETHERITE_SHOVEL
				|| item == Items.DIAMOND_HOE || item == Items.NETHERITE_HOE
				|| item == Items.MACE;
	}

	private static boolean isDiamondOrNetheriteArmor(Item item) {
		return item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE
				|| item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS
				|| item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE
				|| item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS;
	}

	public static boolean isCrit(PlayerEntity player, Entity target) {
		return player.getAttackCooldownProgress(0.5F) > 0.9F && player.fallDistance > 0.0F && !player.isOnGround() && !player.isClimbing() && !player.isSubmergedInWater() && !player.hasStatusEffect(StatusEffects.BLINDNESS) && target instanceof LivingEntity;
	}

	public static void hitEntity(Entity entity, boolean swingHand) {
		mc.interactionManager.attackEntity(mc.player, entity);

		if (swingHand)
			mc.player.swingHand(Hand.MAIN_HAND);
	}
}
