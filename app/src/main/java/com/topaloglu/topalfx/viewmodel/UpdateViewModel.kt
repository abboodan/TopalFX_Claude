package com.topaloglu.topalfx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topaloglu.topalfx.updater.ApkDownloader
import com.topaloglu.topalfx.updater.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val tag: String, val changelog: String, val apkUrl: String) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class ReadyToInstall(val file: File) : UpdateState
    data object Failed : UpdateState
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String
        get() = UpdateChecker.currentVersionName(getApplication())

    fun checkForUpdate(silent: Boolean = false) {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Checking
        viewModelScope.launch {
            val release = UpdateChecker.fetchLatest()
            _state.value = when {
                release == null -> if (silent) UpdateState.Idle else UpdateState.Failed
                UpdateChecker.isUpdateAvailable(release, getApplication()) ->
                    UpdateState.Available(release.tag, release.changelog, release.apkUrl!!)
                else -> if (silent) UpdateState.Idle else UpdateState.UpToDate
            }
        }
    }

    fun download() {
        val available = _state.value as? UpdateState.Available ?: return
        _state.value = UpdateState.Downloading(0f)
        viewModelScope.launch {
            val file = ApkDownloader.download(getApplication(), available.apkUrl) { progress ->
                _state.value = UpdateState.Downloading(progress)
            }
            _state.value = if (file != null) UpdateState.ReadyToInstall(file) else UpdateState.Failed
        }
    }

    fun install() {
        val ready = _state.value as? UpdateState.ReadyToInstall ?: return
        // State stays ReadyToInstall: the first attempt may bounce the user to the
        // "allow from this source" settings screen, and they need the button again.
        ApkDownloader.install(getApplication(), ready.file)
    }

    fun dismiss() {
        if (_state.value !is UpdateState.Downloading) _state.value = UpdateState.Idle
    }
}
