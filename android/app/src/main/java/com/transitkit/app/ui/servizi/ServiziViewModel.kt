package com.transitkit.app.ui.servizi

import androidx.lifecycle.ViewModel
import com.transitkit.app.config.OperatorConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ServiziViewModel @Inject constructor(
    private val config: OperatorConfig,
) : ViewModel() {
    val operatorConfig: OperatorConfig = config
}
