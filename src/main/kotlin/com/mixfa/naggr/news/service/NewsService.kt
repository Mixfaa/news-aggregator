package com.mixfa.naggr.news.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

@Service
class NewsService(newsProviders: List<NewsProvider>) {
    init {
        val logger = LoggerFactory.getLogger(this.javaClass)

        newsProviders.forEach {
            logger.info("New news provider: ${it.javaClass}")
        }
    }

    var newsFlux =
        Flux.merge(newsProviders.map { it.newsFlux.subscribeOn(Schedulers.boundedElastic()) })
}