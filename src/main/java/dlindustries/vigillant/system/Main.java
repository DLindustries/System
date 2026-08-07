package dlindustries.vigillant.system;

import net.fabricmc.api.ModInitializer;
import java.io.IOException;

public final class Main implements ModInitializer {
	@Override
	public void onInitialize() {
		try {
			new VigillantSystem();
		} catch (InterruptedException | IOException ignored) {}
	}
}
