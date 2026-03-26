package com.verdant.core.sms

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val BANK_SENDERS = setOf(
            // HDFC
            "HDFCBK", "HDFCBN",
            // ICICI
            "ICICIB", "ICICIS",
            // SBI
            "SBIINB", "SBIATM", "SBIPSG",
            // Axis
            "AXISBK",
            // Kotak
            "KOTAKB",
            // PNB
            "PNBSMS",
            // BOB
            "BOBTXN",
            // Yes Bank
            "YESBK",
            // IndusInd
            "IDFCFB", "INDBNK",
            // Federal
            "FEDBK",
            // UPI
            "PYTM1", "JIOPAY", "PHONPE", "GPAY",
        )

        private val SMS_URI: Uri = Uri.parse("content://sms/inbox")
    }

    fun readInboxSms(since: Long): List<RawSms> {
        val results = mutableListOf<RawSms>()

        val cursor = context.contentResolver.query(
            SMS_URI,
            arrayOf("_id", "address", "body", "date", "read"),
            "date > ?",
            arrayOf(since.toString()),
            "date DESC",
        ) ?: return results

        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("_id")
            val addressIdx = it.getColumnIndexOrThrow("address")
            val bodyIdx = it.getColumnIndexOrThrow("body")
            val dateIdx = it.getColumnIndexOrThrow("date")
            val readIdx = it.getColumnIndexOrThrow("read")

            while (it.moveToNext()) {
                val address = it.getString(addressIdx) ?: continue
                if (!isBankSender(address)) continue

                results.add(
                    RawSms(
                        id = it.getLong(idIdx),
                        address = address,
                        body = it.getString(bodyIdx) ?: continue,
                        date = it.getLong(dateIdx),
                        read = it.getInt(readIdx) == 1,
                    )
                )
            }
        }
        return results
    }

    fun readSingleSms(id: Long): RawSms? {
        val cursor = context.contentResolver.query(
            SMS_URI,
            arrayOf("_id", "address", "body", "date", "read"),
            "_id = ?",
            arrayOf(id.toString()),
            null,
        ) ?: return null

        cursor.use {
            if (!it.moveToFirst()) return null
            return RawSms(
                id = it.getLong(it.getColumnIndexOrThrow("_id")),
                address = it.getString(it.getColumnIndexOrThrow("address")) ?: return null,
                body = it.getString(it.getColumnIndexOrThrow("body")) ?: return null,
                date = it.getLong(it.getColumnIndexOrThrow("date")),
                read = it.getInt(it.getColumnIndexOrThrow("read")) == 1,
            )
        }
    }

    private fun isBankSender(address: String): Boolean {
        val upper = address.uppercase()
        return BANK_SENDERS.any { upper.contains(it) }
    }
}
