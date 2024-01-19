package com.mixfa.naggr.webCli

import com.mixfa.naggr.telegram.service.TelegramNewsBotService
import com.mixfa.naggr.webCli.request.InputCommand
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.TimeUnit


@Component
final class CliService(
    private val telegramNewsBotService: TelegramNewsBotService
) {
    private val cliHandlers = mutableMapOf<UUID, ExpirableCliHandler>()

    fun generateIdentifier(): UUID {
        val uuid = UUID.randomUUID()
        cliHandlers[uuid] = ExpirableCliHandler(15, TimeUnit.MINUTES, telegramNewsBotService)
        return uuid
    }

    fun forwardCommand(inputCommand: InputCommand): String {
        val cliHandler = cliHandlers[inputCommand.identifier] ?: return "Not identified"

        if (cliHandler.isExpired()) {
            cliHandlers.remove(inputCommand.identifier)
            return "Your session expired"
        }
        cliHandler.renew()
        return cliHandler.handle(inputCommand.command)
    }

    @Scheduled(fixedRate = 30000)
    fun checkExpiration() {
        for ((uuid, cliHandler) in cliHandlers.entries)
            if (cliHandler.isExpired()) cliHandlers.remove(uuid)
    }
}