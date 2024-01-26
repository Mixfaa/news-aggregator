package com.mixfa.naggr

import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.service.OpenAiService
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import java.time.Duration

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
        return JDABuilder.createDefault(token).build()
    }

    @Bean
    fun openAiService(
        @Value("\${aiprovider.apikey}") aiApiKey: String,
        @Value("\${aiprovider.baseurl}") aiBaseurl: String,
    ): OpenAiService {
        val objectMapper = OpenAiService.defaultObjectMapper()
        val httpClient = OpenAiService.defaultClient(aiApiKey, Duration.ofMinutes(5))

        val retrofit = Retrofit.Builder().client(httpClient).baseUrl(aiBaseurl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build()

        val openAiApi = retrofit.create(OpenAiApi::class.java)
        return OpenAiService(openAiApi)
    }

}

fun main(args: Array<String>) {
    runApplication<NaggrApplication>(*args)
}
