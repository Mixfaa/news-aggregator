package com.mixfa.naggr

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession


@SpringBootApplication
@EnableScheduling
@EnableReactiveMongoRepositories
@Configuration
class NaggrApplication {
    @Bean
    fun telegramBotsApi(@Value("\${telegrambot.token}") token: String): TelegramBotsApi {
        return TelegramBotsApi(DefaultBotSession::class.java)
    }
//
//    @Bean
//    fun redisTelegramSubscribersTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<TelegramNewsSubscriber, String> {
//        val serializationContext =
//            RedisSerializationContext.newSerializationContext<TelegramNewsSubscriber, String>(StringRedisSerializer())
//                .hashKey(StringRedisSerializer())
//                .hashValue(StringRedisSerializer())
//                .build()
//
//        return ReactiveRedisTemplate(factory, serializationContext)
//            .also {
//                it.connectionFactory.reactiveConnection.serverCommands().flushAll().subscribe()
//            }
//    }


}

fun main(args: Array<String>) {
    runApplication<NaggrApplication>(*args)
}
