package dlindustries.vigillant.system.module.modules.mace;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.KeybindSetting; // Added
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.KeyUtils; // Added
import net.minecraft.util.Hand;

public class FireworkMacro extends Module implements TickListener {

    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), 32, false); // Default: Spacebar owner preference
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 20, 1, 1);
    private final NumberSetting fireworkSlot = new NumberSetting(EncryptedString.of("Firework Slot"), 1, 9, 3, 1)
            .setDescription(EncryptedString.of("Slot 1-9 for fireworks"));
    private final NumberSetting restoreSlot = new NumberSetting(EncryptedString.of("Restore Slot"), 1, 9, 1, 1)
            .setDescription(EncryptedString.of("Slot 1-9 to restore"));

    private boolean active, hasSwitched, hasUsed, hasSwitchedBack;
    private int switchClock, useClock, switchBackClock;
    private boolean wasKeyPressed; // Tracks key press state

    public FireworkMacro() {
        super(
                EncryptedString.of("Firework Macro"),
                EncryptedString.of("Press a key while flying to use fireworks"),
                -1,
                Category.mace
        );
        addSettings(activateKey, switchDelay, fireworkSlot, restoreSlot);
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
        reset();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;

        handleKeyDetection();
        handleFlightMonitoring();
        handleFireworkSequence();
    }

    private void handleKeyDetection() {
        boolean keyPressed = KeyUtils.isKeyPressed(activateKey.getKey());


        if (keyPressed && !wasKeyPressed) {
            if (mc.player != null && mc.player.isFallFlying() && !active) {
                active = true;
            }
        }
        wasKeyPressed = keyPressed;
    }

    private void handleFlightMonitoring() {
        if (mc.player == null) return;

        if (active && !mc.player.isFallFlying()) {
            reset();
        }
    }

    private void handleFireworkSequence() {
        if (!active || mc.player == null) return;

        if (!hasSwitched) {
            if (switchClock < switchDelay.getValueInt()) {
                switchClock++;
                return;
            }
            mc.player.getInventory().selectedSlot = fireworkSlot.getValueInt() - 1;
            hasSwitched = true;
            switchClock = 0;
        } else if (!hasUsed) {
            if (useClock < 1) {
                useClock++;
                return;
            }
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            hasUsed = true;
            useClock = 0;
        } else if (!hasSwitchedBack) {
            if (switchBackClock < switchDelay.getValueInt()) {
                switchBackClock++;
                return;
            }
            mc.player.getInventory().selectedSlot = restoreSlot.getValueInt() - 1;
            hasSwitchedBack = true;
            reset();
        }
    }

    private void reset() {
        active = false;
        hasSwitched = false;
        hasUsed = false;
        hasSwitchedBack = false;
        switchClock = 0;
        useClock = 0;
        switchBackClock = 0;
    }
}