package dlindustries.vigillant.system.module.modules.client;

import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.StringSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class NameProtect extends Module {
    public enum Mode {
        OFF, SELF, EVERYONE
    }

    private final ModeSetting<Mode> mode = new ModeSetting<>(EncryptedString.of("Mode"), Mode.SELF, Mode.class);
    private final StringSetting selfName = new StringSetting(EncryptedString.of("Your Name"), "You");
    private final StringSetting othersName = new StringSetting(EncryptedString.of("Others Name"), "Player");

    public NameProtect() {
        super(EncryptedString.of("NameProtect"),
                EncryptedString.of("Hides player names in chat and nametags"),
                -1,
                Category.CLIENT);
        addSettings(mode, selfName, othersName);
    }

    public String replaceName(String string) {
        if (string == null || !isEnabled() || mode.getMode() == Mode.OFF) {
            return string;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        String localName = client.getSession().getUsername();

        switch (mode.getMode()) {
            case SELF:
                return replaceNames(string, localName, selfName.getValue());

            case EVERYONE:
                if (client.world == null) return string;
                List<String> names = new ArrayList<>();
                for (var player : client.world.getPlayers()) {
                    names.add(player.getName().getString());
                }
                names.sort(Comparator.comparingInt(String::length).reversed());
                Map<String, String> replacements = new HashMap<>();
                for (String name : names) {
                    if (name.equals(localName)) {
                        replacements.put(name, selfName.getValue());
                    } else {
                        replacements.put(name, othersName.getValue());
                    }
                }
                for (String name : names) {
                    string = replaceNames(string, name, replacements.get(name));
                }
                return string;

            default:
                return string;
        }
    }

    private String replaceNames(String input, String original, String replacement) {
        return input.replaceAll("\\b" + Pattern.quote(original) + "\\b", replacement);
    }
}