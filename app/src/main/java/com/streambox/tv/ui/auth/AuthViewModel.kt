package com.streambox.tv.ui.auth

import androidx.lifecycle.ViewModel
import com.streambox.tv.data.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: IptvRepository,
) : ViewModel() {
    fun addM3u(name: String, url: String, epgUrl: String?, user: String?, pass: String?) {
        repo.addM3uProvider(name, url, epgUrl, user, pass)
    }

    fun addStalker(portal: String, mac: String, deviceId: String?, serial: String?) {
        repo.addStalkerProvider(portal, mac, deviceId, serial)
    }

    fun addXtream(name: String, host: String, user: String, pass: String, output: String) {
        repo.addXtreamProvider(name, host, user, pass, output)
    }
}
