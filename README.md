<p>
  <img src="app/src/main/ic_launcher-playstore.png" width="140" alt="PingMate Logo">
</p>

<h1>PingMate ⚡</h1>

<p>
  Notifications today are overwhelming. Important messages get buried under constant alerts, breaking your focus throughout the day.

<strong>PingMate brings structure to the chaos.</strong>

It intelligently collects, filters, groups, and summarizes notifications into a clean, unified feed — helping you focus only on what truly matters.

Built entirely with modern Android technologies, PingMate processes notifications securely using on-device Machine Learning, with optional Gemini AI integration for enhanced summaries.

</p>

<p>
  <a href="https://kotlinlang.org">
    <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg" alt="Kotlin 2.0.21">
  </a>
  <a href="https://developer.android.com/jetpack/compose">
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg" alt="Jetpack Compose">
  </a>
  <a href="https://developer.android.com">
    <img src="https://img.shields.io/badge/Android-10%2B-3DDC84.svg" alt="Android 10+">
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/License-MIT-lightgrey.svg" alt="MIT License">
  </a>
  <a href="https://github.com/your-username/PingMate/releases">
    <img src="https://img.shields.io/badge/Build-Passing-brightgreen.svg" alt="Build Passing">
  </a>
</p>

---

## 🚀 Key Features

Features are organized by what you see in the app—from onboarding to daily use.

| Feature | What you get |
| :--- | :--- |
| **🎙️ Voice AI Assistant** | Tap the mic FAB on Home or the widget. Speak or type (e.g. *“Summarize today’s WhatsApp”*). Results are powered by **Google Gemini** using your API key from Settings. |
| **📋 Unified notification feed** | One list for all tracked apps (WhatsApp, Gmail, Instagram, etc.). Paginated list with app icon, title, content, time. Filter by app, search by text, filter by date (Today, Yesterday, …). |
| **⏰ Reminders** | On any notification, set a **date & time** and optional note in one dialog. Or create standalone reminders. Upcoming reminders appear on Home; alarms fire at the set time. |
| **🖼️ Sender avatars & media** | When the source app provides them, you see contact avatars (large icon) and shared images (big picture) in the feed and in notification details. |
| **🔧 Settings** | **Gemini API key** (required for AI), **Choose applications** (which apps to track), **Exclude from AI** (per-app toggles), **Clear messages** (by app or clear all). |
| **📱 Home screen widget** | Add the PingMate widget to open the AI assistant in one tap. |
| **🔒 Privacy first** | No account or sign-up. Your API key and data stay on device. Notifications are only sent to Gemini when you use the AI features. |

---

## 📱 Screenshots

Screens in app flow order: **Onboarding → Permission → Choose App → Home → Voice Assistant → AI Result → Settings → Home Widget**.

| 1. Onboarding | 2. Permission | 3. Choose App | 4. Home |
|:---:|:---:|:---:|:---:|
| <img src="app_screens/Onboarding Screen.jpeg" width="210" alt="Onboarding"/> | <img src="app_screens/Notifcation Permision Screen.jpeg" width="210" alt="Permission"/> | <img src="app_screens/Choose App Screen.jpeg" width="210" alt="Choose App"/> | <img src="app_screens/Home Screen.jpeg" width="210" alt="Home"/> |
| 5. Voice Assistant | 6. AI Result | 7. Settings | 8. Home Widget |
|:---:|:---:|:---:|:---:|
| <img src="app_screens/Assistant Screen.jpeg" width="210" alt="Voice Assistant"/> | <img src="app_screens/Ai Result Screen.jpeg" width="210" alt="AI Result"/> | <img src="app_screens/Setting Screen.jpeg" width="210" alt="Settings"/> | <img src="app_screens/Home Widget Screen.jpeg" width="210" alt="Home Widget"/> |

---

## 📖 How to Use the App

A clear, step-by-step guide from first launch to every main action in the app.

---

### 1️⃣ First-time setup

| Step | Screen | What to do |
| :---: | :--- | :--- |
| 1 | **Welcome** | Open PingMate. Read the feature cards and tap **Get Started**. |
| 2 | **Notification access** | The app explains why it needs notification access. Tap the button to open **system settings**, find PingMate under *Notification access*, and **turn it on**. Return to the app. |
| 3 | **Choose Apps** | Select the apps you want to track (e.g. WhatsApp, Gmail, Instagram). Tap **Done** or **Continue**. |
| 4 | **Home** | You land on the main feed. Notifications from the chosen apps will appear here as they arrive. |

---

### 2️⃣ Home feed

| Action | How |
| :--- | :--- |
| **Browse** | Scroll the list. Each card shows app icon, title, snippet, and time. |
| **Filter by date** | Tap a chip in the date strip (Today, Yesterday, or a specific date). |
| **Filter by app / search** | Use the **filter** icon to pick an app, or the **search** icon to type keywords. |
| **Open a notification** | Tap a card → **detail sheet** opens. From here you can set a reminder, favorite, delete, or open in the original app. |
| **Remove from feed** | Swipe the card left to delete. |

---

### 3️⃣ Voice AI assistant

| Step | What happens |
| :---: | :--- |
| 1 | On **Home**, tap the **microphone FAB** (or add the PingMate **widget** and tap it). Grant **microphone** permission if asked. |
| 2 | The **Assistant screen** opens (full-screen overlay). You see a listening animation. **Speak** your request (e.g. *“Summarize today’s WhatsApp”*, *“What did I get from Gmail?”*). |
| 3 | After you finish, the app sends recent notifications as context to **Gemini**. A **result card** appears on the same screen with your request and the AI summary. You can copy the text or tap **Done** to close. |
| 4 | **To use AI:** Set your **Gemini API key** in **Settings → Gemini API Key** ([get one here](https://aistudio.google.com/apikey)). |

---

### 4️⃣ Reminders

| Step | What happens |
| :---: | :--- |
| 1 | From **Home**, tap a **notification** → the **detail sheet** opens. |
| 2 | Tap **Set reminder** (or use the reminder entry point from the reminders section). The **Set Reminder dialog** opens. |
| 3 | Tap the **date & time** row to open the picker. Choose date and time; add an optional **note**. Tap **Schedule**. |
| 4 | The reminder appears in the **Reminders** section on Home. At the set time, you get a **system notification**. |

---

### 5️⃣ Settings

| Option | What it does |
| :--- | :--- |
| **Gemini API Key** | Paste your key so the Voice AI and summarization work. Stored only on your device. |
| **Choose applications** | Opens the same app picker as onboarding; change which apps are tracked. |
| **Exclude from AI** | Per-app toggles so that app’s notifications are not sent to Gemini. |
| **Clear messages** | Opens a dialog listing apps with message counts. Clear one app’s data or **Clear all**. |

*Open Settings from the **gear icon** in the Home top bar.*

---
## 📥 Download

Get the latest APK from the releases page:

[Download APK](https://drive.google.com/file/d/1c-CVaWi_G2wuR3qxgvyLBgFuSWi_U9nY/view?usp=sharing)

---

## 🛠️ Tech Stack & Architecture

PingMate leverages modern Android development paradigms to ensure a robust, maintainable, and highly performant application.

### 🏗️ MVVM & Clean Architecture
- **UI Architecture:** 100% Jetpack Compose (Material 3), Navigation Compose
- **Concurrency:** Kotlin Coroutines & Flow
- **Data Persistence:** Room Database (Paging 3 Support), DataStore Preferences
- **Dependency Injection:** Koin
- **Background Work:** WorkManager, Android Services (`NotificationListenerService`)
- **AI & ML:** Gemini SDK (offline summarization engine), Google ML Kit (Entity Extraction, Smart Reply)
- **Widgets:** Glance App Widget
- **Build System:** Gradle Kotlin DSL (KTS), Version Catalogs

### 🧩 App flow (navigation)

Full flow as in the app: onboarding, then from Home you reach Assistant screen, Assistant result, Settings, and Reminder dialog.

```
┌─────────────┐     ┌──────────────────────┐     ┌───────────────┐     ┌────────────┐
│   Welcome   │ ──► │ Notification Access  │ ──► │  Choose Apps  │ ──► │    Home    │
│ Get Started │     │ (system settings)    │     │ (tracked apps) │    │ (main feed)│
└─────────────┘     └──────────────────────┘     └───────────────┘     └─────┬──────┘
                                                                             │
         ┌──────────────────────────────────────────┬────────────────────────┼────────────────────────┐
         │                                          │                        │                        │
         ▼                                          ▼                        ▼                        ▼
┌─────────────────┐                    ┌─────────────────────┐    ┌─────────────────┐         ┌─────────────────┐
│    Settings     │                    │  Voice AI Assistant  │   │  Notification   │         │  Home Widget    │
│ (gear on Home)  │                    │  (FAB on Home)       │   │    Detail       │         │ (tap → same     │
│                 │                    │         │            │   │    Sheet        │         │  as FAB)        │
│ • Gemini API    │                    │         ▼            │   │       │         │         └─────────────────┘
│ • Choose apps   │                    │  Assistant Screen    │   │       │         │
│ • Exclude from  │                    │  (listening →        │   │       ▼         │
│   AI            │                    │   speak command)     │   │ Set Reminder    │
│ • Clear messages│                    │         │            │   │   Dialog        │
└─────────────────┘                    │         ▼            │   │ (date & time    │
                                       │  Result card on      │   │  + note)        │
                                       │  same Assistant      │   └─────────────────┘
                                       │  screen              │
                                       └──────────────────────┘
```

| From | You can open |
| :--- | :--- |
| **Home** | **Settings** (gear) · **Voice Assistant** (FAB or widget) · **Notification detail** (tap a card) |
| **Notification detail** | **Set Reminder** dialog (date, time, note) · Favorite · Delete · Open in original app |
| **Assistant screen** | Speak command → **result card** on same screen → Done to close |
| **Settings** | Choose applications (app picker) · Clear messages (by app or all) |

---

## 🎮 Getting Started (For Developers)

> [!IMPORTANT]  
> **Gemini API key required for AI features**  
> PingMate is free and does not require an account. To use **AI Voice Assistant** and **Smart Summarization**, you need your own Google Gemini API key.  
> 1. Get a free key from **[Google AI Studio](https://aistudio.google.com/apikey)**.  
> 2. In the app: **Settings → Gemini API Key** → paste your key.  
> Your key is stored only on your device and is never sent to our servers.

### Prerequisites

- **Android Studio** (Koala or newer recommended)
- **JDK 11+**
- **Android SDK** API 29 or higher (Android 10+)
- **Device or Emulator** (API 29+) required for `NotificationListenerService` and Exact Alarms testing.

### Local Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/HimanshuMDev/PingMate
   cd PingMate
   ```
2. **Open the project in Android Studio** and allow Gradle to sync.
3. **Build & Run** on your device or emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## 📂 Project Structure

```text
com.app.pingmate/
├── data/             # Room DB configurations, DAOs, Entities
├── presentation/     # Jetpack Compose UI, ViewModels, Navigation Graphs
│   ├── onboarding/   # Walkthrough & Permissions flow
│   ├── dashboard/    # Home Feed, AI Voice Overlay, Reminders dialog
│   └── settings/     # App Configuration, Exclusions, API Keys
├── service/          # Core NotificationListenerService implementation
├── receiver/         # AlarmManager Broadcast Receivers for reminders
├── widget/           # Glance-based Home Screen widgets
└── utils/            # AI Engines, ML Kit integration, Date formatters
```

---

## 🔮 Roadmap & Upcoming Features

- [ ] **Auto OTP picker** — Automatically detect and pick OTPs from notifications so you can copy or auto-fill them in one tap.
- [ ] **Better utilization of AI** — Deeper integration and smarter use of AI across the app (summaries, actions, and insights).
- [ ] **Specific alert & reminder for specific notifications** — Custom sounds and high-priority alerts for chosen contacts or apps (e.g. when a specific person messages you).
- [ ] **Statistics & analysis of notifications** — Predefined analytics (volume, apps, trends) and predefined quick replies; clearer insight into your notification patterns.
- [ ] **Automatic processes** — More automation (e.g. auto-categorize, auto-reply templates, scheduled summaries) so routine actions happen without extra steps.

---

## 🤝 Contributing

We welcome contributions to make PingMate even better! 

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---
