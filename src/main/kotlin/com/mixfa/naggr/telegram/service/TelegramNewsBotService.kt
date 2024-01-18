package com.mixfa.naggr.telegram.service

import com.mixfa.naggr.news.model.Flag
import com.mixfa.naggr.news.model.News
import com.mixfa.naggr.news.service.NewsService
import com.mixfa.naggr.shared.InputHandler
import com.mixfa.naggr.shared.LambdaInputHandler
import com.mixfa.naggr.shared.handle
import com.mixfa.naggr.telegram.model.TelegramNewsSubscriber
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

private object TelegramUpdatePredicates {
    fun byMessageText(targetText: String): (Update) -> Boolean {
        return func@{ upd ->
            val message = upd.message ?: return@func false
            val text = message.text ?: return@func false

            return@func text.contentEquals(targetText)
        }
    }
}

@Component
final class TelegramNewsBotService(
    telegramBotsApi: TelegramBotsApi,
    private val newsSubscribersRepository: TgSubscribersRepository,
    newsService: NewsService,
    @Value("\${telegrambot.username}") private val username: String,
    @Value("\${telegrambot.token}") private val token: String
) : TelegramLongPollingBot(token) {
    private val inputHandlers: List<InputHandler<Update, Unit>>

    init {
        telegramBotsApi.registerBot(this)
        inputHandlers = listOf(
            LambdaInputHandler(TelegramUpdatePredicates.byMessageText("/news_aggregator"), this::handleTelegramCmd),
        )

        newsService.newsFlux.subscribe(this::handleNews)
    }

    private fun handleTelegramCmd(upd: Update) {
        val chatId = upd.message.chatId
        val sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text("Your chat id is $chatId, use it to setup bot")
            .build()

        executeAsync(sendMessage)
    }

    fun setSubscriberTargetFlags(chatId: Long, flags: List<Flag>) {
        newsSubscribersRepository.findByChatId(chatId)
            .subscribe {
                it.targetFlags = flags
                newsSubscribersRepository.save(it)
                    .subscribe {
                        val sendMessage = SendMessage
                            .builder()
                            .chatId(chatId)
                            .text("Flags changed to $flags")
                            .build()

                        executeAsync(sendMessage)
                    }
            }
    }

    fun subscribeChannel(chatId: Long) {
        newsSubscribersRepository.findByChatId(chatId)
            .publishOn(Schedulers.boundedElastic())
            .switchIfEmpty(Mono.error(Throwable("Not found")))
            .doOnError {
                val replyMessage =
                    SendMessage(
                        chatId.toString(),
                        "Subscribed to news with all flags"
                    )
                executeAsync(replyMessage)
                newsSubscribersRepository.save(
                    TelegramNewsSubscriber(chatId = chatId, targetFlags = Flag.entries)
                ).subscribe()

            }
            .onErrorComplete()
            .subscribe {
                newsSubscribersRepository.delete(it).subscribe()
                val replyMessage = SendMessage(chatId.toString(), "Unsubscribed from news")
                executeAsync(replyMessage)
            }
    }

    private fun handleNews(news: News) {
        val sendPhoto = SendPhoto.builder()
            .caption(news.caption)
            .photo(InputFile(news.imageRef))
            .chatId(0)
            .build()

        newsSubscribersRepository
            .findAllByTargetFlagsContaining(news.flags)
            .subscribe {
                sendPhoto.chatId = it.chatId.toString()
                execute(sendPhoto)
            }

    }


    override fun getBotUsername(): String = username

    override fun onUpdateReceived(update: Update) {
        inputHandlers.handle(update)
    }
}