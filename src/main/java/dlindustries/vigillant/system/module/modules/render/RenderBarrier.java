package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.utils.EncryptedString;

public final class RenderBarrier extends Module {
    public RenderBarrier() {
        super(EncryptedString.of("Render Barrier"),
                EncryptedString.of("Renders invisible barriers"),
                -1,
                Category.RENDER);
    }
}
