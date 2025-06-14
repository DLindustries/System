package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.CameraUpdateListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.KeyUtils;

public final class CameraOptimizer extends Module implements CameraUpdateListener {
    private final KeybindSetting toggleKey = new KeybindSetting(
            EncryptedString.of("Toggle Key"),
            -1,
            false
    );

    private final BooleanSetting noClip = new BooleanSetting(
            EncryptedString.of("No Clip"),
            true
    ).setDescription(EncryptedString.of("Allows camera to pass through blocks"));

    private final BooleanSetting noOverlay = new BooleanSetting(
            EncryptedString.of("No Overlay"),
            true
    ).setDescription(EncryptedString.of("Removes water/lava visual effects"));

    public CameraOptimizer() {
        super(
                EncryptedString.of("Camera Optimizer"),
                EncryptedString.of("Improves camera behavior and removes restrictions"),
                -1,
                Category.RENDER
        );
        addSettings(toggleKey, noClip, noOverlay);
    }

    @Override
    public void onEnable() {
        eventManager.add(CameraUpdateListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(CameraUpdateListener.class, this);
        super.onDisable();
    }

    @Override
    public void onCameraUpdate(CameraUpdateEvent event) {
        // Camera position modification logic if needed
    }

    public boolean isToggleKeyPressed() {
        return toggleKey.getKey() != -1 && KeyUtils.isKeyPressed(toggleKey.getKey());
    }

    public boolean isNoClipEnabled() {
        return isEnabled() && noClip.getValue() && isToggleKeyPressed();
    }

    public boolean isNoOverlayEnabled() {
        return isEnabled() && noOverlay.getValue() && isToggleKeyPressed();
    }
}