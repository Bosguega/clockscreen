package br.com.clockscreen

import android.app.AlarmManager
import android.content.Context
import android.os.Build

object PermissionUtils {

    /**
     * Verifica se o app tem permissão para agendar alarmes exatos.
     * Necessário a partir do Android 12 (API 31).
     */
    fun hasAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
