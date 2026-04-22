package com.arpositionset.app.data.local

import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.model.ObjectCategory
import com.arpositionset.app.domain.model.ObjectSource
import com.arpositionset.app.domain.model.PivotPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seed list of gallery models.
 *
 * Built-in models are bundled in `assets/models/` (pulled from the Khronos glTF
 * Sample Models repository — CC-BY / CC0). Cloud models are kept as an explicit
 * remote catalog so users can test the download flow without touching assets.
 */
@Singleton
class GalleryCatalog @Inject constructor() {

    // defaultScale is applied to the model's NATIVE size (we don't normalise
    // via scaleToUnits). Values chosen experimentally: some Khronos samples
    // are authored in metres, some in centimetres or millimetres, so the
    // multipliers look wildly different.
    val builtInObjects: List<ArObject> = listOf(
        // Primitive cube — explicit bottom-centre pivot proves the placement
        // pipeline works regardless of authored glTF origin.
        builtin("builtin:cube", "Куб", "Примитив — демонстрация корректного размещения",
            "Box.glb", ObjectCategory.Decor, 0.30f, "🟥", pivot = PivotPoint.BottomCenter),
        // Duck native origin is at body centre → override to bottom so it
        // stands on the floor instead of sinking.
        builtin("builtin:duck", "Утка", "Классическая жёлтая утка",
            "Duck.glb", ObjectCategory.Decor, 0.25f, "🦆", pivot = PivotPoint.BottomCenter),
        builtin("builtin:fox", "Лиса", "Анимированная модель лисы",
            "Fox.glb", ObjectCategory.Nature, 0.01f, "🦊", pivot = PivotPoint.BottomCenter),
        // Avocado / WaterBottle / BoomBox / Lantern are authored with origin
        // at base already — no override needed.
        builtin("builtin:avocado", "Авокадо", "Реалистичный PBR-фрукт",
            "Avocado.glb", ObjectCategory.Nature, 8f, "🥑"),
        builtin("builtin:waterbottle", "Бутылка воды", "Прозрачная пластиковая бутылка",
            "WaterBottle.glb", ObjectCategory.Decor, 2f, "💧"),
        builtin("builtin:boombox", "Бумбокс", "Винтажная кассетная колонка",
            "BoomBox.glb", ObjectCategory.Tech, 8f, "📻"),
        builtin("builtin:lantern", "Фонарь", "Декоративный уличный фонарь",
            "Lantern.glb", ObjectCategory.Decor, 0.05f, "🏮"),
    )

    val cloudCatalog: List<ArObject> = listOf(
        cloud("cloud:damagedHelmet", "Повреждённый шлем", "Детализированный шлем с PBR-материалом",
            "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/main/2.0/DamagedHelmet/glTF-Binary/DamagedHelmet.glb",
            ObjectCategory.Art, 0.5f, 3_800_000, "🪖"),
        cloud("cloud:flightHelmet", "Лётный шлем", "Высокодетализированный шлем пилота",
            "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/main/2.0/FlightHelmet/glTF-Binary/FlightHelmet.glb",
            ObjectCategory.Art, 0.6f, 18_300_000, "✈️"),
        cloud("cloud:sponza", "Сцена Sponza", "Архитектурная сцена (тяжёлая)",
            "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/main/2.0/Sponza/glTF-Binary/Sponza.glb",
            ObjectCategory.Art, 0.3f, 22_000_000, "🏛️"),
        cloud("cloud:cesiumMan", "Cesium Man", "Анимированный персонаж",
            "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/main/2.0/CesiumMan/glTF-Binary/CesiumMan.glb",
            ObjectCategory.Art, 1f, 500_000, "🚶"),
    )

    private fun builtin(
        id: String, name: String, description: String, file: String,
        category: ObjectCategory, scale: Float, emoji: String,
        pivot: PivotPoint? = null,
    ) = ArObject(
        id = id,
        name = name,
        description = description,
        source = ObjectSource.BuiltIn,
        modelUri = "file:///android_asset/models/$file",
        previewUri = null,
        previewEmoji = emoji,
        category = category,
        defaultScale = scale,
        pivotOverride = pivot,
    )

    private fun cloud(
        id: String, name: String, description: String, url: String,
        category: ObjectCategory, scale: Float, sizeBytes: Long, emoji: String,
    ) = ArObject(
        id = id,
        name = name,
        description = description,
        source = ObjectSource.Cloud(remoteUrl = url),
        modelUri = url,
        previewUri = null,
        previewEmoji = emoji,
        category = category,
        defaultScale = scale,
        sizeBytes = sizeBytes,
    )
}
