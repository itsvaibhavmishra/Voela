package com.vaibhawmishra.voela.data.youtube

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.vaibhawmishra.voela.MainActivity
import com.vaibhawmishra.voela.R

// Ongoing progress notification (foreground service) + a completion ping so the
// user sees the result land even while off the app.
class ExtractionNotifications(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    init {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, context.getString(R.string.notif_channel_progress), NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, context.getString(R.string.notif_channel_done), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun foregroundInfo(progress: Int): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(PROGRESS_ID, progressNotification(progress), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(PROGRESS_ID, progressNotification(progress))
        }

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    fun updateProgress(progress: Int) {
        if (hasPermission()) manager.notify(PROGRESS_ID, progressNotification(progress))
    }

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    fun showComplete(title: String) {
        if (!hasPermission()) return
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_ready_title))
            .setContentText(title)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(DONE_ID, notification)
    }

    private fun progressNotification(progress: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_extracting_title))
            .setContentText("$progress%")
            .setProgress(100, progress, progress <= 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_PROGRESS = "voela_extraction"
        const val CHANNEL_DONE = "voela_results"
        const val PROGRESS_ID = 1001
        const val DONE_ID = 1002
    }
}
