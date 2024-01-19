package com.mixfa.naggr.utils

import java.util.*
import java.util.concurrent.TimeUnit

class DefaultExpirableImpl(timeToExpire: Long, timeUnit: TimeUnit) : Expirable {
    private var expirationDate: Date
    private val timeInMillis = TimeUnit.MILLISECONDS.convert(timeToExpire, timeUnit)

    init {
        expirationDate = Date(System.currentTimeMillis() + timeInMillis)
    }

    override fun renew() {
        expirationDate.time = (System.currentTimeMillis() + timeInMillis)
    }

    override fun isExpired(): Boolean = Calendar.getInstance().time.after(expirationDate)
}


interface Expirable {
    fun renew()
    fun isExpired(): Boolean
}