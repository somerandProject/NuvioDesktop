package com.nuvio.app.features.player

import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.streams.StreamsUiState

internal sealed interface NextEpisodeStreamSelectionDecision {
    data class Selected(val stream: StreamItem) : NextEpisodeStreamSelectionDecision
    data object Waiting : NextEpisodeStreamSelectionDecision
    data object ManualSelection : NextEpisodeStreamSelectionDecision
}

internal class NextEpisodeStreamSelectionCoordinator(
    private val selectAfterDelay: (List<StreamItem>) -> StreamItem?,
    private val selectPreferred: (List<StreamItem>) -> StreamItem?,
    private val selectFromProvider: (List<StreamItem>) -> StreamItem? = { null },
) {
    private var selectionDelayElapsed = false

    fun onStreamsChanged(state: StreamsUiState): NextEpisodeStreamSelectionDecision = decide(state)

    fun onSelectionDelayElapsed(state: StreamsUiState): NextEpisodeStreamSelectionDecision {
        selectionDelayElapsed = true
        return decide(state)
    }

    private fun decide(state: StreamsUiState): NextEpisodeStreamSelectionDecision {
        val streams = state.groups.flatMap { it.streams }
        val isLoading = state.isAnyLoading || state.groups.any { it.isLoading }
        val selected = if (selectionDelayElapsed || !isLoading) {
            // Same provider as the episode that is ending wins over generic rules,
            // so the next episode starts from the source already in use.
            selectFromProvider(streams)
                ?: selectAfterDelay(streams)
                ?: selectPreferred(streams)
        } else {
            selectPreferred(streams) ?: selectFromProvider(streams)
        }
        return when {
            selected != null -> NextEpisodeStreamSelectionDecision.Selected(selected)
            isLoading -> NextEpisodeStreamSelectionDecision.Waiting
            else -> NextEpisodeStreamSelectionDecision.ManualSelection
        }
    }
}
