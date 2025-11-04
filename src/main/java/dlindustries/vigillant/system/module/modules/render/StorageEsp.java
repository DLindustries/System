package dlindustries.vigillant.system.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dlindustries.vigillant.system.event.events.GameRenderListener;
import dlindustries.vigillant.system.event.events.PacketReceiveListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class StorageEsp extends Module implements GameRenderListener, PacketReceiveListener {
    private final BooleanSetting tracers = new BooleanSetting("Tracers", false)
            .setDescription("Draws lines from player to storage blocks");
    private final BooleanSetting donutBypass = new BooleanSetting("Possible Bypass", false)
            .setDescription("Prevents chunk updates from anti esp");
    private final NumberSetting transparency = new NumberSetting("Transparency", 1, 255, 80, 1)
            .setDescription("Opacity of the ESP boxes");
    private final BooleanSetting renderOutline = new BooleanSetting("Outline Only", false)
            .setDescription("Render outlined boxes instead of filled");
    private final StorageEspGroup chests = new StorageEspGroup("Chests", new Color(156, 91, 0));
    private final StorageEspGroup trappedChests = new StorageEspGroup("Trapped Chests", new Color(200, 91, 0));
    private final StorageEspGroup enderChests = new StorageEspGroup("Ender Chests", new Color(131, 44, 236));
    private final StorageEspGroup shulkers = new StorageEspGroup("Shulkers", new Color(0, 153, 158));
    private final StorageEspGroup furnaces = new StorageEspGroup("Furnaces", new Color(125, 125, 125));
    private final StorageEspGroup barrels = new StorageEspGroup("Barrels", new Color(255, 0, 0));
    private final StorageEspGroup enchantTables = new StorageEspGroup("Enchant Tables", new Color(80, 80, 255));
    private final StorageEspGroup spawners = new StorageEspGroup("Spawners", new Color(27, 207, 0));
    private final StorageEspGroup anvils = new StorageEspGroup("Anvils (LAGGY)", new Color(0, 0, 0));

    private final List<StorageEspGroup> groups = List.of(
            chests, trappedChests, enderChests, shulkers,
            furnaces, barrels, enchantTables, spawners, anvils
    );
    private int tickCounter = 0;
    private static final int ANVIL_UPDATE_INTERVAL = 200;
    private final List<BlockPos> anvilCache = new ArrayList<>();

    public StorageEsp() {
        super(EncryptedString.of("Storage ESP"),
                EncryptedString.of("Renders Storage through walls"),
                -1,
                Category.RENDER);
        addSettings(donutBypass, transparency, tracers, renderOutline);
        groups.forEach(group -> addSetting(group.enabled));
    }

    @Override
    public void onEnable() {
        eventManager.add(GameRenderListener.class, this);
        eventManager.add(PacketReceiveListener.class, this);
        anvilCache.clear();
        tickCounter = 0;
    }

    @Override
    public void onDisable() {
        eventManager.remove(GameRenderListener.class, this);
        eventManager.remove(PacketReceiveListener.class, this);
        anvilCache.clear();
    }

    @Override
    public void onGameRender(GameRenderListener.GameRenderEvent event) {
        if (mc.world == null || mc.player == null) return;


        groups.forEach(StorageEspGroup::clear);


        if (anvils.isEnabled()) {
            tickCounter++;
            if (tickCounter >= ANVIL_UPDATE_INTERVAL) {
                updateAnvilCache();
                tickCounter = 0;
            }
            anvils.positions.addAll(anvilCache);
        }

        int viewDist = mc.options.getClampedViewDistance();
        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;

        for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
            for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz, false);
                if (chunk != null) {
                    processChunk(chunk);
                }
            }
        }


        renderStorages(event);
    }

    private void updateAnvilCache() {
        anvilCache.clear();
        if (!anvils.isEnabled()) return;

        int viewDist = mc.options.getClampedViewDistance();
        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;

        for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
            for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz, false);
                if (chunk != null) {
                    scanChunkForAnvils(chunk);
                }
            }
        }
    }

    private void scanChunkForAnvils(WorldChunk chunk) {
        int startX = chunk.getPos().x * 16;
        int startZ = chunk.getPos().z * 16;
        int endX = startX + 15;
        int endZ = startZ + 15;

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {

                for (int y = mc.world.getTopY(); y >= mc.world.getBottomY() + 10; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) {
                        anvilCache.add(pos);
                        break;
                    }
                }
            }
        }
    }

    private void processChunk(WorldChunk chunk) {
        for (BlockPos pos : chunk.getBlockEntityPositions()) {
            BlockEntity blockEntity = mc.world.getBlockEntity(pos);
            if (blockEntity == null) continue;

            if (blockEntity instanceof TrappedChestBlockEntity && trappedChests.isEnabled()) {
                trappedChests.add(pos);
            } else if (blockEntity instanceof ChestBlockEntity && chests.isEnabled()) {
                chests.add(pos);
            } else if (blockEntity instanceof EnderChestBlockEntity && enderChests.isEnabled()) {
                enderChests.add(pos);
            } else if (blockEntity instanceof ShulkerBoxBlockEntity && shulkers.isEnabled()) {
                shulkers.add(pos);
            } else if (blockEntity instanceof FurnaceBlockEntity && furnaces.isEnabled()) {
                furnaces.add(pos);
            } else if (blockEntity instanceof BarrelBlockEntity && barrels.isEnabled()) {
                barrels.add(pos);
            } else if (blockEntity instanceof EnchantingTableBlockEntity && enchantTables.isEnabled()) {
                enchantTables.add(pos);
            } else if (blockEntity instanceof MobSpawnerBlockEntity && spawners.isEnabled()) {
                spawners.add(pos);
            }
        }
    }

    private void renderStorages(GameRenderListener.GameRenderEvent event) {
        MatrixStack matrices = event.matrices;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthFunc(GL11.GL_ALWAYS);  // Render through walls

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180));
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (StorageEspGroup group : groups) {
            if (!group.isEnabled()) continue;

            Color baseColor = group.color;
            Color boxColor = new Color(
                    baseColor.getRed(),
                    baseColor.getGreen(),
                    baseColor.getBlue(),
                    transparency.getValueInt()
            );

            for (BlockPos pos : group.positions) {
                Box box = new Box(pos);
                if (renderOutline.getValue()) {
                    RenderUtils.renderOutlinedBox(matrices, box, boxColor);
                } else {
                    RenderUtils.renderFilledBox(matrices, box, boxColor);
                }
            }

            if (tracers.getValue()) {
                Vec3d start = new Vec3d(0, 0, 0);
                for (BlockPos pos : group.positions) {
                    Vec3d end = Vec3d.ofCenter(pos).subtract(cameraPos);
                    RenderUtils.renderLine(matrices, start, end, baseColor);
                }
            }
        }

        matrices.pop();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @Override
    public void onPacketReceive(PacketReceiveListener.PacketReceiveEvent event) {
        if (donutBypass.getValue() && event.packet instanceof ChunkDeltaUpdateS2CPacket) {
            event.cancel();
        }
    }

    private static class StorageEspGroup {
        private final BooleanSetting enabled;
        private final Color color;
        private final List<BlockPos> positions = new ArrayList<>();

        public StorageEspGroup(String name, Color defaultColor) {
            this.enabled = new BooleanSetting(name, true);
            this.color = defaultColor;
        }

        public void add(BlockPos pos) {
            positions.add(pos);
        }

        public void clear() {
            positions.clear();
        }

        public boolean isEnabled() {
            return enabled.getValue();
        }
    }
}