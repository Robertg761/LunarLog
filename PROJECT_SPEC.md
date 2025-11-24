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
- [ ] **DailyLog Entity Update**: Ensure fields for:
    - Flow (Int: 0-4)
    - Symptoms (List<String> or Bitmask)
    - Mood (List<String>)
    - Notes (String)
- [ ] **Symptom Logging UI**:
    - Create a "Daily Details" screen/bottom sheet.
    - **Flow Intensity**: Custom Slider or Segmented Button.
    - **Symptoms/Mood**: Multi-select `FilterChip`s.
- [ ] **Repository Layer**: Methods to insert/update `DailyLog` entries.
- [ ] **Integration**: Link Home Screen "Log Today" button to this new UI.

### 📅 Phase 3: The Calendar & Visualization
*Goal: Visual feedback for patterns at a glance.*
- [ ] **Custom Calendar Composable**:
    - Build a monthly grid view.
    - Support swiping between months.
- [ ] **Visual Indicators**:
    - 🔴 Red filled circle: Confirmed period.
    - 🔴 Red outline circle: Predicted period.
    - 🟢 Green dot: Fertile window.
    - 🔵 Blue ring: Ovulation day.
- [ ] **Interactivity**: Clicking a date opens the Daily Details/Log for that specific date.

### 🧠 Phase 4: The Intelligence (Prediction Engine)
*Goal: Smarter predictions that adapt to the user.*
- [ ] **Advanced Logic**:
    - Calculate Standard Deviation of cycle lengths.
    - Detect "Irregular Cycle" (high variance).
- [ ] **Fertility Calculator**:
    - Implement standard ovulation math (typically 14 days before next period).
    - Define fertile window (Ovulation - 5 days).
- [ ] **Local Notifications** (using `WorkManager`):
    - "Period due in 2 days".
    - "Fertile window starting".
    - *Privacy Mode*: Generic messages ("Check LunarLog").

### 📈 Phase 5: "Premium" Features (Forever Free)
*Goal: High-value features usually behind paywalls.*
- [ ] **Wellness Tracker**:
    - **Water**: Counter widget (Cups).
    - **Sleep**: Hours slider + Quality rating.
    - **Libido**: Activity tracker.
- [ ] **Doctor's Report**:
    - Generate a PDF summary of the last 3-6 months.
    - Include cycle lengths, symptom frequency, and regularity.
    - Export Raw Data (CSV) for backup/interoperability.
- [ ] **Trends & Charts**:
    - Cycle Length History (Line Chart).
    - Symptom Frequency (Bar Chart).
    - Mood Correlation (e.g., "You often feel Anxious on Day 25").

### 🔒 Phase 6: Privacy & Polish
*Goal: Security, reliability, and aesthetics.*
- [ ] **Security**:
    - App Lock (Biometric/PIN) on startup.
- [ ] **Data Management**:
    - Local Backup/Restore (Export DB to file).
    - "Nuke Data" button.
- [ ] **Onboarding**:
    - Welcome screen for first run.
    - Ask for "Last Period Date" to jumpstart predictions.
- [ ] **Theming**:
    - Dark Mode support.
    - Custom accent colors (allow user to choose theme color).

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