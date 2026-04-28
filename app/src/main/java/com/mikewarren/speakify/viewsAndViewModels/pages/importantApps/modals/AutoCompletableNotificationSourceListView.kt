package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.AutoCompletableView
import com.mikewarren.speakify.viewsAndViewModels.widgets.BaseAutoCompletableViewModel

@Composable
fun <T : Any?> AutoCompletableNotificationSourceListView(
    viewModel: BaseAutoCompletableNotificationSourceListViewModel<T>
) {
    NotificationSourceListView(viewModel) { viewModel ->
        AutoCompletableView(
            viewModel as BaseAutoCompletableViewModel<T>,
            onGetDefaultValues = { viewModel ->
                val vm = viewModel as BaseAutoCompletableNotificationSourceListViewModel<T>
                if (vm.isDataLoading) {
                    return@AutoCompletableView emptyList()
                }
                vm.allAddableSourceModels
                    .value
            },
            onHandleSelection = { viewModel, selection -> (viewModel as BaseAutoCompletableNotificationSourceListViewModel<T>).addNotificationSource(selection) },
            onGetAnnotatedString = { choice: T ->
                val viewString = viewModel.toViewString(choice)

                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    ) {
                        append(viewString)
                    }
                    addStringAnnotation(
                        tag = "Clickable",
                        annotation = viewString,
                        start = 0,
                        end = viewString.length
                    )
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add),
                )
            }
        )
    }
}