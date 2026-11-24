package com.example.parishsync.workers

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.parishsync.data.local.ParishDatabase
import com.example.parishsync.data.local.PdfScheduleJobEntity

/**
 * WorkManager CoroutineWorker that executes Smart PDF Schedule Indexing in the background.
 * Extracts dates and MAIN/BACKUP duty rows from a PDF document, cross-references the congregation's
 * member roster in Room, highlights local assignments, and posts a system notification.
 */
class PdfScheduleParseWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
    val congregationId = inputData.getString(KEY_CONGREGATION_ID) ?: return Result.failure()
    val pdfFileName = inputData.getString(KEY_PDF_FILE_NAME) ?: "November_2026_Volunteer_Schedule.pdf"
    val uploadedBy = inputData.getString(KEY_UPLOADED_BY) ?: "Rector / Volunteer Scheduler"
    val pdfUriString = inputData.getString(KEY_PDF_URI)
    val customScheduleText = inputData.getString(KEY_CUSTOM_SCHEDULE_TEXT)

    val dao = ParishDatabase.getInstance(applicationContext).parishDao()

    return runCatching {
      val roster = dao.getCongregationRosterOnce(congregationId)
      val activeUser = dao.getActiveUserOnce()

      val rawText = if (!customScheduleText.isNullOrBlank()) {
        customScheduleText
      } else {
        val uri = pdfUriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        PdfScheduleParser.extractTextFromPdfUri(applicationContext, uri, roster)
      }

      val parseResult = PdfScheduleParser.parseScheduleText(
        rawText = rawText,
        pdfFileName = pdfFileName,
        congregationId = congregationId,
        roster = roster,
        activeUser = activeUser
      )

      dao. upsertPdfScheduleJob(
        PdfScheduleJobEntity(
          id = jobId,
          congregationId = congregationId,
          pdfFileName = pdfFileName,
          uploadedBy = uploadedBy,
          status = "COMPLETED",
          extractedDatesCount = parseResult.extractedDates.size,
          matchedRosterCount = parseResult.matchedRosterCount,
          highlightedLocalMatches = parseResult.highlightedLocalMatches,
          detailedLog = parseResult.summaryReport,
          parsedScheduleTableText = parseResult.formattedEmailTable
        )
      )

      Result.success(
        workDataOf(
          KEY_EXTRACTED_DATES to parseResult.extractedDates.size,
          KEY_MATCHED_ROSTER to parseResult.matchedRosterCount,
          KEY_SUMMARY_REPORT to parseResult.summaryReport
        )
      )
    }.getOrElse { error ->
      dao.upsertPdfScheduleJob(
        PdfScheduleJobEntity(
          id = jobId,
          congregationId = congregationId,
          pdfFileName = pdfFileName,
          uploadedBy = uploadedBy,
          status = "FAILED",
          detailedLog = "PDF Parsing error: ${error.message}"
        )
      )
      Result.failure()
    }
  }

  companion object {
    const val KEY_JOB_ID = "key_job_id"
    const val KEY_CONGREGATION_ID = "key_congregation_id"
    const val KEY_PDF_FILE_NAME = "key_pdf_file_name"
    const val KEY_UPLOADED_BY = "key_uploaded_by"
    const val KEY_PDF_URI = "key_pdf_uri"
    const val KEY_CUSTOM_SCHEDULE_TEXT = "key_custom_schedule_text"
    const val KEY_EXTRACTED_DATES = "key_extracted_dates"
    const val KEY_MATCHED_ROSTER = "key_matched_roster"
    const val KEY_SUMMARY_REPORT = "key_summary_report"
  }
}
