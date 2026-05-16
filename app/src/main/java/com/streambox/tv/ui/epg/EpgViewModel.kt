package com.streambox.tv.ui.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streambox.tv.data.Channel
import com.streambox.tv.data.EpgProgram
import com.streambox.tv.data.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class EpgUiState(
    val channels: List<Channel> = emptyList(),
    val epgByChannel: Map<String, List<EpgProgram>> = emptyMap(),
    val timeLabel: String = "20:00",
)

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val repo: IptvRepository,
) : ViewModel() {
    val state = repo.channels.map { channels ->
        EpgUiState(
            channels = channels,
            epgByChannel = channels.associate { ch ->
                ch.id to repo.epg.filter { it.channelId == ch.id }.sortedBy { it.startMinute }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EpgUiState())
}
