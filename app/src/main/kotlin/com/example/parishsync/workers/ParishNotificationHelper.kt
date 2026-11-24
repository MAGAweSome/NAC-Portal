package com.example.parishsync.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.parishsync.MainActivity
import com.example.parishsync.R
import kotlin.random.Random

object ParishNotificationHelper {
  const val CHANNEL_URGENT_BROADCAST = "nac_urgent_broadcasts"
  const val CHANNEL_ROSTER_REMINDERS = "nac_volunteer_reminders"
  const val CHANNEL_PDF_INDEXER = "nac_pdf_indexing"
  const val CHANNEL_MINIHQ_SYNC = "nac_minihq_sync"
  const val CHANNEL_DOWNLOADS = "nac_downloads"

  fun ensureChannelsCreated(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val broadcastChannel = NotificationChannel(
        CHANNEL_URGENT_BROADCAST,
        "Urgent NAC Portal Broadcasts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Instant push alerts from the Rector or Portal Administrator"
      }
      val reminderChannel = NotificationChannel(
        CHANNEL_ROSTER_REMINDERS,
        "Volunteer Roster Commitments",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Immediate push reminders for Altar Setup, Ushering, AV, and Greeter duties"
        enableVibration(true)
      }
      val pdfChannel = NotificationChannel(
        CHANNEL_PDF_INDEXER,
        "Smart PDF Schedule Indexer",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Background WorkManager notifications for PDF schedule parsing jobs"
      }
      val minihqChannel = NotificationChannel(
        CHANNEL_MINIHQ_SYNC,
        "MiniHQ Schedule Synchronization",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Periodic checks of the NAC Canada MiniHQ portal every 2 days"
      }
      val downloadsChannel = NotificationChannel(
        CHANNEL_DOWNLOADS,
        "Schedule & DSG File Downloads",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Download progress and completion alerts for schedules, DSG guides, and audio"
      }
      manager.createNotificationChannels(
        listOf(broadcastChannel, reminderChannel, pdfChannel, minihqChannel, downloadsChannel)
      )
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
  }

  fun formatNaturalItemList(items: List<String>, fallback: String): String {
    val clean = items.map { it.trim() }.filter { it.isNotBlank() }
    return when (clean.size) {
      0 -> fallback
      1 -> clean[0]
      2 -> "${clean[0]} and ${clean[1]}"
      else -> "${clean.dropLast(1).joinToString(", ")}, and ${clean.last()}"
    }
  }

  fun buildVolunteerReminderBody(
    roleTier: String,
    items: List<String>,
    fallbackTitle: String,
    serviceDate: String,
    serviceTime: String,
    isToday: Boolean
  ): String {
    val formattedItems = formatNaturalItemList(items, fallbackTitle)
    val whenClause = if (isToday) "for service today" else "for service on $serviceDate"
    val timeClause = if (serviceTime.isNotBlank()) " at $serviceTime" else ""
    return "You are scheduled as ${roleTier.uppercase()} to take care of $formattedItems $whenClause$timeClause."
  }

  fun sendVolunteerReminderNotification(
    context: Context,
    assignmentId: String,
    dutyTitle: String,
    subTaskLabels: List<String>,
    serviceDate: String,
    serviceTime: String,
    roleTier: String = "MAIN",
    isToday: Boolean = false
  ): Boolean {
    ensureChannelsCreated(context)
    val body = buildVolunteerReminderBody(
      roleTier = roleTier,
      items = subTaskLabels,
      fallbackTitle = dutyTitle,
      serviceDate = serviceDate,
      serviceTime = serviceTime,
      isToday = isToday
    )
    return postNotification(
      context = context,
      channelId = CHANNEL_ROSTER_REMINDERS,
      title = dutyTitle,
      body = body,
      priority = NotificationCompat.PRIORITY_HIGH,
      notificationId = "duty-reminder-$assignmentId".hashCode()
    )
  }

  fun sendTestPhoneNotification(
    context: Context,
    userName: String
  ): Boolean {
    ensureChannelsCreated(context)
    return postNotification(
      context = context,
      channelId = CHANNEL_ROSTER_REMINDERS,
      title = "NAC Portal • Test Notification",
      body = "Phone notifications are active for $userName. You will receive Volunteer Schedule reminders here.",
      priority = NotificationCompat.PRIORITY_HIGH
    )
  }

  fun sendUrgentBroadcastNotification(
    context: Context,
    title: String,
    summary: String,
    authorName: String
  ) {
    ensureChannelsCreated(context)
    postNotification(
      context = context,
      channelId = CHANNEL_URGENT_BROADCAST,
      title = "⚡ NAC Portal Alert: $title",
      body = "$summary — Sent by $authorName",
      priority = NotificationCompat.PRIORITY_HIGH
    )
  }

  fun sendPdfIndexingCompleteNotification(
    context: Context,
    pdfFileName: String,
    extractedDatesCount: Int,
    matchedRosterCount: Int,
    localHighlights: Int
  ) {
    ensureChannelsCreated(context)
    postNotification(
      context = context,
      channelId = CHANNEL_PDF_INDEXER,
      title = "Smart PDF Schedule Indexed: $pdfFileName",
      body = "Extracted $extractedDatesCount dates, matched $matchedRosterCount roster duties ($localHighlights local user highlights).",
      priority = NotificationCompat.PRIORITY_LOW
    )
  }

  fun sendMiniHqSyncNotification(
    context: Context,
    title: String,
    body: String
  ) {
    ensureChannelsCreated(context)
    postNotification(
      context = context,
      channelId = CHANNEL_MINIHQ_SYNC,
      title = title,
      body = body,
      priority = NotificationCompat.PRIORITY_LOW
    )
  }

  private fun postNotification(
    context: Context,
    channelId: String,
    title: String,
    body: String,
    priority: Int,
    notificationId: Int = Random.nextInt(1000, 99999)
  ): Boolean {
    if (!hasNotificationPermission(context)) {
      return false
    }
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("open_route", "volunteer_schedule")
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_nac_emblem)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(priority)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    return runCatching {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
      true
    }.getOrDefault(false)
  }

  fun showDownloadStartedNotification(
    context: Context,
    notificationId: Int,
    fileName: String
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    ensureChannelsCreated(context)
    val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
      .setSmallIcon(R.drawable.ic_nac_emblem)
      .setContentTitle("Downloading $fileName")
      .setContentText("Your file download has started...")
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setProgress(0, 0, true)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .build()
    return runCatching {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
      true
    }.getOrDefault(false)
  }

  fun showDownloadCompleteNotification(
    context: Context,
    notificationId: Int,
    fileName: String,
    contentUri: Uri?,
    mimeType: String
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    ensureChannelsCreated(context)
    val openIntent = if (contentUri != null) {
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    } else {
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
      .setSmallIcon(R.drawable.ic_nac_emblem)
      .setContentTitle("Download Ready: $fileName")
      .setContentText("Tap to open your downloaded file.")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setProgress(0, 0, false)
      .setOngoing(false)
      .build()
    return runCatching {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
      true
    }.getOrDefault(false)
  }

  fun cancelDownloadNotification(context: Context, notificationId: Int) {
    runCatching {
      NotificationManagerCompat.from(context).cancel(notificationId)
    }
  }
}
