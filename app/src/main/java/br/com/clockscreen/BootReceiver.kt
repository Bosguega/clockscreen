package br.com.clockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver para BOOT_COMPLETED.
 * Reagenda o alarme de meia-noite e atualiza o widget após reboot do dispositivo.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (PermissionUtils.hasAlarmPermission(context)) {
                DateAlarmScheduler.scheduleMidnightUpdate(context)
            }
            ClockWidgetProvider.updateDate(context)
        }
    }
}
