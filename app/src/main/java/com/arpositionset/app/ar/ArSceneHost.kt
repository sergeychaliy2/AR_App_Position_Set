package com.arpositionset.app.ar

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arpositionset.app.domain.model.ArSessionState
import com.arpositionset.app.domain.model.PendingPlacement
import com.arpositionset.app.domain.model.PlacedObject
import com.arpositionset.app.domain.model.SceneBinding
import com.google.android.filament.LightManager
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch

private const val TAG = "AR_HOST"

/** Fallback demo marker used when the user hasn't configured any scene bindings yet. */
private const val DEMO_MARKER_NAME = "helmet_marker"
private const val DEMO_MARKER_ASSET = "markers/demo_marker.jpg"
private const val DEMO_MARKER_WIDTH_METERS = 0.10f

/**
 * Compose host for the ARSceneView. Sync responsibilities:
 *  - marker detection (AugmentedImage) → coordinator.trackedMarker state
 *  - surface taps → SurfaceTapped events with opaque anchor handles
 *  - [syncPlaced] / [syncPending] keep the scene graph aligned with domain state
 */
@Composable
fun ArSceneHost(
    coordinator: ArSceneCoordinator,
    placed: List<PlacedObject>,
    pending: PendingPlacement?,
    planesVisible: Boolean,
    selectedPlacementId: String?,
    bindings: List<SceneBinding>,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val host = remember { ArSceneHostState(coordinator) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            // IMPORTANT: in SceneView 2.2.1, `sessionConfiguration` and callbacks
            // MUST be passed as constructor arguments to take effect on the
            // initial session configure. Assigning via `.apply{ sessionConfiguration = ... }`
            // happens AFTER SceneView has already created the session with its
            // default config — at that point reassigning the property doesn't
            // retrigger the configure call, so our settings never land.
            val sessionConfig: (com.google.ar.core.Session, Config) -> Unit = { session, config ->
                config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                    Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.focusMode = Config.FocusMode.AUTO
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE

                try {
                    val db = AugmentedImageDatabase(session)
                    val effectiveBindings = if (bindings.isNotEmpty()) bindings else emptyList()
                    if (effectiveBindings.isEmpty()) {
                        // Fallback demo marker for new installs with no user bindings.
                        val bitmap = context.assets.open(DEMO_MARKER_ASSET).use {
                            BitmapFactory.decodeStream(it)
                        } ?: error("demo marker decode failed")
                        val idx = db.addImage(DEMO_MARKER_NAME, bitmap, DEMO_MARKER_WIDTH_METERS)
                        Log.i(TAG, "Demo marker added (index=$idx)")
                    } else {
                        effectiveBindings.forEach { b ->
                            runCatching {
                                val path = b.markerAssetUri.removePrefix("file://")
                                val bitmap = java.io.File(path).inputStream().use {
                                    BitmapFactory.decodeStream(it)
                                } ?: return@runCatching
                                val idx = db.addImage(b.id, bitmap, b.markerWidthMeters)
                                Log.i(TAG, "Binding '${b.title}' added (id=${b.id}, idx=$idx, w=${b.markerWidthMeters}m)")
                            }.onFailure {
                                Log.e(TAG, "Binding '${b.title}' rejected: ${it.javaClass.simpleName}: ${it.message}")
                            }
                        }
                    }
                    config.augmentedImageDatabase = db
                } catch (t: Throwable) {
                    Log.e(TAG, "Marker DB build failed: ${t.javaClass.simpleName}: ${t.message}", t)
                }
            }

            ARSceneView(
                context = context,
                sharedLifecycle = lifecycleOwner.lifecycle,
                sessionConfiguration = sessionConfig,
                onSessionCreated = { _ ->
                    Log.i(TAG, "onSessionCreated")
                    coordinator.publishSessionState(ArSessionState.Searching)
                },
                onSessionResumed = { _ ->
                    Log.i(TAG, "onSessionResumed")
                    coordinator.publishSessionState(ArSessionState.Searching)
                },
                onSessionFailed = { error ->
                    Log.e(TAG, "onSessionFailed", error)
                    coordinator.publishSessionState(ArSessionState.Failed(error))
                },
                onTrackingFailureChanged = { reason ->
                    coordinator.publishTrackingFailure(reason)
                },
                onSessionUpdated = { _, frame ->
                    if (coordinator.sessionState.value == ArSessionState.Initializing) {
                        coordinator.publishSessionState(ArSessionState.Searching)
                    }
                    val anyPlane = frame.getUpdatedTrackables(Plane::class.java)
                        .any { it.trackingState == TrackingState.TRACKING }
                    if (anyPlane && coordinator.sessionState.value == ArSessionState.Searching) {
                        coordinator.publishSessionState(ArSessionState.Ready)
                    }
                    frame.getUpdatedTrackables(AugmentedImage::class.java).forEach { img ->
                        when (img.trackingState) {
                            TrackingState.TRACKING ->
                                coordinator.publishMarkerUpdate(img.name, img.centerPose, tracking = true)
                            TrackingState.PAUSED ->
                                coordinator.publishMarkerUpdate(img.name, img.centerPose, tracking = false)
                            TrackingState.STOPPED ->
                                coordinator.publishMarkerUpdate(img.name, null, tracking = false)
                            null -> Unit
                        }
                    }
                },
            ).apply {
                planeRenderer.isVisible = planesVisible
                planeRenderer.isShadowReceiver = true
                coordinator.boundView = this

                // Fill lights — default mainLight is a single directional sun,
                // so surfaces facing away from it render pitch-black when
                // there's no IBL ambient. Three directional fills from
                // roughly orthogonal directions give uniform illumination
                // without touching the environment/IBL.
                addChildNode(
                    LightNode(
                        engine = engine,
                        type = LightManager.Type.DIRECTIONAL,
                        apply = {
                            color(1f, 1f, 1f)
                            intensity(60_000f)
                            direction(0f, -1f, 0f) // top-down
                            castShadows(false)
                        },
                    ),
                )
                addChildNode(
                    LightNode(
                        engine = engine,
                        type = LightManager.Type.DIRECTIONAL,
                        apply = {
                            color(1f, 1f, 1f)
                            intensity(40_000f)
                            direction(1f, -0.3f, 0.5f) // from left-front
                            castShadows(false)
                        },
                    ),
                )
                addChildNode(
                    LightNode(
                        engine = engine,
                        type = LightManager.Type.DIRECTIONAL,
                        apply = {
                            color(1f, 1f, 1f)
                            intensity(40_000f)
                            direction(-1f, -0.3f, -0.5f) // from right-back
                            castShadows(false)
                        },
                    ),
                )

                // SceneView 2.2.1: single entry point for every gesture on the
                // scene. `node` is the topmost Node that was hit (via
                // collision system). If null, the tap landed on "empty space"
                // and we fall through to an ARCore plane hit-test so the user
                // can place a new anchor.
                setOnGestureListener(
                    onSingleTapConfirmed = { event, node ->
                        val placementId = coordinator.anchors.values
                            .firstOrNull { it.modelNode === node }?.placementId
                        if (node is ModelNode && placementId != null && placementId != "__pending__") {
                            coordinator.publishPlacedTap(placementId)
                        } else {
                            // Use the raw ARCore frame hit-test: hitTestAR() in
                            // SceneView 2.2.1 has many implicit filters that
                            // reject valid plane hits under default settings.
                            val hit = frame?.hitTest(event.x, event.y)
                                ?.firstOrNull { res ->
                                    val t = res.trackable
                                    (t is Plane && t.isPoseInPolygon(res.hitPose)) ||
                                        (t is Point && t.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
                                }
                            if (hit != null) {
                                val anchor = hit.createAnchor()
                                val anchorNode = AnchorNode(engine, anchor).also(::addChildNode)
                                val handle = coordinator.registerAnchor(anchor, anchorNode)
                                coordinator.publishSurfaceTap(handle)
                            } else {
                                coordinator.publishEmptyTap(event.x, event.y)
                            }
                        }
                    },
                )

                host.attach(this)
            }
        },
        update = { view ->
            view.planeRenderer.isVisible = planesVisible
            scope.launch {
                host.syncPlaced(view, placed, selectedPlacementId)
                host.syncPending(view, pending)
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            host.detach()
            coordinator.reset()
        }
    }

    LaunchedEffect(Unit) {
        coordinator.publishSessionState(ArSessionState.Initializing)
    }
}

internal class ArSceneHostState(private val coordinator: ArSceneCoordinator) {

    private var view: ARSceneView? = null
    private var lastPendingHandle: String? = null
    private val mountedPlacements = mutableSetOf<String>()

    fun attach(view: ARSceneView) {
        this.view = view
    }

    fun detach() {
        view = null
        mountedPlacements.clear()
        lastPendingHandle = null
    }

    suspend fun syncPending(view: ARSceneView, pending: PendingPlacement?) {
        if (pending == null) {
            val handle = lastPendingHandle ?: return
            lastPendingHandle = null
            coordinator.release(handle)?.let { binding ->
                binding.modelNode?.let(view::removeChildNode)
                view.removeChildNode(binding.anchorNode)
                binding.anchor.detach()
            }
            return
        }
        if (pending.anchorHandle == lastPendingHandle) return
        lastPendingHandle?.let { previous ->
            coordinator.release(previous)?.let { binding ->
                binding.modelNode?.let(view::removeChildNode)
                view.removeChildNode(binding.anchorNode)
                binding.anchor.detach()
            }
        }
        val binding = coordinator.anchors[pending.anchorHandle] ?: return
        val currentPlacementId = binding.placementId
        if (currentPlacementId != null && currentPlacementId != PENDING_MARKER) return
        val model = loadModel(
            view = view,
            uri = pending.candidateObject.modelUri,
            pivot = pending.candidateObject.pivotOverride,
        ) ?: run {
            lastPendingHandle = null
            return
        }
        binding.anchorNode.addChildNode(model)
        val s = pending.candidateObject.defaultScale
        model.scale = Float3(s, s, s)
        model.isTouchable = false
        coordinator.bindPlacement(pending.anchorHandle, PENDING_MARKER, model)
        lastPendingHandle = pending.anchorHandle
    }

    suspend fun syncPlaced(
        view: ARSceneView,
        placed: List<PlacedObject>,
        selected: String?,
    ) {
        val seen = mutableSetOf<String>()
        placed.forEach { item ->
            seen += item.placementId

            val existing = coordinator.bindingForPlacement(item.placementId)
            if (existing != null) {
                val currentHandle = coordinator.anchors.entries
                    .firstOrNull { it.value === existing }?.key
                if (currentHandle != null && currentHandle != item.anchorHandle) {
                    val newBinding = coordinator.anchors[item.anchorHandle]
                    val model = existing.modelNode
                    if (newBinding != null && model != null) {
                        existing.anchorNode.removeChildNode(model)
                        newBinding.anchorNode.addChildNode(model)
                        coordinator.bindPlacement(item.anchorHandle, item.placementId, model)
                        coordinator.release(currentHandle)
                        view.removeChildNode(existing.anchorNode)
                        existing.anchor.detach()
                    }
                }
                val activeBinding = coordinator.bindingForPlacement(item.placementId) ?: existing
                activeBinding.modelNode?.apply {
                    val s = item.transform.scale * item.sourceObject.defaultScale
                    scale = Float3(s, s, s)
                    rotation = Float3(0f, item.transform.rotationYDegrees, 0f)
                    isTouchable = true
                }
                mountedPlacements += item.placementId
                return@forEach
            }

            val binding = coordinator.anchors[item.anchorHandle] ?: return@forEach
            val modelNode = when {
                binding.modelNode != null && binding.placementId == PENDING_MARKER ->
                    binding.modelNode!!
                binding.modelNode == null -> {
                    val loaded = loadModel(
                        view = view,
                        uri = item.sourceObject.modelUri,
                        pivot = item.sourceObject.pivotOverride,
                    ) ?: return@forEach
                    binding.anchorNode.addChildNode(loaded)
                    loaded
                }
                else -> return@forEach
            }

            val s = item.transform.scale * item.sourceObject.defaultScale
            modelNode.scale = Float3(s, s, s)
            modelNode.rotation = Float3(0f, item.transform.rotationYDegrees, 0f)
            modelNode.isTouchable = true
            modelNode.onSingleTapConfirmed = { _ ->
                coordinator.publishPlacedTap(item.placementId)
                true
            }
            coordinator.bindPlacement(item.anchorHandle, item.placementId, modelNode)
            if (item.anchorHandle == lastPendingHandle) lastPendingHandle = null
            mountedPlacements += item.placementId
        }

        val stale = mountedPlacements - seen
        stale.forEach { placementId ->
            val binding = coordinator.bindingForPlacement(placementId) ?: return@forEach
            val handle = coordinator.anchors.entries.firstOrNull { it.value === binding }?.key ?: return@forEach
            coordinator.release(handle)
            binding.modelNode?.let(view::removeChildNode)
            view.removeChildNode(binding.anchorNode)
            binding.anchor.detach()
        }
        mountedPlacements.removeAll(stale)
    }

    private suspend fun loadModel(
        view: ARSceneView,
        uri: String,
        pivot: com.arpositionset.app.domain.model.PivotPoint?,
    ): ModelNode? = try {
        val instance = view.modelLoader.loadModelInstance(uri)
        instance?.let {
            val center = pivot?.let { p -> Position(p.x, p.y, p.z) }
            ModelNode(
                modelInstance = it,
                scaleToUnits = null,
                centerOrigin = center,
            )
        }
    } catch (t: Throwable) {
        coordinator.publishLoadFailure(t.message ?: "model load failed")
        null
    }

    private companion object {
        const val PENDING_MARKER = "__pending__"
    }
}
