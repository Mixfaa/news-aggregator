package com.mixfa.naggr.news.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

/*
    Service merges all reactive providers to one flux, and then receiver services subscribe to it
 */
@Service
class NewsletterService(newsProviders: List<ReactiveNewsProvider>) {
    init {
        val logger = LoggerFactory.getLogger(this.javaClass)

        newsProviders.forEach {
            logger.info("New news provider: ${it.javaClass}")
        }
    }

    val newsFlux = Flux.merge(newsProviders.map { it.newsFlux.subscribeOn(Schedulers.boundedElastic()) }).share()

    @Scheduled(fixedRate = 1000 * 60 * 60)
    private fun keepAlive() {
    }
}