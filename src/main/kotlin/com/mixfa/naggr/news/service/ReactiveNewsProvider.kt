package com.mixfa.naggr.news.service

import com.mixfa.naggr.news.model.News
import reactor.core.publisher.Flux

/*
    Reactive news providers must provide flux, where they will push news objects
*/
interface ReactiveNewsProvider {
    val newsFlux: Flux<News>
}