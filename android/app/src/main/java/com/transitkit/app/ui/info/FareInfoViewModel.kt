package com.transitkit.app.ui.info

import androidx.lifecycle.ViewModel
import com.transitkit.app.config.FareInfo
import com.transitkit.app.config.OperatorConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FareInfoViewModel @Inject constructor(
    private val config: OperatorConfig,
) : ViewModel() {
    val fares: FareInfo? = config.fares
    val operatorUrl: String? = config.url.takeIf { it.isNotBlank() }
}
