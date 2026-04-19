# FiboHealth Android App — UI Guidelines

## Overview

The FiboHealth Android app is built with **Jetpack Compose** and **Material Design 3**. It follows a modern, adaptive navigation pattern that adjusts to screen size (bottom bar on phones, navigation rail on tablets). The design system emphasizes health metrics visualization, intuitive data entry, and consistent theming across light and dark modes.

**Framework:** Jetpack Compose
**Design System:** Material Design 3 (adaptive)
**Theme:** Responsive light/dark mode based on system settings
**Navigation:** NavigationSuiteScaffold (adaptive navigation)

---

## Color System

### Core Colors

| Name | Hex Value | Usage |
|------|-----------|-------|
| Indigo | `#6366F1` | Primary action, brand color, emphasis |
| Cyan | `#06B6D4` | Secondary action, accents |
| Dark Bg | `#0F172A` | Dark mode background |
| Dark Surface | `#1E293B` | Dark mode surface/card background |
| Dark Border | `#334155` | Dark mode dividers and borders |
| Light Bg | `#F8FAFC` | Light mode background |
| Light Surface | `#FFFFFF` | Light mode surface/card background |
| Light Border | `#E2E8F0` | Light mode dividers and borders |

### Material 3 Color Scheme

The app uses Material Design 3's `ColorScheme` for automatic theme adaptation:

**Dark Mode:**
- `primary` → Indigo
- `secondary` → Cyan
- `background` → Dark Bg
- `surface` → Dark Surface
- `onBackground` → White
- `onSurface` → White
- `onSurfaceVariant` → `#94A3B8` (muted)
- `outline` → Dark Border

**Light Mode:**
- `primary` → Indigo
- `secondary` → Cyan
- `background` → Light Bg
- `surface` → White
- `onBackground` → `#0F172A` (dark)
- `onSurface` → `#0F172A` (dark)
- `onSurfaceVariant` → `#64748B` (muted)
- `outline` → Light Border

### Status Colors (Theme-Independent)

| Name | Hex Value | Usage |
|------|-----------|-------|
| StatusGreen | `#22C55E` | Healthy items, success states |
| StatusAmber | `#F59E0B` | Warning, approaching limit |
| StatusRed | `#EF4444` | Exceeded goal, error states |

### Progress Ring Color States

The `CalorieRing` component uses progress-based coloring:

| Progress | Color | Meaning |
|----------|-------|---------|
| < 50% | `#22C55E` | Green – plenty of room |
| 50–75% | Indigo | On track |
| 75–90% | `#F59E0B` | Amber – approaching goal |
| ≥ 90% | `#EF4444` | Red – at or over goal |

### Theme Usage in Compose

Always use `MaterialTheme.colorScheme` for text and backgrounds to ensure proper light/dark mode support:

```kotlin
Text("Label", color = MaterialTheme.colorScheme.onSurfaceVariant)
Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface))
```

---

## Typography

### Font Styles

The app defines four key text styles via `FiboTypography`:

| Style | Weight | Size | Usage |
|-------|--------|------|-------|
| `headlineMedium` | Bold (700) | 22sp | Screen titles, major headings |
| `titleLarge` | SemiBold (600) | 18sp | Section headers, card titles |
| `bodyMedium` | Regular (400) | 14sp | Body text, descriptions |
| `labelSmall` | Regular (400) | 11sp | Labels, metadata, hints, BLE status pill |

### Typography Hierarchy

**Page Headings**
```kotlin
Text("Screen Title", style = MaterialTheme.typography.headlineMedium)
```

**Section Headings**
```kotlin
Text("Recent Scans", style = MaterialTheme.typography.titleLarge)
```

**Body Text**
```kotlin
Text("Supporting detail", style = MaterialTheme.typography.bodyMedium)
```

**Small Labels**
```kotlin
Text("Time ago", style = MaterialTheme.typography.labelSmall,
     color = MaterialTheme.colorScheme.onSurfaceVariant)
```

### Font Family

The app uses system-provided sans-serif fonts via Material Design 3 defaults. Avoid specifying custom fonts; rely on `MaterialTheme.typography` for consistency.

---

## Navigation

### Navigation Structure

The app uses `NavigationSuiteScaffold` for adaptive navigation that responds to screen width:

**Destinations:**

| Route | Label | Icon | Screen |
|-------|-------|------|--------|
| `home` | Home | Home | Dashboard |
| `log` | Log | Restaurant Menu | Food Log |
| `activity` | Activity | Bolt | Activity metrics |
| `profile` | Profile | Person | User profile |
| `device` | Device | Bluetooth | Device settings |

### Navigation Behavior

- **Phone:** Bottom navigation bar (MaterialNavigationBar)
- **Tablet:** Navigation rail on left side
- **State Preservation:** `saveState = true` and `restoreState = true` preserve scroll position and form state when switching tabs
- **Single Top:** `launchSingleTop = true` prevents duplicate stack entries
- **Animations:** Compose handles transitions automatically

### Active State Styling

The navigation automatically highlights the current route. Current route is determined by:
```kotlin
val currentRoute = backStack?.destination?.route
```

---

## Components

### CalorieRing

A circular progress indicator showing daily calorie consumption with dynamic color feedback.

**Properties:**
```kotlin
CalorieRing(
    remaining: Int = 500,    // Calories remaining to goal
    goal: Int = 2000,        // Daily calorie goal
    size: Dp = 180.dp,       // Diameter
    strokeWidth: Dp = 16.dp  // Ring thickness
)
```

**Features:**
- Progress animates over 800ms using tween easing
- Color shifts automatically based on consumption ratio
- Center text shows "kcal left"
- Remaining calories clamped to 0 minimum
- Gray background track always visible

### StatCard

A compact card displaying a metric with label and colored value.

**Properties:**
```kotlin
StatCard(
    value: String = "2000",          // Numeric value
    label: String = "Goal kcal",     // Label text
    valueColor: Color = Indigo,      // Value text color
    modifier: Modifier = Modifier
)
```

**Features:**
- Uses Material Design 3 `Card` component
- 12dp padding, 4dp surrounding padding
- Value in 18sp bold
- Label in `labelSmall` style with muted color
- Centered content

### HealthBadge

A small indicator showing food item health status (healthy/unhealthy).

**Usage:**
```kotlin
HealthBadge(isHealthy = entry.isHealthy)
```

**Features:**
- Color-coded: green for healthy, red for unhealthy
- Typically used in list items as trailing content

### ListItem (Material3)

The app uses Material3's standard `ListItem` for food log entries and similar data.

**Structure:**
```kotlin
ListItem(
    headlineContent = { Text("Food name") },
    supportingContent = { Text("Details") },
    trailingContent = { HealthBadge(...) }
)
HorizontalDivider()
```

**Features:**
- Built-in text styling via Material3
- Automatic color theming
- Standard Material spacing and alignment

---

## Screens & Patterns

### Dashboard Screen

**Layout:**
1. Date and greeting header
2. BLE connection status pill
3. Calorie progress ring (centered)
4. Three-column stat card row (Goal, Eaten, Left)
5. Recent scans list (if data exists)

**Spacing:**
- Column spacing: 16dp
- Content padding: 16dp
- Stats row: 8dp horizontal spacing

**Key Components:**
```kotlin
LazyColumn(
    Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    // Date + greeting
    item { /* header */ }
    
    // BLE status
    item { SuggestionChip(...) }
    
    // Calorie ring
    item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CalorieRing(...)
    }}
    
    // Stats row
    item { Row(...) { StatCard(...) } }
    
    // Recent scans
    if (recentScans.isNotEmpty()) {
        item { Text("Recent Scans", style = headlineMedium) }
        items(recentScans) { /* list items */ }
    }
}
```

### Connection Status Indicator

A `SuggestionChip` showing BLE connection state:

**Connected:** 
- Background: Green with 15% opacity
- Text: "Pi Connected"

**Disconnected:**
- Background: `surface` color
- Text: "Pi Disconnected"

```kotlin
SuggestionChip(
    onClick = {},
    label = { Text(if (isConnected) "Pi Connected" else "Pi Disconnected") },
    colors = SuggestionChipDefaults.suggestionChipColors(
        containerColor = if (isConnected) StatusGreen.copy(0.15f)
                         else MaterialTheme.colorScheme.surface
    )
)
```

### Profile Screen

**Layout:**
1. "Profile" heading
2. Text input fields (Name, Age, Weight, Height)
3. Sex dropdown
4. Activity level dropdown
5. Calculated daily goal display
6. Health Connect logging toggle
7. "Save & Sync to Pi" button
8. "Remove entries" button (danger action)

**Form Spacing:**
- Column spacing: 12dp
- Content padding: 16dp
- Full-width inputs

**Input Fields:**
```kotlin
OutlinedTextField(
    value = draft.name,
    onValueChange = { draft = draft.copy(name = it) },
    label = { Text("Name") },
    modifier = Modifier.fillMaxWidth()
)
```

### Food Log Screen

Uses a two-pane adaptive layout:
- **Phone:** Single pane (stacked)
- **Tablet:** Side-by-side (list + detail)

### Activity Screen

Displays health metrics including steps, active minutes, and calories burned.

---

## Spacing & Padding

### Standard Sizes

| Measurement | Usage |
|-------------|-------|
| 16dp | Page padding, major spacing |
| 12dp | Card padding, form spacing |
| 8dp | Stat card row spacing, minor gaps |
| 4dp | Card elevation, button padding within |

### LazyColumn/Column Spacing

```kotlin
Column(
    Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
)
```

### Row Spacing

```kotlin
Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
)
```

---

## Forms & Input

### Input Field Styling

All text input uses `OutlinedTextField`:

```kotlin
OutlinedTextField(
    value = state,
    onValueChange = { state = it },
    label = { Text("Field name") },
    modifier = Modifier.fillMaxWidth()
)
```

**Features:**
- Automatic Material3 theming
- Full width in columns
- Built-in focus and error states
- Material3 colors for borders and text

### Dropdowns

Multi-select dropdowns use `ExposedDropdownMenuBox`:

```kotlin
ExposedDropdownMenuBox(expanded, onExpandedChange) {
    OutlinedTextField(
        value = selected,
        readOnly = true,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        modifier = Modifier.menuAnchor().fillMaxWidth()
    )
    ExposedDropdownMenu(expanded, onDismissRequest) {
        items.forEach { item ->
            DropdownMenuItem(
                text = { Text(item) },
                onClick = { selected = item }
            )
        }
    }
}
```

### Toggles & Switches

Use Material3 `Switch` for boolean options:

```kotlin
ListItem(
    headlineContent = { Text("Log Food to Health Connect") },
    trailingContent = { Switch(enabled, { setEnabled(it) }) }
)
```

---

## Buttons

### Primary Actions

```kotlin
Button(
    onClick = { /* action */ },
    modifier = Modifier.fillMaxWidth()
) {
    Text("Save & Sync to Pi")
}
```

**Styling:**
- Indigo background (primary color)
- White text
- Full width in forms

### Secondary / Danger Actions

```kotlin
TextButton(
    onClick = { /* action */ },
    modifier = Modifier.fillMaxWidth()
) {
    Text("Remove FiboHealth Food Entries", color = StatusRed)
}
```

**Styling:**
- No background fill
- Red text for destructive actions
- Full width in forms

---

## Cards & Surfaces

### Standard Card

Material3 `Card` is used for all surface elements:

```kotlin
Card {
    Column(Modifier.padding(12.dp)) {
        // content
    }
}
```

**Features:**
- Automatic Material3 color (surface)
- Built-in elevation (1.dp)
- Responsive to dark mode

### Dividers

Use `HorizontalDivider` between list items:

```kotlin
HorizontalDivider()
```

**Features:**
- Auto-colored (outline)
- Standard Material3 height (1.dp)
- Responsive to theme

---

## Responsive Layout

The app uses Material Design 3's adaptive navigation:

- **Phone (< 600dp width):** Bottom NavigationBar
- **Tablet (600–840dp):** Navigation rail (left side)
- **Large (> 840dp):** Full navigation rail with labels

The `NavigationSuiteScaffold` handles this automatically. Screens should use `Modifier.fillMaxSize()` and let the scaffold handle layout.

### Two-Pane Adaptive Layout

The Food Log uses a two-pane pattern:

**Phone:**
```kotlin
Column {
    FoodLogListPane()
    FoodLogDetailPane() // Only shown when item selected
}
```

**Tablet:**
```kotlin
Row {
    FoodLogListPane(Modifier.weight(1f))
    FoodLogDetailPane(Modifier.weight(1f))
}
```

---

## Animations & Transitions

### Progress Ring Animation

```kotlin
val animated by animateFloatAsState(
    fraction,
    animationSpec = tween(800),
    label = "ring"
)
```

**Features:**
- 800ms duration
- Smooth interpolation
- Label for debugging

### Navigation Transitions

Compose automatically handles screen transitions:
- Cross-fade between destinations
- Smooth state preservation (scroll position, form state)

---

## Accessibility

### Text Colors

- **Dark mode text:** White (`#FFFFFF`) on dark surface
- **Light mode text:** Dark (`#0F172A`) on light surface
- **Muted text:** `onSurfaceVariant` color

All text automatically meets WCAG AA contrast requirements.

### Touch Targets

- **Minimum size:** 48dp × 48dp (Material Design standard)
- **Buttons:** 44dp minimum height
- **List items:** 48dp minimum height

### Semantic Structure

- Use proper heading styles (headlineMedium, titleLarge, etc.)
- Use ListItem for structured lists
- Buttons have descriptive labels
- Form fields have labels

---

## Material Design 3 Integration

The app leverages Material Design 3 components:

- **Navigation:** `NavigationSuiteScaffold`, `NavigationBar`, `NavigationRail`
- **Forms:** `OutlinedTextField`, `ExposedDropdownMenuBox`, `Switch`
- **Lists:** `ListItem`, `HorizontalDivider`
- **Cards:** `Card`, `SuggestionChip`
- **Buttons:** `Button`, `TextButton`, `FilledTonalButton`
- **Dialogs:** Material3 AlertDialog (for confirmations)

Never override Material Design 3 default behavior. Rely on the provided components for theming consistency.

---

## State Management & Reactive Updates

The app uses Flow and StateFlow for reactive UI updates:

```kotlin
@Composable
fun DashboardScreen(vm: DashboardViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    // Use state reactively
}
```

**Key Pattern:**
- ViewModel exposes Flow properties
- Composables use `collectAsStateWithLifecycle()` for lifecycle-aware collection
- State changes automatically recompose affected composables

---

## Design Tokens Summary

```kotlin
// Kotlin Color definitions
val Indigo = Color(0xFF6366F1)
val Cyan = Color(0xFF06B6D4)
val StatusGreen = Color(0xFF22C55E)
val StatusAmber = Color(0xFFF59E0B)
val StatusRed = Color(0xFFEF4444)

// Material3 ColorScheme
darkColorScheme(
    primary = Indigo,
    secondary = Cyan,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

lightColorScheme(
    primary = Indigo,
    secondary = Cyan,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0)
)

// Typography
Typography(
    headlineMedium = TextStyle(fontWeight = Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = SemiBold, fontSize = 18.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 0.5.sp)
)
```

---

## Best Practices

1. **Always use Material3 components** — They handle theming automatically
2. **Use ColorScheme properties** — Don't hardcode colors except status colors
3. **Follow typography hierarchy** — Use defined styles, don't create custom sizes
4. **Preserve state** — Use `saveState` and `restoreState` in navigation
5. **Test both themes** — Dark and light mode must work correctly
6. **Respect adaptive layouts** — NavigationSuiteScaffold handles it automatically
7. **Use ListItem for structured data** — It's optimized for accessibility

---

## Future Enhancements

- **Compose animations library:** Consider advanced animations for data transitions
- **Custom theme selector:** Allow users to choose accent colors
- **Offline support:** Skeleton screens and loading states
- **Tablet landscape:** Optimize layouts for landscape orientation
- **Accessibility:** Expand label annotations for screen readers
