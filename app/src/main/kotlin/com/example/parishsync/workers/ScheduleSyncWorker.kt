package com.example.parishsync.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.parishsync.data.local.ParishDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * WorkManager CoroutineWorker that synchronizes local Room schedule state and dispatches
 * local notification reminders for upcoming volunteer commitments.
 */
class ScheduleSyncWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val dao = ParishDatabase.getInstance(applicationContext).parishDao()
    return runCatching {
      val activeUser = dao.getActiveUserOnce() ?: return Result.success()
      val assignments = dao.getVolunteerAssignmentsOnce(activeUser.congregationId)

      val upcomingUserDuties = assignments.filter { assignment ->
        assignment.reminderScheduled &&
          (assignment.isLocalUserMatch || assignment.assigneeName.equals(activeUser.fullName, ignoreCase = true))
      }

      upcomingUserDuties.firstOrNull()?.let { nextDuty ->
        val today = LocalDate.now()
        val parsedDate = listOf(
          "EEE d MMM yyyy",
          "EEE MMM d, yyyy",
          "EEE, MMM d, yyyy",
          "yyyy-MM-dd",
          "MMM d, yyyy"
        ).firstNotNullOfOrNull { pattern ->
          runCatching { LocalDate.parse(nextDuty.serviceDate.trim(), DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)) }.getOrNull()
        }
        val dutyTitle = nextDuty.customTaskTitle.ifBlank { nextDuty.dutyType.displayName }
        val isBackup = nextDuty.backupAssigneeName.split(",").map { it.trim() }.any { it.equals(activeUser.fullName, ignoreCase = true) } &&
          nextDuty.assigneeName.split(",").map { it.trim() }.none { it.equals(activeUser.fullName, ignoreCase = true) }
        ParishNotificationHelper.sendVolunteerReminderNotification(
          context = applicationContext,
          assignmentId = nextDuty.id,
          dutyTitle = dutyTitle,
          subTaskLabels = nextDuty.subTasks.map { it.label }.filter { it.isNotBlank() },
          serviceDate = nextDuty.serviceDate,
          serviceTime = nextDuty.serviceTime,
          roleTier = if (isBackup) "BACKUP" else "MAIN",
          isToday = parsedDate == today
        )
      }

      Result.success()
    }.getOrElse {
      Result.retry()
    }
  }
}
