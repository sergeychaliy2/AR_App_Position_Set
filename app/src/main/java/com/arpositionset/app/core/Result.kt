package com.arpositionset.app.core

/**
 * Explicit result wrapper used by the domain layer so callers can discriminate
 * between success, failure, and progress without relying on exceptions.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: Throwable, val message: String? = null) : Outcome<Nothing>
    data class Progress(val percent: Float) : Outcome<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun valueOrNull(): T? = (this as? Success)?.value
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
    is Outcome.Progress -> this
}

inline fun <T> Outcome<T>.onSuccess(block: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) block(value)
    return this
}

inline fun <T> Outcome<T>.onFailure(block: (Throwable) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) block(error)
    return this
}
