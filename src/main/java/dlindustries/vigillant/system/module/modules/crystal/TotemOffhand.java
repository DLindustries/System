package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.InventoryUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;

public final class TotemOffhand extends Module implements TickListener {
    private final NumberSetting minSlotDelay = new NumberSetting("Min Slot Delay", 50, 200, 80, 1);
    private final NumberSetting maxSlotDelay = new NumberSetting("Max Slot Delay", 100, 300, 150, 1);
    private final NumberSetting offhandDelay = new NumberSetting("Offhand Delay", 20, 60, 35, 1);
    private final BooleanSetting dynamicJitter = new BooleanSetting("Dynamic Jitter", true);
    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);

    private int ticksToNextAction;
    private int previousSlot = -1;
    private boolean sentSwapPacket = false;
    private boolean isActive = false;
    private long lastTotemTime = 0;

    private final Deque<Long> recentDelays = new ArrayDeque<>();
    private double currentJitterFactor = 1.0;

    public TotemOffhand() {
        super("Auto offhand", "bannable", -1, Category.CRYSTAL);
        addSettings(minSlotDelay, maxSlotDelay, offhandDelay, dynamicJitter, switchBack);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (shouldPause()) {
            reset();
            return;
        }

        if (needsTotem()) {
            isActive = true;
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastTotemTime < getPingAdjustedThreshold()) {
                return;
            }

            executeStealthProtocol();
        }
    }

    private boolean shouldPause() {
        return mc.currentScreen != null || !mc.player.isAlive() || mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
    }

    private boolean needsTotem() {
        return mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING
                && InventoryUtils.hasItem(Items.TOTEM_OF_UNDYING);
    }

    private int getPing() {
        if (mc.getNetworkHandler() == null) return 0;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private void executeStealthProtocol() {
        if (ticksToNextAction > 0) {
            ticksToNextAction--;
            return;
        }

        if (sentSwapPacket && switchBack.getValue()) {
            if (previousSlot != -1) {
                mc.player.getInventory().selectedSlot = previousSlot;
            }
            reset();
            return;
        }

        if (previousSlot == -1) {
            initializeSlotChange();
            return;
        }

        if (!sentSwapPacket) {
            performDeceptiveSwap();
            return;
        }

        finalizeSwap();
    }

    private void initializeSlotChange() {
        previousSlot = mc.player.getInventory().selectedSlot;
        int baseDelay = ThreadLocalRandom.current().nextInt(
                minSlotDelay.getValueInt(),
                maxSlotDelay.getValueInt() + 1
        );

        if (dynamicJitter.getValue()) {
            currentJitterFactor = 0.8 + (ThreadLocalRandom.current().nextDouble() * 0.4);
            baseDelay = (int) (baseDelay * currentJitterFactor);
        }

        ticksToNextAction = msToTicks(baseDelay);
    }

    private void performDeceptiveSwap() {
        if (InventoryUtils.selectItemFromHotbar(Items.TOTEM_OF_UNDYING)) {
            double gaussian = ThreadLocalRandom.current().nextGaussian() * 5;
            int uniformJitter = ThreadLocalRandom.current().nextInt(-10, 10);
            int finalDelay = (int) (offhandDelay.getValueInt() + gaussian + uniformJitter);
            finalDelay = Math.max(25, Math.min(60, finalDelay));
            ticksToNextAction = msToTicks(finalDelay);
            sentSwapPacket = true;
        } else {
            reset();
        }
    }

    private void finalizeSwap() {
        if (mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN,
                Direction.DOWN
        ));

        if (switchBack.getValue()) {
            ticksToNextAction = msToTicks(ThreadLocalRandom.current().nextInt(15, 25));
            sentSwapPacket = true;
        } else {
            reset();
        }

        lastTotemTime = System.currentTimeMillis();
    }

    private int msToTicks(int milliseconds) {
        return (int) Math.ceil(milliseconds / 50.0);
    }

    private long getPingAdjustedThreshold() {
        return Math.max(50, getPing() + 20);
    }

    private void reset() {
        ticksToNextAction = 0;
        previousSlot = -1;
        sentSwapPacket = false;
        isActive = false;
    }
}