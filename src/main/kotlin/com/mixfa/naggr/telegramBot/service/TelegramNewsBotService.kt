package com.mixfa.naggr.telegramBot.service

import com.mixfa.naggr.newsletter.model.News
import com.mixfa.naggr.newsletter.model.flagsSet
import com.mixfa.naggr.newsletter.service.NewsletterService
import com.mixfa.naggr.telegramBot.model.TelegramNewsSubscriber
import com.mixfa.naggr.utils.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllChatAdministrators
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

private object TelegramUpdatePredicates {
    fun byChatMemberAdded(targetUser: User): (Update) -> Boolean {
        return func@{ upd ->
            return@func upd.message?.newChatMembers?.any { it.id == targetUser.id } ?: false
        }
    }

    fun byChatMemberRemoved(targetUser: User): (Update) -> Boolean {
        return func@{ upd ->
            return@func upd.message?.leftChatMember?.id == targetUser.id
        }
    }
}

private class TelegramLambdaCommandHandler(
    private val command: String,
    private val handler: (Update, List<String>) -> Unit,
    var splitArgs: Boolean = true,
    private val errorHandler: ((Throwable) -> Unit)? = null
) : InputHandler<Update, Unit> {

    override fun test(input: Update): Boolean {
        val message = input.message ?: return false

        return if (message.isGroupMessage)
            message.text?.substringBefore('@')?.startsWith(command) ?: false
        else
            message.text?.startsWith(command) ?: false
    }

    override fun handle(input: Update) {
        val args = if (splitArgs) input.message.text.split(' ').drop(1) else emptyList()
        handler.invoke(input, args)
    }

    override fun handleError(throwable: Throwable) {
        errorHandler?.invoke(throwable)
    }
}


@Service
final class TelegramNewsBotService(
    telegramBotsApi: TelegramBotsApi,
    newsletterService: NewsletterService,
    private val newsSubscribersRepository: TelegramSubscribersRepository,
    @Value("\${telegrambot.username}") private val username: String,
    @Value("\${telegrambot.token}") private val token: String
) : TelegramLongPollingBot(token) {
    private val inputHandlers: List<InputHandler<Update, Unit>>

    init {
        telegramBotsApi.registerBot(this)

        val setMyCommands = SetMyCommands(
            listOf(
                BotCommand("start", "Start receiving newsletter"),
                BotCommand(
                    "set_flags",
                    "set news tags you want to receive, available tags are: ${News.Flag.flagsList}"
                ),
            ), BotCommandScopeAllChatAdministrators(), null
        )
        executeAsync(setMyCommands)

        val telegramBotUser = me
        inputHandlers = listOf(
            LambdaInputHandler(
                TelegramUpdatePredicates.byChatMemberAdded(telegramBotUser),
                this::handleBotAddedToChat
            ),
            LambdaInputHandler(
                TelegramUpdatePredicates.byChatMemberRemoved(telegramBotUser),
                this::handleBotRemovedFromChat
            ),
            TelegramLambdaCommandHandler(
                "/set_flags", this::handleSetFlags
            ),
            TelegramLambdaCommandHandler(
                "/start", this::handleStart, splitArgs = false
            )
        )

        newsletterService.newsFlux
            .bufferTimeout(3, Duration.ofMinutes(5))
            .onErrorContinue { throwable, obj ->
                println(throwable.localizedMessage)
                println(obj)
            }.subscribe(this::handleNews)
    }

    private fun handleStart(upd: Update, args: List<String>) {
        val chatId = upd.message.chatId

        newsSubscribersRepository
            .existsByChatId(chatId)
            .subscribe { exists ->
                if (!exists)
                    newsSubscribersRepository.save(TelegramNewsSubscriber(chatId = chatId)).subscribe()
                executeAsync(SendMessage(chatId.toString(), "Added to newsletter list"))
                    .exceptionally { _ ->
                        removeFromNewsletterByChatId(chatId)
                        null
                    }
            }
    }

    private fun handleBotAddedToChat(upd: Update) {
        val chatId = upd.message?.chatId ?: return
        newsSubscribersRepository.save(TelegramNewsSubscriber(chatId = chatId)).subscribe()
    }

    private fun handleBotRemovedFromChat(upd: Update) {
        val chatId = upd.message?.chatId ?: return
        newsSubscribersRepository.deleteByChatId(chatId).subscribe()
    }

    private fun handleSetFlags(upd: Update, args: List<String>) {
        val usageMessage = "Usage: /set_flags [flags...]"
        val chatId = upd.message.chatId

        if (args.isEmpty()) {
            executeAsync(SendMessage(chatId.toString(), usageMessage))
            return
        }

        newsSubscribersRepository
            .findByChatId(chatId)
            .switchIfEmpty(Mono.error(EmptyMonoError))
            .onErrorComplete { error ->
                executeAsync(SendMessage(chatId.toString(), "Your chat is not in newsletter yet"))
                error == EmptyMonoError
            }.subscribe {
                val newFlags = args.mapNotNull { arg ->
                    try {
                        News.Flag.valueOf(arg.uppercase())
                    } catch (ex: Exception) {
                        null
                    }
                }

                it.targetFlags = newFlags

                newsSubscribersRepository.save(it).subscribe {
                    executeAsync(SendMessage(chatId.toString(), "Newsletter flags changed to $newFlags"))
                }
            }
    }

    private fun handleNews(newsList: List<News>) {
        val allFlags = newsList.flagsSet()

        val newsMessages = newsList
            .map { newsEl ->
                newsEl to SendMessage.builder()
                    .text(newsEl.textForTelegram)
                    .chatId(0)
                    .build()
            }

        newsSubscribersRepository
            .findAllByTargetFlagsContaining(allFlags, defaultPageable)
            .onErrorContinue { throwable, obj ->
                println(throwable.localizedMessage)
                println(obj)
            }
            .publishOn(Schedulers.parallel())
            .subscribe { subscriber ->
                for ((news, message) in newsMessages) {
                    if (subscriber.targetFlags.none { news.flags.contains(it) })
                        continue

                    message.setChatId(subscriber.chatId)
                    executeAsync(message).exceptionally { _ ->
                        removeFromNewsletterByChatId(subscriber.chatId)
                        null
                    }
                }
            }
    }

    private fun removeFromNewsletterByChatId(chatId: Long) {
        newsSubscribersRepository.deleteByChatId(chatId).subscribe()
    }

    override fun getBotUsername(): String = username

    override fun onUpdateReceived(update: Update) {
        inputHandlers.handle(update)
    }


}

