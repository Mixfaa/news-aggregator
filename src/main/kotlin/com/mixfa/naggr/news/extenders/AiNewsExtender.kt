package com.mixfa.naggr.news.extenders

import com.mixfa.naggr.news.model.News
import com.mixfa.naggr.news.service.NewsDataExtender
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import org.springframework.stereotype.Component

private fun writePrompt(news: News): String {
    return """
        Analyze news message below and give your opinion as you are professional financial analytic
        
        If there is not enough information for you, you answer must contain "Not enough info"

        News message: 
        Title: ${news.title}
        Caption: ${news.caption}
        Additional info:
        ${news.additionalInfo.values.joinToString("\n")}
    """.trimIndent()
}

@Component
class AiNewsExtender(
    private val aiService: OpenAiService
) : NewsDataExtender {
    private val targetFlags = listOf(News.Flag.CRYPTO, News.Flag.FINANCE)
    private val maxTokens = 16385

    override fun extend(news: News) {
        if (targetFlags.none { it in news.flags }) return

        val prompt = writePrompt(news)

        if (prompt.length >= maxTokens) return

        val chatRequest = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(listOf(ChatMessage("user", prompt)))
            .build()

        val response = aiService.createChatCompletion(chatRequest)
            .choices
            .firstOrNull { it.finishReason == "stop" }
            ?.message?.content ?: return

        if (response.contains("Not enough info")) return

        news.additionalInfo["ai_forecast"] = response
    }
}