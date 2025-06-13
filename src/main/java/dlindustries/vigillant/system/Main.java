package dlindustries.vigillant.system;

import net.fabricmc.api.ModInitializer;

import java.io.IOException;

public final class Main implements ModInitializer {
	@Override
	public void onInitialize() {
		try {
			new system();
		} catch (InterruptedException | IOException ignored) {}
	}
}
