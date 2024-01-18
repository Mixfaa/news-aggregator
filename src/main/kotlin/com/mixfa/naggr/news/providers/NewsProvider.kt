package com.mixfa.naggr.news.providers

import reactor.core.publisher.Flux
import com.mixfa.naggr.news.model.News

interface NewsProvider {
    val newsFlux: Flux<News>
}