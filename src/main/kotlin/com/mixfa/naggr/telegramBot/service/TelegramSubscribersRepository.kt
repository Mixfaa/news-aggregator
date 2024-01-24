package com.mixfa.naggr.telegramBot.service

import com.mixfa.naggr.newsletter.model.News
import com.mixfa.naggr.telegramBot.model.TelegramNewsSubscriber
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface TelegramSubscribersRepository : ReactiveMongoRepository<TelegramNewsSubscriber, String> {
    fun findByChatId(chatId: Long): Mono<TelegramNewsSubscriber>
    fun findAllByTargetFlagsContaining(flags: Iterable<News.Flag>, page: Pageable): Flux<TelegramNewsSubscriber>
    fun deleteByChatId(chatId: Long): Mono<Void>
    fun existsByChatId(chatId: Long): Mono<Boolean>
}