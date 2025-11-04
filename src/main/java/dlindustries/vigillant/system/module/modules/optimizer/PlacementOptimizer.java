package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;

public final class PlacementOptimizer extends Module implements TickListener {
    private final BooleanSetting excludeAnchors = new BooleanSetting(
            EncryptedString.of("Exclude Anchors/Glowstone"),
            true
    ).setDescription(EncryptedString.of("Keeps vanilla delays for anchors and glowstone - keeps anchor macro look legit"));

    private final NumberSetting blockDelay = new NumberSetting(EncryptedString.of("Block delay"), 0, 5, 3, 0.1)
            .setDescription(EncryptedString.of("Default vanilla block placement delay is 5 ticks"));
    private final NumberSetting crystalDelay = new NumberSetting(EncryptedString.of("Crystal delay"), 0, 2, 0, 1)
            .setDescription(EncryptedString.of("Default Vanilla crystal placement delay is 2 ticks"));
    public PlacementOptimizer() {
        super(EncryptedString.of("Placement Optimizer"),
                EncryptedString.of("Adjusts block/crystal placement delays"),
                -1,
                Category.optimizer);
        addSettings(excludeAnchors, blockDelay, crystalDelay);
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
    }

    public boolean shouldExcludeAnchors() {
        return excludeAnchors.getValue();
    }
    public int getBlockDelay() {
        return blockDelay.getValueInt();
    }

    public int getCrystalDelay() {
        return crystalDelay.getValueInt();
    }
}