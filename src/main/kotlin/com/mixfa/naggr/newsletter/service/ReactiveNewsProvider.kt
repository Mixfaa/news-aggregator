package com.mixfa.naggr.newsletter.service

import com.mixfa.naggr.newsletter.model.News
import reactor.core.publisher.Flux

/**
    Reactive news providers must provide flux, where they will push news objects
*/
interface ReactiveNewsProvider {
    val newsFlux: Flux<News>
}