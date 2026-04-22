package com.arpositionset.app.domain.model

/**
 * Where the model bytes come from. Keeps load strategies decoupled from the
 * rest of the domain so repositories can route requests accordingly.
 */
sealed interface ObjectSource {
    data object BuiltIn : ObjectSource
    data class Imported(val localPath: String) : ObjectSource
    data class Cloud(val remoteUrl: String, val cachedPath: String? = null) : ObjectSource
}
