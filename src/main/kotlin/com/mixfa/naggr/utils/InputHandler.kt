package com.mixfa.naggr.utils

val noHandlerFoundError = Throwable("No input handler hound")

interface InputHandler<T, R> {
    fun test(input: T): Boolean
    fun handle(input: T): R
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

    override fun handle(input: T): R {
        return handler.invoke(input)
    }

    override fun handleError(throwable: Throwable) {
        errorHandler?.invoke(throwable)
    }
}

fun <T, R> Iterable<InputHandler<T, R>>.handle(upd: T): Result<R> {
    for (handler in this) {
        if (handler.test(upd)) {
            try {
                return Result.success(handler.handle(upd)
            } catch (ex: Throwable) {
                handler.handleError(ex)
                return Result.failure(ex)
            }
        }
    }
    return Result.failure(noHandlerFoundError)
}

