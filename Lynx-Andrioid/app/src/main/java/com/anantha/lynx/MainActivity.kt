package com.anantha.lynx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.anantha.lynx.app.RootScreen
import com.anantha.lynx.ui.theme.LynxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LynxTheme {
                // Lynx screens draw their own full-bleed dark background and
                // manage insets themselves (mirrors RootView.swift's
                // .ignoresSafeArea() usage), so no Scaffold/innerPadding here.
                RootScreen(activity = this, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
