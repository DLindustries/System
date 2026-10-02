# In-Game Announcements & Bulletin Guide

This document describes how to create, format, and publish in-game announcements for **System Client** without needing Discord.

---

## 1. Where Announcements are Hosted

* **Repository:** `https://github.com/DLindustries/System`
* **Branch:** `Ingameannounce`
* **File Name:** `announcements.json`
* **Direct Raw Endpoint:** `https://raw.githubusercontent.com/DLindustries/System/refs/heads/Ingameannounce/announcements.json`

Whenever you push changes to `announcements.json` on the `Ingameannounce` branch, all clients will automatically receive the update in-game. Players can also click the `↻` refresh button on the bulletin header to load the newest changes instantly without restarting Minecraft.

---

## 2. Root Structure

Every `announcements.json` file must follow this root structure:

```json
{
  "id": "notice-2026-10-01-v2",
  "title": "System Bulletin",
  "blocks": [
    ...
  ]
}
```

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` | **Unique identifier.** When you publish a new announcement and change the `id`, the client will automatically un-collapse the card and show an unread glowing notification dot on the Client icon in the rail. |
| `title` | `string` | The header title shown at the top of the card (e.g. `"System Bulletin"`). |
| `blocks` | `array` | An ordered list of content components (see below). |

---

## 3. Supported Content Blocks

### 3.1. `banner` (Hero / Spotlight Video Card)
Best for featuring a new YouTube tutorial, major trailer, or primary announcement.

```json
{
  "type": "banner",
  "tag": "NEW TUTORIAL",
  "title": "Mastering the Anchor Macro & D-Tap Setup",
  "description": "Watch our official walkthrough on YouTube covering delay optimization, D-Tap placement, and anti-ban configurations.",
  "primary_button": {
    "label": "▶ Watch on YouTube",
    "url": "https://www.youtube.com/@DLindustries",
    "style": "accent"
  }
}
```
* `tag` *(optional)*: Badge text in top-left corner (e.g. `"NEW TUTORIAL"`, `"HOT"`).
* `title`: Video or guide title (automatically word-wrapped).
* `description`: Explanatory text (automatically word-wrapped).
* `primary_button`: Main call-to-action button (opens the URL in the player's browser).
* `secondary_button` *(optional)*: Second action button side-by-side with primary.

---

### 3.2. `version_check` (Real GitHub Release Checker)
Compares the client's current version against the **latest official release on GitHub** (`https://api.github.com/repos/DLindustries/System/releases/latest`).

```json
{
  "type": "version_check"
}
```
* **If the client is up to date or newer:** This block hides itself automatically (takes up 0px).
* **If an update is available:** It displays an orange update alert box (`▲ Update Available: {latestVersion}`) with a direct `⬇ Update` button linking to the GitHub download.

---

### 3.3. `header` (Section Headings)
Used to divide sections inside the bulletin.

```json
{
  "type": "header",
  "text": "What's New in System",
  "size": "h2",
  "divider": true
}
```
* `text`: The heading text.
* `size`: `"h1"` (larger, menu font), `"h2"` (medium bold, recommended), or `"h3"` (small).
* `divider` *(optional)*: `true` to draw a subtle separator line under the heading.

---

### 3.4. `bullet_list` (Changelog / Patch Notes)
A clean list of bullet points with automatic word-wrapping.

```json
{
  "type": "bullet_list",
  "items": [
    "Added Dynamic In-Game Announcement & Guide System",
    "Improved AnchorMacro auto-charge and safety threshold",
    "Mace Macro timing variance and randomized delay",
    "Modern Glass UI theme with smooth fluid animations"
  ]
}
```
* `items`: Array of strings. Each item displays an accent bullet dot and automatically wraps across lines if long.

---

### 3.5. `callout` (Alert / Notice Box)
A highlighted card with a left accent bar.

```json
{
  "type": "callout",
  "style": "youtube",
  "title": "NO DISCORD NEEDED!",
  "text": "All upcoming video tutorials, configuration tips, and update drops will be published directly inside this in-game panel."
}
```
* `style`:
  * `"youtube"`: Red accent line (great for video guides).
  * `"tip"`: Purple/client accent line.
  * `"warning"`: Amber/orange accent line.
  * `"update"`: Green accent line.
* `title`: Bold header for the callout.
* `text`: Body text (word-wrapped).

---

### 3.6. `button_row` (Multi-Button Row)
Displays 2 or 3 buttons side-by-side.

```json
{
  "type": "button_row",
  "buttons": [
    {
      "label": "YouTube",
      "url": "https://www.youtube.com/@DLindustries",
      "style": "accent"
    },
    {
      "label": "Releases",
      "url": "https://github.com/DLindustries/System/releases",
      "style": "outline"
    },
    {
      "label": "Playlists",
      "url": "https://www.youtube.com/@DLindustries/playlists",
      "style": "glass"
    }
  ]
}
```
* `buttons`: Array of button objects.
* Button `style`:
  * `"accent"`: Filled with theme accent color.
  * `"outline"`: Semi-transparent background with accent border.
  * `"glass"`: Dark subtle card style.
  * `"danger"`: Red button.

---

### 3.7. `button` (Single Full-Width Button)
```json
{
  "type": "button",
  "label": "Join our Community Website",
  "url": "https://dlindustries.uk",
  "style": "accent"
}
```

---

### 3.8. `text` (Paragraph / Note)
```json
{
  "type": "text",
  "text": "Make sure to check our YouTube playlists for in-depth PvP macro configuration tutorials.",
  "color": "muted"
}
```
* `color`: `"default"` (white), `"muted"` (light gray), or `"accent"` (theme accent color).

---

### 3.9. `divider`
Draws a subtle glassmorphic horizontal rule:
```json
{
  "type": "divider"
}
```

---

### 3.10. `image`
Displays an image from a web URL or an in-game Minecraft texture:
```json
{
  "type": "image",
  "url": "https://example.com/banner.png",
  "height": 80
}
```
* `url`: Web image URL (asynchronously downloaded and cached).
* `texture` *(optional)*: In-game resource identifier (e.g. `"system:textures/gui/client_icon.png"`).
* `height`: Height in pixels.

---

## 4. Full Working Example

```json
{
  "id": "notice-2026-10-01-v2",
  "title": "System Bulletin",
  "blocks": [
    {
      "type": "banner",
      "tag": "NEW TUTORIAL",
      "title": "Mastering the Anchor Macro & D-Tap Setup",
      "description": "Watch our official walkthrough on YouTube covering delay optimization, D-Tap placement, and anti-ban configurations.",
      "primary_button": {
        "label": "▶ Watch on YouTube",
        "url": "https://www.youtube.com/@DLindustries",
        "style": "accent"
      }
    },
    {
      "type": "version_check"
    },
    {
      "type": "callout",
      "style": "youtube",
      "title": "NO DISCORD NEEDED!",
      "text": "All upcoming video tutorials, configuration tips, and update drops will be published directly inside this in-game panel."
    },
    {
      "type": "header",
      "text": "What's New in System",
      "size": "h2",
      "divider": true
    },
    {
      "type": "bullet_list",
      "items": [
        "Added Dynamic In-Game Announcement & Guide System",
        "Improved AnchorMacro auto-charge and safety threshold",
        "Mace Macro timing variance and randomized delay",
        "Modern Glass UI theme with smooth fluid animations"
      ]
    },
    {
      "type": "button_row",
      "buttons": [
        {
          "label": "YouTube",
          "url": "https://www.youtube.com/@DLindustries",
          "style": "accent"
        },
        {
          "label": "Releases",
          "url": "https://github.com/DLindustries/System/releases",
          "style": "outline"
        },
        {
          "label": "Playlists",
          "url": "https://www.youtube.com/@DLindustries/playlists",
          "style": "glass"
        }
      ]
    }
  ]
}
```
