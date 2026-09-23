package com.eimsound.eimusic.media

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.eimsound.eimusic.AppStateProvider
import com.eimsound.eimusic.EIMusicApplication
import com.eimsound.eimusic.MainActivity
import com.eimsound.eimusic.R
import com.eimsound.eimusic.viewmodel.NotificationState
import kotlinx.coroutines.flow.StateFlow

actual class NotificationService actual constructor(
    private val state: StateFlow<NotificationState>,
) {
    private fun playNotificationSound() {
        !AppStateProvider.isAppInForeground || return

        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(EIMusicApplication.instance, notification)
        r.play()
    }

    private fun backgroundNotify(title: String, content: String, sound: Boolean) {
        val context = EIMusicApplication.instance
        val intent = Intent(context, MainActivity::class.java).apply {
            // 关键：如果 Activity 已经在运行，则直接唤醒它，而不是新建
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, EIMusicApplication.NEW_MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(!sound)


        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    actual suspend fun notify(title: String, content: String) {
        state.value.enabledNotification || return
        !AppStateProvider.isAppInForeground || return
        backgroundNotify(title, content, state.value.enabledNotificationSound)
    }

    actual suspend fun playSound() {
        playNotificationSound()
    }
}