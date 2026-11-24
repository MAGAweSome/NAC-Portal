package com.example.parishsync.workers

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.parishsync.data.local.VolunteerAssignmentEntity
import com.example.parishsync.data.model.MinistryDutyType
import java.io.ByteArrayOutputStream

/**
 * Generates a print-ready, tabular PDF document (Letter Landscape: 792x612 pt)
 * for Volunteer Schedule email attachments.
 *
 * Groups multiple assignees for the same date and duty into a single unified cell
 * with stacked MAIN: and BACKUP: lines, shaded header row (#F1F5F9), 1px borders,
 * and automatic page-break protection so rows never split mid-line across pages.
 */
object SchedulePdfGenerator {

  private const val PAGE_WIDTH = 792
  private const val PAGE_HEIGHT = 612
  private const val MARGIN_X = 28f
  private const val MARGIN_TOP = 28f
  private const val MARGIN_BOTTOM = 28f
  private const val CELL_PAD_X = 6f
  private const val CELL_PAD_Y = 6f

  private data class ColumnDef(
    val title: String,
    val key: String
  )

  fun generateTabularSchedulePdf(
    congregationName: String,
    scheduleTitle: String,
    assignments: List<VolunteerAssignmentEntity>
  ): ByteArray {
    if (assignments.isEmpty()) {
      return buildFallbackPdfBytes(congregationName, scheduleTitle, emptyList())
    }

    return runCatching {
      renderWithAndroidPdfDocument(congregationName, scheduleTitle, assignments)
    }.getOrElse {
      // Fallback for JVM unit tests where android.graphics.pdf.PdfDocument is mocked/unavailable
      buildFallbackPdfBytes(congregationName, scheduleTitle, assignments)
    }
  }

  private fun renderWithAndroidPdfDocument(
    congregationName: String,
    scheduleTitle: String,
    assignments: List<VolunteerAssignmentEntity>
  ): ByteArray {
    val baseColumns = listOf(
      ColumnDef("AV", MinistryDutyType.AV_SOUND_BOOTH.name),
      ColumnDef("ALTAR SETUP/TAKEDOWN", MinistryDutyType.ALTAR_SETUP.name),
      ColumnDef("GREETER", MinistryDutyType.GREETER.name),
      ColumnDef("USHER", MinistryDutyType.USHERING.name)
    )

    val customColumns = assignments
      .map { it.customTaskTitle.ifBlank { it.dutyType.displayName } to (it.customTaskTitle.ifBlank { it.dutyType.name }) }
      .filter { (_, key) -> baseColumns.none { it.key == key } }
      .distinctBy { it.second }
      .map { (title, key) -> ColumnDef(title.uppercase(), key) }

    val dutyColumns = baseColumns + customColumns
    val totalCols = 1 + dutyColumns.size
    val tableWidth = PAGE_WIDTH - (MARGIN_X * 2)
    val dateColWidth = 96f
    val dutyColWidth = (tableWidth - dateColWidth) / dutyColumns.size.coerceAtLeast(1)

    val dates = assignments.map { it.serviceDate }.distinct()
    val dateRangeSubtitle = if (dates.size >= 2) {
      "${dates.first()} – ${dates.last()}"
    } else {
      dates.firstOrNull().orEmpty()
    }

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#0F172A")
      textSize = 15f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#475569")
      textSize = 10f
      textAlign = Paint.Align.CENTER
    }
    val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#0F172A")
      textSize = 8.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val dateCellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#0F172A")
      textSize = 8.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val mainCellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#1E293B")
      textSize = 8f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val backupCellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#64748B")
      textSize = 7.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val headerBgPaint = Paint().apply {
      color = Color.parseColor("#F1F5F9")
      style = Paint.Style.FILL
    }
    val borderPaint = Paint().apply {
      color = Color.parseColor("#CBD5E1")
      style = Paint.Style.STROKE
      strokeWidth = 1f
    }

    val document = PdfDocument()
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
    var canvas = page.canvas
    var cursorY = MARGIN_TOP

    fun drawDocumentHeader() {
      val centerX = PAGE_WIDTH / 2f
      canvas.drawText(congregationName.ifBlank { "NAC Portal" }, centerX, cursorY + 12f, titlePaint)
      cursorY += 18f
      canvas.drawText(scheduleTitle.ifBlank { "Volunteer Schedule" }, centerX, cursorY + 10f, subtitlePaint)
      cursorY += 14f
      if (dateRangeSubtitle.isNotBlank()) {
        canvas.drawText(dateRangeSubtitle, centerX, cursorY + 9f, subtitlePaint)
        cursorY += 14f
      }
      cursorY += 6f
    }

    fun colWidth(colIdx: Int): Float = if (colIdx == 0) dateColWidth else dutyColWidth

    fun drawTableHeaderRow() {
      val headerHeight = 24f
      var x = MARGIN_X
      val allHeaders = listOf("DATE") + dutyColumns.map { it.title }
      allHeaders.forEachIndexed { idx, label ->
        val w = colWidth(idx)
        val rect = RectF(x, cursorY, x + w, cursorY + headerHeight)
        canvas.drawRect(rect, headerBgPaint)
        canvas.drawRect(rect, borderPaint)
        val layout = StaticLayout.Builder.obtain(label, 0, label.length, headerTextPaint, (w - CELL_PAD_X * 2).toInt().coerceAtLeast(10))
          .setAlignment(Layout.Alignment.ALIGN_NORMAL)
          .build()
        canvas.save()
        canvas.translate(x + CELL_PAD_X, cursorY + (headerHeight - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()
        x += w
      }
      cursorY += headerHeight
    }

    drawDocumentHeader()
    drawTableHeaderRow()

    val byDate = assignments.groupBy { it.serviceDate }
    for ((date, dayAssignments) in byDate) {
      // Build unified cell text for each duty column
      val cellContents = dutyColumns.map { col ->
        val matching = dayAssignments.filter { a ->
          val key = if (a.customTaskTitle.isNotBlank()) a.customTaskTitle else a.dutyType.name
          key == col.key
        }
        val mains = matching
          .flatMap { a ->
            if (a.assignmentTier.equals("BACKUP", ignoreCase = true)) emptyList()
            else a.assigneeName.split(",").map { it.trim() }.filter { it.isNotBlank() }
          }
          .distinct()
          .joinToString(", ")
        val backups = matching
          .flatMap { a ->
            val fromBackupField = a.backupAssigneeName.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val fromBackupTier = if (a.assignmentTier.equals("BACKUP", ignoreCase = true)) {
              a.assigneeName.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else emptyList()
            fromBackupField + fromBackupTier
          }
          .distinct()
          .joinToString(", ")
        val mainLine = if (mains.isNotBlank()) "MAIN: $mains" else "MAIN: —"
        val backupLine = if (backups.isNotBlank()) "BACKUP: $backups" else "BACKUP: —"
        mainLine to backupLine
      }

      // Compute StaticLayouts to measure exact row height (prevents mid-row page splits)
      val dateLayout = StaticLayout.Builder.obtain(
        date, 0, date.length, dateCellPaint, (dateColWidth - CELL_PAD_X * 2).toInt().coerceAtLeast(10)
      ).build()

      val cellLayouts = cellContents.map { (mLine, bLine) ->
        val w = (dutyColWidth - CELL_PAD_X * 2).toInt().coerceAtLeast(10)
        val mLayout = StaticLayout.Builder.obtain(mLine, 0, mLine.length, mainCellPaint, w).build()
        val bLayout = StaticLayout.Builder.obtain(bLine, 0, bLine.length, backupCellPaint, w).build()
        mLayout to bLayout
      }

      val maxContentHeight = maxOf(
        dateLayout.height.toFloat(),
        cellLayouts.maxOfOrNull { (m, b) -> m.height + 3f + b.height } ?: 0f
      )
      val rowHeight = maxContentHeight + (CELL_PAD_Y * 2)

      // Page-break protection: start new page if this row would overflow bottom margin
      if (cursorY + rowHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
        document.finishPage(page)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        cursorY = MARGIN_TOP
        drawTableHeaderRow()
      }

      // Draw Date Cell
      var x = MARGIN_X
      val dateRect = RectF(x, cursorY, x + dateColWidth, cursorY + rowHeight)
      canvas.drawRect(dateRect, borderPaint)
      canvas.save()
      canvas.translate(x + CELL_PAD_X, cursorY + CELL_PAD_Y)
      dateLayout.draw(canvas)
      canvas.restore()
      x += dateColWidth

      // Draw Duty Cells
      cellLayouts.forEach { (mLayout, bLayout) ->
        val cellRect = RectF(x, cursorY, x + dutyColWidth, cursorY + rowHeight)
        canvas.drawRect(cellRect, borderPaint)
        canvas.save()
        canvas.translate(x + CELL_PAD_X, cursorY + CELL_PAD_Y)
        mLayout.draw(canvas)
        canvas.translate(0f, mLayout.height + 3f)
        bLayout.draw(canvas)
        canvas.restore()
        x += dutyColWidth
      }

      cursorY += rowHeight
    }

    document.finishPage(page)
    val out = ByteArrayOutputStream()
    document.writeTo(out)
    document.close()
    return out.toByteArray()
  }

  private fun buildFallbackPdfBytes(
    congregationName: String,
    scheduleTitle: String,
    assignments: List<VolunteerAssignmentEntity>
  ): ByteArray {
    val lines = buildList {
      add(congregationName.ifBlank { "NAC Portal" })
      add(scheduleTitle)
      add("")
      assignments.groupBy { it.serviceDate }.forEach { (date, items) ->
        add(date)
        items.groupBy { it.customTaskTitle.ifBlank { it.dutyType.displayName } }.forEach { (duty, grp) ->
          val mains = grp.filter { !it.assignmentTier.equals("BACKUP", true) }.joinToString(", ") { it.assigneeName }
          val backups = grp.mapNotNull { it.backupAssigneeName.takeIf { b -> b.isNotBlank() } }.joinToString(", ")
          add("  $duty | MAIN: ${mains.ifBlank { "—" }} | BACKUP: ${backups.ifBlank { "—" }}")
        }
      }
    }.take(45).map { it.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)") }

    val textOps = buildString {
      appendLine("BT")
      appendLine("/F1 10 Tf")
      appendLine("36 756 Td")
      appendLine("14 TL")
      lines.forEach { line -> appendLine("($line) Tj T*") }
      appendLine("ET")
    }
    val pdfContent = """
      %PDF-1.4
      1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
      2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
      3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj
      4 0 obj << /Length ${textOps.length} >>
      stream
      $textOps
      endstream
      endobj
      5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj
      xref
      0 6
      0000000000 65535 f 
      trailer << /Size 6 /Root 1 0 R >>
      startxref
      0
      %%EOF
    """.trimIndent()
    return pdfContent.toByteArray(Charsets.ISO_8859_1)
  }
}
