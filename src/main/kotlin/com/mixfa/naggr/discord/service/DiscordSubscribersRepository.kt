package com.mixfa.naggr.discord.service

import com.mixfa.naggr.discord.model.DiscordNewsSubscriber
import com.mixfa.naggr.news.model.Flag
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface DiscordSubscribersRepository : ReactiveMongoRepository<DiscordNewsSubscriber, String> {
    fun findByChannelId(channelId: Long): Mono<DiscordNewsSubscriber>
    fun findAllByTargetFlagsContaining(flags: List<Flag>): Flux<DiscordNewsSubscriber>
}