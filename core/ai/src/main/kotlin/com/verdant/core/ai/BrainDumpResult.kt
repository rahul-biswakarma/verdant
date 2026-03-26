package com.verdant.core.ai

import com.verdant.core.model.Habit

/** What the AI determined the user did with a particular habit in a brain dump. */
enum class BrainDumpAction { COMPLETE, SKIP, SET_VALUE }

/**
 * A single parsed entry from a brain dump log.
 *
 * @param habit      The matched habit from the user's habit list.
 * @param action     What the user did: completed it, skipped it, or set a numeric value.
 * @param value      Populated when [action] is [BrainDumpAction.SET_VALUE] — minutes for
 *                   DURATION habits, the raw quantity for QUANTITATIVE, or an expense
 *                   amount for FINANCIAL.
 * @param skipReason Optional reason text extracted when [action] is [BrainDumpAction.SKIP].
 */
data class BrainDumpResult(
    val habit: Habit,
    val action: BrainDumpAction,
    val value: Double? = null,
    val skipReason: String? = null,
)
