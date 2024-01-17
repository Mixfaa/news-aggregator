package ua.helpme.naggr.news.providers

import reactor.core.publisher.Flux
import ua.helpme.naggr.news.model.News

interface NewsProvider {
    val newsFlux: Flux<News>
}