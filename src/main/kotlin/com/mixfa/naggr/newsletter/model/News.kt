package com.mixfa.naggr.newsletter.model

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

fun Iterable<News>.flagsSet(): Set<News.Flag> {
    val allFlags = HashSet<News.Flag>(News.Flag.entries.size)
    this.forEach { allFlags.addAll(it.flags) }
    return allFlags
}