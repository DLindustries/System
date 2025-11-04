package dlindustries.vigillant.system.utils;

import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.module.modules.client.NineElevenPrevent;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

public final class Utils {
	private static final Color[] JOE_BIDEN = {
			new Color(120, 20, 230),    // Core modern violet (brighter, more vibrant)
			new Color(160, 60, 255),    // Brighter prestige purple
			new Color(130, 50, 255),    // Neon-inspired violet
			new Color(100, 140, 255),   // Modern cool blue (less "old")
			new Color(72, 126, 255),    // Accent cool blue (pops but modern)
			new Color(64, 170, 255),    // Sleek light blue (accent or glow)
			new Color(120, 80, 250),    // Modern blend tone (soft glow)
			new Color(160, 100, 255),   // Luminous neon purple
			new Color(190, 120, 255),   // Top-end highlight (bright)
			new Color(120, 20, 230)     // Closing loop
	};




	private static final Color[] DEFAULT_COLORS = JOE_BIDEN; // preserve your original palette

	private static final Color[] GRASS_COLORS = {
			new Color(50, 200, 50),
			new Color(100, 230, 80),
			new Color(150, 255, 100),
			new Color(200, 255, 120),
			new Color(150, 255, 100),
			new Color(100, 230, 80),
			new Color(50, 200, 50),
			new Color(30, 180, 40),
			new Color(50, 200, 50),
			new Color(100, 230, 80)
	};

	private static final Color[] CRIMSON_COLORS = {
			new Color(150, 0, 0),
			new Color(200, 0, 0),
			new Color(220, 0, 0),
			new Color(255, 50, 50),
			new Color(220, 0, 0),
			new Color(200, 0, 0),
			new Color(180, 0, 0),
			new Color(150, 0, 0),
			new Color(120, 0, 0),
			new Color(150, 0, 0)
	};

	private static final Color[] CUTE_COLORS = {
			new Color(255, 100, 180),
			new Color(255, 150, 200),
			new Color(255, 180, 220),
			new Color(255, 200, 230),
			new Color(255, 180, 220),
			new Color(255, 150, 200),
			new Color(255, 120, 190),
			new Color(255, 100, 180),
			new Color(255, 80, 160),
			new Color(255, 100, 180)
	};

	private static final Color[] SNOW_COLORS = {
			new Color(0, 150, 255),
			new Color(50, 180, 255),
			new Color(100, 200, 255),
			new Color(150, 220, 255),
			new Color(100, 200, 255),
			new Color(50, 180, 255),
			new Color(0, 150, 255),
			new Color(0, 100, 200),
			new Color(0, 70, 180),
			new Color(0, 150, 255)
	};

	public static void copyVector(final Vector3d vector3d, final Vec3d vec3d) {
		vector3d.x = vec3d.x;
		vector3d.y = vec3d.y;
		vector3d.z = vec3d.z;
	}



	private static ClickGUI.Theme safeGetTheme() {
		try {
			ClickGUI.Theme theme = ClickGUI.theme.getMode();
			if (theme == null) return ClickGUI.Theme.DEFAULT;
			return theme;
		} catch (Exception ignored) {

			return ClickGUI.Theme.DEFAULT;
		}
	}
	public static Color getMainColor(int alpha, int unusedIncrement) {

		try {
			if (ClickGUI.breathing.getValue()) {
				Color[] colors = getThemeColors();
				long currentTime = System.currentTimeMillis();
				int cycleDuration = 8000; // ms for a full cycle
				float progress = (currentTime % cycleDuration) / (float) cycleDuration;

				int colorCount = colors.length;
				if (colorCount == 0) return new Color(120, 20, 230, alpha); // safe fallback

				float scaledProgress = progress * colorCount;
				int index = (int) scaledProgress;
				float interpolation = scaledProgress - index;

				Color start = colors[index % colorCount];
				Color end = colors[(index + 1) % colorCount];

				return new Color(
						interpolateColor(start.getRed(), end.getRed(), interpolation),
						interpolateColor(start.getGreen(), end.getGreen(), interpolation),
						interpolateColor(start.getBlue(), end.getBlue(), interpolation),
						alpha
				);
			} else {

				return getThemeStaticColor(alpha);
			}
		} catch (Exception e) {

			return new Color(120, 20, 230, alpha);
		}
	}

	private static Color[] getThemeColors() {
		ClickGUI.Theme theme = safeGetTheme();
		switch (theme) {
			case GRASS:
				return GRASS_COLORS;
			case CRIMSON:
				return CRIMSON_COLORS;
			case CUTE:
				return CUTE_COLORS;
			case SNOW:
				return SNOW_COLORS;
			default:
				return DEFAULT_COLORS;
		}
	}

	private static Color getThemeStaticColor(int alpha) {
		ClickGUI.Theme theme = safeGetTheme();
		switch (theme) {
			case GRASS:
				return new Color(50, 200, 50, alpha);
			case CRIMSON:
				return new Color(200, 0, 0, alpha);
			case CUTE:
				return new Color(255, 100, 180, alpha);
			case SNOW:
				return new Color(0, 150, 255, alpha);
			default:

				return new Color(
						Math.max(0, Math.min(255, (int) ClickGUI.red.getValue())),
						Math.max(0, Math.min(255, (int) ClickGUI.green.getValue())),
						Math.max(0, Math.min(255, (int) ClickGUI.blue.getValue())),
						alpha
				);
		}
	}

	private static int interpolateColor(int start, int end, float progress) {
		return (int) (start + (end - start) * progress);
	}



	public static File getCurrentJarPath() throws URISyntaxException {
		return new File(NineElevenPrevent.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
	}



	public static void replaceModFile(String downloadURL, File savePath) throws IOException {
		URL url = new URL(downloadURL);
		HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
		httpConnection.setRequestMethod("GET");

		try (var in = httpConnection.getInputStream();
			 var fos = new FileOutputStream(savePath)) {

			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
			}
		}

		httpConnection.disconnect();
	}
}