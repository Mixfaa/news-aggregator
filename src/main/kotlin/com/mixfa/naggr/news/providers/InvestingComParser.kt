package com.mixfa.naggr.news.providers

import com.mixfa.naggr.news.model.Flag
import com.mixfa.naggr.news.model.News
import com.mixfa.naggr.news.service.NewsProvider
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.*


@Component
class InvestingComParser : NewsProvider {
    private var lastParsedNews: News? = null

    override val newsFlux: Flux<News> = Flux.create {
        while (!it.isCancelled) {
            val news = parseNews()
            if (news != null) it.next(news)
            Thread.sleep(Duration.ofMinutes(5))
        }
    }

    fun parseNews(): News? {
        return try {
            val document = Jsoup.connect("https://ru.investing.com/news/cryptocurrency-news").get() ?: return null

            val article = document.selectFirst("article.js-article-item.articleItem") ?: return null

            val imageRef = article.selectFirst("img")?.attr("src")

            val textDiv = article.selectFirst("div.textDiv") ?: return null

            val titleElement = textDiv.selectFirst("a") ?: return null

            val link = "https://ru.investing.com" + (titleElement.attr("href") ?: return null)

            if (lastParsedNews?.link == link) return null

            val title = titleElement.attr("title") ?: return null

            val content = textDiv.selectFirst("p")?.text()

            lastParsedNews = News(
                link, title, imageRef,
                if (content != null) mapOf("content" to content) else emptyMap(), listOf(Flag.CRYPTO))
            lastParsedNews
        } catch (ex: Exception) {
            null
        }
    }
}