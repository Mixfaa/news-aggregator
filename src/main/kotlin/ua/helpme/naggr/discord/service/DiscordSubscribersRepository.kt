package ua.helpme.naggr.discord.service

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import ua.helpme.naggr.discord.model.DiscordNewsSubscriber

@Repository
interface DiscordSubscribersRepository : ReactiveMongoRepository<DiscordNewsSubscriber, String> {
    fun findByChannelId(channelId: Long): Mono<DiscordNewsSubscriber>
}