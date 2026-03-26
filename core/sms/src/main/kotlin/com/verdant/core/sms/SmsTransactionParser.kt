package com.verdant.core.sms

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsTransactionParser @Inject constructor(
    private val regexParser: RegexSmsParser,
) {
    fun parse(sms: RawSms): ParsedTransaction? {
        // Regex-first strategy: fast, deterministic, and private
        return regexParser.parse(sms)
    }
}
