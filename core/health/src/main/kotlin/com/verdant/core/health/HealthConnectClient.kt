package com.verdant.core.health

import com.verdant.core.model.HealthRecord
import com.verdant.core.model.HealthRecordType

interface HealthConnectClient {
    suspend fun isAvailable(): Boolean
    suspend fun hasPermissions(types: Set<HealthRecordType>): Boolean
    suspend fun readRecords(type: HealthRecordType, startTime: Long, endTime: Long): List<HealthRecord>
    suspend fun readSteps(startTime: Long, endTime: Long): List<HealthRecord>
    suspend fun readSleep(startTime: Long, endTime: Long): List<HealthRecord>
    suspend fun readHeartRate(startTime: Long, endTime: Long): List<HealthRecord>
    suspend fun readExercise(startTime: Long, endTime: Long): List<HealthRecord>
    suspend fun readWeight(startTime: Long, endTime: Long): List<HealthRecord>
    suspend fun readHydration(startTime: Long, endTime: Long): List<HealthRecord>
}
