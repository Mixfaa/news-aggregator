package ua.helpme.naggr

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.utils.cache.CacheFlag
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

    @Bean
    fun discordBot(@Value("\${discordbot.token}") token: String): JDA {
        val builder = JDABuilder.createDefault(token)
//        builder.setBulkDeleteSplittingEnabled(false)
        builder.setActivity(Activity.customStatus("сосет крипто-тунца(хуйнца)"))

        return builder.build()
    }
}

fun main(args: Array<String>) {
    runApplication<NaggrApplication>(*args)
}
