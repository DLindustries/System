package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;

public final class NameTags extends Module {
    public final NumberSetting scale = new NumberSetting(EncryptedString.of("Scale"), 0.05, 5, 1, 0.05)
            .setDescription(EncryptedString.of("Nametag size multiplier"));

    public final BooleanSetting unlimitedRange = new BooleanSetting(EncryptedString.of("Unlimited Range"), true)
            .setDescription(EncryptedString.of("Show nametags at any distance"));

    public final BooleanSetting seeThrough = new BooleanSetting(EncryptedString.of("See-Through"), false)
            .setDescription(EncryptedString.of("Render through walls (may cause glitches)"));

    public final BooleanSetting forceMobNametags = new BooleanSetting(EncryptedString.of("Force Mob Tags"), true)
            .setDescription(EncryptedString.of("Always show named mob nametags"));

    public final BooleanSetting forcePlayerNametags = new BooleanSetting(EncryptedString.of("Force Player Tags"), false)
            .setDescription(EncryptedString.of("Ignore team settings for players"));

    public NameTags() {
        super(EncryptedString.of("NameTags"), EncryptedString.of("Customize nametag rendering"), -1, Category.RENDER);
        addSettings(scale, unlimitedRange, seeThrough, forceMobNametags, forcePlayerNametags);
    }
    public float getScale() {
        return scale.getValueFloat();
    }

    public boolean isUnlimitedRange() {
        return isEnabled() && unlimitedRange.getValue();
    }

    public boolean isSeeThrough() {
        return isEnabled() && seeThrough.getValue();
    }

    public boolean shouldForceMobNametags() {
        return isEnabled() && forceMobNametags.getValue();
    }

    public boolean shouldForcePlayerNametags() {
        return isEnabled() && forcePlayerNametags.getValue();
    }
}