package com.mixfa.naggr.news.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

/**
Service merges all reactive providers to one flux, and then receiver services subscribe to it
 */
@Service
class NewsletterService(
    newsProviders: List<ReactiveNewsProvider>,
    newsExtenders: MutableList<NewsDataExtender>
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        newsProviders.forEach {
            logger.info("New news provider: ${it.javaClass}")
        }
    }

    val newsFlux = Flux
        .merge(newsProviders.map { it.newsFlux.subscribeOn(Schedulers.boundedElastic()) })
        .doOnNext { news ->
            newsExtenders.forEach { extender ->
                try {
                    extender.extend(news)
                } catch (ex: Exception) {
                    logger.error(ex.message)
                    logger.info("Extender $extender will not be called anymore")

                    newsExtenders.remove(extender)
                }
            }
        }
        .share()

    @Scheduled(fixedRate = 1000 * 60 * 60)
    private fun keepAlive() {
    }
}