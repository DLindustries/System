package dlindustries.vigillant.system.module.modules.mace;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class DiveBomber extends Module implements TickListener, AttackListener {

    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 20, 1, 1);
    private final NumberSetting chestplateSlot = new NumberSetting(EncryptedString.of("Chestplate Slot"), 1, 9, 2, 1)
            .setDescription(EncryptedString.of("Slot 1-9 for chestplate"));
    private final NumberSetting maceSlot = new NumberSetting(EncryptedString.of("Mace Slot"), 1, 9, 1, 1)
            .setDescription(EncryptedString.of("Slot 1-9 for mace"));

    private enum EquipState {
        IDLE,
        SWITCHING_TO_CHESTPLATE,
        EQUIPPING,
        SWITCHING_BACK
    }

    private EquipState equipState = EquipState.IDLE;
    private int equipTimer = 0;
    private boolean isElytraEquipped = false;
    private long groundTime = 0;

    public DiveBomber() {
        super(
                EncryptedString.of("DiveBomber"),
                EncryptedString.of("Auto chestplate swap when you left click while flying to deal alot more damage with mace"),
                -1,
                Category.mace
        );
        addSettings(switchDelay, chestplateSlot, maceSlot);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        eventManager.add(AttackListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        eventManager.remove(AttackListener.class, this);
        resetState();
        super.onDisable();
    }

    @Override
    public void onTick() {

        if (mc.currentScreen != null) return;
        if (mc.player == null) return;
        ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        isElytraEquipped = chestItem.getItem() == Items.ELYTRA;
        if (equipState != EquipState.IDLE) {
            if (++equipTimer >= switchDelay.getValueInt()) {
                equipTimer = 0;
                processEquipState();
            }
        }
        if (mc.player.isOnGround() && !mc.player.isFallFlying() && isElytraEquipped) {
            if (groundTime == 0) {
                groundTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - groundTime > 1000) {
                startEquipProcess();
                groundTime = 0;
            }
        } else {
            groundTime = 0;
        }
    }

    @Override
    public void onAttack(AttackEvent event) {

        if (mc.currentScreen != null) return;
        if (mc.player == null || !isElytraEquipped) return;

        startEquipProcess();
    }

    private void startEquipProcess() {
        if (equipState == EquipState.IDLE) {
            equipState = EquipState.SWITCHING_TO_CHESTPLATE;
            processEquipState(); // Start immediately
        }
    }

    private void processEquipState() {

        if (mc.currentScreen != null) {
            resetState();
            return;
        }

        PlayerEntity player = mc.player;
        if (player == null) {
            resetState();
            return;
        }

        switch (equipState) {
            case SWITCHING_TO_CHESTPLATE:
                player.getInventory().selectedSlot = chestplateSlot.getValueInt() - 1;
                equipState = EquipState.EQUIPPING;
                break;

            case EQUIPPING:
                mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
                equipState = EquipState.SWITCHING_BACK;
                break;

            case SWITCHING_BACK:
                player.getInventory().selectedSlot = maceSlot.getValueInt() - 1;
                resetState();
                break;
        }
    }

    private void resetState() {
        equipState = EquipState.IDLE;
        equipTimer = 0;
    }
}