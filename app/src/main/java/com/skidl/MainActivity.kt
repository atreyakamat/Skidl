package com.skidl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skidl.ui.AppNavHost
import com.skidl.ui.SkidlViewModel
import com.skidl.ui.theme.SkidlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val app = application as SkidlApplication
        setContent {
            val viewModel: SkidlViewModel = viewModel(
                factory = SkidlViewModelFactory(app)
            )
            val uiState by viewModel.state.collectAsState()
            SkidlTheme(darkTheme = isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier.systemBarsPadding()
                    )
                }
            }
        }
    }
}

class SkidlViewModelFactory(
    private val app: SkidlApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SkidlViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SkidlViewModel(app.applicationScope, app.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.simpleName}")
    }
}
