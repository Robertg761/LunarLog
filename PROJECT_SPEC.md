# LunarLog: Project Specification & Roadmap

**Mission:** To build a privacy-first, offline, and feature-complete menstrual cycle tracker for Android. LunarLog aims to provide "premium" insights and features (PDF reports, advanced analytics, wellness tracking) completely free and without data collection.

## 🛠 Technical Stack
*   **Language:** Kotlin
*   **UI:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM (Clean Architecture)
*   **DI:** Hilt
*   **Database:** Room (SQLite)
*   **Asynchrony:** Coroutines & Flow
*   **Navigation:** Jetpack Navigation Compose

---

## 📅 Development Phases

### 🏁 Phase 1: The Skeleton (Current Status)
*Goal: A running app with basic database structure and navigation.*
- [x] **Project Setup**: Gradle, Version Control, Hilt, Room, Compose dependencies.
- [x] **Database Layer (Cycle)**: `Cycle` entity created.
- [x] **Logic Layer**: Basic average cycle calculation and prediction.
- [x] **UI Skeleton**: Home Screen with "Day X" indicator and "Log Period" screen.
- [x] **Navigation**: Basic graph set up.
- [x] **Database Layer (DailyLog)**: **(DONE)** Create `DailyLog` entity for tracking daily symptoms/mood.
- [x] **Database Migration**: Add `DailyLog` to `AppDatabase`.

### 📝 Phase 2: Core Data Entry
*Goal: Enable detailed health tracking beyond just dates.*
- [x] **DailyLog Entity Update**: Ensure fields for:
    - Flow (Int: 0-4)
    - Symptoms (List<String> or Bitmask)
    - Mood (List<String>)
    - Notes (String)
- [x] **Symptom Logging UI**:
    - Create a "Daily Details" screen/bottom sheet.
    - **Flow Intensity**: Custom Slider or Segmented Button.
    - **Symptoms/Mood**: Multi-select `FilterChip`s.
- [x] **Repository Layer**: Methods to insert/update `DailyLog` entries.
- [x] **Integration**: Link Home Screen "Log Today" button to this new UI.

### 📅 Phase 3: The Calendar & Visualization
*Goal: Visual feedback for patterns at a glance.*
- [x] **Custom Calendar Composable**:
    - Build a monthly grid view.
    - Support swiping between months. (Implemented via navigation buttons for now)
- [x] **Visual Indicators**:
    - 🔴 Red filled circle: Confirmed period.
    - 🔴 Red outline circle: Predicted period.
    - 🟢 Green dot: Fertile window.
    - 🔵 Blue ring: Ovulation day.
- [x] **Interactivity**: Clicking a date opens the Daily Details/Log for that specific date.

### 🧠 Phase 4: The Intelligence (Prediction Engine)
*Goal: Smarter predictions that adapt to the user.*
- [x] **Advanced Logic**:
    - Calculate Standard Deviation of cycle lengths.
    - Detect "Irregular Cycle" (high variance).
- [x] **Fertility Calculator**:
    - Implement standard ovulation math (typically 14 days before next period).
    - Define fertile window (Ovulation - 5 days).
- [x] **Local Notifications** (using `WorkManager`):
    - "Period due in 2 days".
    - "Fertile window starting".
    - *Privacy Mode*: Generic messages ("Check LunarLog").

### 📈 Phase 5: "Premium" Features (Forever Free)
*Goal: High-value features usually behind paywalls.*
- [x] **Wellness Tracker**:
    - **Water**: Counter widget (Cups).
    - **Sleep**: Hours slider + Quality rating.
    - **Libido**: Activity tracker.
- [x] **Doctor's Report**:
    - Generate a PDF summary of the last 3-6 months.
    - Include cycle lengths, symptom frequency, and regularity.
    - Export Raw Data (CSV) for backup/interoperability.
- [x] **Trends & Charts**:
    - Cycle Length History (Line Chart).
    - Symptom Frequency (Bar Chart).
    - Mood Correlation (e.g., "You often feel Anxious on Day 25").

### 🔒 Phase 6: Privacy & Polish
*Goal: Security, reliability, and aesthetics.*
- [x] **Security**:
    - App Lock (Biometric/PIN) on startup.
- [x] **Data Management**:
    - Local Backup/Restore (Export DB to file).
    - "Nuke Data" button.
- [x] **Onboarding**:
    - Welcome screen for first run.
    - Ask for "Last Period Date" to jumpstart predictions.
- [x] **Theming**:
    - Dark Mode support.
    - Custom accent colors (allow user to choose theme color).

### 🚀 Phase 7: Advanced Intelligence & UX (Prioritized Roadmap)

#### 1. Architecture: Foundation
*Establish a clean build system before adding complex dependencies.*
- [x] **Version Catalog Migration**: Migrate dependencies to `gradle/libs.versions.toml`.
- [ ] **Strict Mode**: Enable Compose compiler metrics for performance benchmarking.

#### 2. Feature: Advanced Intelligence (Logic)
*Build the "brain" so the UI has something to show.*
- [x] **Symptom Correlation Engine**: Analyze `DailyLog` for patterns (e.g., "Headaches often occur on Day 26").
- [x] **Dynamic Ovulation Prediction**: Integrate BBT and Cervical Mucus data for adaptive predictions.
- [x] **Smart Anomalies**: Detect persistent shifts in cycle trends.
- [x] **Medication Tracking**: Dedicated entity for recurring meds with reminders.

#### 3. UI/UX: Data Visualization
*Visualize the intelligent data generated in the previous step.*
- [x] **Calendar Heatmap**: Visual intensity indicators (opacity/color) on the calendar grid.
- [x] **Interactive Charts**: Animated, zoomable charts using Vico for cycle history and symptoms.

#### 4. UI/UX: Delight & Polish
*Enhance the user experience.*
- [x] **Dynamic Theming Engine**: User-selectable "Seed Color" to regenerate the entire app theme at runtime.
- [x] **Home Screen Widget**: Glance widget for quick status checks.
- [x] **Animated Transitions**: Shared Element Transitions between screens.
- [x] **Partner Sync (Privacy-First)**: Static/encrypted snapshots (QR code/Link).

#### 5. Security & Privacy
*Harden the application.*
- [ ] **Database Encryption**: Migrate to SQLCipher.
- [ ] **Encrypted Backups**: AES-256 encryption for exports.
- [ ] **Incognito Mode**: Disguise app icon and hide data.

#### 6. Architecture: Scalability & Maintenance
*Refactor for long-term growth.*
- [ ] **Extract `core-logic` & `core-data`**: Modularize the codebase.
- [ ] **Testing Suite**: Screenshot testing, fake repositories, and automated checks.

### 💎 Phase 8: UI/UX Master Polish
*Goal: Elevate the app from functional to premium with detailed interactions and visual consistency.*

#### 1. Core Visual Identity & Theming (Material 3 Polish)
*This foundation ensures the app looks cohesive and modern before adding complex animations.*
- [x] **Dynamic Color & Tonal Palettes**: Ensure `ui/theme` leverages Material You (Dynamic Color) with a fallback "brand" color.
- [x] **Typography Scale Refinement**: Strictly use Material 3 tokens (`Display`, `Headline`, `Body`, `Label`).
- [x] **Consistent Iconography**: Standardize `Icons.Outlined` vs `Icons.Filled` usage.
- [x] **Component Styling**: Apply consistent elevation and corner radius across all screens.
- [x] **Dark Mode Optimization**: Test and adjust period indicators for dark backgrounds.

#### 2. Interactive Data Visualization (Smart Insights)
*Make charts and calendars interactive and informative.*
- [x] **Interactive Chart Tooltips**: Show precise data points on drag in `ui/analysis`.
- [x] **Calendar Heatmap Micro-interactions**: Long-press dates for summary tooltips.
- [x] **Animated Progress Indicators**: Animate wellness trackers on load/update.
- [x] **Smart Empty States**: Illustrative states explaining missing data and how to generate it.
- [x] **Haptic Feedback**: Subtle vibration for scrolling, snapping, and toggling.

#### 3. Navigation & Transition Flows (Fluidity)
*Smooth out screen transitions for a responsive feel.*
- [x] **Shared Element Transitions**: Morph "Day" circle to "Date Header".
- [x] **Predictive Back Gesture**: Support Android's predictive back gesture.
- [x] **Skeleton Loading States**: Use "Shimmer" skeletons instead of spinners.
- [x] **Bottom Navigation Animation**: Scale/slide animation on tab switch.
- [x] **Contextual Top App Bars**: Fade/slide title based on scroll position.

#### 4. Micro-Interactions & Delight (Engagement)
*Small details that reward consistency.*
- [x] **Success Animations**: Lottie/Vector animations for logging.
- [x] **Goal Completion Confetti**: Visual reward for hitting wellness goals.
- [x] **Ripple & Touch Targets**: Audit ripple effects on custom elements.
- [x] **Input Validation Feedback**: "Shake" animations for invalid input.
- [x] **Scroll-Triggered Parallax**: Parallax effect on Home screen headers.

#### 5. Accessibility & System Integration (Inclusivity)
*Deep system integration and usability for all.*
- [x] **Semantic Content Descriptions**: Meaningful descriptions for all images/icons.
- [x] **Minimum Touch Targets**: Ensure 48x48dp minimum for all interactive elements.
- [x] **Dynamic Type Support**: Verify layout integrity with largest system font settings.
- [x] **Widget Interactivity**: One-tap logging from Home Screen Widget.
- [x] **App Shortcuts**: Static shortcuts for common actions (Log, Calendar, Report).

### 🔮 Phase 9: Enhanced Usability & Architecture (Refined)
*Goal: Streamline daily usage, deepen data insight, and solidify the data architecture for scalability.*

#### 1. Architecture: Symptom Management System (Foundation)
*Establish a "Source of Truth" for symptoms to enable categories and custom tags.*
- [x] **Symptom Definition Entity**: Create `SymptomDefinition` (id, displayName, category, isCustom).
- [x] **Database Pre-population**: Seed the database with default symptoms/moods via Room callback.
- [x] **Symptom Repository**: Manage fetching definitions and adding user-generated custom symptoms.
- [x] **Migration**: Ensure existing string-based logs map correctly to new definitions.

#### 2. Feature: Advanced Organization & Input
*Leverage the new architecture for a better UI.*
- [x] **Categorized Input UI**: Group chips in `LogDetailsScreen` by category (Physical, Emotional, Discharge, Custom).
- [x] **Custom Symptoms**: UI dialog allowing users to create new tracking tags (`isCustom = true`).
- [x] **Smart Ordering Logic**: Implement logic to fetch last 90 days of logs, compute frequency, and sort UI chips accordingly.

#### 3. Feature: Log History & Robust Retrieval
*Make finding past data performant and easy.*
- [x] **FTS4 Integration**: Implement Full-Text Search (FTS4) virtual table in Room for high-performance note searching.
- [x] **Log History Screen**: Chronological `LazyColumn` list of all past entries.
- [x] **Advanced Search UI**: Filter logs by Symptom (using definitions) or Text content.

#### 4. UI/UX: Rapid Data Entry
*Reduce friction for daily logging.*
- [x] **"Quick Log" Bottom Sheet**: Replace FAB with a ModalBottomSheet for 1-tap actions (Period Start/End).
- [x] **Contextual Shortcuts**: Show top 3 most used symptoms for the *current cycle phase* in the quick menu.

#### 5. Feature: Narrative Insights
*Make data digestible at a glance.*
- [x] **NarrativeGenerator Engine**: Pure logic class to analyze a Cycle + associated Logs.
- [x] **Cycle Summaries**: Generate text (e.g., "Cycle 4 was 29 days long...") for the Analysis screen.
- [x] **Weekly Digest**: View for the past week's trends.

#### 6. Technical Polish
- [ ] **Container Transforms**: Implement Material 3 Motion `ContainerTransform` for smooth Circle-to-Rect transitions (List to Detail).
- [ ] **Performance**: Optimize haptic feedback calls and reduce composition overhead in grids.

---

## 📂 File Structure & Conventions
*   **`data/`**: Entities, DAOs, Repositories, Database.
*   **`logic/`**: Pure Kotlin business logic (predictions, math).
*   **`ui/`**: Composable screens, ViewModels, Theme.
    *   `ui/home/`
    *   `ui/calendar/`
    *   `ui/log/`
    *   `ui/settings/`
*   **`di/`**: Dependency Injection modules.
