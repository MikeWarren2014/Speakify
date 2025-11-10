package com.mikewarren.speakify.activities

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mikewarren.speakify.data.events.BaseEventBus
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.viewsAndViewModels.pages.fetcher.BaseFetcherViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.fetcher.FetcherView
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

abstract class BaseFetcherActivity<Model, Event> (
    protected val eventBus: BaseEventBus<Event>,
    protected val permission: String,
    protected val permissionRequestCode : Int,
): AppCompatActivity() {
    protected abstract val viewModel: BaseFetcherViewModel

    private val dataFlow: MutableSharedFlow<List<Model>> =
        MutableSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FetcherView(viewModel)
                }
            }
        }

        requestDataAccessPermission()
    }

    protected fun requestDataAccessPermission() {
        if (!hasDataAccessPermission()) {
            requestPermissions(
                arrayOf(this.permission),
                permissionRequestCode
            )
            return
        }
        fetchData()

    }

    protected fun hasDataAccessPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            this.permission,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != permissionRequestCode)
            return

        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            fetchData()
            return
        }
        eventBus.post(getPermissionDeniedEvent())
        finish()
    }

    private fun fetchData() {
        lifecycleScope.launch {
            try {
                viewModel.setIsLoading(true)
                val data = fetchDataFromSystem()
                eventBus.post(getDataFetchedEvent(data))
            } catch (e: Exception) {
                eventBus.post(getFetchFailedEvent(e.message ?: "Unknown error"))
            } finally {
                finish()
                viewModel.setIsLoading(false)
            }
        }
    }

    abstract fun getPermissionDeniedEvent() : Event
    abstract fun getDataFetchedEvent(data: List<Model>) : Event
    abstract fun getFetchFailedEvent(message: String) : Event

    protected abstract suspend fun fetchDataFromSystem(): List<Model>



}