package com.mixfa.naggr.news.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import com.mixfa.naggr.news.providers.NewsProvider

@Service
class NewsService(newsProviders: List<NewsProvider>) {
    var newsFlux =
        Flux.concat(newsProviders.map(NewsProvider::newsFlux)).subscribeOn(Schedulers.boundedElastic()).share()
            .log()
//
//    @Scheduled(fixedRate = 1000000)
//    fun keepAlive() {
//        println(System.currentTimeMillis())
//    }

//    @PostConstruct
//    fun initialize() {
//        newsFlux.subscribe(::println)
//        newsFlux.subscribe {
//            println("Hel $it")
//        }
//    }
}