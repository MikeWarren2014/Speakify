package com.mikewarren.speakify.viewsAndViewModels.pages

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikewarren.speakify.R

@Composable
fun AboutView() {
    val viewModel: AboutViewModel = hiltViewModel()
    val info = viewModel.aboutInfo

    Column(modifier = Modifier.padding(16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        HeaderView(R.string.about_header_what)
        Text(text = stringResource(R.string.about_description_para1))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.about_description_para2))
        Spacer(modifier = Modifier.height(16.dp))
        HeaderView(R.string.about_header_app_info)

        Text(text = stringResource(R.string.about_label_app, info.appName))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.about_label_version, info.version))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.about_label_author, info.author))
    }
}

@Composable
fun HeaderView(@StringRes headerResId: Int) {
    Text(
        text = stringResource(headerResId),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
}
