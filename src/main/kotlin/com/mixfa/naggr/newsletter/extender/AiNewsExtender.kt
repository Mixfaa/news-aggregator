package com.mixfa.naggr.newsletter.extender

import com.mixfa.naggr.newsletter.model.News
import com.mixfa.naggr.newsletter.service.NewsDataExtender
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private fun writePrompt(news: News): String {
    return """
        Analyze news message below as you are professional financial analytic and give 
        value between -10 and 10, where -10 means this news is very negative and 10 is very positive

        If there is not enough information for you, you answer must be "Not enough info"
     
        Only number or "Not enough info" expected

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
    private val logger = LoggerFactory.getLogger(AiNewsExtender::class.java)

    override fun extend(news: News) {
        if (news.flags.none { it in TARGET_FLAGS }) return

        val prompt = writePrompt(news)

        if (prompt.length >= MAX_TOKENS) return

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

    companion object {
        private val TARGET_FLAGS = listOf(News.Flag.CRYPTO, News.Flag.FINANCE)
        private const val MAX_TOKENS = 16385
        private val numberRegex = "-?\\d+(?:\\.\\d+)?".toRegex()
    }
}