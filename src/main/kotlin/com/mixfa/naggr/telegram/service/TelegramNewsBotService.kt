package com.mixfa.naggr.telegram.service

import com.mixfa.naggr.news.model.Flag
import com.mixfa.naggr.news.model.News
import com.mixfa.naggr.news.service.NewsService
import com.mixfa.naggr.telegram.model.TelegramNewsSubscriber
import com.mixfa.naggr.utils.EmptyMonoError
import com.mixfa.naggr.utils.InputHandler
import com.mixfa.naggr.utils.LambdaInputHandler
import com.mixfa.naggr.utils.handle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllChatAdministrators
import reactor.core.publisher.Mono

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

class TelegramLambdaCommandHandler(
    private val command: String,
    private val handler: (Update, List<String>) -> Unit,
    var parseArgs: Boolean = true,
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
        val args = if (parseArgs) input.message.text.split(' ').drop(1) else emptyList()

        handler.invoke(input, args)
    }

    override fun handleError(throwable: Throwable) {
        errorHandler?.invoke(throwable)
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

        val setMyCommands = SetMyCommands(
            listOf(
                BotCommand("start", "Start receiving newsletter"),
                BotCommand("set_flags", "set news tags you want to receive, available tags are: ${Flag.flagsList}"),
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
                "/start", this::handleStart, parseArgs = false
            )
        )

        newsService.newsFlux.share().onErrorContinue { throwable, obj ->
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

        newsSubscribersRepository.findByChatId(chatId).switchIfEmpty(Mono.error(EmptyMonoError))
            .onErrorComplete { error ->
                executeAsync(SendMessage(chatId.toString(), "Your chat is not in newsletter yet"))
                error == EmptyMonoError
            }.subscribe {
                val newFlags = args.asSequence().mapNotNull { arg ->
                    try {
                        Flag.valueOf(arg.uppercase())
                    } catch (ex: Exception) {
                        null
                    }
                }.toList()

                it.targetFlags = newFlags

                newsSubscribersRepository.save(it).subscribe {
                    executeAsync(SendMessage(chatId.toString(), "Newsletter flags changed to $newFlags"))
                }
            }
    }

    private fun handleNews(news: News) {

        val sendRequest = SendMessage.builder().text(news.link).chatId(0).build()

        newsSubscribersRepository
            .findAllByTargetFlagsContaining(news.flags)
            .onErrorContinue { throwable, obj ->
                println(throwable.localizedMessage)
                println(obj)
            }.subscribe {
                sendRequest.setChatId(it.chatId)
                executeAsync(sendRequest).exceptionally { _ ->
                    newsSubscribersRepository.deleteByChatId(it.chatId).subscribe()
                    null
                }
            }
    }

    override fun getBotUsername(): String = username

    override fun onUpdateReceived(update: Update) {
        inputHandlers.handle(update)
    }
}


/*

    private fun handleDisableNewsletter(upd: Update, args: List<String>) {
        val usageMessage = "Usage: /disable_this_chat [id]"

        if (args.isEmpty()) {
            val sendMessage = SendMessage.builder().chatId(upd.message.chatId).text(usageMessage).build()

            executeAsync(sendMessage)
            return
        }

        val subscriberId = args[0]

        newsSubscribersRepository.existsById(subscriberId).subscribe { exist ->
            val responseMessage = SendMessage.builder().chatId(upd.message.chatId).text("").build()

            if (exist) {
                newsSubscribersRepository.deleteById(subscriberId).subscribe {
                    responseMessage.text = "Chat removed from newsletter"
                    executeAsync(responseMessage)
                }
            } else {
                responseMessage.text = "No chat found for $subscriberId"
                executeAsync(responseMessage)
            }
        }
    }

    private fun handleTelegramCmd(upd: Update) {
        val chatId = upd.message.chatId

        newsSubscribersRepository.findByChatId(chatId).switchIfEmpty(Mono.error(EmptyMonoError))
            .onErrorComplete { error ->
                newsSubscribersRepository.save(TelegramNewsSubscriber(chatId = chatId, targetFlags = Flag.entries))
                    .subscribe {
                        val sendMessage =
                            SendMessage.builder().chatId(chatId).text("Your id is: ${it.id}, use it manage chat")
                                .build()

                        executeAsync(sendMessage)
                    }
                error == EmptyMonoError
            }.subscribe {
                val sendMessage =
                    SendMessage.builder().chatId(chatId).text("Your id is: ${it.id}, use it manage chat").build()

                executeAsync(sendMessage)
            }
    }

 */