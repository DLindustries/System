package dlindustries.vigillant.system;

import dlindustries.vigillant.system.event.EventManager;
import dlindustries.vigillant.system.gui.ClickGui;
import dlindustries.vigillant.system.managers.FriendManager;
import dlindustries.vigillant.system.managers.ProfileManager;
import dlindustries.vigillant.system.module.ModuleManager;
import dlindustries.vigillant.system.utils.rotation.RotatorManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@SuppressWarnings("all")
public final class system {
	public RotatorManager rotatorManager;
	public ProfileManager profileManager;
	public ModuleManager moduleManager;
	public EventManager eventManager;
	public FriendManager friendManager;
	public static MinecraftClient mc;
	public String version = " b1.4";
	public static boolean BETA;
	public static system INSTANCE;
	public boolean guiInitialized;
	public ClickGui clickGui;
	public Screen previousScreen = null;
	public long lastModified;
	public File systemJar;

	public system() throws InterruptedException, IOException {
		INSTANCE = this;
		this.eventManager = new EventManager();
		this.moduleManager = new ModuleManager();
		this.clickGui = new ClickGui();
		this.rotatorManager = new RotatorManager();
		this.profileManager = new ProfileManager();
		this.friendManager = new FriendManager();

		this.getProfileManager().loadProfile();
		this.setLastModified();

		this.guiInitialized = false;
		mc = MinecraftClient.getInstance();
	}

	public ProfileManager getProfileManager() {
		return profileManager;
	}

	public ModuleManager getModuleManager() {
		return moduleManager;
	}

	public FriendManager getFriendManager() {
		return friendManager;
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public ClickGui getClickGui() {
		return clickGui;
	}

	public void resetModifiedDate() {
		this.systemJar.setLastModified(lastModified);
	}

	public String getVersion() {
		return version;
	}

	public void setLastModified() {
		try {
			this.systemJar = new File(system.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			// Comment out when debugging
			this.lastModified = systemJar.lastModified();
		} catch (URISyntaxException ignored) {}
	}
}