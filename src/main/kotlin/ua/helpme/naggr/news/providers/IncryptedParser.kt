package ua.helpme.naggr.news.providers


import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import ua.helpme.naggr.news.model.News
import java.time.Duration

@Component
class IncryptedParser : NewsProvider {
    private var lastParsedNews: News? = null

    override val newsFlux: Flux<News> = Flux.create {
        while (!it.isCancelled) {
            val news = parseNews() ?: continue
            it.next(news)
            Thread.sleep(Duration.ofSeconds(60))
        }
    }

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

    fun parseNews(): News? {
        return News(
            "test link", "test title", "https://incrypted.com/wp-content/uploads/2023/07/haker-s-380x235.jpg",
            mapOf("image" to "https://discord4j.com/jetbrains.svg")
        )
        return try {
            val document = Jsoup.connect("https://incrypted.com/news/?order_by=new").get() ?: return null

            val postArticle = document.selectFirst("article.news-layout-item.post.type-post") ?: return null

            val imageDiv = postArticle.selectFirst("div.incr_loop-image") ?: return null

            val imageRef = imageDiv.selectFirst("img[src]")?.attr("src") ?: return null

            val titleDiv = postArticle.selectFirst("div.incr_loop-title") ?: return null

            val title = titleDiv.text()

            if (lastParsedNews?.title.contentEquals(title)) return null

            val refEl = titleDiv.selectFirst("[href]") ?: return null
            val link = refEl.attr("href")

            val descriptionDiv = postArticle.selectFirst("div.incr_loop-deskr") ?: return null

            val description = descriptionDiv.text()

            val additionalInfo = buildMap {
                this["description"] = description
                parsePage(link)?.let {
                    this.putAll(it)
                }
            }

            lastParsedNews = News(link, title, imageRef,additionalInfo)
            lastParsedNews
        } catch (ex: Exception) {
            println(ex)
            null
        }
    }
}
