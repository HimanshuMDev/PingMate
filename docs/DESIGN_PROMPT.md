# PingMate — Design Prompt (for UI / design AI)

Use this prompt with any design or image-generation AI to get a consistent visual direction for the PingMate app. It defines screens, colors, and components so the output can be used as a reference for implementation in Jetpack Compose.

---

## Design prompt (copy-paste ready)

```
You are a senior UI/UX designer. Design the visual UI for an Android app named PingMate — a smart notification hub where users see all notifications from selected apps in one feed, with AI summaries and reminders.

TONE AND FEEL
- Premium, modern, dark theme. Friendly but professional. The user should feel in control of their notifications. No clutter; clear hierarchy. Suitable for a mobile-first, single-hand use.

COLOR PALETTE (use these exactly)
- Background (main): #0E0E12 (charcoal).
- Surface (cards, dialogs): #1A1A24 (dark surface).
- Surface variant (elevated cards, list rows): #24243A.
- Primary (actions, links, highlights, FAB): #4A80F5 (blue).
- Primary light (hover/pressed): #6B9FFF.
- Secondary / accent: #9B59F5 (purple).
- Destructive (delete, clear, danger): #FF3B5C (red).
- Success: #34D399 (green).
- Primary text: #FFFFFF (white).
- Secondary text: #B0B3C8 (muted white).
- Muted text / hints: #6B6F8A.
- Borders (subtle): #2A2D45.
- Borders (active/focus): #4A80F5.
- Chips / tags background: #252535.

TYPOGRAPHY
- App title / screen titles: Bold, 20–24sp equivalent, white.
- Section headers: SemiBold, 14–16sp, white or primary blue.
- Body text: Regular, 14–16sp, primary or secondary text color.
- Captions / metadata (time, counts): 12sp, muted color.
- Buttons: SemiBold, 14–16sp. Sufficient contrast on background.

SCREENS TO DESIGN (describe layout, key elements, and hierarchy)

1) WELCOME / ONBOARDING
- Full-screen. App name “PingMate” prominent at top.
- 3 short value bullets (e.g. “One feed for all notifications”, “AI summaries with Gemini”, “Reminders — no account needed”).
- Single primary CTA button: “Get Started” (filled, primary blue, rounded).
- Background: charcoal with optional subtle gradient or soft glow. No heavy decoration.

2) CHOOSE APP SCREEN
- Title: “Choose apps to track” or similar.
- List of installed apps: each row = app icon (circle, 40–48dp), app name (primary text), optional subtitle. Checkbox or toggle on the right to “track” or “exclude”.
- Search bar at top (optional): rounded, dark surface, hint “Search apps”.
- Bottom: primary button “Done” or “Continue” to save and proceed.
- Same dark surfaces and rounded corners (e.g. 12–16dp for list items).

3) HOME SCREEN
- Top bar: app identity (logo or name), optional notification count badge, settings icon (gear) on the right.
- Date filter: horizontal scrollable chips (Today, Yesterday, specific dates). Chips: rounded, surface variant background, selected = primary blue border or fill.
- Notification list: each card = app icon (circle, 40–48dp), title (bold), content preview (1–2 lines, secondary text), time (caption, right-aligned). Optional: small thumbnail or “big picture” area if notification has image. Cards on surface dark, rounded 12–16dp, subtle border.
- Reminders section (above or below list): “Upcoming reminders” header; each reminder row = icon, title/note, date and time. Same card style.
- Floating Action Button (FAB): primary blue, circular, mic icon — “Voice AI” or “Ask AI”. Bottom-right.
- Empty state: icon (e.g. bell off), “No notifications yet”, short subtitle, optional “Refresh” text button.

4) SETTINGS SCREEN
- Title: “Settings”.
- Sections as cards or grouped rows (same surface dark, rounded):
  - “Gemini API Key”: single-line input (password-style or visible), placeholder “Paste your API key”. Subtitle: “Stored only on device.”
  - “Choose applications”: one row, chevron right; opens Choose App screen.
  - “Exclude from AI”: one row, chevron right; opens list of apps with toggles.
  - “Clear messages”: one row, chevron right; opens dialog to clear by app or clear all.
- Each section: icon (optional), title, subtitle or value, trailing chevron. Dividers or spacing between sections.
- Same color system: primary blue for links/actions, red for “Clear” if needed.

5) SET REMINDER (dialog / bottom sheet)
- Modal overlay. Centered or bottom sheet: dark surface (#1A1A24), rounded top corners (20–24dp), gradient or subtle border (primary blue at low opacity).
- Header: “Set reminder” (or “Remind me”), close (X) button right.
- Fields: 
  - Date & time: one row with calendar icon and time; tap to open date/time picker. Show selected as “25 Mar 2026, 10:30 AM”.
  - Note (optional): multi-line or single-line, placeholder “Add a note”.
- Primary button: “Schedule” or “Set reminder” (filled, primary blue).
- Secondary: “Cancel” (text or outlined). Enough padding so content is not cramped.

COMPONENTS (reusable)
- Buttons: primary = filled, primary blue, rounded 12dp. Secondary = outlined or text, same radius.
- Cards: background surface dark or surface variant, rounded 12–16dp, border 1dp subtle or none.
- Dialogs: same surface, rounded 20–32dp, header with icon + title + close. Optional gradient border (primary blue + white at low opacity).
- List items: min height 48dp for touch; icon left, title + subtitle, optional trailing (chevron, switch, icon).
- Chips: rounded pill or 8dp, surface variant or chip background; selected state with primary border or fill.
- Inputs: dark surface, rounded 12dp, clear placeholder and label; focus state with primary border.

CONSTRAINTS
- Mobile-first: single column, scrollable lists. Dialogs and bottom sheets for secondary flows.
- Accessibility: contrast ratio at least 4.5:1 for text; touch targets ≥ 48dp.
- No flat rectangles: use rounded corners consistently. Spacing: 16–24dp between sections, 8–12dp between related elements.

OUTPUT
Produce a consistent design system (and, if possible, wireframes or mockups for each screen) that a developer can implement in Jetpack Compose. All colors and spacing should match the palette and rules above.
```

---

## How to use this prompt

- Paste the prompt above into your design AI (e.g. image generator, UI generator, or design assistant).
- You can ask for: full-screen mockups per screen, or a single “design system” summary with colors and component specs.
- Use the output as **reference** for implementation; adjust spacing and exact values (e.g. dp) when building in Compose to match your theme and components.

---

*For the coding prompt (to generate the actual Android app with Kotlin and Jetpack Compose), see `CODE_PROMPT.md`.*
