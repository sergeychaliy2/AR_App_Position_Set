package com.arpositionset.app.domain.model

/**
 * Presentation-agnostic transform applied to a placed object.
 * Translation stays in the AR layer (backed by an anchor); this holds
 * only the deltas that the user controls directly.
 */
data class TransformState(
    val scale: Float = 1f,
    val rotationYDegrees: Float = 0f,
) {
    fun withClampedScale(min: Float, max: Float): TransformState =
        copy(scale = scale.coerceIn(min, max))

    fun withRotationDelta(delta: Float): TransformState =
        copy(rotationYDegrees = (rotationYDegrees + delta).mod(360f))

    companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 4.0f
        val Default = TransformState()
    }
}
