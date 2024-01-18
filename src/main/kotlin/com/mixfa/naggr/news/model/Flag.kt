package com.mixfa.naggr.news.model

enum class Flag {
    CRYPTO,
    FINANCE;

    companion object {
        val flagsList = entries.joinToString(separator = "\n")
    }
}