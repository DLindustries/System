package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class HitOptimizer extends Module implements AttackListener, TickListener {

    private final BooleanSetting requireSword = new BooleanSetting(EncryptedString.of("Require Sword"), true)
            .setDescription(EncryptedString.of("Only switch if sword is available"));

    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
            .setDescription(EncryptedString.of("Switch back to original slot after attack"));

    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 5, 1, 1)
            .setDescription(EncryptedString.of("Delay after attacking before switching back"));
    private boolean shouldSwitchBack;
    private int originalSlot = -1;
    private int switchTimer;

    public HitOptimizer() {
        super(EncryptedString.of("Hit Optimizer"),
                EncryptedString.of("Automatically switches to sword for attacks / make sure they take KB"),
                -1,
                Category.optimizer);
        addSettings(requireSword, switchBack, switchDelay);
    }

    @Override
    public void onEnable() {
        eventManager.add(AttackListener.class, this);
        eventManager.add(TickListener.class, this);
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(AttackListener.class, this);
        eventManager.remove(TickListener.class, this);
        if (shouldSwitchBack && originalSlot != -1) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
        super.onDisable();
    }

    @Override
    public void onAttack(AttackEvent event) {
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) mc.crosshairTarget).getEntity();
        if (target == null) return;

        ItemStack currentStack = mc.player.getMainHandStack();
        Item currentItem = currentStack.getItem();


        if (isWeapon(currentItem)) return;


        if (shouldSwitchBack) {
            switchTimer = 0;
            return;
        }


        if (requireSword.getValue()) {
            int swordSlot = findSwordSlot();
            if (swordSlot == -1) return;


            if (switchBack.getValue() && originalSlot == -1) {
                originalSlot = mc.player.getInventory().selectedSlot;
            }


            mc.player.getInventory().selectedSlot = swordSlot;


            if (switchBack.getValue()) {
                shouldSwitchBack = true;
                switchTimer = 0;
            }
        }
    }

    @Override
    public void onTick() {
        if (!shouldSwitchBack || originalSlot == -1) return;

        if (switchTimer < switchDelay.getValueInt()) {
            switchTimer++;
            return;
        }


        mc.player.getInventory().selectedSlot = originalSlot;
        resetState();
    }

    private void resetState() {
        shouldSwitchBack = false;
        originalSlot = -1;
        switchTimer = 0;
    }

    private boolean isWeapon(Item item) {
        return item instanceof SwordItem ||
                item instanceof AxeItem ||
                item instanceof MaceItem ||
                item instanceof ElytraItem ||
                (item instanceof ArmorItem && ((ArmorItem) item).getSlotType() == EquipmentSlot.CHEST);
    }

    private int findSwordSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.getItem() instanceof SwordItem) {
                return slot;
            }
        }
        return -1;
    }
}