package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.item.Items;
import net.minecraft.item.BlockItem;
import net.minecraft.block.Blocks;

public final class Sprint extends Module implements TickListener {
    private final BooleanSetting noSprintOnAnchor = new BooleanSetting(EncryptedString.of("No Sprint on Anchor"), false);
    private final BooleanSetting noSprintOnObsidian = new BooleanSetting(EncryptedString.of("No Sprint on Obsidian"), false);
    public Sprint() {
        super(
                EncryptedString.of("Sprint Optimizer"),
                EncryptedString.of("optimization for d-taps/sprinting ect - bypasses all anticheats."),
                -1,
                Category.optimizer
        );
        addSettings(noSprintOnAnchor, noSprintOnObsidian);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick() {
        assert mc.player != null;

        boolean shouldSprint = true;

        if (noSprintOnObsidian.getValue() && mc.player.getMainHandStack().isOf(Items.OBSIDIAN)) {
            shouldSprint = false;
        }

        if (noSprintOnAnchor.getValue() && mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
            shouldSprint = false;
        }

        if (shouldSprint) {
            mc.player.setSprinting(mc.player.input.pressingForward);
        } else {
            mc.player.setSprinting(false);
        }
    }
}

