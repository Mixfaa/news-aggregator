package com.mixfa.naggr.news.providers

import com.mixfa.naggr.news.model.News
import com.mixfa.naggr.news.service.ReactiveNewsProvider
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.Duration

@Component
class ForklogParser : ReactiveNewsProvider {
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
            val document = Jsoup.connect("https://forklog.com/news").get() ?: return null

            val newsDiv = document.selectFirst("div.category_page_grid") ?: return null

            val newsCell = newsDiv.selectFirst("div.cell") ?: return null

            val link = newsCell.selectFirst("a")?.attr("href") ?: return null

            if (lastParsedNews?.link == link) return null

            val title = newsCell.selectFirst("div.text_blk")?.selectFirst("p")?.text() ?: return null

//            val imageRef = newsCell.selectFirst("img")?.attr("src") ?: return null

            lastParsedNews = News(link, title, null, mutableMapOf(), listOf(News.Flag.CRYPTO))
            lastParsedNews
        } catch (ex: Exception) {
            null
        }
    }
}