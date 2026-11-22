# 🐜 Worker Ant

**An all-in-one Android productivity app: to-do list, unified Google + Outlook calendar, and a doomscroll blocker — with 50 themes.**

Like Sisyphus, the work never ends. Worker Ant helps you keep pushing the boulder anyway: plan your tasks, see your whole schedule in one place, and get bounced out of Reels and Shorts before they eat your evening.

> Built because syncing Outlook and Google calendars natively is a pain, and a good doomscroll blocker shouldn't cost a subscription.

---

## Features

### ✅ Tasks
- Add, check off, and delete tasks — saved locally and kept between launches
- Scheduled tasks show their time and place right in the list
- Each theme has its own animated mascot in the corner (a spinning revolver for Old Western, steaming coffee for Lo-fi Café, a rattling coffin for Gothic…)

### 📅 Calendar (Google + Outlook together)
- **Month, Day, and 3-Day** views, styled after the Outlook mobile app
- Shows events from **every calendar synced to your phone** — Google and Outlook side by side, color-coded and labeled
- **Pending Scheduled Tasks:** pick an unscheduled task, give it a time (required) and a place (optional), and it's added to **both** your Google and Outlook calendars at once
- Swipe left/right to change months or days; red "now" line in the day views
- No logins, no API keys, no third-party sync service — it reads and writes Android's built-in calendar database

### 🚫 Doomscroll Blocker
- Choose what to block: **Instagram Reels, YouTube Shorts, TikTok**
- Blocks only the feed, not the app — your Instagram DMs and regular YouTube videos still work
- Presses Back the moment a blocked feed opens, then sends a notification: *"Worker Ant blocked Instagram Reels · Blocked 3 times today"*
- Only watches the apps it can block, and never reads or stores your content

### 🎨 50 Themes
Each styled theme changes colors, fonts, corner shapes, and the Tasks mascot.

| Category | Themes |
|---|---|
| Basic colors | Match wallpaper (Material You), Grape, Forest, Ocean, Sunset |
| Cute & girly | Bubblegum Glam, Coquette, Kawaii Pastel, Strawberry Milk, Sakura Bloom, Fairycore, Mermaidcore, Magical Girl, Y2K Pop, Pop Diva |
| Fandom-inspired | Wizard School, Galactic Saga, Elven Realm, Comic Hero, Night Vigilante, Shonen Hero, Idol Stage, Block Builder, Pirate Adventure |
| Aesthetics | Dark Academia, Light Academia, Cottagecore, Gothic, Witchy, Celestial, Steampunk, Lo-fi Café, Matcha, Tropical Beach, Grunge, Minimalist |
| Bold & retro | Cyberpunk, Old Western, Metallic, Muscle Car, Synthwave, Vaporwave, Retro Arcade, Retro Diner |
| Seasonal | Spooky Season, Holiday Cheer, Snow Day, Sweetheart, Spring Garden, Autumn Harvest |

---

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Room** (SQLite) for tasks, via **KSP**
- **Navigation Compose** for the bottom-tab layout
- **CalendarContract** (Android's calendar provider) for Google/Outlook sync
- **AccessibilityService** for the doomscroll blocker
- Min SDK 26 (Android 8.0) · Target SDK 37

---

## Getting started

### Requirements
- [Android Studio](https://developer.android.com/studio) (recent stable version)
- An Android phone (Android 8.0+) with USB debugging enabled, or an emulator

### Build and run
1. Clone the repo:
   ```bash
   git clone https://github.com/InTheHiggsField/Worker_Ant.git
   ```
2. Open the folder in Android Studio and let Gradle sync finish.
3. Plug in your phone (or start an emulator), select it in the device dropdown, and press ▶ **Run**.

### First-time setup on the phone
| Feature | What to do |
|---|---|
| Calendar | Open the Calendar tab and tap **Grant calendar access**. |
| Outlook events | In the **Outlook** app: *Settings → your account → turn on "Sync calendars"*. Outlook events then appear in your phone's calendar database, and Worker Ant can see them. |
| Blocker | Blocker tab → **Open Accessibility settings** → *Installed apps → WorkerAnt Doomscroll Blocker → On*. If it's greyed out: *Settings → Apps → WorkerAnt → ⋮ → Allow restricted settings*. |
| Blocked notifications | Blocker tab → **Allow notifications** (Android 13+). |

---

## Permissions

| Permission | Why |
|---|---|
| `READ_CALENDAR` / `WRITE_CALENDAR` | Show Google + Outlook events and add scheduled tasks to them |
| `POST_NOTIFICATIONS` | Tell you when something was blocked |
| Accessibility service (user-enabled) | Detect Reels/Shorts/TikTok and press Back. Limited to those apps in `res/xml/blocker_accessibility_config.xml`. |

Everything stays on the device. Worker Ant has no server, no analytics, and no internet permission.

---

## Project structure

```
app/src/main/java/com/inthehiggsfield/workerant/
├── MainActivity.kt              # App shell, bottom navigation, theme state
├── data/
│   ├── tasks/                   # Room: Task, TaskEvent, TaskDao, AppDatabase
│   └── calendar/                # CalendarRepository (CalendarContract reads/writes)
├── blocker/
│   ├── BlockTarget.kt           # What can be blocked + the view IDs that identify each feed
│   ├── DoomscrollBlockerService.kt
│   ├── BlockNotifier.kt
│   └── BlockerPrefs.kt
└── ui/
    ├── tasks/                   # Tasks screen + ViewModel
    ├── calendar/                # Month / Day / 3-Day views, scheduling dialog
    ├── blocker/                 # Blocker settings screen
    ├── settings/                # Theme picker
    └── theme/                   # AppThemes (all 50 themes), mascots, typography
```

---

## Customizing

**Add a theme** — add one entry to the `AppTheme` enum in `ui/theme/AppThemes.kt` (name, tagline, category, five colors, fonts, corner style), then add its mascot in `MascotDrawings.kt`. The Settings screen picks it up automatically.

**Add a blocker target** — add an entry to `BlockTarget.kt` with the app's package name and the view IDs of its feed screen, and add the package name to `res/xml/blocker_accessibility_config.xml`.

**When an app update breaks blocking** — apps rename their internal view IDs from time to time. Turn on *Blocker → Developer → Log screen IDs*, open the feed, and filter Logcat by `WorkerAntBlocker` to find the new ID.

---

## Roadmap

- [ ] Reschedule / unschedule tasks from the calendar
- [ ] Tap an event for details
- [ ] Recurring tasks and reminders
- [ ] More blocker targets (Facebook Reels, Snapchat Spotlight)
- [ ] Prominent-disclosure screen for the Accessibility service (required for Google Play)
- [ ] Google Play release

---

## Credits

Fonts are bundled from [Google Fonts](https://fonts.google.com) under the SIL Open Font License or Apache License 2.0: Orbitron, Rye, Michroma, Bebas Neue, Pacifico, Fredoka, Playfair Display, Righteous, Cinzel, Bangers, Press Start 2P, Audiowide, UnifrakturMaguntia, Special Elite, Creepster, and Mountains of Christmas.

Theme names describe aesthetics and are not affiliated with or endorsed by any brand or franchise.

---

Made by **Johnathan Shirley** ([@InTheHiggsField](https://github.com/InTheHiggsField))
