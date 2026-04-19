package com.transitkit.app.ui.info

import androidx.lifecycle.ViewModel
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    private val config: OperatorConfig,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {
    val operatorConfig: OperatorConfig = config
    val lastUpdated: StateFlow<String?> = scheduleRepository.lastUpdated
}
