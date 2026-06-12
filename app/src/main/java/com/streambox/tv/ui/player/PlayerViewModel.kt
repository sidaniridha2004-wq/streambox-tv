package com.streambox.tv.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streambox.tv.data.Channel
import com.streambox.tv.data.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PlayerUiState(
    val currentChannel: Channel? = null,
    val channels: List<Channel> = emptyList(),
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    repo: IptvRepository,
) : ViewModel() {
    val state = repo.channels.map { channels ->
        PlayerUiState(currentChannel = channels.firstOrNull(), channels = channels)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerUiState())
}
