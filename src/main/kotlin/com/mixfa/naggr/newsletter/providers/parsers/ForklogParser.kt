package com.mixfa.naggr.newsletter.providers.parsers

import com.mixfa.naggr.newsletter.model.News
import com.mixfa.naggr.newsletter.service.ParsingNewsProvider
import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component
class ForklogParser : ParsingNewsProvider {
    private var lastParsedNews: News? = null

    override fun parseNews(): News? {
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