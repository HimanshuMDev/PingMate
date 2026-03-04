# PingMate — Code Prompt (first build / senior Android)

Use this prompt with an AI coding assistant (e.g. Cursor, ChatGPT, or Claude) to generate the initial Android app. It gives the AI full context: **design reference** (PNG screens from the design prompt), **Jetpack Compose components**, **min/target SDK**, **color system**, **architecture**, and **flows**.

**How this fits in the flow:** First, the design prompt (`DESIGN_PROMPT.md`) is used with a design AI to generate PNG mockups for Welcome, Choose Apps, Home, Settings, and Set Reminder. Then this code prompt is used with a coding AI. The code prompt **refers to those generated images** so the implementation matches the design in color contrast, spacing, and component style. If you have the PNGs, you can attach or describe them when sending this prompt; the prompt also spells out all colors and layout so the AI can implement correctly without the images.

---

## Code prompt (copy-paste ready)

Copy everything inside the code block below and paste it into your AI coding assistant. You can add a line like: “Use the attached PNG screens as visual reference for layout and colors” if you are providing the design output.

```
You are a senior Android engineer. Build an Android application named PingMate in Kotlin using Jetpack Compose. This prompt is used after design mockups have been created: the designer (or I) ran a design prompt through a design AI and obtained PNG screens for Welcome, Choose Apps, Home, Settings, and Set Reminder. Use those generated images as visual reference for layout, color contrast, spacing, and design elements. The implemented app should match the look and feel of those design outputs — same color contrast, same component style (rounded cards, dark surfaces, primary blue for actions), and same hierarchy. If you do not have the images, follow the color and layout specifications in this prompt exactly.

———
PURPOSE (what the app does)
———
PingMate is a smart notification hub for Android. It (1) shows notifications from user-selected apps in one feed, (2) lets the user ask AI (Gemini API) for summaries via voice or text, and (3) lets the user set reminders from any notification. No account is required; the user’s Gemini API key is stored only on device. Implement the structure and UI first; full AI and reminder logic can be stubbed or completed in iteration.

———
DESIGN REFERENCE (important)
———
Before coding, I have design mockups (PNG screens) generated from a design prompt. They define:
- Color contrast: dark backgrounds (#0E0E12, #1A1A24), primary blue (#4A80F5) for actions, white/off-white for primary text, muted grays for secondary text. Use these exact hex values so the app matches the design.
- Design elements: rounded corners (no flat rectangles), cards with subtle borders or elevation, dialogs with dark surface and optional gradient border (primary blue at low opacity), primary filled buttons (blue) and secondary outlined/text buttons.
- Layout and hierarchy: clear section headers, consistent spacing (e.g. 16–24dp between sections), list items with icon + title + subtitle, touch targets at least 48dp where possible.
When implementing each screen, align with this visual system and, if available, with the generated PNGs so the result looks like the design.

———
SDK AND BUILD (use exactly)
———
- Package name: com.app.pingmate  (do not change)
- Min SDK: 24  (Android 7.0; required for Compose and modern APIs)
- Target SDK / Compile SDK: 34  (Android 14; use for latest behavior and compatibility)
- Kotlin: 1.9 or higher
- Build: Android Gradle Plugin 8.x, Kotlin DSL (build.gradle.kts). Use latest stable Compose BOM, Material 3, Navigation Compose, Room, and your chosen DI (Hilt or Koin).
- Architecture: Single Activity; all UI in Jetpack Compose. No XML layouts for the main app screens (Welcome, Permission, Choose Apps, Home, Settings, dialogs).

———
ARCHITECTURE (Clean, feature-oriented)
———
Organize the code so that:
- presentation/  — All Composables (screens and dialogs), ViewModels, and navigation. Group by feature: e.g. onboarding (Welcome, Permission, ChooseApps), dashboard (Home, Voice AI placeholder, Set Reminder dialog), settings (SettingsScreen and related dialogs). Each screen has a ViewModel that holds state (StateFlow) and talks to the data layer.
- data/  — Local data only: Room database (entities and DAOs for notifications and reminders), and DataStore or SharedPreferences for onboarding flag, tracked app list, excluded-from-AI list, and Gemini API key. Repositories are optional but recommended for a clean separation.
- service/  — Android Service that extends NotificationListenerService. It receives system notifications, parses them, and inserts/updates records in Room. Declared in the manifest; the Permission screen must explain how the user can enable it.
- receiver/  — BroadcastReceiver for AlarmManager. When a reminder time is reached, the receiver runs and shows a system notification. Reminder data is stored in Room.
- di/  — Dependency injection (Hilt or Koin). Provide Application context, Room database, DAOs, repositories (if any), and ViewModels. Keep modules focused (e.g. app module, database module, VM module).
- ui/theme/  — Central place for colors (Color.kt), typography (Type.kt), and theme (Theme.kt). Use Material 3 darkColorScheme and map the hex values below to primary, surface, background, error, etc., so the whole app uses one consistent theme.

Use ViewModel for screen state and business logic; expose state via StateFlow and collect it in Composables with collectAsState(). Use Kotlin coroutines and Dispatchers.IO for any database or network work. Do not block the main thread.

———
JETPACK COMPOSE (which components to use)
———
- Scaffold  — Use for full-screen layouts. Home and Settings should use Scaffold with topBar and, on Home, floatingActionButton (the mic FAB). This keeps the app bar and FAB consistent with Material 3 patterns.
- TopAppBar / CenterAlignedTopAppBar  — For the app bar. Include title, optional trailing actions (e.g. notification count badge, settings icon). Use the theme’s surface/onSurface colors.
- Surface, Card  — For grouping content. Cards for notification items and reminder rows; Surface for dialog and bottom-sheet backgrounds. Apply the dark surface color and rounded corners (12–16dp for cards, 20–24dp for dialogs).
- Layout: Row, Column, Box, LazyColumn, LazyRow, Spacer  — Use for composing screens. LazyColumn for scrollable lists (notifications, apps, settings rows); LazyRow for horizontal date chips. Keep spacing consistent (e.g. 8dp, 16dp, 24dp).
- Inputs: TextField, OutlinedTextField  — For Gemini API key, search in Choose Apps, and note in Set Reminder. Use dark background and the border/muted colors from the design so contrast matches the mockups.
- Buttons: Button (filled), OutlinedButton, IconButton, TextButton  — Primary actions use filled primary button (blue). Secondary or cancel use outlined or text. IconButton for top-bar icons (settings, back).
- Switch, Checkbox  — For “Track” in Choose Apps and “Exclude from AI” toggles. Style to match the dark theme.
- Dialog, ModalBottomSheet  — For Set Reminder and Clear Messages. Use dark surface, rounded corners, and optional thin gradient or primary-blue border so they match the design prompt output.
- Navigation: use the Navigation Compose library (NavHost, composable, NavController). Define routes as constants and pass ViewModels or inject them. No XML navigation graphs.

Use only Material 3 Compose components where possible. Avoid mixing in legacy Material 2 or AppCompat UI so the app stays consistent and maintainable.

———
COLORS AND THEME (implement exactly — matches design PNGs)
———
Define these in ui/theme/Color.kt and use them in Theme.kt (darkColorScheme). The design mockups use this palette; the app must use the same for color contrast and visual consistency.

- Background (main):  #0E0E12  — CharcoalBackground. Use for the default background behind all screens.
- Surface (cards, dialogs):  #1A1A24  — SurfaceDark. Use for cards, list items, dialog surfaces.
- Surface variant:  #24243A  — SurfaceVariant. Use for elevated areas or alternate list rows if needed.
- Primary:  #4A80F5  — NotiBlue. Use for FAB, primary buttons, links, icons that indicate action, selected chip border/fill.
- Primary light:  #6B9FFF  — NotiBlueLight. Use for pressed or hover state of primary if needed.
- Secondary / accent:  #9B59F5  — VipPurple. Use for secondary accents (e.g. optional badges or secondary highlights).
- Error / destructive:  #FF3B5C  — UrgentRed. Use for “Clear”, “Delete”, and other destructive actions.
- Success:  #34D399  — SuccessGreen. Use for success messages or indicators.
- Text primary:  #FFFFFF  — Main text (titles, body). Must sit on dark background for contrast.
- Text secondary:  #B0B3C8  — Secondary text (subtitles, hints). Use for less prominent copy.
- Text muted:  #6B6F8A  — Timestamps, captions, disabled text.
- Border subtle:  #2A2D45  — Dividers, card borders. Keep borders thin (1dp) so the UI is not heavy.

Typography: Use Material 3 typography as base. Override so that screen titles are Bold ~20sp, section headers SemiBold 14–16sp, body Regular 14–16sp, captions 12sp with muted color. Buttons: SemiBold 14–16sp. Ensure contrast ratios are sufficient for readability (e.g. white on #1A1A24).

Shapes: Rounded corners everywhere — 12–16dp for cards and list items, 20–24dp for dialogs and bottom sheets. No sharp rectangles. Buttons: rounded (e.g. 12dp). This matches the design prompt and the generated screens.

———
NAVIGATION FLOW (user journey)
———
- Routes to implement: welcome, permission, choose_apps, home, settings. (Set Reminder and Clear Messages are dialogs/sheets shown from Home or Settings, not separate routes.)
- First-time flow: When the user has not completed onboarding, start at welcome. From Welcome → permission → choose_apps → home. After “Done” on Choose Apps, save the selected apps and set an “onboarding complete” flag, then navigate to home.
- Later launches: If onboarding is complete, start directly at home. Do not show Welcome again.
- From Home: The user can tap the gear icon to open settings. From Settings, the back arrow or system back returns to home. Optional: “Choose applications” from Settings can navigate to the same Choose Apps screen but with a “from settings” mode (e.g. return to Settings or Home after Done).
- Implement a single NavHost in MainActivity. The startDestination of the NavHost should depend on the onboarding-complete flag (read from DataStore or SharedPreferences). Use composable() for each route and pass or inject the corresponding ViewModel.

———
SCREENS — LAYOUT AND BEHAVIOUR (match design mockups)
———
Describe each screen so the implementation matches the design PNGs and the design prompt. Pay attention to spacing, contrast, and component style.

1) Welcome (onboarding)
- Content: App name “PingMate” prominently at top. Two or three short value bullets (e.g. “One feed for all your app notifications”, “AI summaries with Gemini”, “Reminders without leaving the app”). One primary button: “Get Started” (filled, primary blue, rounded).
- Layout: Centered or top-aligned column with comfortable padding (e.g. 24dp). Background: charcoal. The design mockup shows a clean, minimal welcome with no clutter.
- Behaviour: On “Get Started” click, navigate to the permission route. No persistence needed on this screen.

2) Permission
- Content: Title such as “Allow notification access”. Short explanation (one paragraph or bullets) of why PingMate needs notification access (to show notifications from selected apps in one feed). Two actions: a button that opens the system Notification access settings (use an Intent so the user can enable PingMate), and a “Continue” or “Next” button that navigates to choose_apps.
- Layout: Same dark background. Text readable (primary and secondary colors). Optional: a small status line or badge showing “Access granted” or “Not granted” if you can query the listener status.
- Behaviour: User is expected to enable the service in system settings and return; then tap Continue to go to Choose Apps.

3) Choose Apps
- Content: A search field at the top (hint: “Search apps”). Below, a scrollable list of installed apps. Each row: app icon (circular, 40–48dp), app name (primary text), optional package name or subtitle (muted), and a Switch or Checkbox for “Track”. At the bottom, a primary button “Done” or “Continue”.
- Layout: Use LazyColumn for the list. Match the design: same card or row style (dark surface, rounded, subtle border). Spacing between rows (e.g. 8dp). Load installed apps via PackageManager on a background thread.
- Behaviour: When the user taps Done, save the set of selected package names to DataStore or SharedPreferences, set onboarding complete, and navigate to home. Only notifications from these packages will be shown in the feed (the NotificationListenerService can filter by this list).

4) Home (main dashboard)
- Content: Top bar with app title or logo, optional notification count (e.g. “12 Notifications”), and a settings (gear) icon. Below the top bar: a horizontal strip of date chips (Today, Yesterday, or specific dates) — use LazyRow. Below that: the main notification list (LazyColumn of cards). Each card: app icon, title (bold), content preview (1–2 lines), time (muted, right-aligned), optional favorite icon. Below or above the list (your choice): an “Upcoming reminders” section with reminder rows (icon, title/note, date and time). A floating action button (FAB) with mic icon, primary blue, bottom-right.
- Layout: Scaffold with topBar and floatingActionButton. Cards: Surface or Card with 12–16dp radius, dark surface color. Empty state: when there are no notifications for the selected filter, show an icon (e.g. bell off), message “No notifications yet”, and a “Refresh” text button. Match the design mockup for spacing and hierarchy.
- Behaviour: Date chip selection filters the notification list (query Room by date range). Tapping a notification can open a detail bottom sheet or dialog with “Set reminder” and other actions. FAB opens the Voice AI screen or a placeholder (“Voice AI” placeholder is fine for first build). Settings icon navigates to settings. Refresh reloads or re-queries the list.

5) Settings
- Content: Top bar “Settings” with back arrow. Body: a vertical list of sections. (1) Gemini API Key: label, an OutlinedTextField (or TextField) with hint “Paste your API key”, and a short subtitle “Stored only on this device.” (2) “Choose applications”: one row with label and chevron; tap navigates to choose_apps. (3) “Exclude from AI”: one row with label and chevron; opens a screen or dialog where the user can toggle “Exclude from AI” per tracked app. (4) “Clear messages”: one row; tap opens a dialog that lists apps with message counts and offers “Clear this app” and “Clear all”. Use red/destructive color for clear actions.
- Layout: Same dark surfaces and rounded cards/rows as in the design. Section headers can be bold or semi-bold; rows with consistent height (e.g. 56dp) and padding. Save API key to DataStore/SharedPreferences on change or via a Save button.
- Behaviour: Back returns to home. “Choose applications” and “Exclude from AI” navigate to the appropriate sub-screens or dialogs; persist choices. Clear messages dialog should call Room DAO to delete notifications (by package or all).

6) Set Reminder (dialog or bottom sheet)
- Content: Title “Set reminder”. One row showing the selected date and time (e.g. “25 Mar 2026, 10:30 AM”) with a calendar and clock icon; tapping opens the system DatePicker and TimePicker. A text field for an optional note (“Add a note”). Buttons: “Cancel” (secondary) and “Schedule” (primary blue).
- Layout: Dialog or ModalBottomSheet with dark surface (#1A1A24), rounded corners (20–24dp), and optional gradient or primary-blue border to match the design. Padding so content is not cramped.
- Behaviour: On Schedule: create a reminder entity in Room, schedule an AlarmManager alarm for that time, and dismiss the dialog. The BroadcastReceiver will later show a notification when the alarm fires. Cancel just dismisses.

———
SERVICES AND DATA LAYER
———
- NotificationListenerService: Implement a service (e.g. PingMateNotificationService) that extends NotificationListenerService. In onNotificationPosted (or equivalent), read the notification’s package, title, text, timestamp, and optionally icon; insert or update a NotificationEntity in Room. Register the service in AndroidManifest with the required permission and intent-filter. The Permission screen must direct the user to enable this service in system settings. Filter stored notifications by the list of tracked package names from preferences.
- Reminders: Use AlarmManager to schedule exact alarms (or inexact if required by policy). Store each reminder in Room (e.g. GeneralReminderEntity with id, title/note, triggerTimeMillis). When the alarm fires, a BroadcastReceiver runs and posts a notification via NotificationManager. Optionally reschedule pending alarms on device boot (BOOT_COMPLETED).
- Room: Define entities NotificationEntity (id, packageName, title, content, timestamp, isFavorite, etc.) and GeneralReminderEntity (id, title, note, triggerTimeMillis, …). DAOs: insert, update, delete; query notifications by date range and package; count by package (for Clear messages); query upcoming reminders. Use TypeConverters if you store lists or dates.
- DataStore or SharedPreferences: Store onboarding complete (boolean), set of tracked package names (Set<String> or JSON), set of excluded-from-AI package names, and Gemini API key (string). Read/write on a background dispatcher where appropriate.

———
AI INTEGRATION (first build)
———
Create a class (e.g. OfflineSummarizationEngine or similar) that will later call the Gemini API. For the first build it can: read the API key from preferences; accept a list of recent notifications and a user query string (e.g. “Summarize today’s WhatsApp”); return a stub or hard-coded summary string. Run any work on a background dispatcher so the UI does not block. Full HTTP integration with Gemini can be added in a follow-up iteration.

———
DELIVERABLES (what you must produce)
———
- A single Android application module with package name com.app.pingmate, building with the SDK and dependencies above.
- Theme (PingMateTheme) that applies the exact color palette and typography described, so the app matches the design mockups in color contrast and style.
- Navigation graph with routes welcome, permission, choose_apps, home, settings; startDestination based on onboarding flag; all screens implemented as Composables with the structure and behaviour described.
- Room database and DAOs for notifications and reminders; DataStore or SharedPreferences for settings and onboarding.
- NotificationListenerService skeleton that writes to Room; BroadcastReceiver and AlarmManager skeleton for reminders.
- Use collectAsState() and LaunchedEffect (or similar) for reactive UI from flows. Do not add extra screens or features beyond what is specified; keep the first build focused and aligned with the design prompt and design-generated PNGs.
```

---

## How to use

- Paste the block above into Cursor, ChatGPT, Claude, or similar.
- Use the exact package name and color hex values so the app matches the design.
- After the first build, use intermediate prompts (e.g. "Add clear messages dialog", "Match SettingsScreen style") as in `TEAM_GUIDE_AI_APP_JOURNEY.md`.

---

*Design prompt (UI only): `DESIGN_PROMPT.md`*
