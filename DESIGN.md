# Scribe Design System (DESIGN.md)

## 1. Visual Atmosphere & Philosophy
* **Vibe**: High-end minimalist studio / editorial tool (Raycast + Apple Intelligence).
* **Density**: Daily App Balanced (Level 5). Generous breathing room, deliberate typography hierarchy.
* **Variance**: Controlled Asymmetric. Clean, distinct visual anchors without visual noise.
* **Core Rule**: Zero "developer rectangle prisons." No stacked cards with thick gray borders and trash can icons.

---

## 2. Calibrated Color Palette (Warm Editorial Light)
* **Canvas / Background**: `#F9F8F6` (Linen Porcelain — soft, warm, natural paper tone, zero glare).
* **Surface / Container**: `#FFFFFF` (Pure Porcelain — crisp white elevation for interactive rows and sheets).
* **Primary Ink**: `#191715` (Deep Charcoal Ink — razor-sharp contrast for titles, triggers, and active states).
* **Secondary Ink**: `#6E6A65` (Muted Warm Gray — readable descriptions and metadata).
* **Hairline Border**: `#EAE6DF` (Subtle Warm Hairline — soft definition, never 1px harsh gray).
* **Accent**: `#1D4ED8` (Scribe Cobalt — restrained, focused interactive state).
* **Destructive**: `#DC2626` (Muted Crimson — reserved exclusively for confirmed deletion).

---

## 3. Typographic Architecture
* **Display / Screen Headers**: Heavy Grotesk, track-tight, 24sp, Bold.
* **Trigger Pills**: Monospace / Geometric, 13sp, Bold, uppercase trigger badges with high-contrast ink fill.
* **Prompt Previews**: Clean Sans-Serif, 14sp, relaxed leading, max 2 lines with graceful ellipsis.
* **Metadata Tags**: Micro-caps, 10sp, letter-spacing +0.1em, medium weight.

---

## 4. Component Standards

### A. Snippet Row (Commands)
* **Structure**: Single continuous surface row with soft hairline border.
* **Leading**: Dark Ink badge pill (`/fix`, `/reply`, `/formal`) with white monospace text.
* **Center**: Shortcut name and prompt preview with clean vertical rhythm.
* **Trailing**: Subtle chip indicator (`AI Rewrite` or `Instant Text`).
* **Interaction**: Tap opens a clean detail/edit bottom sheet. Swipe or long-press for delete. Zero ugly trash cans in the main list.

### B. Floating Action Dock (Navigation)
* Floating glass/porcelain pill hovering smoothly over the bottom screen edge.
* Detached from screen borders with subtle ambient shadow.

### C. Haptics & Motion
* Spring animations (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessMedium`).
* Haptic feedback on every tap (`TextHandleMove` for selections, `LongPress` for actions).
