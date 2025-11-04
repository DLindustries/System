package dlindustries.vigillant.system.module.setting;

public class StringSetting extends Setting<StringSetting> {
    private String value;
    public StringSetting(CharSequence name, String defaultValue) {
        super(name);
        this.value = (defaultValue != null) ? defaultValue : "";
    }

    public void setValue(String value) {
        this.value = (value != null) ? value : "";
    }

    public String getValue() {
        if (this.value == null) {
            this.value = "";
        }
        return this.value;
    }
}
