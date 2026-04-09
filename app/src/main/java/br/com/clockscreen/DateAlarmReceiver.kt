package br.com.clockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DateAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ClockWidgetProvider.updateDate(context)
    }
}
