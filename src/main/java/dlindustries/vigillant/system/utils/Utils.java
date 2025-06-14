package dlindustries.vigillant.system.utils;

import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.module.modules.client.SelfDestruct;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

public final class Utils {
	private static final Color[] SOLO_LEVELING_COLORS = {
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


	public static Color getMainColor(int alpha, int unusedIncrement) {
		if (ClickGUI.breathing.getValue()) {
			long currentTime = System.currentTimeMillis();
			int cycleDuration = 8000; // 2 second full cycle
			float progress = (currentTime % cycleDuration) / (float)cycleDuration;

			int colorCount = SOLO_LEVELING_COLORS.length;
			float scaledProgress = progress * colorCount;
			int index = (int)scaledProgress;
			float interpolation = scaledProgress - index;

			Color start = SOLO_LEVELING_COLORS[index % colorCount];
			Color end = SOLO_LEVELING_COLORS[(index + 1) % colorCount];

			return new Color(
					interpolateColor(start.getRed(), end.getRed(), interpolation),
					interpolateColor(start.getGreen(), end.getGreen(), interpolation),
					interpolateColor(start.getBlue(), end.getBlue(), interpolation),
					alpha
			);
		} else {
			return new Color(
					(int)ClickGUI.red.getValue(),
					(int)ClickGUI.green.getValue(),
					(int)ClickGUI.blue.getValue(),
					alpha
			);
		}
	}

	private static int interpolateColor(int start, int end, float progress) {
		return (int)(start + (end - start) * progress);
	}



	public static File getCurrentJarPath() throws URISyntaxException {
		return new File(SelfDestruct.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
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