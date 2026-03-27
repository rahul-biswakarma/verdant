package com.verdant.core.database.entity

import com.verdant.core.model.Achievement
import com.verdant.core.model.ActivityRecord
import com.verdant.core.model.ActivityType
import com.verdant.core.model.Budget
import com.verdant.core.model.BudgetPeriod
import com.verdant.core.model.CrossCorrelation
import com.verdant.core.model.DeviceSignal
import com.verdant.core.model.DeviceStat
import com.verdant.core.model.DeviceStatType
import com.verdant.core.model.EmotionalContext
import com.verdant.core.model.EvolutionPath
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.HealthRecord
import com.verdant.core.model.HealthRecordType
import com.verdant.core.model.InferredMood
import com.verdant.core.model.Label
import com.verdant.core.model.LifeScore
import com.verdant.core.model.PlayerProfile
import com.verdant.core.model.PlayerRank
import com.verdant.core.model.PlayerStats
import com.verdant.core.model.Prediction
import com.verdant.core.model.PredictionType
import com.verdant.core.model.Quest
import com.verdant.core.model.QuestDifficulty
import com.verdant.core.model.QuestStatus
import com.verdant.core.model.RecurringTransaction
import com.verdant.core.model.ScoreType
import com.verdant.core.model.Transaction
import com.verdant.core.model.TransactionType
import com.verdant.core.model.WeatherCondition
import com.verdant.core.model.WeatherSnapshot

// ── HabitEntity ↔ Habit ───────────────────────────────────────────────────────

fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    description = description,
    icon = icon,
    color = color,
    label = label,
    trackingType = trackingType,
    visualizationType = visualizationType,
    unit = unit,
    targetValue = targetValue,
    checkpointSteps = if (checkpointSteps.isBlank()) emptyList()
                      else checkpointSteps.split("|").filter { it.isNotBlank() },
    frequency = frequency,
    scheduleDays = scheduleDays,
    isArchived = isArchived,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime,
    reminderDays = reminderDays,
    sortOrder = sortOrder,
    createdAt = createdAt,
    outdoorActivity = outdoorActivity,
)

fun Habit.toEntity() = HabitEntity(
    id = id,
    name = name,
    description = description,
    icon = icon,
    color = color,
    label = label,
    trackingType = trackingType,
    visualizationType = visualizationType,
    unit = unit,
    targetValue = targetValue,
    checkpointSteps = checkpointSteps.joinToString("|"),
    frequency = frequency,
    scheduleDays = scheduleDays,
    isArchived = isArchived,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime,
    reminderDays = reminderDays,
    sortOrder = sortOrder,
    createdAt = createdAt,
    outdoorActivity = outdoorActivity,
)

// ── HabitEntryEntity ↔ HabitEntry ────────────────────────────────────────────

fun HabitEntryEntity.toDomain() = HabitEntry(
    id = id,
    habitId = habitId,
    date = date,
    completed = completed,
    value = value,
    latitude = latitude,
    longitude = longitude,
    note = note,
    category = category,
    skipped = skipped,
    missedReason = missedReason,
    stressLevel = stressLevel,
    energyLevel = energyLevel,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun HabitEntry.toEntity() = HabitEntryEntity(
    id = id,
    habitId = habitId,
    date = date,
    completed = completed,
    value = value,
    latitude = latitude,
    longitude = longitude,
    note = note,
    category = category,
    skipped = skipped,
    missedReason = missedReason,
    stressLevel = stressLevel,
    energyLevel = energyLevel,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── LabelEntity ↔ Label ───────────────────────────────────────────────────────

fun LabelEntity.toDomain() = Label(id = id, name = name, color = color)

fun Label.toEntity() = LabelEntity(id = id, name = name, color = color)

// ── TransactionEntity ↔ Transaction ──────────────────────────────────────────

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = amount,
    type = TransactionType.valueOf(transactionType),
    merchant = merchant,
    category = category,
    subCategory = subCategory,
    accountTail = accountTail,
    bank = bank,
    upiId = upiId,
    balanceAfter = balanceAfter,
    transactionDate = transactionDate,
    rawSmsId = rawSmsId,
    rawSmsBody = rawSmsBody,
    isRecurring = isRecurring,
    parseConfidence = parseConfidence,
    userVerified = userVerified,
    createdAt = createdAt,
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    amount = amount,
    transactionType = type.name,
    merchant = merchant,
    category = category,
    subCategory = subCategory,
    accountTail = accountTail,
    bank = bank,
    upiId = upiId,
    balanceAfter = balanceAfter,
    transactionDate = transactionDate,
    rawSmsId = rawSmsId,
    rawSmsBody = rawSmsBody,
    isRecurring = isRecurring,
    parseConfidence = parseConfidence,
    userVerified = userVerified,
    createdAt = createdAt,
)

// ── HealthRecordEntity ↔ HealthRecord ────────────────────────────────────────

fun HealthRecordEntity.toDomain() = HealthRecord(
    id = id,
    recordType = HealthRecordType.valueOf(recordType),
    value = value,
    secondaryValue = secondaryValue,
    unit = unit,
    recordedAt = recordedAt,
    source = source,
    createdAt = createdAt,
)

fun HealthRecord.toEntity() = HealthRecordEntity(
    id = id,
    recordType = recordType.name,
    value = value,
    secondaryValue = secondaryValue,
    unit = unit,
    recordedAt = recordedAt,
    source = source,
    createdAt = createdAt,
)

// ── ActivityRecordEntity ↔ ActivityRecord ────────────────────────────────────

fun ActivityRecordEntity.toDomain() = ActivityRecord(
    id = id,
    activityType = ActivityType.valueOf(activityType),
    confidence = confidence,
    durationMinutes = durationMinutes,
    recordedAt = recordedAt,
    createdAt = createdAt,
)

fun ActivityRecord.toEntity() = ActivityRecordEntity(
    id = id,
    activityType = activityType.name,
    confidence = confidence,
    durationMinutes = durationMinutes,
    recordedAt = recordedAt,
    createdAt = createdAt,
)

// ── DeviceStatEntity ↔ DeviceStat ────────────────────────────────────────────

fun DeviceStatEntity.toDomain() = DeviceStat(
    id = id,
    statType = DeviceStatType.valueOf(statType),
    value = value,
    detail = detail,
    recordedDate = recordedDate,
    createdAt = createdAt,
)

fun DeviceStat.toEntity() = DeviceStatEntity(
    id = id,
    statType = statType.name,
    value = value,
    detail = detail,
    recordedDate = recordedDate,
    createdAt = createdAt,
)

// ── WeatherSnapshotEntity ↔ WeatherSnapshot ──────────────────────────────────

fun WeatherSnapshotEntity.toDomain() = WeatherSnapshot(
    id = id,
    date = date,
    temperature = temperature,
    condition = WeatherCondition.valueOf(condition),
    humidity = humidity,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt,
)

fun WeatherSnapshot.toEntity() = WeatherSnapshotEntity(
    id = id,
    date = date,
    temperature = temperature,
    condition = condition.name,
    humidity = humidity,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt,
)

// ── LifeScoreEntity ↔ LifeScore ─────────────────────────────────────────────

fun LifeScoreEntity.toDomain() = LifeScore(
    id = id,
    scoreType = ScoreType.valueOf(scoreType),
    score = score,
    components = components,
    computedDate = computedDate,
    createdAt = createdAt,
)

fun LifeScore.toEntity() = LifeScoreEntity(
    id = id,
    scoreType = scoreType.name,
    score = score,
    components = components,
    computedDate = computedDate,
    createdAt = createdAt,
)

// ── PredictionEntity ↔ Prediction ────────────────────────────────────────────

fun PredictionEntity.toDomain() = Prediction(
    id = id,
    predictionType = PredictionType.valueOf(predictionType),
    targetPeriod = targetPeriod,
    predictionData = predictionData,
    confidence = confidence,
    generatedAt = generatedAt,
    expiresAt = expiresAt,
)

fun Prediction.toEntity() = PredictionEntity(
    id = id,
    predictionType = predictionType.name,
    targetPeriod = targetPeriod,
    predictionData = predictionData,
    confidence = confidence,
    generatedAt = generatedAt,
    expiresAt = expiresAt,
)

// ── BudgetEntity ↔ Budget ────────────────────────────────────────────────────

fun BudgetEntity.toDomain() = Budget(
    id = id,
    name = name,
    category = category,
    amount = amount,
    period = BudgetPeriod.valueOf(period),
    isActive = isActive,
    createdAt = createdAt,
)

fun Budget.toEntity() = BudgetEntity(
    id = id,
    name = name,
    category = category,
    amount = amount,
    period = period.name,
    isActive = isActive,
    createdAt = createdAt,
)

// ── RecurringTransactionEntity ↔ RecurringTransaction ────────────────────────

fun RecurringTransactionEntity.toDomain() = RecurringTransaction(
    id = id,
    merchant = merchant,
    category = category,
    typicalAmount = typicalAmount,
    frequencyDays = frequencyDays,
    lastSeen = lastSeen,
    nextExpected = nextExpected,
    confidence = confidence,
    isActive = isActive,
    createdAt = createdAt,
)

fun RecurringTransaction.toEntity() = RecurringTransactionEntity(
    id = id,
    merchant = merchant,
    category = category,
    typicalAmount = typicalAmount,
    frequencyDays = frequencyDays,
    lastSeen = lastSeen,
    nextExpected = nextExpected,
    confidence = confidence,
    isActive = isActive,
    createdAt = createdAt,
)

// ── EmotionalContextEntity ↔ EmotionalContext ────────────────────────────────

fun EmotionalContextEntity.toDomain() = EmotionalContext(
    id = id,
    date = date,
    inferredMood = InferredMood.valueOf(inferredMood),
    energyLevel = energyLevel,
    confidence = confidence,
    contributingSignals = contributingSignals,
    userConfirmed = userConfirmed,
)

fun EmotionalContext.toEntity() = EmotionalContextEntity(
    id = id,
    date = date,
    inferredMood = inferredMood.name,
    energyLevel = energyLevel,
    confidence = confidence,
    contributingSignals = contributingSignals,
    userConfirmed = userConfirmed,
)

// ── PlayerProfileEntity ↔ PlayerProfile ──────────────────────────────────────

fun PlayerProfileEntity.toDomain(): PlayerProfile {
    val statParts = stats.split(",").map { it.toIntOrNull() ?: 0 }
    return PlayerProfile(
        id = id,
        level = level,
        title = title,
        totalXP = totalXP,
        currentLevelXP = currentLevelXP,
        xpToNextLevel = xpToNextLevel,
        rank = PlayerRank.valueOf(rank),
        stats = PlayerStats(
            vitality = statParts.getOrElse(0) { 0 },
            discipline = statParts.getOrElse(1) { 0 },
            wisdom = statParts.getOrElse(2) { 0 },
            focus = statParts.getOrElse(3) { 0 },
            resilience = statParts.getOrElse(4) { 0 },
            awareness = statParts.getOrElse(5) { 0 },
        ),
        evolutionPath = EvolutionPath.valueOf(evolutionPath),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun PlayerProfile.toEntity() = PlayerProfileEntity(
    id = id,
    level = level,
    title = title,
    totalXP = totalXP,
    currentLevelXP = currentLevelXP,
    xpToNextLevel = xpToNextLevel,
    rank = rank.name,
    stats = "${stats.vitality},${stats.discipline},${stats.wisdom},${stats.focus},${stats.resilience},${stats.awareness}",
    evolutionPath = evolutionPath.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── QuestEntity ↔ Quest ──────────────────────────────────────────────────────

fun QuestEntity.toDomain() = Quest(
    id = id,
    title = title,
    description = description,
    difficulty = QuestDifficulty.valueOf(difficulty),
    xpReward = xpReward,
    conditions = conditions,
    timeLimit = timeLimit,
    generatedBy = generatedBy,
    reasoning = reasoning,
    status = QuestStatus.valueOf(status),
    startedAt = startedAt,
    completedAt = completedAt,
)

fun Quest.toEntity() = QuestEntity(
    id = id,
    title = title,
    description = description,
    difficulty = difficulty.name,
    xpReward = xpReward,
    conditions = conditions,
    timeLimit = timeLimit,
    generatedBy = generatedBy,
    reasoning = reasoning,
    status = status.name,
    startedAt = startedAt,
    completedAt = completedAt,
)

// ── AchievementEntity ↔ Achievement ──────────────────────────────────────────

fun AchievementEntity.toDomain() = Achievement(
    id = id,
    title = title,
    description = description,
    xpReward = xpReward,
    unlockedAt = unlockedAt,
    category = category,
)

fun Achievement.toEntity() = AchievementEntity(
    id = id,
    title = title,
    description = description,
    xpReward = xpReward,
    unlockedAt = unlockedAt,
    category = category,
)

// ── DeviceSignalEntity ↔ DeviceSignal ────────────────────────────────────────

fun DeviceSignalEntity.toDomain() = DeviceSignal(
    id = id,
    deviceId = deviceId,
    signalType = signalType,
    value = value,
    unit = unit,
    timestamp = timestamp,
    createdAt = createdAt,
)

fun DeviceSignal.toEntity() = DeviceSignalEntity(
    id = id,
    deviceId = deviceId,
    signalType = signalType,
    value = value,
    unit = unit,
    timestamp = timestamp,
    createdAt = createdAt,
)

// ── CrossCorrelationEntity ↔ CrossCorrelation ────────────────────────────────

fun CrossCorrelationEntity.toDomain() = CrossCorrelation(
    id = id,
    dimensionA = dimensionA,
    dimensionB = dimensionB,
    correlationStrength = correlationStrength,
    description = description,
    discoveredAt = discoveredAt,
    sampleSize = sampleSize,
)

fun CrossCorrelation.toEntity() = CrossCorrelationEntity(
    id = id,
    dimensionA = dimensionA,
    dimensionB = dimensionB,
    correlationStrength = correlationStrength,
    description = description,
    discoveredAt = discoveredAt,
    sampleSize = sampleSize,
)
