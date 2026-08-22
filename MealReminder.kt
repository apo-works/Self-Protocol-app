package jp.bodyprotocol.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object MealReminderScheduler {
    private val anchor = LocalDate.of(2026, 8, 23)
    private val time = LocalTime.of(18, 30)

    fun nextOccurrence(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        var date = if (now.toLocalDate().isBefore(anchor)) anchor else {
            val days = ChronoUnit.DAYS.between(anchor, now.toLocalDate())
            anchor.plusDays(((days + 2) / 3) * 3)
        }
        var candidate = LocalDateTime.of(date, time)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(3)
        return candidate
    }

    fun schedule(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MealReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, 1830, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val millis = nextOccurrence().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        } else {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }
}

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "meal_checkin"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(channelId, "3日献立", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).putExtra("open_meal", true), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("次の3日、何食べたい？")
            .setContentText("残り物・外食予定・ジム予定を答えて献立を作ります")
            .setContentIntent(open).setAutoCancel(true).build()
        if (Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) nm.notify(1830, n)
        MealReminderScheduler.schedule(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) { MealReminderScheduler.schedule(context) }
}
