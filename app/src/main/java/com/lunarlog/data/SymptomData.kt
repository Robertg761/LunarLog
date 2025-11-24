package com.lunarlog.data

object SymptomData {
    val defaultSymptoms = listOf(
        // Physical
        SymptomDefinition(name = "Cramps", displayName = "Cramps", category = SymptomCategory.PHYSICAL),
        SymptomDefinition(name = "Headache", displayName = "Headache", category = SymptomCategory.PHYSICAL),
        SymptomDefinition(name = "Bloating", displayName = "Bloating", category = SymptomCategory.PHYSICAL),
        SymptomDefinition(name = "Backache", displayName = "Backache", category = SymptomCategory.PHYSICAL),
        SymptomDefinition(name = "Breast Tenderness", displayName = "Breast Tenderness", category = SymptomCategory.PHYSICAL),
        SymptomDefinition(name = "Acne", displayName = "Acne", category = SymptomCategory.PHYSICAL),
        SymptomDefinition(name = "Fatigue", displayName = "Fatigue", category = SymptomCategory.PHYSICAL),
        SymptomDefinition(name = "Nausea", displayName = "Nausea", category = SymptomCategory.PHYSICAL),
        SymptomDefinition(name = "Insomnia", displayName = "Insomnia", category = SymptomCategory.PHYSICAL),
        SymptomDefinition(name = "Cravings", displayName = "Cravings", category = SymptomCategory.PHYSICAL),

        // Emotional (Moods - mapping current 'mood' logs to this if needed, or keeping distinct)
        // Since DailyLog separates Mood and Symptoms, we might treat these as 'Symptoms' for now 
        // OR we include Moods here if we want a unified definition table.
        // The spec says "Symptom Definition Entity: ... category".
        // Let's include Moods but user UI might filter them.
        SymptomDefinition(name = "Happy", displayName = "Happy", category = SymptomCategory.EMOTIONAL),
        SymptomDefinition(name = "Sad", displayName = "Sad", category = SymptomCategory.EMOTIONAL),
        SymptomDefinition(name = "Anxious", displayName = "Anxious", category = SymptomCategory.EMOTIONAL),
        SymptomDefinition(name = "Irritable", displayName = "Irritable", category = SymptomCategory.EMOTIONAL),
        SymptomDefinition(name = "Energetic", displayName = "Energetic", category = SymptomCategory.EMOTIONAL),
        SymptomDefinition(name = "Calm", displayName = "Calm", category = SymptomCategory.EMOTIONAL),
        SymptomDefinition(name = "Mood Swings", displayName = "Mood Swings", category = SymptomCategory.EMOTIONAL),
        SymptomDefinition(name = "Sensitive", displayName = "Sensitive", category = SymptomCategory.EMOTIONAL),
        SymptomDefinition(name = "Angry", displayName = "Angry", category = SymptomCategory.EMOTIONAL),

        // Discharge
        SymptomDefinition(name = "Spotting", displayName = "Spotting", category = SymptomCategory.DISCHARGE),
        SymptomDefinition(name = "Heavy", displayName = "Heavy Flow", category = SymptomCategory.DISCHARGE), // Note: Flow is int in DailyLog, but maybe user wants to tag it?
        // Actually, Flow is separate. Cervical Mucus is separate (int).
        // Discharge category might be for textual descriptions like "Clotty", "Brown", etc.
        SymptomDefinition(name = "Clots", displayName = "Clots", category = SymptomCategory.DISCHARGE),
        SymptomDefinition(name = "Watery", displayName = "Watery", category = SymptomCategory.DISCHARGE),
        SymptomDefinition(name = "Sticky", displayName = "Sticky", category = SymptomCategory.DISCHARGE)
    )
}
