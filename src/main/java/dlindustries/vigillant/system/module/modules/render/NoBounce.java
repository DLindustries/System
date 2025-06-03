package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.utils.EncryptedString;

public final class NoBounce extends Module {
	public NoBounce() {
		super(EncryptedString.of("Crystal bounce"),
				EncryptedString.of("Makes the crystals stay where they are"),
				-1,
				Category.RENDER);
	}
}
