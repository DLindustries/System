package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.event.events.GameRenderListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.RenderUtils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.*;

import java.awt.Color;
import java.nio.channels.Channels;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.CopyUtils;

public final class ChunkFinder extends Module implements GameRenderListener, TickListener {

    private static final boolean USE_THREADS = true;
    private static final int THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    private static final int SCAN_DELAY_MS = 100;
    private static final int MAX_CONCURRENT_SCANS = 3;
    private static final long RESET_INTERVAL_MS = 500;

    private final Set<ChunkPos> flaggedChunks = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<ChunkPos, ChunkAnalysis> chunkData = new ConcurrentHashMap<>();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
    private final Queue<ChunkPos> scanQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong activeScans = new AtomicLong(0);

    private final BooleanSetting alertCoords = new BooleanSetting(EncryptedString.of("Alert Coordinates"), true);
    private final BooleanSetting ignorePlayerChunk = new BooleanSetting(EncryptedString.of("Ignore Player Chunk"),
            true);
    private final BooleanSetting showReasons = new BooleanSetting(EncryptedString.of("Show Reasons"), true);
    private final BooleanSetting detectItems = new BooleanSetting(EncryptedString.of("Check Items"), true);
    private final NumberSetting maxItems = new NumberSetting(EncryptedString.of("Max Items"), 0, 100, 3, 1);
    private final BooleanSetting detectXP = new BooleanSetting(EncryptedString.of("Check XP Orbs"), true);
    private final NumberSetting maxXP = new NumberSetting(EncryptedString.of("Max XP Orbs"), 0, 100, 3, 1);
    private final NumberSetting deepslateThreshold = new NumberSetting(EncryptedString.of("Deepslate Limit"), 1, 50, 3,
            1);
    private final NumberSetting rotatedThreshold = new NumberSetting(EncryptedString.of("Rotated Deepslate Limit"), 1,
            20, 1, 1);

    private final Map<ChunkPos, Integer> chunkItemCounts = new ConcurrentHashMap<>();
    private final Map<ChunkPos, Integer> chunkXPCounts = new ConcurrentHashMap<>();

    // Detection settings (hardcoded flags matching original)
    private final boolean detectDeepslate = false;
    private final boolean detectRotatedDeepslate = true;
    private final boolean detectLongDripstone = true;
    private final int minDripstoneLength = 7;
    private final boolean detectFullVines = true;
    private final int minVineLength = 30;
    private final boolean detectFullKelp = true;
    private final int minKelpLength = 6;
    private final boolean detectDioriteVeins = true;
    private final int minDioriteVeinLength = 5;
    private final boolean detectObsidianVeins = true;
    private final int minObsidianVeinLength = 15;
    private final int minScanY = -5;
    private final int maxScanY = 25;
    private final int scanRadius = 16;

    private ChunkPos lastPlayerChunk = null;
    private ExecutorService pool;
    private volatile boolean scanning = false;
    private long lastResetTime = 0;

    public ChunkFinder() {
        super(EncryptedString.of("Chunk Finder"),
                EncryptedString.of("Detects suspicious chunks that might contain bases."), -1, Category.RENDER);
        addSettings(alertCoords, ignorePlayerChunk, showReasons, detectItems, maxItems, detectXP, maxXP,
                deepslateThreshold, rotatedThreshold);
    }

    private void hardReset() {
        scanning = false;
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
        scannedChunks.clear();
        chunkData.clear();
        scanQueue.clear();
        lastPlayerChunk = null;
        scanning = true;
        if (USE_THREADS) {
            pool = Executors.newFixedThreadPool(THREAD_COUNT);
        }
    }

    @Override
    public void onTick() {
        if (mc.world == null) {
            if (scanning) {
                scanning = false;
                if (pool != null) {
                    pool.shutdownNow();
                    pool = null;
                }
                scannedChunks.clear();
                flaggedChunks.clear();
                chunkData.clear();
                scanQueue.clear();
                lastPlayerChunk = null;
            }
            return;
        }

        // Update entity item/xp counts
        chunkItemCounts.clear();
        chunkXPCounts.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity) {
                chunkItemCounts.merge(entity.getChunkPos(), 1, Integer::sum);
            } else if (entity instanceof ExperienceOrbEntity) {
                chunkXPCounts.merge(entity.getChunkPos(), 1, Integer::sum);
            }
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetTime >= RESET_INTERVAL_MS) {
            hardReset();
            lastResetTime = currentTime;
        }
    }

    @Override
    public void onEnable() {
        eventManager.add(GameRenderListener.class, this);
        eventManager.add(TickListener.class, this);
        scanning = true;
        scannedChunks.clear();
        flaggedChunks.clear();
        chunkData.clear();
        scanQueue.clear();
        lastPlayerChunk = null;
        if (USE_THREADS) {
            pool = Executors.newFixedThreadPool(THREAD_COUNT);
        }
    }

    @Override
    public void onDisable() {
        eventManager.remove(GameRenderListener.class, this);
        eventManager.remove(TickListener.class, this);
        scanning = false;
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
        scannedChunks.clear();
        flaggedChunks.clear();
        chunkData.clear();
        scanQueue.clear();
        lastPlayerChunk = null;
    }

    @Override
    public void onGameRender(GameRenderEvent event) {
        if (mc.player == null || mc.world == null)
            return;

        if (lastPlayerChunk == null && scanning) {
            int playerChunkX = (int) Math.floor(mc.player.getX() / 16.0);
            int playerChunkZ = (int) Math.floor(mc.player.getZ() / 16.0);
            lastPlayerChunk = new ChunkPos(playerChunkX, playerChunkZ);
            buildBFSScanQueue(lastPlayerChunk);
            tryStartScans();
        }

        updateScanQueue();
        tryStartScans();
        renderFlaggedChunks(event.matrices);
    }

    private void updateScanQueue() {
        if (mc.player == null)
            return;
        int playerChunkX = (int) Math.floor(mc.player.getX() / 16.0);
        int playerChunkZ = (int) Math.floor(mc.player.getZ() / 16.0);
        ChunkPos currentPlayerChunk = new ChunkPos(playerChunkX, playerChunkZ);
        if (currentPlayerChunk.equals(lastPlayerChunk))
            return;
        lastPlayerChunk = currentPlayerChunk;
        cleanupDistantChunks(currentPlayerChunk);
        scanQueue.clear();
        buildBFSScanQueue(currentPlayerChunk);
    }

    private void cleanupDistantChunks(ChunkPos center) {
        int cleanupRadius = scanRadius + 2;
        scannedChunks.removeIf(chunk -> {
            int dx = Math.abs(chunk.x - center.x);
            int dz = Math.abs(chunk.z - center.z);
            return dx > cleanupRadius || dz > cleanupRadius;
        });
    }

    private void buildBFSScanQueue(ChunkPos center) {
        Set<ChunkPos> visited = new HashSet<>();
        Queue<ChunkPos> bfsQueue = new LinkedList<>();
        bfsQueue.offer(center);
        visited.add(center);
        while (!bfsQueue.isEmpty()) {
            ChunkPos current = bfsQueue.poll();
            if (!scannedChunks.contains(current)) {
                scanQueue.offer(current);
            }
            int[][] offsets = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };
            for (int[] offset : offsets) {
                ChunkPos neighbor = new ChunkPos(current.x + offset[0], current.z + offset[1]);
                int dx = Math.abs(neighbor.x - center.x);
                int dz = Math.abs(neighbor.z - center.z);
                if (dx <= scanRadius && dz <= scanRadius && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    bfsQueue.offer(neighbor);
                }
            }
        }
    }

    private void tryStartScans() {
        if (!scanning || mc.world == null || mc.player == null)
            return;
        while (activeScans.get() < MAX_CONCURRENT_SCANS && !scanQueue.isEmpty()) {
            ChunkPos pos = scanQueue.poll();
            if (pos == null || scannedChunks.contains(pos))
                continue;
            scannedChunks.add(pos);
            Runnable task = () -> analyzeChunk(pos);
            if (USE_THREADS && pool != null) {
                pool.submit(wrapScanTask(task));
            } else {
                wrapScanTask(task).run();
            }
        }
    }

    private Runnable wrapScanTask(Runnable task) {
        return () -> {
            activeScans.incrementAndGet();
            try {
                task.run();
                Thread.sleep(SCAN_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                activeScans.decrementAndGet();
            }
        };
    }

    private void analyzeChunk(ChunkPos pos) {
        if (mc.world == null)
            return;
        int startX = pos.getStartX();
        int startZ = pos.getStartZ();
        int minY = Math.max(minScanY, mc.world.getBottomY());
        int maxY = Math.min(maxScanY, mc.world.getTopY(null, null) - 1);
        ChunkAnalysis analysis = new ChunkAnalysis();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (!scanning)
                        return;
                    BlockPos bp = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = mc.world.getBlockState(bp);
                    analyzeBlock(bp, state, y, analysis);
                }
            }
        }

        if (detectLongDripstone) {
            BlockPos p = checkHasLongDripstone(pos);
            if (p != null) {
                analysis.hasLongDripstone = true;
                if (analysis.susBlockPos == null)
                    analysis.susBlockPos = p;
            }
        }
        if (detectFullVines) {
            BlockPos p = checkHasLongVine(pos);
            if (p != null) {
                analysis.hasLongVine = true;
                if (analysis.susBlockPos == null)
                    analysis.susBlockPos = p;
            }
        }
        if (detectFullKelp) {
            BlockPos p = checkAllKelpFullyGrown(pos);
            if (p != null) {
                analysis.allKelpFull = true;
                if (analysis.susBlockPos == null)
                    analysis.susBlockPos = p;
            }
        }
        if (detectDioriteVeins) {
            BlockPos p = checkHasDioriteVein(pos);
            if (p != null) {
                analysis.hasDioriteVein = true;
                if (analysis.susBlockPos == null)
                    analysis.susBlockPos = p;
            }
        }
        if (detectObsidianVeins) {
            BlockPos p = checkHasObsidianVein(pos);
            if (p != null) {
                analysis.hasObsidianVein = true;
                if (analysis.susBlockPos == null)
                    analysis.susBlockPos = p;
            }
        }

        chunkData.put(pos, analysis);
        evaluateChunk(pos, analysis);
    }

    private void analyzeBlock(BlockPos pos, BlockState state, int worldY, ChunkAnalysis analysis) {
        if (detectDeepslate && isNormalDeepslate(state) && worldY >= 8) {
            analysis.deepslateCount++;
            if (analysis.susBlockPos == null)
                analysis.susBlockPos = pos;
        }
        if (detectRotatedDeepslate && isRotatedDeepslate(state)) {
            analysis.rotatedCount++;
            if (analysis.susBlockPos == null)
                analysis.susBlockPos = pos;
        }
    }

    private BlockPos checkHasDioriteVein(ChunkPos chunkPos) {
        if (mc.world == null)
            return null;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int yMin = Math.max(mc.world.getBottomY(), -64);
        int yMax = mc.world.getTopY(null, null);
        Set<BlockPos> visited = new HashSet<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yMin; y < yMax; y++) {
                    if (!scanning)
                        return null;
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    if (visited.contains(pos))
                        continue;
                    BlockState state = mc.world.getBlockState(pos);
                    if (!isTargetBlock(state))
                        continue;
                    int lenUp = countVerticalRun(pos, Direction.UP);
                    int lenDown = countVerticalRun(pos, Direction.DOWN);
                    int total = lenUp + 1 + lenDown;
                    if (total >= minDioriteVeinLength) {
                        BlockPos start = pos.offset(Direction.DOWN, lenDown);
                        boolean enclosed = true;
                        for (int i = 0; i < total; i++) {
                            BlockPos bp = start.offset(Direction.UP, i);
                            visited.add(bp);
                            if (!isEnclosedByStone(bp))
                                enclosed = false;
                        }
                        if (enclosed)
                            return pos;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos checkHasObsidianVein(ChunkPos chunkPos) {
        if (mc.world == null)
            return null;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        Set<BlockPos> visited = new HashSet<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 15; y <= 63; y++) {
                    if (!scanning)
                        return null;
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    if (visited.contains(pos))
                        continue;
                    BlockState state = mc.world.getBlockState(pos);
                    if (!state.isOf(Blocks.OBSIDIAN))
                        continue;
                    int lenUp = countVerticalRunObsidian(pos, Direction.UP);
                    int lenDown = countVerticalRunObsidian(pos, Direction.DOWN);
                    int total = lenUp + 1 + lenDown;
                    if (total >= minObsidianVeinLength) {
                        BlockPos start = pos.offset(Direction.DOWN, lenDown);
                        boolean enclosed = true;
                        for (int i = 0; i < total; i++) {
                            BlockPos bp = start.offset(Direction.UP, i);
                            visited.add(bp);
                            if (!isEnclosedByNonObsidian(bp))
                                enclosed = false;
                        }
                        if (enclosed)
                            return pos;
                    }
                }
            }
        }
        return null;
    }

    private boolean isTargetBlock(BlockState state) {
        return state.isOf(Blocks.GRANITE) || state.isOf(Blocks.DIORITE) || state.isOf(Blocks.ANDESITE);
    }

    private int countVerticalRun(BlockPos from, Direction dir) {
        int count = 0;
        BlockPos.Mutable m = new BlockPos.Mutable(from.getX(), from.getY(), from.getZ());
        while (true) {
            m.move(dir);
            if (isTargetBlock(mc.world.getBlockState(m)))
                count++;
            else
                break;
            if (count > 20)
                break;
        }
        return count;
    }

    private int countVerticalRunObsidian(BlockPos from, Direction dir) {
        int count = 0;
        BlockPos.Mutable m = new BlockPos.Mutable(from.getX(), from.getY(), from.getZ());
        while (true) {
            m.move(dir);
            if (mc.world.getBlockState(m).isOf(Blocks.OBSIDIAN))
                count++;
            else
                break;
            if (count > 20)
                break;
        }
        return count;
    }

    private boolean isEnclosedByStone(BlockPos pos) {
        for (Direction d : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            if (!mc.world.getBlockState(pos.offset(d)).isOf(Blocks.STONE))
                return false;
        }
        return true;
    }

    private boolean isEnclosedByNonObsidian(BlockPos pos) {
        for (Direction d : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            if (mc.world.getBlockState(pos.offset(d)).isOf(Blocks.OBSIDIAN))
                return false;
        }
        return true;
    }

    private BlockPos checkHasLongDripstone(ChunkPos chunkPos) {
        if (mc.world == null)
            return null;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int worldMinY = mc.world.getBottomY();
        int worldMaxY = mc.world.getTopY(null, null) - 1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = worldMaxY; y >= worldMinY; y--) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.getBlock() == Blocks.POINTED_DRIPSTONE && isTopOfDripstone(pos, state)) {
                        int length = 1;
                        BlockPos current = pos.down();
                        while (current.getY() >= worldMinY && length < 50) {
                            BlockState cs = mc.world.getBlockState(current);
                            if (cs.getBlock() != Blocks.POINTED_DRIPSTONE)
                                break;
                            if (!cs.contains(Properties.VERTICAL_DIRECTION))
                                break;
                            if (cs.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN)
                                break;
                            length++;
                            current = current.down();
                        }
                        if (length >= minDripstoneLength)
                            return pos;
                    }
                }
            }
        }
        return null;
    }

    private boolean isTopOfDripstone(BlockPos pos, BlockState state) {
        if (mc.world == null || state.getBlock() != Blocks.POINTED_DRIPSTONE)
            return false;
        if (!state.contains(Properties.VERTICAL_DIRECTION))
            return false;
        if (state.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN)
            return false;
        return mc.world.getBlockState(pos.up()).getBlock() != Blocks.POINTED_DRIPSTONE;
    }

    private BlockPos checkHasLongVine(ChunkPos chunkPos) {
        if (mc.world == null)
            return null;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        Set<BlockPos> processedVineTops = ConcurrentHashMap.newKeySet();
        int scanTopY = Math.min(mc.world.getTopY(null, null) - 1, 320);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = scanTopY; y >= 40; y--) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    if (processedVineTops.contains(pos))
                        continue;
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.getBlock() != Blocks.VINE)
                        continue;
                    BlockPos topPos = pos.up();
                    BlockState topState = mc.world.getBlockState(topPos);
                    boolean isVineTop = topState.getBlock() != Blocks.VINE
                            && (topState.isSolidBlock(mc.world, topPos) || !topState.isAir());
                    if (!isVineTop)
                        continue;
                    processedVineTops.add(pos);
                    int vineLength = 1;
                    BlockPos current = pos.down();
                    while (current.getY() >= Math.max(mc.world.getBottomY(), 40)) {
                        if (mc.world.getBlockState(current).getBlock() == Blocks.VINE) {
                            vineLength++;
                            current = current.down();
                        } else
                            break;
                    }
                    if (vineLength >= minVineLength)
                        return pos;
                }
            }
        }
        return null;
    }

    private BlockPos checkAllKelpFullyGrown(ChunkPos chunkPos) {
        if (mc.world == null)
            return null;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int kelpPlantsFound = 0, fullKelpPlants = 0;
        BlockPos firstKelpPos = null;
        Set<BlockPos> processedKelpBases = ConcurrentHashMap.newKeySet();
        int worldMinY = mc.world.getBottomY();
        int worldMaxY = mc.world.getTopY(null, firstKelpPos) - 1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = worldMinY; y <= worldMaxY; y++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    if (processedKelpBases.contains(pos))
                        continue;
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.getBlock() != Blocks.KELP && state.getBlock() != Blocks.KELP_PLANT)
                        continue;
                    BlockState belowState = mc.world.getBlockState(pos.down());
                    if (belowState.getBlock() == Blocks.KELP || belowState.getBlock() == Blocks.KELP_PLANT)
                        continue;
                    processedKelpBases.add(pos);
                    if (firstKelpPos == null)
                        firstKelpPos = pos;
                    BlockPos current = pos.up();
                    boolean reachedWaterSurface = false;
                    int kelpLength = 1;
                    while (current.getY() <= worldMaxY) {
                        BlockState cs = mc.world.getBlockState(current);
                        if (cs.getBlock() == Blocks.KELP || cs.getBlock() == Blocks.KELP_PLANT) {
                            kelpLength++;
                            current = current.up();
                        } else if (!cs.getFluidState().isEmpty()) {
                            break;
                        } else {
                            reachedWaterSurface = true;
                            break;
                        }
                    }
                    if (kelpLength < minKelpLength && reachedWaterSurface)
                        continue;
                    kelpPlantsFound++;
                    if (reachedWaterSurface)
                        fullKelpPlants++;
                }
            }
        }
        if (kelpPlantsFound < 10)
            return null;
        return (kelpPlantsFound == fullKelpPlants) ? firstKelpPos : null;
    }

    private void evaluateChunk(ChunkPos pos, ChunkAnalysis analysis) {
        boolean suspicious = false;
        List<String> reasonList = new ArrayList<>();

        if (ignorePlayerChunk.getValue() && pos.equals(lastPlayerChunk)) {
            flaggedChunks.remove(pos);
            return;
        }

        if (detectItems.getValue()) {
            int items = chunkItemCounts.getOrDefault(pos, 0);
            if (items > maxItems.getValue()) {
                suspicious = true;
                reasonList.add("Items: " + items);
            }
        }
        if (detectXP.getValue()) {
            int xp = chunkXPCounts.getOrDefault(pos, 0);
            if (xp > maxXP.getValue()) {
                suspicious = true;
                reasonList.add("XP Orbs: " + xp);
            }
        }

        if (detectDeepslate && analysis.deepslateCount >= deepslateThreshold.getValue()) {
            suspicious = true;
            reasonList.add("Deepslate: " + analysis.deepslateCount);
        }
        if (detectRotatedDeepslate && analysis.rotatedCount >= rotatedThreshold.getValue()) {
            suspicious = true;
            reasonList.add("Rotated: " + analysis.rotatedCount);
        }
        if (detectLongDripstone && analysis.hasLongDripstone) {
            suspicious = true;
            reasonList.add("Long Dripstone");
        }
        if (detectFullVines && analysis.hasLongVine) {
            suspicious = true;
            reasonList.add("Long Vine");
        }
        if (detectFullKelp && analysis.allKelpFull) {
            suspicious = true;
            reasonList.add("Grown Kelp");
        }
        if (detectDioriteVeins && analysis.hasDioriteVein) {
            suspicious = true;
            reasonList.add("Diorite Vein");
        }
        if (detectObsidianVeins && analysis.hasObsidianVein) {
            suspicious = true;
            reasonList.add("Obsidian Vein");
        }

        analysis.reasons = reasonList;

        if (suspicious) {
            if (flaggedChunks.add(pos)) {
                StringBuilder reasons = new StringBuilder();
                for (String r : reasonList)
                    reasons.append(r).append(" ");
                int susBlockX = analysis.susBlockPos != null ? analysis.susBlockPos.getX() : pos.getStartX() + 8;
                int susBlockZ = analysis.susBlockPos != null ? analysis.susBlockPos.getZ() : pos.getStartZ() + 8;
                if (mc.player != null) {
                    String msg = "Suspicious Chunk at [" + susBlockX + ", " + susBlockZ + "] - "
                            + reasons.toString().trim();
                    mc.player.sendMessage(net.minecraft.text.Text.literal(msg), false);
                    try {
                        mc.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } else {
            flaggedChunks.remove(pos);
        }
    }

    private boolean isNormalDeepslate(BlockState state) {
        return state.getBlock() == Blocks.DEEPSLATE;
    }

    private boolean isRotatedDeepslate(BlockState state) {
        if (!state.contains(Properties.AXIS))
            return false;
        Direction.Axis axis = state.get(Properties.AXIS);
        if (axis == Direction.Axis.Y)
            return false;
        return state.getBlock() == Blocks.DEEPSLATE;
    }

    private void renderFlaggedChunks(MatrixStack matrices) {
        if (flaggedChunks.isEmpty())
            return;
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null)
            return;

        BlockPos camPos = cam.getBlockPos();
        int worldMinY = mc.world.getBottomY();
        int worldMaxY = mc.world.getTopY(null, camPos);
        int rendered = 0;

        // Setup world-space transform once
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180F));
        matrices.translate(-camPos.getX(), -camPos.getY(), -camPos.getZ());

        for (ChunkPos pos : flaggedChunks) {
            if (rendered++ >= 50)
                break;

            double startX = pos.getStartX();
            double startZ = pos.getStartZ();
            double endX = pos.getEndX() + 1;
            double endZ = pos.getEndZ() + 1;

            // Draw full chunk pillar
            RenderUtils.renderFilledBox(
                    matrices,
                    (float) startX, (float) worldMinY, (float) startZ,
                    (float) endX, (float) worldMaxY, (float) endZ,
                    new java.awt.Color(0, 255, 0, 70));

            if (showReasons.getValue()) {
                ChunkAnalysis analysis = chunkData.get(pos);
                if (analysis != null && analysis.reasons != null && !analysis.reasons.isEmpty()) {
                    float scale = 0.05f;
                    double cx = startX + 8;
                    double cz = startZ + 8;

                    matrices.push();
                    matrices.translate(cx, mc.player.getY() + 2.0, cz);
                    // Billboard the text to face camera
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cam.getYaw()));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
                    matrices.scale(-scale, -scale, scale);

                    int yOffset = 0;
                    for (String reason : analysis.reasons) {
                        float w = mc.textRenderer.getWidth(reason);
                        mc.textRenderer.draw(
                                reason,
                                -w / 2f,
                                yOffset,
                                0xFFFFFFFF,
                                true,
                                matrices.peek().getPositionMatrix(),
                                mc.getBufferBuilders().getEntityVertexConsumers(),
                                TextRenderer.TextLayerType.NORMAL,
                                0,
                                LightmapTextureManager.MAX_LIGHT_COORDINATE);
                        yOffset += 10;
                    }
                    matrices.pop();
                }
            }
        }

        matrices.pop();
    }

    private static class ChunkAnalysis {
        int deepslateCount = 0;
        int rotatedCount = 0;
        boolean hasLongDripstone = false;
        boolean hasLongVine = false;
        boolean allKelpFull = false;
        boolean hasDioriteVein = false;
        boolean hasObsidianVein = false;
        List<String> reasons = new ArrayList<>();
        BlockPos susBlockPos = null;
    }
}
