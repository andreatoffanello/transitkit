package com.transitkit.app.ui.info

import androidx.lifecycle.ViewModel
import com.transitkit.app.config.OperatorConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OperatorInfoViewModel @Inject constructor(
    val config: OperatorConfig,
) : ViewModel()
