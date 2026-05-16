package com.transitkit.app.ui.dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.push.PushApiClient
import com.transitkit.app.data.push.PushNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeveloperModeState(
    val operatorId: String = "",
    val fcmToken: String? = null,
    val firebaseConfigured: Boolean = false,
    val consoleBaseUrl: String? = null,
    val lastError: String? = null,
    val defaultLabel: String = "",
    val registering: Boolean = false,
    val resultMessage: String? = null,
    val resultIsError: Boolean = false,
)

@HiltViewModel
class DeveloperModeViewModel @Inject constructor(
    private val config: OperatorConfig,
    private val pushManager: PushNotificationManager,
    private val apiClient: PushApiClient,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DeveloperModeState(
            operatorId = config.id,
            firebaseConfigured = pushManager.isFirebaseConfigured,
            consoleBaseUrl = apiClient.resolveBaseUrl(),
            defaultLabel = apiClient.defaultDeviceLabel(),
        )
    )
    val state: StateFlow<DeveloperModeState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val token = pushManager.ensureToken()
            _state.update { current ->
                current.copy(
                    fcmToken = token,
                    firebaseConfigured = pushManager.isFirebaseConfigured,
                    lastError = pushManager.lastError.value,
                )
            }
        }
    }

    fun registerDevice(label: String) {
        val token = _state.value.fcmToken ?: return
        if (label.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(registering = true, resultMessage = null, resultIsError = false) }
            val result = apiClient.registerTestDevice(token, label)
            _state.update {
                when (result) {
                    is PushApiClient.Result.Ok -> it.copy(
                        registering = false,
                        resultMessage = "✓ Registered. You can now receive previews from the CMS.",
                        resultIsError = false,
                    )
                    is PushApiClient.Result.Err -> it.copy(
                        registering = false,
                        resultMessage = "Registration failed: ${result.message}",
                        resultIsError = true,
                    )
                }
            }
        }
    }
}
