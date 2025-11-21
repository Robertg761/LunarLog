# Project Name: LunarLog (Open Source Period Tracker)

## Project Goal
To build a complete, privacy-focused, offline-first menstrual cycle tracker for Android that includes premium features without paywalls. The app must be fast, modern, and strictly local (no cloud data).

## Technical Stack
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3 Design)
*   **Database:** Room Database (SQLite) for local storage
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Navigation:** Jetpack Compose Navigation

## Core Features Breakdown

### 1. Database Schema (Room)
*   **Cycle Entity:** Stores Start Date, End Date.
*   **DailyLog Entity:** Linked to a date. Stores:
    *   **Flow Level** (None, Spotting, Light, Medium, Heavy)
    *   **Mood** (Happy, Sensitive, Sad, Angry, Anxious)
    *   **Symptoms** (Cramps, Headache, Bloating, Acne, Backache)
    *   **Wellness** (Water intake count, Sleep hours, Sex drive)

### 2. The Logic (CyclePredictionUtils)
*   Calculate average cycle length based on last 6 months of data.
*   Predict next period start date.
*   Predict fertile window (typically 12-16 days before next period).
*   Handle "Irregular Cycle" flagging if variance is high.

### 3. User Interface Screens
*   **Home/Dashboard:**
    *   Circular progress view: "Day X of Cycle".
    *   Text: "X days until next period".
    *   Quick access to "Log Today".
*   **Calendar View:**
    *   Full month view.
    *   Color coded dots: Red (Period), Green (Fertile), Blue (Ovulation).
*   **Logging Screen (BottomSheet):**
    *   Easy "Chip" selection for Symptoms and Moods.
    *   Slider for Flow intensity.
*   **History/Stats:**
    *   List of previous cycles.
    *   Average cycle length calculation.
*   **Settings:**
    *   Toggle "Predict Fertile Window" (On/Off).
    *   Data Management: Export to CSV (for doctor).

## Design Guidelines
*   **Theme:** Clean, modern, soft colors (Customizable, not aggressively pink).
*   **Privacy:** No login screens. No internet permissions required.
*   **Notifications:** Local notification 2 days before predicted period.

## Phase 1 Implementation Plan
*   Set up Android project structure with Hilt, Room, and Compose.
*   Create the Cycle and DailyLog database entities.
*   Build the CycleRepository to handle data access.
*   Create the basic HomeViewModel to calculate predictions.
