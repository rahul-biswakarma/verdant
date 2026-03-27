package com.verdant.core.database.usecase

import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.repository.LabelRepository
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.Label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ExportData(
    val habits: List<Habit>,
    val entries: List<HabitEntry>,
    val labels: List<Label>,
)

@Singleton
class ExportUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val habitEntryRepository: HabitEntryRepository,
    private val labelRepository: LabelRepository,
) {
    suspend fun collectData(): ExportData = withContext(Dispatchers.IO) {
        ExportData(
            habits  = habitRepository.getAllHabits(),
            entries = habitEntryRepository.getAllEntries(),
            labels  = labelRepository.getAllLabels(),
        )
    }

    fun toJson(data: ExportData): String {
        val habitsJson = data.habits.joinToString(",\n", "[\n", "\n]") { h ->
            """  {
    "id": ${h.id.quoted()},
    "name": ${h.name.quoted()},
    "description": ${h.description.quoted()},
    "icon": ${h.icon.quoted()},
    "color": ${h.color},
    "label": ${h.label?.quoted() ?: "null"},
    "trackingType": ${h.trackingType.name.quoted()},
    "unit": ${h.unit?.quoted() ?: "null"},
    "targetValue": ${h.targetValue ?: "null"},
    "frequency": ${h.frequency.name.quoted()},
    "scheduleDays": ${h.scheduleDays},
    "isArchived": ${h.isArchived},
    "reminderEnabled": ${h.reminderEnabled},
    "reminderTime": ${h.reminderTime?.quoted() ?: "null"},
    "sortOrder": ${h.sortOrder},
    "createdAt": ${h.createdAt}
  }"""
        }

        val entriesJson = data.entries.joinToString(",\n", "[\n", "\n]") { e ->
            """  {
    "id": ${e.id.quoted()},
    "habitId": ${e.habitId.quoted()},
    "date": ${e.date.toString().quoted()},
    "completed": ${e.completed},
    "value": ${e.value ?: "null"},
    "note": ${e.note?.quoted() ?: "null"},
    "category": ${e.category?.quoted() ?: "null"},
    "skipped": ${e.skipped},
    "createdAt": ${e.createdAt},
    "updatedAt": ${e.updatedAt}
  }"""
        }

        val labelsJson = data.labels.joinToString(",\n", "[\n", "\n]") { l ->
            """  {"id": ${l.id.quoted()}, "name": ${l.name.quoted()}, "color": ${l.color}}"""
        }

        return """{
  "exportedAt": ${System.currentTimeMillis()},
  "version": 1,
  "habits": $habitsJson,
  "entries": $entriesJson,
  "labels": $labelsJson
}"""
    }

    fun toCsv(data: ExportData): String {
        val sb = StringBuilder()

        sb.appendLine("# Verdant Habit Export")
        sb.appendLine("# Habits")
        sb.appendLine("id,name,description,icon,color,label,trackingType,unit,targetValue,frequency,scheduleDays,isArchived,reminderEnabled,reminderTime,sortOrder,createdAt")
        data.habits.forEach { h ->
            sb.appendLine(
                listOf(
                    h.id, h.name.csvEscape(), h.description.csvEscape(),
                    h.icon, h.color, h.label ?: "",
                    h.trackingType.name, h.unit ?: "", h.targetValue ?: "",
                    h.frequency.name, h.scheduleDays, h.isArchived,
                    h.reminderEnabled, h.reminderTime ?: "", h.sortOrder, h.createdAt,
                ).joinToString(",")
            )
        }

        sb.appendLine()
        sb.appendLine("# Entries")
        sb.appendLine("id,habitId,date,completed,value,note,category,skipped,createdAt,updatedAt")
        data.entries.forEach { e ->
            sb.appendLine(
                listOf(
                    e.id, e.habitId, e.date, e.completed,
                    e.value ?: "", e.note?.csvEscape() ?: "",
                    e.category?.csvEscape() ?: "", e.skipped,
                    e.createdAt, e.updatedAt,
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    fun toMarkdown(data: ExportData): String {
        val sb = StringBuilder()
        sb.appendLine("# Verdant — Year in Review")
        sb.appendLine()
        sb.appendLine("**Exported:** ${java.time.LocalDate.now()}")
        sb.appendLine()

        // Habits summary
        sb.appendLine("## Habits (${data.habits.size})")
        sb.appendLine()
        data.habits.forEach { h ->
            val entries = data.entries.filter { it.habitId == h.id }
            val completed = entries.count { it.completed }
            val total = entries.size
            val rate = if (total > 0) (completed * 100 / total) else 0
            sb.appendLine("### ${h.icon} ${h.name}")
            sb.appendLine("- **Type:** ${h.trackingType.name}")
            sb.appendLine("- **Entries:** $total total, $completed completed ($rate%)")
            if (h.targetValue != null) {
                sb.appendLine("- **Target:** ${h.targetValue} ${h.unit ?: ""}")
            }
            sb.appendLine()
        }

        // Monthly breakdown
        sb.appendLine("## Monthly Activity")
        sb.appendLine()
        val byMonth = data.entries.groupBy {
            "${it.date.year}-${it.date.monthValue.toString().padStart(2, '0')}"
        }.toSortedMap()
        byMonth.forEach { (month, entries) ->
            val completed = entries.count { it.completed }
            sb.appendLine("- **$month:** ${entries.size} entries, $completed completed")
        }

        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine("*Generated by Verdant*")

        return sb.toString()
    }

    private fun String.quoted() = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
    private fun String.csvEscape() = if (contains(',') || contains('"') || contains('\n'))
        "\"${replace("\"", "\"\"")}\""
    else this
}
