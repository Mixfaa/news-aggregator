package ua.helpme.naggr.telegram.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
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
import ua.helpme.naggr.news.service.NewsService
import ua.helpme.naggr.news.model.News
import ua.helpme.naggr.shared.EventHandler
import ua.helpme.naggr.shared.TelegramUpdatePredicates
import ua.helpme.naggr.shared.handleEvent
import ua.helpme.naggr.telegram.model.TelegramNewsSubscriber

@Component
class TelegramNewsBotService(
    private val telegramBotsApi: TelegramBotsApi,
    private val newsSubscribersRepository: TgSubscribersRepository,
    private val newsService: NewsService,
    @Value("\${telegrambot.username}") private val username: String,
    @Value("\${telegrambot.token}") private val token: String
) : TelegramLongPollingBot(token) {
    private lateinit var eventHandlers: List<EventHandler<Update>>

    private fun handleReceiveNewsCmd(upd: Update) {
        val chatId = upd.message.chatId

        newsSubscribersRepository.findByChatId(chatId)
            .publishOn(Schedulers.boundedElastic())
            .switchIfEmpty(Mono.error(Throwable("Not found")))
            .doOnError {
                val replyMessage = SendMessage(chatId.toString(), "Subscribed to news")
                execute(replyMessage)
                newsSubscribersRepository.save(
                    TelegramNewsSubscriber(chatId = chatId)
                ).subscribe()

            }
            .onErrorComplete()
            .subscribe {
                newsSubscribersRepository.delete(it).subscribe()
                val replyMessage = SendMessage(chatId.toString(), "Unsubscribed from news")
                execute(replyMessage)
            }
    }

    private fun handleNews(news: News) {
        val sendPhoto = SendPhoto.builder()
            .caption(news.caption)
            .photo(InputFile(news.imageRef))
            .chatId(0)
            .build()

        newsSubscribersRepository.findAll().subscribe {
            sendPhoto.chatId = it.chatId.toString()
            execute(sendPhoto)
        }

    }

    @PostConstruct
    fun initialize() {
        telegramBotsApi.registerBot(this)
        eventHandlers = listOf(
            EventHandler(TelegramUpdatePredicates.byMessageText("/receive_news"), this::handleReceiveNewsCmd)
        )

        newsService.newsFlux.subscribe(this::handleNews)
    }

    override fun getBotUsername(): String = username

    override fun onUpdateReceived(update: Update) {
        eventHandlers.handleEvent(update)
    }
}