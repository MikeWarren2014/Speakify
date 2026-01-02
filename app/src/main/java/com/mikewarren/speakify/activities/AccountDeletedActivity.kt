package com.mikewarren.speakify.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.accountDeletion.AccountDeletionView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountDeletedActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: SettingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
            val useDarkTheme by settingsViewModel.useDarkTheme.collectAsStateWithLifecycle(isSystemInDarkTheme())

            MyApplicationTheme(darkTheme = useDarkTheme == true) {
                AccountDeletionView(
                    onCancel = {
                        // Handle back navigation or close the screen
                        finish()
                    },
                    onDeleted = {
                        this.finishAffinity()
                        this.startActivity(
                            Intent(this, LoginActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            null
                        )
                    }
                )
            }
        }

    }
}
