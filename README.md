# AR Position Set

Android-приложение дополненной реальности для размещения 3D-моделей на реальных поверхностях с привязкой к физическим маркерам для персистентности между сессиями.

---

## Возможности

- **AR-сцена** на базе ARCore + Filament-рендера (SceneView). Распознавание горизонтальных и вертикальных плоскостей, отслеживание 6DoF, равномерное освещение сцены несколькими directional-источниками.
- **Размещение объектов по тапу**: касание пола/стены → появляется «призрак» модели с подтверждением **Установить / Закрыть**.
- **Галерея 3D-моделей** (Material 3 ModalBottomSheet) с тремя вкладками:
  - **Библиотека** — встроенные `.glb` из `assets/models/` (куб, утка, лиса, авокадо, бутылка, бумбокс, фонарь).
  - **Облако** — модели из Khronos glTF Sample Models (DamagedHelmet, FlightHelmet, Sponza, CesiumMan). Скачиваются по тапу, прогресс-бар, локальный кэш в `filesDir/cloud_models/`.
  - **Импорт** — выбор `.glb`/`.gltf` из памяти устройства через SAF, копия в `filesDir/imported_models/`, запись в Room.
- **Трансформация установленных объектов**:
  - Выделение тапом → выезжает `TransformSheet` с слайдерами масштаба (x0.1 — x4.0) и поворота по Y (0° — 360°).
  - Точные сдвиги по осям **X/Y/Z** кнопками ±5 см (цветная индикация по фирменной палитре: X красный, Y светло-зелёный, Z голубой).
  - Режим «Переместить» — следующий тап по поверхности переанкорит объект в новую точку.
  - Удаление кнопкой-корзиной.
- **Персистентность через AugmentedImages** (без облака, без интернета):
  - Пользователь регистрирует реальную картинку-маркер и её физическую ширину в разделе **Настройки**.
  - ARCore распознаёт маркер → определяет его позу в мире → устанавливается относительная система координат сцены.
  - Объекты сохраняются в Room с относительной позой (`RelativePose`) и трансформом.
  - При повторном запуске — маркер снова распознан → все привязанные объекты появляются на тех же местах в реальном мире.
  - Gravity-aligned base pose: относительные координаты завязаны на мировую вертикаль, а не на вестибулярную наклонность маркера — объекты не «проваливаются» в пол при небольшом скосе.
- **Projects / Bindings** (Настройки):
  - CRUD привязок «картинка-маркер → 3D-объект → физическая ширина».
  - Импорт маркера через SAF (копия в `filesDir/markers/<uuid>.jpg`, стабильный `file://` путь, не зависит от жизненного цикла content URI).
  - Форма: название, превью картинки, ширина в сантиметрах, выпадающий список моделей с эмодзи-превью.
  - При детекте маркера автоматически спавнится привязанный объект в позе маркера (если нет ранее сохранённых placement'ов).
- **Сканирование-overlay** в стиле СБП: анимированная рамка с четырьмя угловыми скобками + горизонтальная развёртка с градиентом, пока маркер не найден. При детекте исчезает через fade-out.
- **UI**:
  - Фирменные цвета: **зелёный #005A5A** (Primary), **красный #e62142** (Tertiary / destructive), **бирюзовый #00988E** (Secondary).
  - Тёмная тема с зелёным подтоном, чтобы камера-фид оставался центральным элементом.
  - Edge-to-edge, обработка display cutout (насечка/камера), все элементы с system bar padding.
  - Плавные анимации (Material3 AnimatedVisibility, infiniteTransition для сканера и пульсации статуса).
  - Эмодзи-превью моделей в галерее: 🦆 🦊 🥑 💧 📻 🏮 и т.д.

---

## Архитектура

Clean Architecture с тремя слоями + MVVM в presentation.

```
app/
├── core/                  # общие утилиты (Outcome, dispatchers, qualifiers)
├── domain/                # чистый Kotlin, без Android-зависимостей
│   ├── model/             # ArObject, PlacedObject, SceneBinding, SceneMarker, TransformState...
│   ├── repository/        # интерфейсы: ObjectRepository, PlacementRepository, SceneBindingRepository, ScenePersistenceRepository, ScenePreferencesRepository
│   └── usecase/           # один класс — одна операция
├── data/                  # реализации + Room + OkHttp
│   ├── local/
│   │   ├── ArDatabase         # Room v3, три сущности
│   │   ├── entity/            # ObjectEntity, PlacementRecordEntity, SceneBindingEntity
│   │   ├── dao/               # ObjectDao, PlacementRecordDao, SceneBindingDao
│   │   └── GalleryCatalog     # seed встроенных/облачных моделей
│   ├── remote/
│   │   ├── ModelDownloader    # OkHttp streaming + прогресс, cache в filesDir
│   │   ├── SafImporter        # импорт GLB/GLTF из SAF
│   │   └── MarkerImageImporter# импорт картинки-маркера из SAF
│   ├── repository/            # импл всех domain-интерфейсов
│   └── mapper/                # ObjectMapper (Entity ↔ Domain)
├── ar/                    # AR-инфраструктура (SceneView + ARCore)
│   ├── ArSceneCoordinator # @Singleton, мост между Compose и Filament-сценой
│   ├── ArSceneHost        # Composable, оборачивает ARSceneView, diff сцены
│   └── ArSceneEvents      # sealed события сцены → VM
└── presentation/
    ├── MainActivity       # @AndroidEntryPoint, NavHost (ar ↔ settings)
    ├── permissions/       # gate разрешения камеры
    ├── theme/             # Material 3 ColorScheme / Typography / Shapes
    ├── ar/                # главный AR-экран + UI-компоненты
    │   ├── ArScreen           # Scaffold с Top/Bottom bar, PlacementPrompt, TransformSheet, SessionStatusChip, MarkerScannerOverlay
    │   ├── ArViewModel        # MVVM, UDF через sealed ArUserAction, combine StateFlow'ов
    │   ├── ArUiState          # состояние экрана + ArUserAction + Axis + MarkerOrientation
    │   └── components/        # ArTopBar, ArBottomBar, PlacementPrompt, TransformSheet (+nudge), SessionStatusChip, MarkerScannerOverlay
    ├── gallery/           # ModalBottomSheet с 3 вкладками (библиотека / облако / импорт)
    └── settings/          # экран CRUD привязок
        ├── SettingsScreen     # список + форма редактирования
        ├── SettingsViewModel
        └── SettingsUiState    # состояние + SettingsAction + BindingEditor
```

### Принципы

- **Чистая архитектура** — domain ни от чего не зависит; presentation зависит от domain; data реализует domain.
- **MVVM + UDF** — ViewModel комбинирует StateFlow'ы в `ArUiState`, экран эмитит `ArUserAction` через `onAction(...)`.
- **Репозитории** — интерфейсы в `domain`, реализации в `data`. Все запросы к Room через DAO.
- **Use Case per action** — одна бизнес-операция на класс (SRP).
- **Dependency Inversion** через Hilt: `@Binds` для `impl → interface`, `@Provides` для Room/OkHttp.
- **Observer pattern** для AR событий — `SharedFlow<ArSceneEvent>` (сцена → VM) + `StateFlow<ArSessionState>` / `StateFlow<TrackedMarker?>` (состояния).
- **Outcome sealed interface** для результатов с прогрессом (`Success`/`Failure`/`Progress`) — избегаем exception-driven flow.
- **Structured concurrency**: `Dispatchers.IO` для сети/SAF, `Dispatchers.Default` для CPU, `viewModelScope` для UI.
- **Debouncing** для частых событий (слайдеры трансформа → запись в Room дебаунсится на 250 мс; события pose маркера эмитятся в StateFlow только на смену tracking-state).

---

## Стек технологий

| Область | Что используется |
|---|---|
| Язык | Kotlin 2.0.20 |
| UI | Jetpack Compose (BOM 2024.09.03), Material 3 (1.3.0) |
| Navigation | androidx.navigation:navigation-compose 2.8.2 |
| AR-движок | ARCore SDK 1.44 + SceneView 2.2.1 (Filament renderer) |
| DI | Hilt 2.52 (+ hilt-navigation-compose 1.2.0) |
| Асинхронность | Kotlin Coroutines 1.9.0, Flow |
| Локальная БД | Room 2.6.1 (KSP) |
| Preferences | DataStore 1.1.1 |
| Сеть | OkHttp 4.12.0 (streaming с прогрессом) |
| JSON | Kotlinx Serialization 1.7.3 |
| Загрузка изображений | Coil 2.7.0 |
| Логи | Timber 5.0.1 |
| Permissions | Accompanist Permissions 0.36.0 |
| Desugaring | Android Tools desugar_jdk_libs 2.1.2 (minSdk 24) |

Сборка: Gradle 8.9 с configuration cache, KSP для Room и Hilt, Kotlin Compose Compiler plugin.

---

## Ключевые потоки

### 1. Размещение объекта по тапу

1. Пользователь открывает Галерею (нижняя карточка) → выбирает объект → `ChoseObject` action → state получает `selectedGallery`.
2. Тап по полу/стене на AR-сцене.
3. `ArSceneHost.setOnGestureListener(onSingleTapConfirmed)` ловит событие:
   - Если тап попал в `ModelNode` → `publishPlacedTap(placementId)` → VM делает `selectPlacement`.
   - Иначе ARCore `frame.hitTest(x, y)` → валидная плоскость → создаётся `Anchor`, `AnchorNode` добавляется в сцену, выдаётся `anchorHandle` (UUID).
4. Событие `SurfaceTapped(handle)` → VM: `beginPendingUseCase(PendingPlacement(handle, selectedGallery))`.
5. `syncPending` в сцене: асинхронная загрузка GLB через `modelLoader.loadModelInstance(uri)` → `ModelNode` с опциональным `centerOrigin` (pivot-override из каталога), прикрепление к AnchorNode. Якорь помечается маркером `__pending__`.
6. UI показывает `PlacementPrompt` с кнопками **Установить** / **Закрыть**.
7. «Установить» → `confirmPendingUseCase` → новый `PlacedObject(placementId, sourceObject, TransformState.Default, anchorHandle)` добавляется в `_placed` StateFlow, pending очищается.
8. `syncPlaced` в сцене находит якорь с маркером `__pending__`, переприсваивает ему настоящий `placementId`, устанавливает финальные transform и `onSingleTapConfirmed`, очищает `lastPendingHandle`.
9. Gallery selection очищается (нельзя бесконечно спавнить копии).

### 2. Редактирование установленного объекта

- Тап по `ModelNode` → `publishPlacedTap` → VM: `selectPlacement` → `state.selected != null`.
- `TransformSheet` открывается:
  - Слайдер масштаба (0.1–4.0, шаг отображения x1.00). Изменение → `ChangeScale` → `updateTransformUseCase` → StateFlow → `syncPlaced` обновляет `modelNode.scale`.
  - Слайдер поворота по Y (0°–360°). Аналогично.
  - Nudge-кнопки ±X/Y/Z: `NudgeAxis` action → `ArSceneCoordinator.nudgeAnchor(handle, dx, dy, dz)` создаёт новый ARCore Anchor в смещённой позе → `reanchorUseCase` меняет `anchorHandle` на `PlacedObject` → `syncPlaced` детектит смену handle → переносит `ModelNode` со старой AnchorNode на новую, старый якорь отпускается (`anchor.detach()`).
  - Переместить (⊕): `RequestMove` → `moveRequestedPlacementId` в state → следующий `SurfaceTapped` срабатывает как reanchor, а не как новое размещение.
  - Удалить: `RemovePlacement` → `removePlacementUseCase` → `deletePersistedUseCase` чистит Room.
- Все изменения трансформа и позы при активном маркере дебаунсятся на 250 мс и пишутся в Room через `persistPlacementUseCase`.

### 3. Персистентность через AugmentedImages

**Сохранение:**
- На каждом кадре `ArSceneHost.onSessionUpdated` проходит по `frame.getUpdatedTrackables(AugmentedImage::class.java)`. При изменении tracking state публикуется в `trackedMarker: StateFlow`, первое TRACKING-событие также эмитит `ArSceneEvent.MarkerAcquired(markerName)`.
- Внутренний `markerPoses: Map<String, Pose>` обновляется каждый кадр тихо (без State-emission, чтобы не рекомпозить сцену).
- При `confirmPending` и каждом изменении трансформа — `ArSceneCoordinator.relativeToMarker(anchorHandle, markerName)` считает:
  1. `basePose` = `markerPose.toGravityAligned()` — позиция маркера + только yaw вокруг мировой Y, pitch/roll обнуляется (мир вертикально остаётся вертикальным независимо от наклона маркера).
  2. `relative = basePose.inverse() · anchorPose`.
- `PersistedPlacement(placementId, objectId, relativePose, transform)` → `ScenePersistenceRepositoryImpl.save(sceneId = markerName, ...)` → Room.

**Восстановление:**
- Первый `MarkerAcquired(sceneId)` за сессию → `restoreScene(sceneId)`:
  1. `loadPersistedUseCase(sceneId)` → `List<RestoreCandidate>`.
  2. Для каждого: `coordinator.createAnchorAtRelative(sceneId, relativePose)` = `basePose · relativePose` → `session.createAnchor(absolutePose)` → новый `AnchorNode` в сцене → возвращается новый `anchorHandle`.
  3. `addRestoredUseCase(PlacedObject(...))` добавляет placement в StateFlow.
  4. `syncPlaced` находит anchor по handle, загружает модель через `loadModel`, прикрепляет.
- Если persist пусто, но есть `SceneBinding` для этого маркера — автоспавн привязанного объекта в позе маркера (`RelativePose(0,0,0, identity)`).

### 4. Projects / Bindings

- Из TopBar → шестерёнка ⚙ → `SettingsScreen`.
- FAB ⊕ → форма: название, SAF-выбор картинки (копия в `filesDir/markers/`), ширина в см, выбор модели из выпадающего списка.
- Сохранение → Room.
- При старте AR-сессии `ArSceneHost.sessionConfiguration` перебирает все `SceneBinding`, для каждой вызывает `AugmentedImageDatabase.addImage(binding.id, bitmap, binding.markerWidthMeters)`. Если ARCore отвергает картинку (`ImageInsufficientQualityException` для слабо-детализированных) — в logcat warning с тегом `AR_HOST`.

> ⚠️ **ВАЖНО**: новые привязки применяются после перезапуска приложения. SceneView кэширует `AugmentedImageDatabase` при первой конфигурации сессии.

---

## Требования к устройству

- Android 7.0+ (minSdk 24), targetSdk 34.
- Устройство в [списке совместимых с ARCore](https://developers.google.com/ar/devices).
- Камера, OpenGL ES 3.0+.
- «Сервисы Google Play для AR» на устройстве (ставится автоматически по ссылке из метаданных манифеста `com.google.ar.core = required`).

---

## Сборка

### Предварительные требования

- **JDK 17** (обязательно; SceneView 2.2+ не собирается на 11).
- **Android SDK** 34 (compile + target).
- **Android Studio Koala+** (для IDE-интеграции).

### Пошагово

1. Склонируйте репозиторий или разверните архив проекта.
2. Создайте `local.properties` в корне и укажите путь до Android SDK:
   ```properties
   sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
   ```
   (пример см. в [local.properties.example](local.properties.example))
3. Откройте проект в Android Studio → **Sync Project with Gradle Files**. Gradle 8.9 (~130 МБ) и все зависимости скачаются автоматически.
4. Положите GLB-модели в `app/src/main/assets/models/` (имена см. в `app/src/main/assets/models/README.md`) — либо уже скачаны, либо замените на ваши.

### CLI-сборка

```bash
# Windows (из Git Bash)
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug
```

APK будет в `app/build/outputs/apk/debug/app-debug.apk`.

### Установка на устройство

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.arpositionset.app.debug/com.arpositionset.app.presentation.MainActivity
```

### Release-сборка

```bash
./gradlew :app:assembleRelease
```

Подписание — свой `keystore.properties`. Включён R8 с правилами в `app/proguard-rules.pro` (защищены Filament/SceneView/ARCore JNI-классы, kotlinx.serialization, Retrofit, Hilt).

---

## Использование приложения

### Первый запуск

1. При старте — запрос разрешения камеры. После согласия появляется AR-экран.
2. Внизу — карточка «Выберите объект», тап открывает галерею.
3. В верхней полосе — три кнопки: ⚙ (настройки), #ℤ (переключатель сетки оцифровки плоскостей), 🗑️ (очистить сцену).
4. В центре — анимированная сканер-рамка пока маркер не найден.

### Сценарий: быстрое размещение без маркеров

1. Открыть галерею → выбрать модель.
2. Навести камеру на пол/стол/стену, подождать появления сетки оцифровки.
3. Тап по поверхности → появляется «призрак» модели.
4. Кнопка **Установить** → объект закреплён. **Закрыть** → отмена.
5. Тап по установленной модели → `TransformSheet`: слайдеры масштаба/поворота, кнопки ±5 см по X/Y/Z, ⊕ переместить, 🗑️ удалить, ✕ закрыть.

> ⚠️ Без маркера объекты не сохраняются между запусками — при перезапуске сцена чистая.

### Сценарий: персистентное размещение с маркером

1. **⚙ Настройки** → FAB ⊕ → создать привязку.
2. Ввести **Название** (например, «Цех-3, станок А»).
3. Нажать на блок «Картинка-маркер» → SAF → выбрать картинку (важно: детализированная текстура, **не QR-код**). Рекомендация — фото постера, обложки журнала, логотип с градиентом. ARCore требует trackability score ≥ ~75.
4. Ввести **Физическую ширину** в сантиметрах (точно, как напечатано/показано на экране).
5. Выбрать **Объект** из галереи.
6. Сохранить.
7. **Перезапустить приложение** (swipe из recents → открыть снова).
8. Показать маркер камере → в статус-чипе зелёная точка «Маркер активен: <title>» → привязанный объект автоматически появляется на маркере → snackbar «Привязка «…» активна».
9. Трансформируйте / двигайте → изменения пишутся в Room относительно маркера.
10. При следующем запуске и наведении на тот же маркер объект появится **ровно там, где вы его оставили**, независимо от того, с какой стороны комнаты вы начали сессию.

### Подсказки

- Для стабильного трекинга маркер лучше размещать на стене (вертикально) — pitch/roll ARCore'а более шумный для плоских горизонтальных картинок.
- Размер маркера: 10-20 см для комнат, A4/A3 для больших помещений.
- Первый детект занимает 2-5 сек — поводите камерой вокруг маркера.
- Gravity-aligned base pose означает, что даже если маркер на полу слегка завален — объекты не будут плавать вверх/вниз.

---

## Производительность и оптимизации

- Gradle configuration-cache + parallel + caching.
- `noCompress glb gltf hdr ktx filamat` — AR-ассеты читаются прямо из APK без распаковки в heap.
- `kotlin.incremental.useClasspathSnapshot=true`.
- StateFlow `WhileSubscribed(5_000)` — не пересчитываем state при перевороте экрана/свёртке Sheet'ов.
- Marker-pose emit debounce (только transition) — нет рекомпоза сцены 30-60 раз/сек.
- Room-writes при драге слайдера дебаунсятся на 250 мс.
- Slider local state не echo-back из внешнего value — нет feedback-петли.
- Три directional-заливочных light'а вместо тяжёлого IBL — fast, даёт равномерное освещение PBR без warmup'а.
- Edge-to-edge + отключенный system bar contrast — камера-фид не «прыгает» при появлении UI.

---

## Пути расширения

- **Динамическая перезагрузка AugmentedImageDatabase** без рестарта — добавить `session.configure { ... }` в `LaunchedEffect(bindings)`.
- **Группировка по проектам** (Projects → Bindings) — ещё одна Room-сущность с FK.
- **Шеринг привязок между устройствами** — минимальный REST-API на корпоративной сети (см. раздел обсуждения persistence в истории переписки по проекту).
- **Экспорт/импорт конфигурации проектов** в JSON (Kotlinx Serialization уже подключён).
- **Preview 3D-моделей** в галерее через мини-SceneView вместо эмодзи.
- **Multi-marker scene** — объекты относительно нескольких маркеров, усреднение позы.

---

## Лицензия

Код приложения — MIT (или корпоративная закрытая, в зависимости от развёртывания).
3D-модели в `assets/models/` — CC-BY / CC0, Khronos glTF Sample Models.
Маркер по умолчанию в `assets/markers/demo_marker.jpg` — Google ARCore SDK sample, Apache 2.0.
