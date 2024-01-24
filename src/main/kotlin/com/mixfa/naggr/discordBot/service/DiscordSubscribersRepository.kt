package com.mixfa.naggr.discordBot.service

import com.mixfa.naggr.discordBot.model.DiscordSubscriber
import com.mixfa.naggr.news.model.News
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface DiscordSubscribersRepository : ReactiveMongoRepository<DiscordSubscriber, String> {
    fun findByChannelId(channelId: Long): Mono<DiscordSubscriber>
    fun findAllByTargetFlagsContaining(
        flags: Iterable<News.Flag>,
        page: Pageable
    ): Flux<DiscordSubscriber>
}