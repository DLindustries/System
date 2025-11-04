package dlindustries.vigillant.system.utils;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Webhook {
    private final String url;
    private String content;
    private String username;
    private String avatarUrl;
    private boolean tts;
    private final List<Embed> embeds = new ArrayList<>();

    public Webhook(String url) {
        this.url = url;
    }

    public Webhook setContent(String content) {
        this.content = content;
        return this;
    }

    public Webhook setUsername(String username) {
        this.username = username;
        return this;
    }

    public Webhook setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public Webhook setTts(boolean tts) {
        this.tts = tts;
        return this;
    }

    public Webhook addEmbed(Embed embed) {
        this.embeds.add(embed);
        return this;
    }

    public void execute() {
        try {
            if ((content == null || content.isEmpty()) && embeds.isEmpty()) {
                throw new IllegalStateException("Must set content or add at least one embed");
            }
            JSONObject payload = new JSONObject();
            payload.put("content", content);
            payload.put("username", username);
            payload.put("avatar_url", avatarUrl);
            payload.put("tts", tts);

            if (!embeds.isEmpty()) {
                List<JSONObject> ja = new ArrayList<>();
                for (Embed e : embeds) {
                    ja.add(e.toJson());
                }
                payload.put("embeds", ja.toArray(new JSONObject[0]));
            }
            URL u = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) u.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", "VigillantWebhook/1.0");
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(body);
            }
            conn.getInputStream().close();
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class JSONObject {
        private final Map<String, Object> map = new HashMap<>();

        void put(String key, Object value) {
            if (value != null) map.put(key, value);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            int i = 0, size = map.size();
            for (Map.Entry<String, Object> e : map.entrySet()) {
                sb.append("\"").append(e.getKey()).append("\":");
                Object v = e.getValue();

                if (v instanceof String) {
                    sb.append("\"").append(escape((String)v)).append("\"");
                } else if (v instanceof Boolean || v instanceof Number) {
                    sb.append(v);
                } else {
                    sb.append(v.toString());
                }

                if (++i < size) sb.append(",");
            }
            sb.append("}");
            return sb.toString();
        }

        private String escape(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    public static class Embed {
        private String title, description, url;
        private Integer color;            // null = no color
        private final List<Field> fields = new ArrayList<>();

        public Embed setTitle(String t)        { title = t; return this; }
        public Embed setDescription(String d)  { description = d; return this; }
        public Embed setUrl(String u)          { url = u; return this; }
        public Embed setColor(int r, int g, int b) {
            this.color = (r << 16) | (g << 8) | b;
            return this;
        }
        public Embed addField(String name, String val, boolean inline) {
            fields.add(new Field(name, val, inline));
            return this;
        }

        private JSONObject toJson() {
            JSONObject e = new JSONObject();
            e.put("title", title);
            e.put("description", description);
            e.put("url", url);
            e.put("color", color);
            if (!fields.isEmpty()) {
                JSONObject[] fa = fields.stream()
                        .map(Field::toJson)
                        .toArray(JSONObject[]::new);
                e.put("fields", fa);
            }
            return e;
        }
    }

    private static class Field {
        private final String name, value;
        private final boolean inline;

        Field(String n, String v, boolean i) { name = n; value = v; inline = i; }

        JSONObject toJson() {
            JSONObject f = new JSONObject();
            f.put("name", name);
            f.put("value", value);
            f.put("inline", inline);
            return f;
        }
    }
}
