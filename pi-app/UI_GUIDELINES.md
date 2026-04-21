# AntiDonut Pi App — UI Guidelines

## Overview

The Pi app is a Qt/QML desktop application with a mobile-first, touch-optimized interface. It features a modern design system built around a consistent color palette, clear typography hierarchy, and reusable components.

**Display Size:** 720×1280px (portrait orientation)
**Theme Support:** Light and dark modes
**Navigation:** Bottom tab bar with 4 primary screens

---

## Color System

### Background Colors

| Property | Dark Mode | Light Mode | Usage |
|----------|-----------|-----------|-------|
| `bgPrimary` | `#0f172a` | `#f8fafc` | Main page backgrounds |
| `bgSurface` | `#1e293b` | `#ffffff` | Card, component backgrounds |
| `bgBorder` | `#334155` | `#e2e8f0` | Dividers, borders, borders between sections |

### Text Colors

| Property | Dark Mode | Light Mode | Usage |
|----------|-----------|-----------|-------|
| `textPrimary` | `#ffffff` | `#0f172a` | Headings, primary text |
| `textMuted` | `#64748b` | `#94a3b8` | Secondary text, labels, hints |

### Accent Colors

| Property | Value | Usage |
|----------|-------|-------|
| `accentStart` | `#6366f1` | Indigo: primary brand color, buttons, nav indicators |
| `accentEnd` | `#06b6d4` | Cyan: gradient end, secondary accents |

### Semantic Colors

| Property | Dark Mode | Light Mode | Usage |
|----------|-----------|-----------|-------|
| `healthy` | `#34d399` | `#065f46` | Health positive indicator (text) |
| `healthyBg` | `#064e3b` | `#d1fae5` | Health positive indicator (background) |
| `unhealthy` | `#f87171` | `#991b1b` | Health negative indicator (text) |
| `unhealthyBg` | `#450a0a` | `#fee2e2` | Health negative indicator (background) |
| `caloriesBurned` | `#f59e0b` | `#f59e0b` | Calories burned stat (amber) |

### Progress Ring Color States

The calorie progress ring uses a 4-state color system that shifts based on daily progress:

| Progress | Color | Visual Meaning |
|----------|-------|----------------|
| < 60% | `#22c55e` (Green) | Plenty of room to goal |
| 60–85% | `#6366f1` (Indigo) | On track to goal |
| 85–100% | `#f59e0b` (Amber) | Approaching goal |
| ≥ 100% | `#ef4444` (Red) | At or over daily goal |

---

## Typography

### Font Family
- **Primary:** Inter, system-ui, sans-serif
- **Fallback:** System UI sans-serif

### Font Sizes

| Size | Name | Usage |
|------|------|-------|
| 28px | `fontSizeHero` | Large stat numbers in progress ring |
| 20px | `fontSizeTitle` | Section headings |
| 14px | `fontSizeBody` | Body text, card values |
| 11px | `fontSizeSmall` | Labels, secondary text, nav bar |

### Font Weights

- **Bold (700):** Headings, primary values, labels with emphasis
- **Regular (400):** Body text, secondary information

### Text Styling Rules

- **Headings:** Use `fontSizeTitle` (20px) or `fontSizeHero` (28px) with bold weight
- **Labels:** Use `fontSizeSmall` (11px) with `textMuted` color; apply `letterSpacing: 1` for section headers (e.g., "LAST SCANS")
- **Stat Values:** Use bold weight, sized by context (11px to 28px depending on component)
- **Secondary Text:** Use `fontSizeSmall` or `fontSizeBody` with `textMuted` color

---

## Components

### StatCard
A compact card displaying a single metric with label.

**Dimensions:** Height 60px (standard), 40px (compact)
**Structure:**
- Value (bold, colored)
- Label (muted text below)
- Horizontally centered

**Properties:**
```qml
StatCard {
    value: "2050"           // Numeric value
    label: "Consumed"       // Label text
    valueColor: "#06b6d4"   // Color for the value
    isDark: true            // Apply dark theme
}
```

### CalorieRing
A circular progress indicator with dynamic color states.

**Dimensions:** 180×180px
**Features:**
- Animated color transitions (260ms ease-in-out-quad)
- Center label showing "kcal left"
- Background ring (always visible, muted)
- Progress arc (color-coded by progress ratio)

**Properties:**
```qml
CalorieRing {
    consumed: 1200    // Calories consumed today
    goal: 2000        // Daily calorie goal
    isDark: true      // Apply dark theme
}
```

### HealthBadge
A small indicator showing whether a food item is healthy or unhealthy.

**Usage:** Display next to food log entries to indicate nutritional status.

### StatCard Row (Activity Strip)
Three `StatCard` components arranged horizontally with vertical dividers.

**Dimensions:** Full width, 72px height
**Dividers:** 1px vertical lines between cards using `bgBorder` color
**Spacing:** 0px (cards are adjacent with dividers)

### Card Containers
Standard rounded rectangular containers for grouping related content.

**Border Radius:** 10px (`radiusCard`)
**Padding:** 12–16px (`paddingPage`)
**Background:** `bgSurface` color
**Spacing:** 12–16px between cards

---

## Layout & Spacing

### Page Margins
- **Default:** 16px (`paddingPage`) on all sides
- **Card Spacing:** 12px between cards

### Card Border Radius
- **Cards:** 10px (`radiusCard`)
- **Buttons:** 8px (`radiusButton`)

### Component Spacing
- **Rows:** 8px spacing between horizontal elements
- **Columns:** 12px spacing between vertical elements
- **Dividers:** 4px gap around borders

### Alignment
- **Center:** For circular elements (avatar, progress ring, centered buttons)
- **Left:** For text content and list items
- **Right:** For secondary controls or metadata

---

## Navigation

### Bottom Tab Bar
- **Height:** 64px
- **Columns:** 4 equal-width tabs (180px each at 720px width)
- **Indicator:** 24px wide, 3px tall rounded bar above tab label
- **Spacing:** 4px between indicator and label

### Tab States

| State | Indicator Color | Label Color |
|-------|-----------------|-------------|
| Active | `#6366f1` | `#6366f1` |
| Inactive (dark) | `#475569` | `#475569` |
| Inactive (light) | `#94a3b8` | `#94a3b8` |

### Tab Labels
- **Home:** Dashboard with user's calorie ring and daily stats
- **Scan:** Food scanning results and identification
- **Log:** Food log history and management
- **Settings:** App configuration and user profile

### Tab Navigation Behavior
- **Direction:** Slides in from left if target tab index is lower; from right if higher
- **Animation Duration:** 220ms with OutCubic easing
- **No Action:** Tapping current tab does nothing (idempotent)

---

## Interaction & Motion

### Transitions
- **Page Transitions:** 220ms OutCubic easing (slide left/right)
- **Button Press:** Scale down to 88% (120ms) then back to 100% on interaction
- **Color Changes:** 260ms InOutQuad easing (used in progress ring color shifts)

### Touch Targets
- **Minimum Size:** 44×44px (buttons, tabs)
- **Padding:** Adequate spacing to prevent accidental taps

### Button States
- **Default:** `bgSurface` background with icon/text in accent color
- **Pressed:** Scale 0.88 with animation
- **Disabled:** Reduced opacity

---

## Special Indicators

### Test Mode Banner
- **Display:** Yellow/orange banner at top when `appState.testMode === true`
- **Background:** `#92400e` (dark orange)
- **Text:** "⚠  TEST MODE ACTIVE" in `#fbbf24` (yellow)
- **Height:** 32px
- **Z-Index:** 100 (above all other content)
- **Text Styling:** Bold, 11px, 1px letter spacing

### Connection State
- **No User Connected:** Shows "Open AntiDonut on your phone" button
- **BLE Not Available:** Shows "Simulate phone" button (dev shortcut)
- **Connected:** Button hidden, normal dashboard display

### Refresh Button
- **Location:** Top right of Dashboard header
- **Icon:** `↻` (circular arrow)
- **Behavior:** Scale animation on tap, triggers data refresh
- **Reset:** Scale returns to 1.0 after 120ms

---

## Grid & Alignment

### Two-Column Layout
```
[Col 1 - 50%] [Spacing] [Col 2 - 50%]
  (width: 8px gap)
```
Used for stat cards side-by-side (e.g., Consumed vs. Goal).

### Three-Column Layout
```
[Col 1] [Divider] [Col 2] [Divider] [Col 3]
(width: 1px dividers)
```
Used for activity strip (Steps, Active minutes, Burned).

### Full-Width Single Column
- Default for main content areas
- Allows content to wrap naturally in scrollable containers

---

## Dark Mode Implementation

The app switches between themes globally via `appState.theme === "dark"`.

All color properties automatically adjust:
```qml
property bool isDark: appState.theme === "dark"
color: isDark ? "#1e293b" : "#ffffff"
```

**Guidelines for New Components:**
- Always use `isDark` property binding
- Define dark/light color pairs upfront
- Use `Theme.qml` Singleton for consistency
- Never hardcode colors—always bind to theme

---

## Mobile Responsiveness

The layout is optimized for a 720×1280px portrait device (mobile-like). Content is vertically scrollable via `Flickable` containers.

**Responsive Rules:**
1. Maintain minimum 16px margins on all sides
2. Full-width elements fill available width minus margins
3. Two-column layouts collapse to single column if width < 400px (not currently needed)
4. Use `Layout.fillWidth: true` for flexible components

---

## Accessibility

### Color Contrast
- **Text on Dark:** White text (ffffff) on dark bg meets WCAG AA
- **Text on Light:** Dark text (0f172a) on light bg meets WCAG AA
- **Semantic Colors:** Avoid color-only differentiation (pair with icons/text like HealthBadge)

### Touch Targets
- **Minimum Size:** 44×44px for interactive elements
- **Spacing:** 8px minimum between touch targets to prevent misclicks

### Text Sizing
- **Minimum Body Text:** 14px (fontSizeBody)
- **Labels:** 11px minimum (fontSizeSmall)

---

## Design Tokens Reference

```qml
// Colors
bgPrimary: isDark ? "#0f172a" : "#f8fafc"
bgSurface: isDark ? "#1e293b" : "#ffffff"
bgBorder: isDark ? "#334155" : "#e2e8f0"
textPrimary: isDark ? "#ffffff" : "#0f172a"
textMuted: isDark ? "#64748b" : "#94a3b8"

// Accents
accentStart: "#6366f1"  // indigo
accentEnd: "#06b6d4"    // cyan

// Semantic
healthy: isDark ? "#34d399" : "#065f46"
healthyBg: isDark ? "#064e3b" : "#d1fae5"
unhealthy: isDark ? "#f87171" : "#991b1b"
unhealthyBg: isDark ? "#450a0a" : "#fee2e2"
caloriesBurned: "#f59e0b"

// Typography
fontFamily: "Inter, system-ui, sans-serif"
fontSizeSmall: 11
fontSizeBody: 14
fontSizeTitle: 20
fontSizeHero: 28

// Spacing & Sizing
radiusCard: 10
radiusButton: 8
paddingPage: 16
```

---

## Common Patterns

### Stat Display Card
Combines a title with a colored numeric value.
```qml
Column {
    spacing: 4
    Text { text: value; bold: true; pixelSize: 16; color: valueColor }
    Text { text: label; pixelSize: 10; color: muted }
}
```

### Section Header
Uses small text with increased letter spacing for visual hierarchy.
```qml
Text {
    text: "SECTION TITLE"
    pixelSize: 9
    letterSpacing: 1
    color: muted
}
```

### Info Row
Combines left-aligned content with right-aligned secondary content.
```qml
RowLayout {
    Column { Layout.fillWidth: true; /* content */ }
    Item { Layout.fillWidth: true }  // spacer
    SecondaryControl { /* right-aligned */ }
}
```

---

## Future Considerations

- **Tablet Layouts:** Consider side-by-side pane layouts for wider displays
- **Animation Performance:** Monitor animation performance on lower-end devices
- **Accessibility:** Expand semantic color pairs for additional states (warning, info, etc.)
- **Component Library:** Consider extracting more reusable components as the app grows
