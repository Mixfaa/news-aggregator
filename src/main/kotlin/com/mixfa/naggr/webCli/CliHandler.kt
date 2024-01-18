package com.mixfa.naggr.webCli

import com.mixfa.naggr.news.model.Flag
import com.mixfa.naggr.shared.*
import com.mixfa.naggr.telegram.service.TelegramNewsBotService
import java.util.concurrent.TimeUnit

class ExpirableCliHandler(
    timeToExpire: Long,
    timeUnit: TimeUnit,
    telegramNewsBotService: TelegramNewsBotService,
) : CliHandler(telegramNewsBotService), Expirable by DefaultExpirableImpl(timeToExpire, timeUnit)

class CliInputHandlerWithArgs(
    private val targetCommand: String,
    private val handler: (List<String>) -> String,
    private val errorHandler: ((Throwable) -> Unit)? = null
) : InputHandler<String, String> {
    override fun test(input: String): Boolean = input.startsWith(targetCommand)
    override fun handleError(throwable: Throwable) {
        errorHandler?.invoke(throwable)
    }

    override fun handle(event: String): String {
        val args = event
            .substringAfter(targetCommand)
            .drop(1)
            .split(' ')

        return handler.invoke(args)
    }
}

class CliInputHandler(
    private val targetCommand: String,
    private val handler: () -> String,
    errorHandler: ((Throwable) -> Unit)? = null
) : LambdaInputHandler<String, String>({ it.startsWith(targetCommand) }, { handler.invoke() }, errorHandler)

/*
    All commands comes from one cli
*/
open class CliHandler(
    private val telegramNewsBotService: TelegramNewsBotService
) {
    private val commandHandlers = listOf(
        CliInputHandler("help", this::helpCmd),
        CliInputHandler("list-flags", this::listFlags),
        CliInputHandlerWithArgs("telegram-setup", this::setupTelegramBot),
        CliInputHandlerWithArgs("telegram-set-target-flags", this::setupTelegramTargetFlags)
    )

    private fun setupTelegramTargetFlags(args: List<String>): String {
        if (args.size < 2) return "Usage: telegram-set-target-flags [chat id] [flags...]"

        val chatId = args[0].toLongOrNull()
            ?: return "Usage: telegram-set-target-flags [chat id] [flags...]"

        val flags = args
            .asSequence()
            .drop(1)
            .mapNotNull {
                try {
                    Flag.valueOf(it.uppercase())
                } catch (_: Exception) {
                    null
                }
            }
            .toList()


        telegramNewsBotService.setSubscriberTargetFlags(chatId, flags)
        return "Request submitted, check telegram chat"
    }

    private fun setupTelegramBot(args: List<String>): String {
        if (args.size != 1) return "Usage: telegram-setup [chat id]"
        val chatId = args.first().toLongOrNull()
            ?: return "Usage: telegram-setup [chat id]"

        telegramNewsBotService.subscribeChannel(chatId)
        return "Request submitted, check telegram chat"
    }

    private fun helpCmd(): String {
        return buildString {
            appendLine("News aggregator bot setup cli, use it to setup your bots")
            appendLine("use list-flags to list available news flags to receive")
            appendLine("use telegram-setup to set telegram chat in which you want to receive news")
            appendLine("use telegram-set-target-flags to set news flag you want to receive")
        }
    }

    private fun listFlags() = Flag.flagsList

    fun handle(command: String): String {
        return commandHandlers.handle(command).recover { it.localizedMessage }.getOrNull()!!
    }
}