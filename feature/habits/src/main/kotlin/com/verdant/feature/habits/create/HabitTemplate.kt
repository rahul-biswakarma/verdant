package com.verdant.feature.habits.create

import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import com.verdant.core.model.VisualizationType

enum class TemplateCategory(val label: String) {
    HEALTH("Health"),
    FITNESS("Fitness"),
    LEARNING("Learning"),
    PROJECTS("Projects"),
    FINANCE("Finance"),
    LIFESTYLE("Lifestyle"),
}

data class HabitTemplate(
    val name: String,
    val icon: String,
    val color: Long,
    val label: String,
    val trackingType: TrackingType,
    val visualizationType: VisualizationType,
    val unit: String?,
    val targetValue: Double?,
    /** Pre-filled milestone steps for CHECKPOINT habits; empty for other types. */
    val checkpointSteps: List<String>,
    val frequency: HabitFrequency,
    val scheduleDays: Int,
    val suggestedReminderTime: String?,
    val category: TemplateCategory,
)

// scheduleDays bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
private const val ALL_DAYS = 0x7F
private const val WEEKDAYS = 0x1F

val habitTemplates: Map<TemplateCategory, List<HabitTemplate>> = mapOf(
    TemplateCategory.HEALTH to listOf(
        HabitTemplate(
            name = "Daily Meditation", icon = "🧘", color = 0xFF9C27B0L,
            label = "Health", trackingType = TrackingType.BINARY,
            visualizationType = VisualizationType.PIXEL_GRID,
            unit = null, targetValue = null, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "07:00",
            category = TemplateCategory.HEALTH,
        ),
        HabitTemplate(
            name = "Take vitamins", icon = "💊", color = 0xFF5A7A60L,
            label = "Health", trackingType = TrackingType.BINARY,
            visualizationType = VisualizationType.PIXEL_GRID,
            unit = null, targetValue = null, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "08:00",
            category = TemplateCategory.HEALTH,
        ),
        HabitTemplate(
            name = "Drink water", icon = "💧", color = 0xFF2196F3L,
            label = "Health", trackingType = TrackingType.QUANTITATIVE,
            visualizationType = VisualizationType.PHYSICS_JAR,
            unit = "glasses", targetValue = 8.0, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "09:00",
            category = TemplateCategory.HEALTH,
        ),
        HabitTemplate(
            name = "Sleep by 11pm", icon = "😴", color = 0xFF3F51B5L,
            label = "Health", trackingType = TrackingType.BINARY,
            visualizationType = VisualizationType.PIXEL_GRID,
            unit = null, targetValue = null, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "22:00",
            category = TemplateCategory.HEALTH,
        ),
        HabitTemplate(
            name = "No junk food", icon = "🥗", color = 0xFF4CAF50L,
            label = "Health", trackingType = TrackingType.BINARY,
            visualizationType = VisualizationType.PIXEL_GRID,
            unit = null, targetValue = null, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = null,
            category = TemplateCategory.HEALTH,
        ),
    ),
    TemplateCategory.FITNESS to listOf(
        HabitTemplate(
            name = "Cycling", icon = "🚴", color = 0xFFFF9800L,
            label = "Fitness", trackingType = TrackingType.QUANTITATIVE,
            visualizationType = VisualizationType.PHYSICS_JAR,
            unit = "km", targetValue = 20.0, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "07:00",
            category = TemplateCategory.FITNESS,
        ),
        HabitTemplate(
            name = "Running", icon = "🏃", color = 0xFFFF5722L,
            label = "Fitness", trackingType = TrackingType.QUANTITATIVE,
            visualizationType = VisualizationType.PHYSICS_JAR,
            unit = "km", targetValue = 5.0, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "06:30",
            category = TemplateCategory.FITNESS,
        ),
        HabitTemplate(
            name = "Gym workout", icon = "🏋️", color = 0xFFE91E63L,
            label = "Fitness", trackingType = TrackingType.DURATION,
            visualizationType = VisualizationType.AUDIO_WAVEFORM,
            unit = "min", targetValue = 60.0, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "07:00",
            category = TemplateCategory.FITNESS,
        ),
        HabitTemplate(
            name = "Walk 10k steps", icon = "👟", color = 0xFF8BC34AL,
            label = "Fitness", trackingType = TrackingType.QUANTITATIVE,
            visualizationType = VisualizationType.PHYSICS_JAR,
            unit = "steps", targetValue = 10000.0, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = null,
            category = TemplateCategory.FITNESS,
        ),
    ),
    TemplateCategory.LEARNING to listOf(
        HabitTemplate(
            name = "Read books", icon = "📚", color = 0xFF00BCD4L,
            label = "Learning", trackingType = TrackingType.QUANTITATIVE,
            visualizationType = VisualizationType.PHYSICS_JAR,
            unit = "pages", targetValue = 20.0, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "21:00",
            category = TemplateCategory.LEARNING,
        ),
        HabitTemplate(
            name = "Deep focus coding", icon = "💻", color = 0xFF607D8BL,
            label = "Learning", trackingType = TrackingType.DURATION,
            visualizationType = VisualizationType.AUDIO_WAVEFORM,
            unit = "min", targetValue = 90.0, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = WEEKDAYS, suggestedReminderTime = "09:00",
            category = TemplateCategory.LEARNING,
        ),
        HabitTemplate(
            name = "Learn language", icon = "🗣️", color = 0xFF009688L,
            label = "Learning", trackingType = TrackingType.DURATION,
            visualizationType = VisualizationType.AUDIO_WAVEFORM,
            unit = "min", targetValue = 15.0, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "19:00",
            category = TemplateCategory.LEARNING,
        ),
    ),
    TemplateCategory.PROJECTS to listOf(
        HabitTemplate(
            name = "Build a Drone", icon = "🚁", color = 0xFF5A6B7AL,
            label = "Projects", trackingType = TrackingType.CHECKPOINT,
            visualizationType = VisualizationType.TOPO_MAP,
            unit = null, targetValue = null,
            checkpointSteps = listOf(
                "Order parts & components",
                "Test motors and ESCs",
                "Assemble frame",
                "Wire electronics",
                "Configure flight controller",
                "First test flight",
            ),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = null,
            category = TemplateCategory.PROJECTS,
        ),
        HabitTemplate(
            name = "Write a Book", icon = "✍️", color = 0xFF795548L,
            label = "Projects", trackingType = TrackingType.CHECKPOINT,
            visualizationType = VisualizationType.TOPO_MAP,
            unit = null, targetValue = null,
            checkpointSteps = listOf(
                "Outline chapters",
                "Write first draft",
                "Edit and revise",
                "Get feedback",
                "Final polish",
                "Publish",
            ),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "09:00",
            category = TemplateCategory.PROJECTS,
        ),
        HabitTemplate(
            name = "Learn to Cook", icon = "🍳", color = 0xFFFF8F00L,
            label = "Projects", trackingType = TrackingType.CHECKPOINT,
            visualizationType = VisualizationType.RPG_RADAR,
            unit = null, targetValue = null,
            checkpointSteps = listOf(
                "Master knife skills",
                "Cook 5 basic sauces",
                "Nail 3 cuisines",
                "Host a dinner party",
            ),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "17:30",
            category = TemplateCategory.PROJECTS,
        ),
    ),
    TemplateCategory.FINANCE to listOf(
        HabitTemplate(
            name = "Track spending", icon = "💰", color = 0xFFFFEB3BL,
            label = "Finance", trackingType = TrackingType.FINANCIAL,
            visualizationType = VisualizationType.PHYSICS_JAR,
            unit = null, targetValue = null, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "20:00",
            category = TemplateCategory.FINANCE,
        ),
        HabitTemplate(
            name = "No impulse buying", icon = "🛒", color = 0xFFFF9800L,
            label = "Finance", trackingType = TrackingType.BINARY,
            visualizationType = VisualizationType.PIXEL_GRID,
            unit = null, targetValue = null, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = null,
            category = TemplateCategory.FINANCE,
        ),
        HabitTemplate(
            name = "Save money", icon = "🏦", color = 0xFF4CAF50L,
            label = "Finance", trackingType = TrackingType.FINANCIAL,
            visualizationType = VisualizationType.PHYSICS_JAR,
            unit = null, targetValue = null, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "09:00",
            category = TemplateCategory.FINANCE,
        ),
    ),
    TemplateCategory.LIFESTYLE to listOf(
        HabitTemplate(
            name = "Journal", icon = "📓", color = 0xFF795548L,
            label = "Lifestyle", trackingType = TrackingType.BINARY,
            visualizationType = VisualizationType.PIXEL_GRID,
            unit = null, targetValue = null, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "21:30",
            category = TemplateCategory.LIFESTYLE,
        ),
        HabitTemplate(
            name = "No social media", icon = "📵", color = 0xFF607D8BL,
            label = "Lifestyle", trackingType = TrackingType.BINARY,
            visualizationType = VisualizationType.PIXEL_GRID,
            unit = null, targetValue = null, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = null,
            category = TemplateCategory.LIFESTYLE,
        ),
        HabitTemplate(
            name = "Cook at home", icon = "🍳", color = 0xFFFF8F00L,
            label = "Lifestyle", trackingType = TrackingType.BINARY,
            visualizationType = VisualizationType.PIXEL_GRID,
            unit = null, targetValue = null, checkpointSteps = emptyList(),
            frequency = HabitFrequency.DAILY,
            scheduleDays = ALL_DAYS, suggestedReminderTime = "17:30",
            category = TemplateCategory.LIFESTYLE,
        ),
    ),
)

/** Ordered list for tab display */
val templateCategories = listOf(
    TemplateCategory.HEALTH,
    TemplateCategory.FITNESS,
    TemplateCategory.LEARNING,
    TemplateCategory.PROJECTS,
    TemplateCategory.FINANCE,
    TemplateCategory.LIFESTYLE,
)

/** Popular quick-pick templates shown as chips on the create screen. */
val popularTemplates: List<HabitTemplate> = listOf(
    habitTemplates[TemplateCategory.HEALTH]!!.first { it.name == "Daily Meditation" },
    habitTemplates[TemplateCategory.HEALTH]!!.first { it.name == "Take vitamins" },
    habitTemplates[TemplateCategory.HEALTH]!!.first { it.name == "Drink water" },
    habitTemplates[TemplateCategory.FITNESS]!!.first { it.name == "Cycling" },
    habitTemplates[TemplateCategory.LEARNING]!!.first { it.name == "Read books" },
    habitTemplates[TemplateCategory.PROJECTS]!!.first { it.name == "Build a Drone" },
    habitTemplates[TemplateCategory.LIFESTYLE]!!.first { it.name == "Journal" },
)
