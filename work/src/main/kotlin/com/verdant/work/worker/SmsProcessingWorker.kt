package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.model.Transaction
import com.verdant.core.model.repository.TransactionRepository
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.sms.SmsReader
import com.verdant.core.sms.SmsTransactionParser
import com.verdant.core.sms.TransactionCategorizer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Periodically reads new bank SMS, parses transactions, and stores them.
 * Runs every 2 hours when SMS permission is granted.
 */
@HiltWorker
class SmsProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val smsReader: SmsReader,
    private val parser: SmsTransactionParser,
    private val categorizer: TransactionCategorizer,
    private val transactionRepository: TransactionRepository,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.smsPermissionGranted.first()) return Result.success()

        val lastProcessed = prefs.lastSmsProcessedTime.first()
        val newSms = smsReader.readInboxSms(since = lastProcessed)

        if (newSms.isEmpty()) return Result.success()

        var latestTimestamp = lastProcessed

        for (sms in newSms) {
            // Skip if we already processed this SMS
            if (transactionRepository.getByRawSmsId(sms.id) != null) continue

            val parsed = parser.parse(sms) ?: continue
            val category = categorizer.categorize(parsed.merchant)

            transactionRepository.insert(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    amount = parsed.amount,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    category = category,
                    subCategory = null,
                    accountTail = parsed.accountTail,
                    bank = parsed.bank,
                    upiId = parsed.upiId,
                    balanceAfter = parsed.balance,
                    transactionDate = parsed.date,
                    rawSmsId = parsed.rawSmsId,
                    rawSmsBody = parsed.rawSmsBody,
                    isRecurring = false,
                    parseConfidence = parsed.confidence,
                    userVerified = false,
                    createdAt = System.currentTimeMillis(),
                )
            )

            if (sms.date > latestTimestamp) latestTimestamp = sms.date
        }

        if (latestTimestamp > lastProcessed) {
            prefs.setLastSmsProcessedTime(latestTimestamp)
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "verdant_sms_processing"
    }
}
