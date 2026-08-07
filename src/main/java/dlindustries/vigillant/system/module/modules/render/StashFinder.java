package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.WorldUtils;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public final class StashFinder extends Module implements TickListener {
	private final BooleanSetting chests = new BooleanSetting(EncryptedString.of("Chests"), true);
	private final BooleanSetting barrels = new BooleanSetting(EncryptedString.of("Barrels"), true);
	private final BooleanSetting shulkers = new BooleanSetting(EncryptedString.of("Shulkers"), true);
	private final BooleanSetting enderChests = new BooleanSetting(EncryptedString.of("Ender Chests"), true);
	private final BooleanSetting furnaces = new BooleanSetting(EncryptedString.of("Furnaces"), true);
	private final BooleanSetting dispensersDroppers = new BooleanSetting(EncryptedString.of("Dispensers Droppers"),
			true);
	private final BooleanSetting hoppers = new BooleanSetting(EncryptedString.of("Hoppers"), true);
	private final BooleanSetting spawners = new BooleanSetting(EncryptedString.of("Spawners"), true);
	private final BooleanSetting spawnerIsStash = new BooleanSetting(EncryptedString.of("Spawner Is Stash"), true)
			.setDescription(EncryptedString.of("Any spawner in a chunk counts as a stash"));
	private final NumberSetting minimumStorage = new NumberSetting(EncryptedString.of("Min Storage"), 1, 100, 4, 1);
	private final NumberSetting minimumDistance = new NumberSetting(EncryptedString.of("Min Distance"), 0, 10000, 0,
			50);
	private final BooleanSetting disconnectOnFind = new BooleanSetting(EncryptedString.of("Disconnect On Find"), false);

	private final Set<ChunkPos> scannedChunks = new HashSet<>();

	public StashFinder() {
		super(EncryptedString.of("Stash Finder"),
				EncryptedString.of("Alerts when loaded chunks contain stash-like storage clusters"),
				-1,
				Category.RENDER);
		addSettings(chests, barrels, shulkers, enderChests, furnaces, dispensersDroppers, hoppers, spawners,
				spawnerIsStash, minimumStorage, minimumDistance, disconnectOnFind);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		scannedChunks.clear();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		scannedChunks.clear();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null)
			return;

		WorldUtils.getLoadedChunks()
				.filter(chunk -> scannedChunks.add(chunk.getPos()))
				.forEach(this::scanChunk);
	}

	private void scanChunk(WorldChunk chunk) {
		ChunkPos pos = chunk.getPos();
		double centerX = pos.x * 16.0 + 8.0;
		double centerZ = pos.z * 16.0 + 8.0;
		double distanceFromPlayer = Math.hypot(centerX - mc.player.getX(), centerZ - mc.player.getZ());
		if (distanceFromPlayer < minimumDistance.getValue())
			return;

		int chestCount = 0;
		int barrelCount = 0;
		int shulkerCount = 0;
		int enderChestCount = 0;
		int furnaceCount = 0;
		int dispenserDropperCount = 0;
		int hopperCount = 0;
		int spawnerCount = 0;

		for (var blockEntity : chunk.getBlockEntities().values()) {
			if (spawners.getValue() && blockEntity instanceof MobSpawnerBlockEntity) {
				spawnerCount++;
				continue;
			}
			if (chests.getValue() && blockEntity instanceof ChestBlockEntity)
				chestCount++;
			else if (barrels.getValue() && blockEntity instanceof BarrelBlockEntity)
				barrelCount++;
			else if (shulkers.getValue() && blockEntity instanceof ShulkerBoxBlockEntity)
				shulkerCount++;
			else if (enderChests.getValue() && blockEntity instanceof EnderChestBlockEntity)
				enderChestCount++;
			else if (furnaces.getValue() && blockEntity instanceof AbstractFurnaceBlockEntity)
				furnaceCount++;
			else if (dispensersDroppers.getValue()
					&& (blockEntity instanceof DispenserBlockEntity || blockEntity instanceof DropperBlockEntity))
				dispenserDropperCount++;
			else if (hoppers.getValue() && blockEntity instanceof HopperBlockEntity)
				hopperCount++;
		}

		int storageCount = chestCount + barrelCount + shulkerCount + enderChestCount + furnaceCount
				+ dispenserDropperCount + hopperCount;
		boolean criticalSpawner = spawnerIsStash.getValue() && spawnerCount > 0;
		if (!criticalSpawner && storageCount < minimumStorage.getValueInt())
			return;

		int x = (int) centerX;
		int z = (int) centerZ;
		String stashType = criticalSpawner ? "spawner base" : "stash";
		StringBuilder breakdown = new StringBuilder();
		appendCount(breakdown, "spawners", spawnerCount);
		appendCount(breakdown, "chests", chestCount);
		appendCount(breakdown, "barrels", barrelCount);
		appendCount(breakdown, "shulkers", shulkerCount);
		appendCount(breakdown, "ender", enderChestCount);
		appendCount(breakdown, "furnaces", furnaceCount);
		appendCount(breakdown, "disp/drop", dispenserDropperCount);
		appendCount(breakdown, "hoppers", hopperCount);

		String message = "§a[Stash Finder] §fFound " + stashType + " at §e" + x + ", " + z
				+ " §7(" + (criticalSpawner ? "spawner" : "storage=" + storageCount) + ") §8" + breakdown;
		mc.player.sendMessage(Text.literal(message), false);
		mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_BOTTLE_THROW, 1.0F, 1.0F);

		if (disconnectOnFind.getValue()) {
			setEnabled(false);
			if (mc.world != null)
				mc.world.disconnect(null);
		}
	}

	private static void appendCount(StringBuilder builder, String label, int count) {
		if (count > 0) {
			if (!builder.isEmpty())
				builder.append(", ");
			builder.append(label).append('=').append(count);
		}
	}
}
