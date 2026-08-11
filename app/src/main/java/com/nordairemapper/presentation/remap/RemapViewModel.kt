package com.nordairemapper.presentation.remap

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.RemapConfigRepository
import com.nordairemapper.presentation.common.conflictKey
import com.nordairemapper.presentation.home.HomeViewModel
import com.nordairemapper.service.ActionDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
)

data class RemapUiState(
    val pressType: PressType = PressType.SINGLE,
    val currentAction: RemapAction = RemapAction.None,
    val hasConflict: Boolean = false,
    val conflictWith: List<PressType> = emptyList(),
)

@HiltViewModel
class RemapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val remapConfigRepository: RemapConfigRepository,
    private val actionDispatcher: ActionDispatcher,
) : ViewModel() {

    val pressType: PressType = PressType.fromKey(
        savedStateHandle.get<String>("pressType") ?: PressType.SINGLE.key
    )

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<RemapUiState> = combine(
        remapConfigRepository.observeConfigs(),
        remapConfigRepository.observeConfigs().map { it[pressType] ?: RemapAction.None },
    ) { all, current ->
        val conflicts = HomeViewModel.conflictPressTypes(all)
        val others = PressType.entries.filter { other ->
            other != pressType &&
                current !is RemapAction.None &&
                all[other]?.conflictKey() == current.conflictKey()
        }
        RemapUiState(
            pressType = pressType,
            currentAction = current,
            hasConflict = pressType in conflicts,
            conflictWith = others,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        RemapUiState(pressType = pressType),
    )

    fun setAction(action: RemapAction) {
        viewModelScope.launch {
            remapConfigRepository.setAction(pressType, action)
            _events.emit("Saved: ${action::class.simpleName}")
        }
    }

    fun tryNow() {
        viewModelScope.launch {
            actionDispatcher.execute(uiState.value.currentAction)
            _events.emit("Tried current action")
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .mapNotNull { resolve ->
                    val info = resolve.activityInfo ?: return@mapNotNull null
                    val label = resolve.loadLabel(pm)?.toString().orEmpty()
                    InstalledAppInfo(packageName = info.packageName, label = label)
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
            _installedApps.value = apps
        }
    }
}
