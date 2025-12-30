package com.mikewarren.speakify.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.accountDeletion.AccountDeletionView

class AccountDeletedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
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

        // Handle account deletion logic here
    }
}