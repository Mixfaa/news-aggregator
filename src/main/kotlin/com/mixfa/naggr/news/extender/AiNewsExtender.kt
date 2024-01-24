package com.mixfa.naggr.news.extender

import com.mixfa.naggr.news.model.News
import com.mixfa.naggr.news.service.NewsDataExtender
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

//private fun writePrompt(news: News): String {
//    return """
//        Analyze news message below and give your opinion as you are professional financial analytic
//
//        If there is not enough information for you, you answer must contain "Not enough info"
//        If you can say at least positive it or not, you answer must not contain "Not enough info"
//
//        News message:
//        Title: ${news.title}
//        Caption: ${news.caption}
//        Additional info:
//        ${news.additionalInfo.values.joinToString("\n")}
//    """.trimIndent()
//}

private fun writePrompt(news: News): String {
    return """
        Analyze news message below as you are professional financial analytic and give 
        value between -10 and 10, where -10 means this news is very negative and 10 is very positive

        If there is not enough information for you, you answer must be "Not enough info"
     
        Only number from you or "Not enough info" expected

        News message: 
        Title: ${news.title}
        Additional info:
        ${news.additionalInfo.values.joinToString("\n")}
    """.trimIndent()
}

/**
 * Just for fun, it does not provide any important information.
 */
@Component
class AiNewsExtender(
    private val aiService: OpenAiService
) : NewsDataExtender {
    private val targetFlags = listOf(News.Flag.CRYPTO, News.Flag.FINANCE)
    private val maxTokens = 16385
    private val logger = LoggerFactory.getLogger(AiNewsExtender::class.java)

    private val numberRegex = "-?\\d+(?:\\.\\d+)?".toRegex()

    override fun extend(news: News) {
        if (targetFlags.none { it in news.flags }) return

        val prompt = writePrompt(news)

        if (prompt.length >= maxTokens) return

        val chatRequest = ChatCompletionRequest.builder()
            .messages(listOf(ChatMessage("user", prompt)))
            .model("gpt-3.5-turbo")
            .build()

        val choices = aiService.createChatCompletion(chatRequest).choices

        choices.forEach {
            logger.info(it.message.content)
        }

        val response = choices
            .firstOrNull { it.finishReason == "stop" }
            ?.message?.content ?: return

        if (response.contains("Not enough info")) return

        val forecast = numberRegex.find(response)?.groupValues?.firstOrNull() ?: return

        news.additionalInfo["ai_forecast"] = "Ai forecast: $forecast"
    }
}