package com.westkitty.dexwallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.westkitty.dexwallpaper.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as DexApplication).graph
        setContent {
            val vm: DexViewModel = viewModel(factory = viewModelFactory { initializer { DexViewModel(graph, createSavedStateHandle()) } })
            DexWorkspace(vm)
        }
    }
}
