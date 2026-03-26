package com.verdant.core.model

enum class TrackingType {
    BINARY,
    QUANTITATIVE,
    DURATION,
    LOCATION,
    FINANCIAL,
    /** Irregular / on-demand habits tracked by time since last completion (e.g. "call parents"). */
    EVENT_DRIVEN,
}
