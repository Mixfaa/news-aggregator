package com.mixfa.naggr.telegram.service

import com.mixfa.naggr.news.model.Flag
import com.mixfa.naggr.telegram.model.TelegramNewsSubscriber
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface TgSubscribersRepository : ReactiveMongoRepository<TelegramNewsSubscriber, String> {
    fun findByChatId(chatId: Long): Mono<TelegramNewsSubscriber>
    fun findAllByTargetFlagsContaining(flags: List<Flag>): Flux<TelegramNewsSubscriber>
    fun deleteByChatId(chatId: Long): Mono<Void>
    fun existsByChatId(chatId: Long): Mono<Boolean>
}