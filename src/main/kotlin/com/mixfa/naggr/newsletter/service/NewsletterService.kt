package com.mixfa.naggr.newsletter.service

import com.mixfa.naggr.newsletter.model.News
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

/**
 * Service merges all reactive providers to one flux, and then receiver services subscribe to it
 * */
@Service
class NewsletterService(
    newsProviders: List<NewsProvider>,
    private val newsExtenders: MutableList<NewsDataExtender>
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        newsProviders.forEach {
            logger.info("New news provider: ${it.javaClass}")
        }
    }

    val newsFlux = Flux
        .merge(newsProviders.map { it.newsFlux.subscribeOn(Schedulers.boundedElastic()) })
        .doOnNext(::executeExtenders)
        .share()

    private fun executeExtenders(news: News) {
        for (extender in newsExtenders) {
            try {
                extender.extend(news)
            } catch (ex: Exception) {
                logger.error(ex.message)
                logger.info("Extender $extender will not be executed anymore")

                newsExtenders.remove(extender)
            }
        }
    }

    @Scheduled(fixedRate = 1000 * 60 * 60)
    private fun keepAlive() {
    }
}