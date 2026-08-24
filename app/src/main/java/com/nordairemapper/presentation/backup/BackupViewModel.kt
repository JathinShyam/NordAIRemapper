package com.nordairemapper.presentation.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.domain.model.ConfigSnapshot
import com.nordairemapper.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val app: Application,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    val snapshots: StateFlow<List<ConfigSnapshot>> = backupRepository.observeSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun saveSnapshot(name: String) {
        viewModelScope.launch {
            backupRepository.createSnapshot(name)
            _messages.emit("Snapshot saved")
        }
    }

    fun deleteSnapshot(id: Long) {
        viewModelScope.launch {
            backupRepository.deleteSnapshot(id)
            _messages.emit("Snapshot deleted")
        }
    }

    fun restoreSnapshot(id: Long) {
        viewModelScope.launch {
            // Corrupt/truncated snapshot payloads throw on decode — surface as
            // a message like imports do, never crash.
            runCatching { backupRepository.restoreSnapshot(id) }
                .onSuccess { _messages.emit("Snapshot restored") }
                .onFailure { _messages.emit("Restore failed: ${it.message}") }
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                app.contentResolver.openOutputStream(uri)?.use { backupRepository.exportTo(it) }
                    ?: error("Could not open file")
            }.onSuccess { _messages.emit("Exported") }
                .onFailure { _messages.emit("Export failed: ${it.message}") }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                app.contentResolver.openInputStream(uri)?.use { backupRepository.importFrom(it) }
                    ?: error("Could not open file")
            }.onSuccess { _messages.emit("Imported") }
                .onFailure { _messages.emit("Import failed: ${it.message}") }
        }
    }
}
