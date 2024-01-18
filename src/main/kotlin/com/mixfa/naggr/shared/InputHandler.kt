package com.mixfa.naggr.shared

import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.telegram.telegrambots.meta.api.objects.Update

//object TelegramUpdatePredicates {
//    fun byMessageText(targetText: String): (Update) -> Boolean {
//        return func@{ upd ->
//            val message = upd.message ?: return@func false
//            val text = message.text ?: return@func false
//
//            return@func text.contentEquals(targetText)
//        }
//    }
//}


interface InputHandler<T, R> {
    fun test(input: T): Boolean
    fun handle(event: T): R
    fun handleError(throwable: Throwable)
}

open class LambdaInputHandler<T, R>(
    private val predicate: (T) -> Boolean,
    private val handler: (T) -> R,
    private val errorHandler: ((Throwable) -> Unit)? = null
) : InputHandler<T, R> {

    override fun test(input: T): Boolean {
        return predicate.invoke(input)
    }

    override fun handle(event: T): R {
        return handler.invoke(event)
    }

    override fun handleError(throwable: Throwable) {
        errorHandler?.invoke(throwable)
    }
}

fun <T, R> Iterable<InputHandler<T, R>>.handle(upd: T): Result<R> {
    for (handler in this) {
        if (handler.test(upd)) {
            try {
                return Result.success(handler.handle(upd))
            } catch (ex: Throwable) {
                handler.handleError(ex)
                return Result.failure(ex)
            }
        }
    }
    return Result.failure(Throwable("No handler found"))
}

