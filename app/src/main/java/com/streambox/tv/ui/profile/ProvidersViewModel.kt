package com.streambox.tv.ui.profile

import androidx.lifecycle.ViewModel
import com.streambox.tv.data.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProvidersViewModel @Inject constructor(
    private val repo: IptvRepository,
) : ViewModel() {
    val providers = repo.providers
    val activeProviderId = repo.activeProviderId
    fun setActive(id: String) = repo.setActiveProvider(id)
    fun delete(id: String) = repo.deleteProvider(id)
}
