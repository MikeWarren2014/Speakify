package com.mikewarren.speakify.viewsAndViewModels.pages.auth.accountDeletion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.EmailVerificationView

import com.mikewarren.speakify.R

@Composable
fun AccountDeletionView(onCancel: () -> Unit, onDeleted: () -> Unit) {
    val viewModel: AccountDeletionViewModel = viewModel()

    val uiState by viewModel.uiState.collectAsState()
    when (uiState) {
        AccountDeletionUiState.RequestMade -> {
            AreYouSureView(viewModel, onCancel)
        }
        AccountDeletionUiState.RequestConfirmed -> {
            VerifyEmailView(viewModel)
        }
        AccountDeletionUiState.Verified -> {
            DeletingAccountView(viewModel)
        }
        AccountDeletionUiState.Deleted -> {
            // TODO: Show deleted screen
            onDeleted()
        }

    }
}

@Composable
fun AreYouSureView(viewModel: AccountDeletionViewModel, onCancel: () -> Unit) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Are You Absolutely Sure?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This action is irreversible and cannot be undone. All of your data, including your settings and saved app preferences, will be permanently deleted.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.startDeletionProcess(onCancel) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete My Account")
                }
            }
        }
    }
}

@Composable
fun VerifyEmailView(viewModel: AccountDeletionViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()

    EmailVerificationView(text = stringResource(R.string.delete_account_verification_message),
        isLoading,
        buttonText = "Verify & Delete Account",
        onDone = viewModel::verify)

}

@Composable
fun DeletingAccountView(viewModel: AccountDeletionViewModel) {
    LaunchedEffect(Unit) {
        viewModel.deleteUser()
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Deleting your account...",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This may take a moment. Please do not close the app.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}