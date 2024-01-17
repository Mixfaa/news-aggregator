package ua.helpme.naggr.news.model

data class News(val link: String, val title: String, val imageRef: String, val additionalInfo: Map<String, String>) {
    val caption: String by lazy {
        buildString {
            appendLine(title)
            additionalInfo
                .filterKeys { it.startsWith("key") }
                .forEach { (key, value) ->
                    append('*')
                    appendLine(value)
                }
            appendLine(link)
        }
    }
}