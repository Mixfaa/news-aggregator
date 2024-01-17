package ua.helpme.naggr.telegram.service

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import ua.helpme.naggr.telegram.model.TelegramNewsSubscriber

@Repository
interface TgSubscribersRepository : ReactiveMongoRepository<TelegramNewsSubscriber, String> {
    fun findByChatId(chatId: Long): Mono<TelegramNewsSubscriber>
}