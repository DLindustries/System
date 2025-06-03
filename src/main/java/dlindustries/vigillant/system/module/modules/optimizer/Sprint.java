package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.item.Items;

public final class Sprint extends Module implements TickListener {
    public Sprint() {
        super(
                EncryptedString.of("Sprint Optimizer"),
                EncryptedString.of("Keeps you sprinting at all times - bypasses all anticheats."),
                -1,
                Category.optimizer
        );
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
        // If main hand is obsidian, skip sprinting this tick
        assert mc.player != null;
        if (mc.player.getMainHandStack().isOf(Items.OBSIDIAN)) {
            return;
        }
        // Otherwise, sprint if pressing forward
        mc.player.setSprinting(mc.player.input.pressingForward);
    }
}
