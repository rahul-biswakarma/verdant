package com.verdant.core.sms.di

import com.verdant.core.sms.RegexSmsParser
import com.verdant.core.sms.SmsReader
import com.verdant.core.sms.SmsTransactionParser
import com.verdant.core.sms.TransactionCategorizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmsModule {

    @Provides
    @Singleton
    fun provideSmsTransactionParser(
        regexParser: RegexSmsParser,
    ): SmsTransactionParser = SmsTransactionParser(regexParser)
}
