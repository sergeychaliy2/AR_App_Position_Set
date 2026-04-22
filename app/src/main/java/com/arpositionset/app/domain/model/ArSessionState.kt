package com.arpositionset.app.domain.model

/**
 * High-level session state consumed by the UI — never exposes ARCore enums
 * directly so presentation tests can run on JVM without AR dependencies.
 */
sealed interface ArSessionState {
    data object Initializing : ArSessionState
    data object RequiresInstall : ArSessionState
    data object Searching : ArSessionState
    data object Ready : ArSessionState
    data object TrackingLost : ArSessionState
    data class Unsupported(val reason: String) : ArSessionState
    data class Failed(val error: Throwable) : ArSessionState
}
