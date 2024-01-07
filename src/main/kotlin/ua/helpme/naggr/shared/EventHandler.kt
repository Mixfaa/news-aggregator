package ua.helpme.naggr.shared

import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.telegram.telegrambots.meta.api.objects.Update

object TelegramUpdatePredicates {
    fun byMessageText(targetText: String): (Update) -> Boolean {
        return func@{ upd ->
            val message = upd.message ?: return@func false
            val text = message.text ?: return@func false

            return@func text.contentEquals(targetText)
        }
    }
}

object DiscordEventPredicates {
    fun byFullCommandName(targetName: String): (GenericCommandInteractionEvent) -> Boolean {
        return func@{ event ->
            return@func targetName.contentEquals(event.fullCommandName)
        }
    }
}

class EventHandler<T>(
    val predicate: (T) -> Boolean, val handler: (T) -> Unit, val errorHandler: ((Throwable) -> Unit)? = null
)

fun <T> Collection<EventHandler<T>>.handleEvent(upd: T) {
    for (updateHandler in this) {
        if (updateHandler.predicate(upd)) {
            try {
                updateHandler.handler(upd)
            } catch (ex: Exception) {
                updateHandler.errorHandler?.invoke(ex)
            }
            break
        }
    }
}

