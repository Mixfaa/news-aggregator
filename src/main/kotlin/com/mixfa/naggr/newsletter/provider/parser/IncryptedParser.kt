package com.mixfa.naggr.newsletter.provider.parser


import com.mixfa.naggr.newsletter.model.News
import com.mixfa.naggr.newsletter.service.ParsingNewsProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component


/*

    override val newsFlux: Flux<News> = Flux.generate<Optional<News>> {
        val news = parseNews()
        it.next(Optional.ofNullable(news))
    }
        .delayElements(Duration.ofMinutes(5))
        .filter { it.isPresent }
        .map { it.get() }

 */

@Component
class IncryptedParser : ParsingNewsProvider {
    private var lastParsedNews: News? = null

    private fun parseKeysFromContent(contentDiv: Element): List<String>? {
        val firstChild = contentDiv.firstElementChild()
            ?: return null

        if (!firstChild.`is`("ul")) return null
        val keys = firstChild.select("em")

        return keys.map { it.text() }
    }

    private fun parsePage(url: String): Map<String, String>? {
        try {
            val document = Jsoup.connect(url).get() ?: return null
            val contentDiv = document.selectFirst("div.row.posts-content") ?: return null

            val keys = parseKeysFromContent(contentDiv)
                ?: return null
            return buildMap {
                keys.forEachIndexed { index, text ->
                    put("key$index", text)
                }
            }
        } catch (ex: Exception) {
            return null
        }
    }

    override fun parseNews(): News? {
        return try {
            val document = Jsoup.connect("https://incrypted.com/news/?order_by=new").get() ?: return null

            val postArticle = document.selectFirst("article.news-layout-item.post.type-post") ?: return null

            val imageDiv = postArticle.selectFirst("div.incr_loop-image") ?: return null

            val imageRef = imageDiv.selectFirst("img[src]")?.attr("src") ?: return null

            val titleDiv = postArticle.selectFirst("div.incr_loop-title") ?: return null

            val title = titleDiv.text()

            if (lastParsedNews?.title == title) return null

            val link = titleDiv.selectFirst("[href]")?.attr("href") ?: return null

            val descriptionDiv = postArticle.selectFirst("div.incr_loop-deskr") ?: return null

            val description = descriptionDiv.text()

            val additionalInfo = mutableMapOf<String, String>()

            additionalInfo["description"] = description
            parsePage(link)?.let {
                additionalInfo.putAll(it)
            }

            lastParsedNews = News(link, title, imageRef, additionalInfo, listOf(News.Flag.CRYPTO))
            lastParsedNews
        } catch (ex: Exception) {
            println(ex)
            null
        }
    }
}
