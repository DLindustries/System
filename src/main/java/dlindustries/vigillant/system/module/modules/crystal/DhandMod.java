package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public final class DhandMod extends Module implements TickListener {
    private final NumberSetting slotSetting = new NumberSetting(EncryptedString.of("Totem Slot"), 1, 9, 9, 1);
    private final NumberSetting delaySetting = new NumberSetting(EncryptedString.of("Open Delay"), 1, 100, 25, 1);

    private boolean shouldOpenInventory = false;
    private int delayTicksRemaining = 0;

    public DhandMod() {
        super(EncryptedString.of("D Hand Mod"), EncryptedString.of("Advanced version of D-hand-Mod - pair with autototem legit and general use."), -1, Category.CRYSTAL);
        addSettings(slotSetting, delaySetting);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
    }

    @Override
    public void onTick() {
        if (shouldOpenInventory && --delayTicksRemaining <= 0) {
            MinecraftClient.getInstance().setScreen(new InventoryScreen(MinecraftClient.getInstance().player));
            shouldOpenInventory = false;
        }
    }

    public static void handleInventoryKey() {
        DhandMod module = system.INSTANCE.getModuleManager().getModule(DhandMod.class);
        if (module == null || !module.isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        int totemSlot = module.slotSetting.getValueInt() - 1;

        if (client.player.getInventory().selectedSlot == totemSlot) {
            client.setScreen(new InventoryScreen(client.player));
        } else {
            client.player.getInventory().selectedSlot = totemSlot;
            module.shouldOpenInventory = true;
            module.delayTicksRemaining = (int) Math.ceil(module.delaySetting.getValue() / 50f);
        }
    }
}