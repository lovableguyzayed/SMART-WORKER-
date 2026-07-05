package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppContainer
import kotlinx.coroutines.flow.StateFlow

/** Provides the app's DI container to any composable in the tree. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

/** Short alias so screens read cleanly. */
@Composable
fun <T> StateFlow<T>.collectAsStateLifecycle(): State<T> = collectAsStateWithLifecycle()
