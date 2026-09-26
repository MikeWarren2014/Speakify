package com.mikewarren.speakify.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.viewsAndViewModels.pages.brokenNotification.BrokenNotificationReportScreen
import com.mikewarren.speakify.viewsAndViewModels.pages.brokenNotification.BrokenNotificationReportViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrokenNotificationReportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val viewModel: BrokenNotificationReportViewModel by viewModels()

        setContent {
            MyApplicationTheme {
                BrokenNotificationReportScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }
}
