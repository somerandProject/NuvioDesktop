package com.nuvio.app.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.blockMouseBackForwardButtons
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.isDesktop
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.p2p.P2pSettingsRepository
import com.nuvio.app.features.p2p.P2pStreamingEngine
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_airs_prefix
import nuvio.composeapp.generated.resources.compose_player_downloaded
import nuvio.composeapp.generated.resources.compose_player_resize_fill
import nuvio.composeapp.generated.resources.compose_player_resize_fit
import nuvio.composeapp.generated.resources.compose_player_resize_stretch
import nuvio.composeapp.generated.resources.compose_player_resize_zoom
import nuvio.composeapp.generated.resources.generic_unknown
import nuvio.composeapp.generated.resources.parental_alcohol
import nuvio.composeapp.generated.resources.parental_frightening
import nuvio.composeapp.generated.resources.parental_nudity
import nuvio.composeapp.generated.resources.parental_profanity
import nuvio.composeapp.generated.resources.parental_severity_mild
import nuvio.composeapp.generated.resources.parental_severity_moderate
import nuvio.composeapp.generated.resources.parental_severity_severe
import nuvio.composeapp.generated.resources.parental_violence
import nuvio.composeapp.generated.resources.compose_player_tba
import org.jetbrains.compose.resources.stringResource

private val playerScopeLog = Logger.withTag("PlayerScreen")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PlayerScreenContent(args: PlayerScreenArgs) {
    val playerSettingsUiState by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val p2pSettingsUiState by remember {
        P2pSettingsRepository.ensureLoaded()
        P2pSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val p2pStreamingState by P2pStreamingEngine.state.collectAsStateWithLifecycle()
    val metaScreenSettingsUiState by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchProgressUiState by remember {
        WatchProgressRepository.ensureLoaded()
        WatchProgressRepository.uiState
    }.collectAsStateWithLifecycle()
    val sourceStreamsState by PlayerStreamsRepository.sourceState.collectAsStateWithLifecycle()
    val episodeStreamsRepoState by PlayerStreamsRepository.episodeStreamsState.collectAsStateWithLifecycle()
    val metaUiState by MetaDetailsRepository.uiState.collectAsStateWithLifecycle()
    val addonsUiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val addonSubtitles by SubtitleRepository.addonSubtitles.collectAsStateWithLifecycle()
    val isLoadingAddonSubtitles by SubtitleRepository.isLoading.collectAsStateWithLifecycle()

    val runtime = remember { PlayerScreenRuntime(args) }
    runtime.args = args

    BoxWithConstraints(
        modifier = args.modifier
            .fillMaxSize()
            .background(Color.Black)
            .blockMouseBackForwardButtons(),
    ) {
        val density = LocalDensity.current
        val horizontalSafePadding = playerHorizontalSafePadding()
        val metrics = remember(maxWidth) { PlayerLayoutMetrics.fromWidth(maxWidth) }

        // The player launches many coroutines (autoplay handoff, source swaps,
        // track refresh, progress persistence). A bare rememberCoroutineScope()
        // lets any uncaught child exception kill the whole app on Android.
        // A supervisor scope with an exception handler degrades that to a logged,
        // surfaced failure instead of a force close.
        val playerScope = remember {
            CoroutineScope(
                Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, error ->
                    if (error is CancellationException) return@CoroutineExceptionHandler
                    playerScopeLog.e("Unhandled player coroutine failure", error)
                    NuvioToastController.show(
                        error.message?.takeIf { it.isNotBlank() }
                            ?: error::class.simpleName
                            ?: "Playback error",
                    )
                },
            )
        }
        DisposableEffect(Unit) {
            onDispose { playerScope.cancel() }
        }
        runtime.scope = playerScope
        runtime.hapticFeedback = LocalHapticFeedback.current
        runtime.gestureController = rememberPlayerGestureController(
            restoreBrightness = playerSettingsUiState.touchGesturesEnabled,
        )
        runtime.playerSettingsUiState = playerSettingsUiState
        runtime.p2pSettingsUiState = p2pSettingsUiState
        runtime.p2pStreamingState = p2pStreamingState
        runtime.metaScreenSettingsUiState = metaScreenSettingsUiState
        runtime.watchedUiState = watchedUiState
        runtime.watchProgressUiState = watchProgressUiState
        runtime.sourceStreamsState = sourceStreamsState
        runtime.episodeStreamsRepoState = episodeStreamsRepoState
        runtime.metaUiState = metaUiState
        runtime.addonsUiState = addonsUiState
        runtime.addonSubtitles = mergeStreamAndAddonSubtitles(addonSubtitles, runtime.externalSubtitles)
        runtime.isLoadingAddonSubtitles = isLoadingAddonSubtitles
        runtime.horizontalSafePadding = horizontalSafePadding
        runtime.metrics = metrics
        runtime.sliderEdgePadding = horizontalSafePadding + metrics.horizontalPadding
        runtime.overlayBottomPadding = if (isDesktop || playerSettingsUiState.useLegacyPlayerLayout) {
            sliderOverlayBottomPadding(metrics)
        } else {
            playerTimelineBottomInsets(metrics).asPaddingValues().calculateBottomPadding() +
                72.dp + PlayerSliderOverlayGap
        }
        runtime.sideGestureSystemEdgeExclusionPx = with(density) {
            PlayerSideGestureSystemEdgeExclusion.toPx()
        }
        runtime.resizeModeFitLabel = stringResource(Res.string.compose_player_resize_fit)
        runtime.resizeModeFillLabel = stringResource(Res.string.compose_player_resize_fill)
        runtime.resizeModeZoomLabel = stringResource(Res.string.compose_player_resize_zoom)
        runtime.resizeModeStretchLabel = stringResource(Res.string.compose_player_resize_stretch)
        runtime.downloadedLabel = stringResource(Res.string.compose_player_downloaded)
        runtime.airsPrefix = stringResource(Res.string.compose_player_airs_prefix)
        runtime.tbaLabel = stringResource(Res.string.compose_player_tba)
        runtime.genericUnknownLabel = stringResource(Res.string.generic_unknown)
        runtime.parentalGuideLabels = ParentalGuideLabels(
            nudity = stringResource(Res.string.parental_nudity),
            violence = stringResource(Res.string.parental_violence),
            profanity = stringResource(Res.string.parental_profanity),
            alcohol = stringResource(Res.string.parental_alcohol),
            frightening = stringResource(Res.string.parental_frightening),
            severe = stringResource(Res.string.parental_severity_severe),
            moderate = stringResource(Res.string.parental_severity_moderate),
            mild = stringResource(Res.string.parental_severity_mild),
        )
        if (runtime.playerMetaVideos.isEmpty()) {
            runtime.playerMetaVideos = MetaDetailsRepository.peek(
                args.parentMetaType,
                args.parentMetaId,
            )?.videos ?: emptyList()
        }
        val settingsResizeMode = playerSettingsUiState.resizeMode.supportedOnCurrentPlatform()
        if (runtime.lastSyncedSettingsResizeMode != settingsResizeMode) {
            runtime.resizeMode = settingsResizeMode
            runtime.lastSyncedSettingsResizeMode = settingsResizeMode
        }
        runtime.resetIdentityStateIfNeeded()

        val keepScreenAwake = runtime.errorMessage == null &&
            (runtime.playbackSnapshot.isPlaying ||
                (runtime.shouldPlay && runtime.playbackSnapshot.isLoading))
        EnterImmersivePlayerMode(keepScreenAwake = keepScreenAwake)
        ManagePlayerPictureInPicture(
            isPlaying = runtime.playbackSnapshot.isPlaying,
            videoSize = IntSize(
                runtime.playbackSnapshot.videoWidth,
                runtime.playbackSnapshot.videoHeight,
            ),
        )
        runtime.BindPlayerRuntimeEffects()
        runtime.RenderPlayerRuntimeUi()
    }
}
