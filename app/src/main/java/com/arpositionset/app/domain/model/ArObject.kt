package com.arpositionset.app.domain.model

/**
 * Pure domain entity representing a 3D object available for placement.
 * The AR/UI layers convert this into their own renderable/model representations.
 */
data class ArObject(
    val id: String,
    val name: String,
    val description: String,
    val source: ObjectSource,
    val modelUri: String,
    val previewUri: String?,
    val previewEmoji: String = "📦",
    val category: ObjectCategory,
    val defaultScale: Float = 1f,
    val sizeBytes: Long? = null,
    /**
     * Optional pivot override in normalised model space: [-1..1] per axis,
     * where (0,0,0) = bbox centre, (0,-1,0) = bbox bottom-centre etc.
     * Used by the AR layer to place the chosen pivot point exactly on the
     * anchor. `null` = keep the glTF's authored pivot (current BoomBox/Avocado
     * behaviour — they sit on the floor naturally; Duck / generic Box do not).
     */
    val pivotOverride: PivotPoint? = null,
)

data class PivotPoint(val x: Float, val y: Float, val z: Float) {
    companion object {
        val BottomCenter = PivotPoint(0f, -1f, 0f)
        val Center = PivotPoint(0f, 0f, 0f)
    }
}

enum class ObjectCategory(val displayName: String) {
    Furniture("Мебель"),
    Decor("Декор"),
    Tech("Техника"),
    Nature("Природа"),
    Art("Арт"),
    Imported("Импорт"),
    Cloud("Облако");

    companion object {
        fun fromStorageKey(key: String): ObjectCategory =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: Decor
    }
}
