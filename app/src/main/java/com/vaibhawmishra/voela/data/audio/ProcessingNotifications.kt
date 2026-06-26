package com.vaibhawmishra.voela.data.audio

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
class ProcessingNotifications(private val context: Context) {

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
        foreground(PROGRESS_ID, context.getString(R.string.notif_extracting_title), progress)

    fun savingForegroundInfo(progress: Int): ForegroundInfo =
        foreground(SAVE_PROGRESS_ID, context.getString(R.string.notif_saving_title), progress)

    fun separatingForegroundInfo(progress: Int): ForegroundInfo =
        foreground(SEPARATE_PROGRESS_ID, context.getString(R.string.notif_separating_title), progress)

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    fun updateSeparating(progress: Int) {
        if (hasPermission()) manager.notify(SEPARATE_PROGRESS_ID, progressNotification(context.getString(R.string.notif_separating_title), progress))
    }

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    fun updateProgress(progress: Int) {
        if (hasPermission()) manager.notify(PROGRESS_ID, progressNotification(context.getString(R.string.notif_extracting_title), progress))
    }

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    fun updateSaving(progress: Int) {
        if (hasPermission()) manager.notify(SAVE_PROGRESS_ID, progressNotification(context.getString(R.string.notif_saving_title), progress))
    }

    fun showComplete(title: String) =
        showDone(DONE_ID, context.getString(R.string.notif_ready_title), title)

    fun showSaved(detail: String, location: String? = null) =
        showDone(SAVE_DONE_ID, location ?: context.getString(R.string.notif_saved_title), detail)

    private fun foreground(id: Int, title: String, progress: Int): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, progressNotification(title, progress), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, progressNotification(title, progress))
        }

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    private fun showDone(id: Int, title: String, text: String) {
        if (!hasPermission()) return
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }

    private fun progressNotification(title: String, progress: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
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
        const val SAVE_PROGRESS_ID = 1003
        const val SAVE_DONE_ID = 1004
        const val SEPARATE_PROGRESS_ID = 1005
    }
}
