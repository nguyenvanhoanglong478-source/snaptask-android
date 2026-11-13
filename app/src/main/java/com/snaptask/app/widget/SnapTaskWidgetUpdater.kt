package com.snaptask.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object SnapTaskWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        scope.launch { updateAllWidgets(appContext) }
    }

    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        runCatching { SnapTaskWidget().updateAll(appContext) }
        runCatching { LocketFocusWidget().updateAll(appContext) }
        runCatching { NotionStackWidget().updateAll(appContext) }
    }
}
