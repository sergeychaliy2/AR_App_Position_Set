package com.arpositionset.app.ar

import com.arpositionset.app.domain.model.ArSessionState
import com.arpositionset.app.domain.model.RelativePose
import com.arpositionset.app.domain.model.TrackedMarker
import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ModelNode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridge between the presentation layer and the ARSceneView. The coordinator
 * holds no UI types on its public API — it exposes domain-friendly state
 * flows + opaque anchor handles so ViewModels remain testable on JVM.
 *
 * The Compose [ArSceneHost] feeds low-level callbacks in via the `internal`
 * surface; ViewModels consume the [events] / [sessionState] / [trackedMarker].
 */
@Singleton
class ArSceneCoordinator @Inject constructor() {

    private val _sessionState = MutableStateFlow<ArSessionState>(ArSessionState.Initializing)
    val sessionState: StateFlow<ArSessionState> = _sessionState.asStateFlow()

    private val _events = MutableSharedFlow<ArSceneEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<ArSceneEvent> = _events.asSharedFlow()

    private val _trackedMarker = MutableStateFlow<TrackedMarker?>(null)
    val trackedMarker: StateFlow<TrackedMarker?> = _trackedMarker.asStateFlow()

    /** Handle → ARCore Anchor + associated scene nodes. */
    internal data class AnchorBinding(
        val anchor: Anchor,
        val anchorNode: AnchorNode,
        var modelNode: ModelNode? = null,
        var placementId: String? = null,
    )

    internal val anchors: MutableMap<String, AnchorBinding> = ConcurrentHashMap()

    /** Latest marker pose, per marker name. Kept as ARCore Pose so we can
     *  use the built-in quaternion math without rolling our own. */
    internal val markerPoses: MutableMap<String, Pose> = ConcurrentHashMap()

    /** Host-bound view + ARCore session. Set by [ArSceneHost] in its factory
     *  block so that coordinator methods which need to create anchors at
     *  arbitrary poses can reach the session without leaking the reference
     *  upward into the VM. */
    internal var boundView: ARSceneView? = null

    // ---------------- Events from the scene host ----------------

    internal fun publishSessionState(state: ArSessionState) {
        _sessionState.value = state
        _events.tryEmit(ArSceneEvent.SessionStateChanged(state))
    }

    internal fun publishTrackingFailure(reason: TrackingFailureReason?) {
        val state = when (reason) {
            null -> ArSessionState.Ready
            TrackingFailureReason.NONE -> ArSessionState.Ready
            TrackingFailureReason.BAD_STATE,
            TrackingFailureReason.INSUFFICIENT_LIGHT,
            TrackingFailureReason.EXCESSIVE_MOTION,
            TrackingFailureReason.INSUFFICIENT_FEATURES,
            TrackingFailureReason.CAMERA_UNAVAILABLE -> ArSessionState.TrackingLost
        }
        publishSessionState(state)
    }

    internal fun publishSurfaceTap(handle: String) {
        _events.tryEmit(ArSceneEvent.SurfaceTapped(handle))
    }

    internal fun publishEmptyTap(x: Float, y: Float) {
        _events.tryEmit(ArSceneEvent.EmptyTap(x, y))
    }

    internal fun publishPlacedTap(placementId: String) {
        _events.tryEmit(ArSceneEvent.PlacedNodeTapped(placementId))
    }

    internal fun publishLoadFailure(message: String) {
        _events.tryEmit(ArSceneEvent.LoadingFailed(message))
    }

    internal fun publishMarkerUpdate(name: String, pose: Pose?, tracking: Boolean) {
        // Always keep the latest pose silently — relative-pose math reads from
        // this map and wants a fresh value. But don't spam the StateFlow: every
        // pose update would trigger a full scene recomposition 30-60 times per
        // second. Only emit on tracking-state transitions.
        if (pose != null) markerPoses[name] = pose
        val previous = _trackedMarker.value
        val wasTracking = previous?.name == name && previous.isTracking
        if (wasTracking == tracking) return
        _trackedMarker.value = TrackedMarker(name = name, isTracking = tracking)
        if (tracking) {
            _events.tryEmit(ArSceneEvent.MarkerAcquired(name))
        }
    }

    // ---------------- API for host & ViewModel ----------------

    internal fun registerAnchor(anchor: Anchor, node: AnchorNode): String {
        val handle = UUID.randomUUID().toString()
        anchors[handle] = AnchorBinding(anchor = anchor, anchorNode = node)
        return handle
    }

    internal fun bindPlacement(handle: String, placementId: String, node: ModelNode) {
        anchors[handle] = anchors[handle]?.copy(
            modelNode = node,
            placementId = placementId,
        ) ?: return
    }

    internal fun release(handle: String): AnchorBinding? = anchors.remove(handle)

    internal fun bindingForPlacement(placementId: String): AnchorBinding? =
        anchors.values.firstOrNull { it.placementId == placementId }

    internal fun reset() {
        anchors.clear()
        markerPoses.clear()
        _sessionState.value = ArSessionState.Initializing
        _trackedMarker.value = null
        boundView = null
    }

    // ---------------- Pose math (ARCore-backed) ----------------

    /**
     * Returns the given anchor's pose expressed in [markerName]'s GRAVITY-
     * ALIGNED frame, or null if either pose is unknown.
     *
     * We deliberately do NOT use the raw marker pose: a floor-mounted marker's
     * pitch / roll are noisy and small tilts get amplified into big vertical
     * drift at restore time (object sinks into the floor or floats up). By
     * stripping everything except the marker's world-Y yaw, the relative frame
     * keeps world gravity as its vertical axis — so Y stays Y.
     */
    fun relativeToMarker(anchorHandle: String, markerName: String): RelativePose? {
        val anchorPose = anchors[anchorHandle]?.anchor?.pose ?: return null
        val markerPose = markerPoses[markerName] ?: return null
        val basePose = markerPose.toGravityAligned()
        val relative = basePose.inverse().compose(anchorPose)
        return relative.toRelativePose()
    }

    /**
     * Creates a new ARCore anchor at (markerBase · relativePose), registers it
     * with the coordinator and adds the anchor node to the scene. Uses the
     * same gravity-aligned base as [relativeToMarker] — the two operations
     * must agree for restored objects to land where they were saved.
     */
    fun createAnchorAtRelative(markerName: String, relative: RelativePose): String? {
        val view = boundView ?: return null
        val session = view.session ?: return null
        val markerPose = markerPoses[markerName] ?: return null
        val basePose = markerPose.toGravityAligned()
        val relativePose = Pose(
            floatArrayOf(relative.tx, relative.ty, relative.tz),
            floatArrayOf(relative.qx, relative.qy, relative.qz, relative.qw),
        )
        val absolute = basePose.compose(relativePose)
        return runCatching {
            val anchor = session.createAnchor(absolute)
            val node = AnchorNode(view.engine, anchor).also(view::addChildNode)
            registerAnchor(anchor, node)
        }.getOrNull()
    }

    /**
     * Nudge an anchor by (dx, dy, dz) metres in WORLD axes. Creates a new
     * ARCore anchor at the offset pose, adds a fresh AnchorNode to the scene
     * and returns its handle. The old anchor stays registered — the caller is
     * expected to update its `PlacedObject.anchorHandle` so [syncPlaced]'s
     * re-anchor migration picks up the swap and cleans up the old node.
     */
    fun nudgeAnchor(anchorHandle: String, dx: Float, dy: Float, dz: Float): String? {
        val view = boundView ?: return null
        val session = view.session ?: return null
        val current = anchors[anchorHandle]?.anchor?.pose ?: return null
        val translated = Pose(
            floatArrayOf(current.tx() + dx, current.ty() + dy, current.tz() + dz),
            floatArrayOf(current.qx(), current.qy(), current.qz(), current.qw()),
        )
        return runCatching {
            val anchor = session.createAnchor(translated)
            val node = AnchorNode(view.engine, anchor).also(view::addChildNode)
            registerAnchor(anchor, node)
        }.getOrNull()
    }

    /**
     * Reduce an arbitrary marker pose to a world-Y-up frame at the same
     * position, keeping only the marker's heading (yaw) around the world Y
     * axis. We derive yaw from the marker's local +X direction projected onto
     * the horizontal plane; this works for floor-flat, wall-mounted and
     * tilted orientations alike.
     */
    private fun Pose.toGravityAligned(): Pose {
        val xWorld = rotateVector(floatArrayOf(1f, 0f, 0f))
        // If the marker's local X axis is (nearly) vertical, it contains no
        // horizontal heading — fall back to local Z axis to derive one.
        val horizontal = if (kotlin.math.abs(xWorld[0]) + kotlin.math.abs(xWorld[2]) < 1e-4f) {
            rotateVector(floatArrayOf(0f, 0f, 1f))
        } else {
            xWorld
        }
        val yaw = kotlin.math.atan2(horizontal[0], horizontal[2])
        val half = yaw * 0.5f
        return Pose(
            floatArrayOf(tx(), ty(), tz()),
            floatArrayOf(0f, kotlin.math.sin(half), 0f, kotlin.math.cos(half)),
        )
    }

    private fun Pose.toRelativePose(): RelativePose = RelativePose(
        tx = tx(), ty = ty(), tz = tz(),
        qx = qx(), qy = qy(), qz = qz(), qw = qw(),
    )
}
