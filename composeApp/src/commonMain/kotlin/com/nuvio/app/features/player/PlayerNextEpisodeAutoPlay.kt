package com.nuvio.app.features.player

import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.debrid.DebridSettingsRepository
import com.nuvio.app.features.debrid.DirectDebridPlayableResult
import com.nuvio.app.features.debrid.DirectDebridPlaybackResolver
import com.nuvio.app.features.debrid.toastMessage
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.downloads.DownloadItem
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.player.skip.NextEpisodeInfo
import com.nuvio.app.features.player.skip.PlayerNextEpisodeRules
import com.nuvio.app.features.streams.StreamAutoPlayMode
import com.nuvio.app.features.streams.StreamAutoPlaySelector
import com.nuvio.app.features.streams.StreamAutoPlaySource
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.watching.domain.isShortPlaceholderDuration
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val preloadFailureLog = Logger.withTag("NextEpisodePreload")

internal fun PlayerScreenRuntime.isAtNextEpisodeThreshold(): Boolean {
    if (playbackSnapshotKey != activePlaybackKey || playbackSnapshot.isLoading ||
        !initialSeekApplied || isScrubbingTimeline || errorMessage != null ||
        isShortPlaceholderDuration(playbackSnapshot.durationMs)
    ) return false
    // Preload: trigger source fetch before the button appears
    if (playerSettingsUiState.preloadNextEpisodeSources && !nextEpisodePreloadTriggered && nextEpisodeInfo != null) {
        // Start fetching the next episode well before the credits so the source
        // list is warm when the handoff happens (never shown as "fetching").
        val preloadLeadMs = maxOf(
            playerSettingsUiState.streamAutoPlayTimeoutSeconds.toLong() * 1_000L,
            NEXT_EPISODE_PRELOAD_LEAD_MS,
        )
        val shouldPreload = PlayerNextEpisodeRules.shouldShowNextEpisodeCard(
            positionMs = playbackSnapshot.positionMs + preloadLeadMs,
            durationMs = playbackSnapshot.durationMs,
            skipIntervals = skipIntervals,
            thresholdMode = playerSettingsUiState.nextEpisodeThresholdMode,
            thresholdPercent = playerSettingsUiState.nextEpisodeThresholdPercent,
            thresholdMinutesBeforeEnd = playerSettingsUiState.nextEpisodeThresholdMinutesBeforeEnd,
        )
        if (shouldPreload) {
            preloadNextEpisodeSources()
        }
    }
    return playbackSnapshot.isEnded || PlayerNextEpisodeRules.shouldShowNextEpisodeCard(
        positionMs = playbackSnapshot.positionMs,
        durationMs = playbackSnapshot.durationMs,
        skipIntervals = skipIntervals,
        thresholdMode = playerSettingsUiState.nextEpisodeThresholdMode,
        thresholdPercent = playerSettingsUiState.nextEpisodeThresholdPercent,
        thresholdMinutesBeforeEnd = playerSettingsUiState.nextEpisodeThresholdMinutesBeforeEnd,
    )
}

internal fun PlayerScreenRuntime.cancelNextEpisodeAutoPlay() {
    nextEpisodeAutoPlayJob?.cancel()
    nextEpisodeAutoPlayJob = null
    nextEpisodeAutoPlayAutomatic = false
    nextEpisodeAutoPlaySearching = false
    nextEpisodeAutoPlaySourceName = null
    nextEpisodeAutoPlayCountdown = null
}

internal fun CoroutineScope.launchPlayerNextEpisodeAutoPlay(
    previousJob: Job?,
    nextEpisodeInfo: NextEpisodeInfo?,
    allEpisodes: List<MetaVideo>,
    parentMetaId: String,
    parentMetaType: String,
    contentType: String?,
    settings: PlayerSettingsUiState,
    currentStreamBingeGroup: String?,
    preferredProviderAddonId: String?,
    preferredProviderName: String?,
    onDownloadedEpisodeSelected: (DownloadItem, MetaVideo) -> Unit,
    onEpisodeStreamSelected: (StreamItem, MetaVideo) -> Unit,
    onManualSelectionRequired: (MetaVideo) -> Unit,
    onSearchingChanged: (Boolean) -> Unit,
    onSourceNameChanged: (String?) -> Unit,
    onCountdownChanged: (Int?) -> Unit,
    onNextEpisodeCardVisibleChanged: (Boolean) -> Unit,
): Job? {
    val nextVideoId = nextEpisodeInfo?.videoId ?: return null
    val nextVideo = allEpisodes.firstOrNull { video -> video.id == nextVideoId } ?: return null
    if (nextEpisodeInfo.hasAired != true) return null

    val downloadedNextEpisode = DownloadsRepository.findPlayableDownload(
        parentMetaId = parentMetaId,
        seasonNumber = nextVideo.season,
        episodeNumber = nextVideo.episode,
        videoId = nextVideo.id,
    )
    if (downloadedNextEpisode != null) {
        onDownloadedEpisodeSelected(downloadedNextEpisode, nextVideo)
        return null
    }

    previousJob?.cancel()
    onSearchingChanged(true)
    onSourceNameChanged(null)
    onCountdownChanged(null)

    val type = contentType ?: parentMetaType
    // Prefer continuing on the provider that is already playing this episode so
    // the next episode starts on the same addon/plugin instead of showing the
    // full source list again.
    val sameProviderRequested = settings.streamAutoPlayPreferBingeGroup
    val providerAddonId = preferredProviderAddonId?.takeIf { it.isNotBlank() }
    val providerName = preferredProviderName?.takeIf { it.isNotBlank() }
    val hasProviderPreference = sameProviderRequested &&
        (
            providerAddonId != null ||
                providerName?.let { name -> AddonRepository.uiState.value.addons.enabledAddons().any { it.displayTitle == name } } == true
            )
    val shouldAutoSelectInManualMode =
        settings.streamAutoPlayMode == StreamAutoPlayMode.MANUAL &&
            (
                settings.streamAutoPlayNextEpisodeEnabled ||
                    settings.streamAutoPlayPreferBingeGroup ||
                    hasProviderPreference
                )

    val bingeGroupOnlyManualMode =
        shouldAutoSelectInManualMode &&
            (!settings.streamAutoPlayNextEpisodeEnabled ||
                !settings.streamAutoPlayNextEpisodeFallbackEnabled) &&
            settings.streamAutoPlayPreferBingeGroup

    val effectiveMode = if (shouldAutoSelectInManualMode) {
        StreamAutoPlayMode.FIRST_STREAM
    } else {
        settings.streamAutoPlayMode
    }
    val effectiveSource = if (shouldAutoSelectInManualMode) {
        StreamAutoPlaySource.ALL_SOURCES
    } else {
        settings.streamAutoPlaySource
    }
    val effectiveSelectedAddons = if (shouldAutoSelectInManualMode) {
        emptySet()
    } else {
        settings.streamAutoPlaySelectedAddons
    }
    val effectiveSelectedPlugins = if (shouldAutoSelectInManualMode) {
        emptySet()
    } else {
        settings.streamAutoPlaySelectedPlugins
    }
    val effectiveRegex = if (shouldAutoSelectInManualMode) {
        ""
    } else {
        settings.streamAutoPlayRegex
    }
    val preferredBingeGroup = if (settings.streamAutoPlayPreferBingeGroup) {
        currentStreamBingeGroup
    } else {
        null
    }

    return launch {
        PlayerStreamsRepository.loadEpisodeStreams(
            type = type,
            videoId = nextVideo.id,
            season = nextVideo.season,
            episode = nextVideo.episode,
        )

        if (effectiveMode == StreamAutoPlayMode.MANUAL) {
            onSearchingChanged(false)
            onNextEpisodeCardVisibleChanged(false)
            onManualSelectionRequired(nextVideo)
            return@launch
        }

        val installedAddonNames = AddonRepository.uiState.value.addons
            .enabledAddons()
            .map { it.displayTitle }
            .toSet()
        val debridSettings = DebridSettingsRepository.snapshot()

        val timeoutSeconds = settings.streamAutoPlayTimeoutSeconds
        var autoSelectTriggered = false
        var selectedStream: StreamItem? = null
        val autoSelectSettled = CompletableDeferred<Unit>()

        fun settleAutoSelect() {
            if (!autoSelectSettled.isCompleted) {
                autoSelectSettled.complete(Unit)
            }
        }

        fun selectStream(stream: StreamItem) {
            autoSelectTriggered = true
            selectedStream = stream
            settleAutoSelect()
        }

        fun finishWithoutSelection() {
            autoSelectTriggered = true
            settleAutoSelect()
        }

        fun trySelectStream(streams: List<StreamItem>): StreamItem? =
            StreamAutoPlaySelector.selectAutoPlayStream(
                streams = streams,
                mode = effectiveMode,
                regexPattern = effectiveRegex,
                source = effectiveSource,
                installedAddonNames = installedAddonNames,
                selectedAddons = effectiveSelectedAddons,
                selectedPlugins = effectiveSelectedPlugins,
                preferredBingeGroup = preferredBingeGroup,
                preferBingeGroupInSelection = settings.streamAutoPlayPreferBingeGroup,
                bingeGroupOnly = bingeGroupOnlyManualMode,
                debridEnabled = debridSettings.canResolvePlayableLinks,
                activeResolverProviderId = debridSettings.activeResolverProviderId,
            )

        fun tryBingeGroupOnly(streams: List<StreamItem>): StreamItem? {
            if (preferredBingeGroup == null || !settings.streamAutoPlayPreferBingeGroup) return null
            return StreamAutoPlaySelector.selectAutoPlayStream(
                streams = streams,
                mode = effectiveMode,
                regexPattern = effectiveRegex,
                source = effectiveSource,
                installedAddonNames = installedAddonNames,
                selectedAddons = effectiveSelectedAddons,
                selectedPlugins = effectiveSelectedPlugins,
                preferredBingeGroup = preferredBingeGroup,
                preferBingeGroupInSelection = true,
                bingeGroupOnly = true,
                debridEnabled = debridSettings.canResolvePlayableLinks,
                activeResolverProviderId = debridSettings.activeResolverProviderId,
            )
        }

        // Continue from the same provider when possible (matched by the addon
        // id, then by display name as a fallback for plugins).
        fun trySelectFromProvider(streams: List<StreamItem>): StreamItem? {
            if (!hasProviderPreference) return null
            val matchesProvider = { stream: StreamItem ->
                stream.serverTarget == null && (
                    (providerAddonId != null && stream.addonId == providerAddonId) ||
                        (providerName != null && stream.addonName == providerName)
                    )
            }
            val providerStreams = streams.filter(matchesProvider)
            if (providerStreams.isEmpty()) return null
            return providerStreams.firstOrNull { it.playableDirectUrl != null } ?: providerStreams.first()
        }

        val selectionCoordinator = NextEpisodeStreamSelectionCoordinator(
            selectAfterDelay = ::trySelectStream,
            selectPreferred = ::tryBingeGroupOnly,
            selectFromProvider = ::trySelectFromProvider,
        )

        fun applySelectionDecision(decision: NextEpisodeStreamSelectionDecision) {
            if (autoSelectTriggered) return
            when (decision) {
                is NextEpisodeStreamSelectionDecision.Selected -> selectStream(decision.stream)
                NextEpisodeStreamSelectionDecision.ManualSelection -> finishWithoutSelection()
                NextEpisodeStreamSelectionDecision.Waiting -> Unit
            }
        }

        val innerJob = launch {
            PlayerStreamsRepository.episodeStreamsState.collectLatest { state ->
                applySelectionDecision(selectionCoordinator.onStreamsChanged(state))
            }
        }

        try {
            val timeoutMs = timeoutSeconds * 1_000L
            val isBoundedTimeout = timeoutSeconds in 1..30

            if (isBoundedTimeout) {
                // The configured delay is a maximum wait for the provider-preferred
                // (binge group / same-provider) match, not a hard stop: if nothing
                // was found we keep waiting for the full stream timeout below.
                withTimeoutOrNull(timeoutMs) { autoSelectSettled.await() }
            }
            applySelectionDecision(
                selectionCoordinator.onSelectionDelayElapsed(PlayerStreamsRepository.episodeStreamsState.value),
            )
            val completed = withTimeoutOrNull(NEXT_EPISODE_HARD_TIMEOUT_MS) { autoSelectSettled.await() }
            innerJob.cancel()
            if (completed == null && !autoSelectTriggered) {
                val allStreams = PlayerStreamsRepository.episodeStreamsState.value.groups.flatMap { it.streams }
                trySelectFromProvider(allStreams)
                    ?: trySelectStream(allStreams)
                    ?: finishWithoutSelection()
            }

            val selected = selectedStream
            if (selected != null) {
                // Clear the searching indicator before the countdown so the card
                // shows the resolved source name while counting down.
                onSearchingChanged(false)
                finishNextEpisodeAutoPlay(
                    stream = selected,
                    nextVideo = nextVideo,
                    type = type,
                    onEpisodeStreamSelected = onEpisodeStreamSelected,
                    onSourceNameChanged = onSourceNameChanged,
                    onCountdownChanged = onCountdownChanged,
                    onNextEpisodeCardVisibleChanged = onNextEpisodeCardVisibleChanged,
                    onManualSelectionRequired = onManualSelectionRequired,
                )
            } else {
                onNextEpisodeCardVisibleChanged(false)
                onManualSelectionRequired(nextVideo)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            // Never leave the player stuck on "fetching sources" if source
            // resolution fails unexpectedly.
            failure.message?.takeIf { it.isNotBlank() }?.let { message ->
                NuvioToastController.show(message)
            }
        } finally {
            innerJob.cancel()
            // Guarantees the "fetching sources" indicator is cleared on every exit
            // path, including cancellation or an unexpected failure, so the player
            // can never get stuck on the loading state.
            onSearchingChanged(false)
        }
    }
}

private suspend fun finishNextEpisodeAutoPlay(
    stream: StreamItem,
    nextVideo: MetaVideo,
    type: String,
    onEpisodeStreamSelected: (StreamItem, MetaVideo) -> Unit,
    onSourceNameChanged: (String?) -> Unit,
    onCountdownChanged: (Int?) -> Unit,
    onNextEpisodeCardVisibleChanged: (Boolean) -> Unit,
    onManualSelectionRequired: (MetaVideo) -> Unit,
) {
    val resolved = runCatching {
        withTimeoutOrNull(NEXT_EPISODE_DEBRID_TIMEOUT_MS) {
            DirectDebridPlaybackResolver.resolveToPlayableStream(stream, nextVideo.season, nextVideo.episode)
        }
    }.getOrNull()
    when (resolved) {
        is DirectDebridPlayableResult.Success -> Unit
        null, DirectDebridPlayableResult.Stale -> {
            PlayerStreamsRepository.loadEpisodeStreams(
                type = type,
                videoId = nextVideo.id,
                season = nextVideo.season,
                episode = nextVideo.episode,
                forceRefresh = true,
            )
        }
        else -> resolved?.toastMessage()?.let { NuvioToastController.show(it) }
    }
    val playable = (resolved as? DirectDebridPlayableResult.Success)?.stream
    if (playable != null) {
        onSourceNameChanged((playable.name?.takeIf { it.isNotBlank() } ?: playable.addonName).trim())
        for (i in 3 downTo 1) {
            onCountdownChanged(i)
            delay(1000)
        }
        onEpisodeStreamSelected(playable, nextVideo)
        onNextEpisodeCardVisibleChanged(false)
        onCountdownChanged(null)
        onSourceNameChanged(null)
    } else {
        onManualSelectionRequired(nextVideo)
        onNextEpisodeCardVisibleChanged(false)
    }
}

// Starts fetching the next episode's sources as soon as the runtime is ready,
// independent of the end-of-episode threshold, so the handoff does not wait on
// a cold source list.
internal fun PlayerScreenRuntime.maybePreloadNextEpisodeSources() {
    if (!playerSettingsUiState.preloadNextEpisodeSources) return
    if (nextEpisodePreloadTriggered) return
    if (nextEpisodeInfo == null) return
    preloadNextEpisodeSources()
}

internal fun PlayerScreenRuntime.preloadNextEpisodeSources() {
    if (nextEpisodePreloadTriggered) return
    val nextEp = nextEpisodeInfo ?: return
    if (nextEp.hasAired != true) return
    val type = contentType ?: parentMetaType

    nextEpisodePreloadTriggered = true
    nextEpisodePreloadJob?.cancel()
    nextEpisodePreloadJob = scope.launch {
        runCatching {
            PlayerStreamsRepository.loadEpisodeStreams(
                type = type,
                videoId = nextEp.videoId,
                season = nextEp.season,
                episode = nextEp.episode
            )
        }.onFailure { failure ->
            if (failure is CancellationException) throw failure
            preloadFailureLog.e("Next-episode preload failed", failure)
        }
    }
}

internal fun PlayerScreenRuntime.cancelNextEpisodePreload() {
    nextEpisodePreloadJob?.cancel()
    nextEpisodePreloadJob = null
    nextEpisodePreloadTriggered = false
}
