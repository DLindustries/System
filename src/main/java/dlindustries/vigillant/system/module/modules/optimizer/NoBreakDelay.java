package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.utils.EncryptedString;

public final class NoBreakDelay extends Module {
	public NoBreakDelay() {
		super(EncryptedString.of("Break optimizer"),
				EncryptedString.of("ignores block break delay for diggers"),
				-1,
				Category.optimizer);
	}
}
