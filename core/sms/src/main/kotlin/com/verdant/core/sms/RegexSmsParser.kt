package com.verdant.core.sms

import com.verdant.core.model.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegexSmsParser @Inject constructor() {

    fun parse(sms: RawSms): ParsedTransaction? {
        val body = sms.body

        val amount = extractAmount(body) ?: return null
        val type = detectTransactionType(body) ?: return null

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = extractMerchant(body),
            accountTail = extractAccountTail(body),
            bank = detectBank(sms.address),
            upiId = extractUpiId(body),
            balance = extractBalance(body),
            date = sms.date,
            rawSmsId = sms.id,
            rawSmsBody = body,
            confidence = 0.85f,
        )
    }

    private fun detectTransactionType(body: String): TransactionType? {
        val lower = body.lowercase()
        return when {
            DEBIT_PATTERNS.any { it.containsMatchIn(lower) } -> TransactionType.DEBIT
            CREDIT_PATTERNS.any { it.containsMatchIn(lower) } -> TransactionType.CREDIT
            else -> null
        }
    }

    private fun extractAmount(body: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(body) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            return raw.toDoubleOrNull()
        }
        return null
    }

    private fun extractMerchant(body: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(body) ?: continue
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotBlank() && merchant.length > 2) return merchant
        }
        return null
    }

    private fun extractAccountTail(body: String): String? {
        val match = ACCOUNT_PATTERN.find(body) ?: return null
        return match.groupValues[1]
    }

    private fun extractBalance(body: String): Double? {
        val match = BALANCE_PATTERN.find(body) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    private fun extractUpiId(body: String): String? {
        val match = UPI_ID_PATTERN.find(body) ?: return null
        return match.groupValues[1]
    }

    private fun detectBank(address: String): String? {
        val upper = address.uppercase()
        return BANK_MAP.entries.firstOrNull { upper.contains(it.key) }?.value
    }

    companion object {
        private val DEBIT_PATTERNS = listOf(
            Regex("debited|withdrawn|spent|paid|purchase|deducted|sent"),
        )

        private val CREDIT_PATTERNS = listOf(
            Regex("credited|received|deposited|refund|cashback"),
        )

        private val AMOUNT_PATTERNS = listOf(
            // INR 1,234.56 or Rs. 1,234.56 or Rs 1234
            Regex("(?:INR|Rs\\.?)\\s*([\\d,]+\\.?\\d*)", RegexOption.IGNORE_CASE),
            // Rs.1,234.56 (no space)
            Regex("(?:INR|Rs\\.?)([\\d,]+\\.?\\d*)", RegexOption.IGNORE_CASE),
        )

        private val MERCHANT_PATTERNS = listOf(
            // "at MERCHANT on" or "at MERCHANT."
            Regex("(?:at|to|from)\\s+(.+?)\\s+(?:on|Ref|UPI|ref|\\.|$)", RegexOption.IGNORE_CASE),
            // UPI: "to MERCHANT UPI"
            Regex("(?:to|from)\\s+(.+?)\\s+(?:UPI|Ref|ref)", RegexOption.IGNORE_CASE),
        )

        private val ACCOUNT_PATTERN =
            Regex("(?:a/c|acct|account|A/C|xx|XX|\\*{2,})\\s*[xX*]*?(\\d{4})", RegexOption.IGNORE_CASE)

        private val BALANCE_PATTERN =
            Regex("(?:Avl\\s*Bal|Available\\s*Balance|Bal)[:\\s]*(?:INR|Rs\\.?)\\s*([\\d,]+\\.?\\d*)", RegexOption.IGNORE_CASE)

        private val UPI_ID_PATTERN =
            Regex("([a-zA-Z0-9.\\-_]+@[a-zA-Z]+)")

        private val BANK_MAP = mapOf(
            "HDFCBK" to "HDFC",
            "HDFCBN" to "HDFC",
            "ICICIB" to "ICICI",
            "ICICIS" to "ICICI",
            "SBIINB" to "SBI",
            "SBIATM" to "SBI",
            "SBIPSG" to "SBI",
            "AXISBK" to "Axis",
            "KOTAKB" to "Kotak",
            "PNBSMS" to "PNB",
            "BOBTXN" to "BOB",
            "YESBK" to "Yes Bank",
            "IDFCFB" to "IDFC First",
            "INDBNK" to "IndusInd",
            "FEDBK" to "Federal",
            "PYTM1" to "Paytm",
            "JIOPAY" to "Jio",
            "PHONPE" to "PhonePe",
            "GPAY" to "GPay",
        )
    }
}
