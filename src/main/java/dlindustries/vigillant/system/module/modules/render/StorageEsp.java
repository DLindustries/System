package dlindustries.vigillant.system.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dlindustries.vigillant.system.event.events.GameRenderListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.RenderUtils;
import net.minecraft.block.entity.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;
import java.util.List;

public final class StorageEsp extends Module implements GameRenderListener {

    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 10, 200, 50, 5);
    private final BooleanSetting chests = new BooleanSetting(EncryptedString.of("Chests"), true);
    private final BooleanSetting enderChests = new BooleanSetting(EncryptedString.of("Ender Chests"), true);
    private final BooleanSetting shulkers = new BooleanSetting(EncryptedString.of("Shulkers"), true);
    private final BooleanSetting barrels = new BooleanSetting(EncryptedString.of("Barrels"), true);
    private final BooleanSetting dispensers = new BooleanSetting(EncryptedString.of("Dispensers"), false);
    private final BooleanSetting hoppers = new BooleanSetting(EncryptedString.of("Hoppers"), false);
    private final BooleanSetting spawners = new BooleanSetting(EncryptedString.of("Spawners"), true);
    private final BooleanSetting furnaces = new BooleanSetting(EncryptedString.of("Furnaces"), true);
    private final BooleanSetting fill = new BooleanSetting(EncryptedString.of("Fill"), true);
    private final BooleanSetting outline = new BooleanSetting(EncryptedString.of("Outline"), true);
    private final BooleanSetting tracers = new BooleanSetting(EncryptedString.of("Tracers"), false);
    private final NumberSetting fillAlpha = new NumberSetting(EncryptedString.of("Fill Alpha"), 0, 255, 77, 1);
    private final NumberSetting outlineOpacity = new NumberSetting(EncryptedString.of("Outline Opacity"), 0, 100, 100,
            5);

    public StorageEsp() {
        super(
                EncryptedString.of("Storage ESP"),
                EncryptedString.of("Highlights storage blocks through walls"),
                -1,
                Category.RENDER);
        addSettings(
                range, chests, enderChests, shulkers, barrels, dispensers, hoppers, spawners, furnaces,
                fill, outline, tracers, fillAlpha, outlineOpacity);
    }

    @Override
    public void onEnable() {
        eventManager.add(GameRenderListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(GameRenderListener.class, this);
        super.onDisable();
    }

    @Override
    public void onGameRender(GameRenderEvent event) {
        if (mc.player == null || mc.world == null)
            return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        MatrixStack matrices = event.matrices;
        double maxRangeSq = range.getValue() * range.getValue();
        Vec3d tracerStart = camPos.add(mc.player.getRotationVec(event.delta).multiply(10.0));

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180F));
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        int viewDist = mc.options.getViewDistance().getValue();
        int playerChunkX = mc.player.getBlockX() >> 4;
        int playerChunkZ = mc.player.getBlockZ() >> 4;

        for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
            for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null)
                    continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!isStorage(blockEntity))
                        continue;

                    BlockPos pos = blockEntity.getPos();
                    if (mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > maxRangeSq)
                        continue;

                    String blockName = blockEntity.getCachedState().getBlock().getTranslationKey();
                    if (!shouldHighlight(blockName))
                        continue;

                    List<Box> boxes = blockEntity.getCachedState().getOutlineShape(mc.world, pos).getBoundingBoxes();
                    if (boxes.isEmpty()) {
                        renderBox(matrices, new Box(pos), blockName, tracerStart);
                    } else {
                        for (Box part : boxes) {
                            renderBox(matrices, part.expand(0.002).offset(pos), blockName, tracerStart);
                        }
                    }
                }
            }
        }

        matrices.pop();
    }

    private void renderBox(MatrixStack matrices, Box box, String blockName, Vec3d tracerStart) {
        if (fill.getValue()) {
            RenderUtils.renderFilledBox(matrices, box, getFillColor(blockName));
        }
        if (outline.getValue()) {
            RenderUtils.renderOutlinedBox(matrices, box, getOutlineColor(blockName));
        }
        if (tracers.getValue()) {
            Vec3d center = new Vec3d(
                    (box.minX + box.maxX) / 2.0,
                    (box.minY + box.maxY) / 2.0,
                    (box.minZ + box.maxZ) / 2.0);
            RenderUtils.renderLine(matrices, tracerStart, center, getOutlineColor(blockName));
        }
    }

    private static boolean isStorage(BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof EnderChestBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof DispenserBlockEntity
                || blockEntity instanceof HopperBlockEntity
                || blockEntity instanceof MobSpawnerBlockEntity
                || blockEntity instanceof AbstractFurnaceBlockEntity;
    }

    private boolean shouldHighlight(String blockName) {
        String name = blockName.toLowerCase();
        if (chests.getValue() && name.contains("chest") && !name.contains("ender"))
            return true;
        if (enderChests.getValue() && name.contains("ender_chest"))
            return true;
        if (shulkers.getValue() && name.contains("shulker"))
            return true;
        if (barrels.getValue() && name.contains("barrel"))
            return true;
        if (dispensers.getValue() && (name.contains("dispenser") || name.contains("dropper")))
            return true;
        if (hoppers.getValue() && name.contains("hopper"))
            return true;
        if (spawners.getValue() && name.contains("spawner"))
            return true;
        return furnaces.getValue() && name.contains("furnace");
    }

    private Color getBaseColor(String blockName) {
        String name = blockName.toLowerCase();
        if (name.contains("ender_chest"))
            return new Color(255, 0, 255);
        if (name.contains("shulker"))
            return new Color(139, 92, 246);
        if (name.contains("barrel"))
            return new Color(139, 69, 19);
        if (name.contains("dispenser") || name.contains("dropper"))
            return new Color(128, 128, 128);
        if (name.contains("hopper"))
            return new Color(64, 64, 64);
        if (name.contains("spawner"))
            return new Color(255, 69, 0);
        if (name.contains("furnace"))
            return new Color(112, 128, 144);
        return new Color(235, 198, 52);
    }

    private Color getFillColor(String blockName) {
        Color base = getBaseColor(blockName);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), fillAlpha.getValueInt());
    }

    private Color getOutlineColor(String blockName) {
        Color base = getBaseColor(blockName);
        int alpha = (int) (outlineOpacity.getValue() * 2.55);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }
}
