package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.utils.EncryptedString;

public final class PlacementOptimizer extends Module implements TickListener {
    private final BooleanSetting excludeAnchors = new BooleanSetting(
            EncryptedString.of("Exclude Anchors/Glowstone"),
            true
    ).setDescription(EncryptedString.of("Keeps vanilla delays for anchors and glowstone - recommended if you have anchor macro turned on"));

    public PlacementOptimizer() {
        super(EncryptedString.of("Placement Optimizer"),
                EncryptedString.of("Removes block place delay to allow faster block placement."),
                -1,
                Category.optimizer);
        addSettings(excludeAnchors);
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
        // Tick implementation if needed
    }

    public boolean shouldExcludeAnchors() {
        return excludeAnchors.getValue();
    }
}