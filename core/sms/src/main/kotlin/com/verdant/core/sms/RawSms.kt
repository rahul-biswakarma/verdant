package com.verdant.core.sms

data class RawSms(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val read: Boolean,
)
