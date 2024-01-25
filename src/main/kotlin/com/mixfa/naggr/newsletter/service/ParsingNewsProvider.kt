package com.mixfa.naggr.newsletter.service

import com.mixfa.naggr.newsletter.model.News
import reactor.core.publisher.Flux
import java.time.Duration

interface ParsingNewsProvider : NewsProvider {
    override val newsFlux: Flux<News>
        get() = Flux.generate {
            val newsOptional = parseNews()
            it.next(newsOptional)
        }
            .delaySequence(Duration.ofMinutes(5))
            .mapNotNull { it }

    fun parseNews(): News?
}