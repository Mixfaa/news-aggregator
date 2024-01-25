package com.mixfa.naggr.newsletter.service

import com.mixfa.naggr.newsletter.model.News
import reactor.core.publisher.Flux

/**
    News providers must provide flux, where they will push news objects
*/
interface NewsProvider {
    val newsFlux: Flux<News>
}