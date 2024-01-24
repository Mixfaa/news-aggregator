package com.mixfa.naggr.news.model

data class News(
    val link: String,
    val title: String,
    val imageRef: String?,
    val additionalInfo: MutableMap<String, String>,
    val flags: List<Flag>
) {
    val textForTelegram: String by lazy {
        """
             $link
             ${additionalInfo["ai_forecast"] ?: ""}
           """.trimIndent()
    }

    enum class Flag {
        CRYPTO,
        FINANCE;

        companion object {
            val flagsList = Flag.entries.joinToString(separator = "\n")
        }
    }
}