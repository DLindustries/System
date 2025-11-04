package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.utils.EncryptedString;

public final class ShieldOptimizer extends Module {
    public ShieldOptimizer() {
        super(EncryptedString.of("Shield Optimizer"),
                EncryptedString.of("Tries to remove the base use time of 5 ticks on shields before they actually work"),
                -1,
                Category.optimizer);
    }
}
