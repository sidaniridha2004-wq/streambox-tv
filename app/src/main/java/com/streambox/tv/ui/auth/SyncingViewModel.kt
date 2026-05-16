package com.streambox.tv.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streambox.tv.data.IptvRepository
import com.streambox.tv.data.Provider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SyncingViewModel @Inject constructor(
    repo: IptvRepository,
) : ViewModel() {
    val activeProvider = combine(repo.providers, repo.activeProviderId) { list, id ->
        list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null as Provider?)
}
