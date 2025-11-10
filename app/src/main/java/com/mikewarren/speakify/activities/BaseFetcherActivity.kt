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
    eventBus: BaseEventBus<Event>,
    protected val permission: String,
    permissionRequestCode : Int,
): BasePermissionRequesterActivity<Event>(eventBus, permissionRequestCode) {
    protected abstract val viewModel: BaseFetcherViewModel

    private val dataFlow: MutableSharedFlow<List<Model>> =
        MutableSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestDataAccessPermission()
    }

    override fun doDisplay() {
        super.doDisplay()
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
    }

    override fun getPermissions(): Array<String> {
        return arrayOf(permission)
    }

    protected fun requestDataAccessPermission() {
        requestPermissions()
    }

    override fun onPermissionGranted() {
        fetchData()
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

    override fun getFailureEvent(message: String) : Event {
        return getFetchFailedEvent(message)
    }

    abstract fun getDataFetchedEvent(data: List<Model>) : Event
    abstract fun getFetchFailedEvent(message: String) : Event

    protected abstract suspend fun fetchDataFromSystem(): List<Model>



}