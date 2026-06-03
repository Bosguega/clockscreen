package br.com.clockscreen

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object DateAlarmScheduler {

    private const val REQUEST_CODE = 100
    private const val UPDATE_INTERVAL_MS = 30 * 60 * 1000L // 30 minutos

    /**
     * Agenda alarme periódico para atualizar o widget a cada 30 minutos.
     * Isso garante que temperatura, background e data sejam atualizados
     * regularmente sem depender apenas do updatePeriodMillis do widget.
     */
    fun schedulePeriodicUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, DateAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Primeira execução: daqui a 30 segundos (para não atrasar a primeira atualização)
        val firstTrigger = System.currentTimeMillis() + 30_000L

        // Usa setInexactRepeating para economia de bateria
        // O Android agrupa alarmes similares, então não precisa ser exact
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            firstTrigger,
            UPDATE_INTERVAL_MS,
            pendingIntent
        )
    }

    /**
     * Mantido para compatibilidade - agora apenas delega para schedulePeriodicUpdate
     */
    fun scheduleMidnightUpdate(context: Context) {
        schedulePeriodicUpdate(context)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DateAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
