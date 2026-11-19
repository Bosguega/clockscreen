package br.com.clockscreen

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateDate(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        updateDate(context, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> updateAllWidgets(context)
        }
    }

    private fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = android.content.ComponentName(context, ClockWidgetProvider::class.java)
        updateDate(context, manager.getAppWidgetIds(component))
    }

    private fun updateDate(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return

        val weekday = WidgetDateFormatter.weekday()
        val date = WidgetDateFormatter.date()

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_clock).apply {
                setTextViewText(R.id.tv_widget_weekday, weekday)
                setTextViewText(R.id.tv_widget_date, date)
            }
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        }
    }
}
