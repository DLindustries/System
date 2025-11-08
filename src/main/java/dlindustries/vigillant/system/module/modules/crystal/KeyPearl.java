package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.KeyUtils;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;


public final class KeyPearl extends Module implements TickListener {

    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), -1, false);

    // Timing / switching settings
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 20, 1, 1);
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
    private final NumberSetting switchslot = new NumberSetting(EncryptedString.of("Switch Slot"), 1, 9, 1, 1)
            .setDescription(EncryptedString.of("The slot it returns to after pearling (e.g., your sword slot)"));
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 1, 20, 1, 1)
            .setDescription(EncryptedString.of("Delay after throwing pearl before switching back"));

    // Integrated pearl boost (exclusive to KeyPearl)
    private final NumberSetting boostMultiplier = new NumberSetting(
            EncryptedString.of("Pearl Boost Multiplier"),
            1.0, 5.0, 1.5, 0.05
    ).setDescription(EncryptedString.of("How much faster pearls travel when thrown by this module (1.0 = normal)"));

    private final BooleanSetting boostFirstTickOnly = new BooleanSetting(
            EncryptedString.of("Only First Tick"), false
    ).setDescription(EncryptedString.of("Boost only applies on the first tick (recommended)"));

    // Internal state
    private boolean active, hasActivated;
    private int clock, previousSlot, switchClock;

    public KeyPearl() {
        super(EncryptedString.of("Pearl Optimizer"), EncryptedString.of("Optimizes and throws pearl for you"), -1, Category.CRYSTAL);
        addSettings(activateKey, delay, switchBack, switchslot, switchDelay, boostMultiplier, boostFirstTickOnly);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        PearlBoostAccessor.INSTANCE.reset();
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        PearlBoostAccessor.INSTANCE.reset();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;

        if (KeyUtils.isKeyPressed(activateKey.getKey())) {
            active = true;
        }

        if (!active) return;

        if (previousSlot == -1)
            previousSlot = mc.player.getInventory().selectedSlot;

        // Apply pearl boost just before throwing
        applyBoost();

        InventoryUtils.selectItemFromHotbar(Items.ENDER_PEARL);

        if (clock < delay.getValueInt()) {
            clock++;
            return;
        }

        if (!hasActivated) {
            ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            if (result.isAccepted() && result.shouldSwingHand())
                mc.player.swingHand(Hand.MAIN_HAND);

            hasActivated = true;
        }

        if (switchBack.getValue())
            switchBack();
        else
            reset();
    }

    private void applyBoost() {
        PearlBoostAccessor accessor = PearlBoostAccessor.INSTANCE;
        accessor.enabled = true;
        accessor.multiplier = boostMultiplier.getValue();
        accessor.firstTickOnly = boostFirstTickOnly.getValue();
    }

    private void switchBack() {
        if (switchClock < switchDelay.getValueInt()) {
            switchClock++;
            return;
        }

        int slot = Math.max(1, Math.min(9, switchslot.getValueInt())) - 1;
        InventoryUtils.setInvSlot(slot);
        reset();
    }

    private void reset() {
        PearlBoostAccessor.INSTANCE.reset();
        previousSlot = -1;
        clock = 0;
        switchClock = 0;
        active = false;
        hasActivated = false;
    }


    public static final class PearlBoostAccessor {
        public static final PearlBoostAccessor INSTANCE = new PearlBoostAccessor();
        public boolean enabled = false;
        public double multiplier = 1.0;
        public boolean firstTickOnly = true;

        private PearlBoostAccessor() {}

        public void reset() {
            enabled = false;
            multiplier = 1.0;
            firstTickOnly = true;
        }
    }
}
