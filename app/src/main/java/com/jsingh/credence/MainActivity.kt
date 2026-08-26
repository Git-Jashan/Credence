package com.jsingh.credence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jsingh.credence.ui.screens.MainApp
import com.jsingh.credence.ui.theme.CredenceTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PDFBoxResourceLoader.init(applicationContext)

        setContent {
            CredenceTheme {
                MainApp()
            }
        }
    }
}