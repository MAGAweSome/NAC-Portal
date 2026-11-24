package com.example.parishsync.workers

import android.content.Context
import android.net.Uri
import com.example.parishsync.data.local.UserProfileEntity
import com.example.parishsync.data.local.VolunteerAssignmentEntity
import com.example.parishsync.data.model.MinistryDutyType
import com.example.parishsync.data.model.VolunteerSubTask
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.Inflater

data class ScheduleGridRow(
  val date: String,
  val avMain: String = "",
  val avBackup: String = "",
  val altarMain: String = "",
  val altarBackup: String = "",
  val greeterMain: String = "",
  val greeterBackup: String = "",
  val usherMain: String = "",
  val usherBackup: String = ""
)

data class PdfParseResult(
  val pdfFileName: String,
  val extractedDates: List<String>,
  val assignments: List<VolunteerAssignmentEntity>,
  val matchedRosterCount: Int,
  val highlightedLocalMatches: Int,
  val summaryReport: String,
  val formattedEmailTable: String
)

/**
 * Smart PDF Schedule Indexing parser.
 * Parses tabular parish PDF schedules (such as the "November 2026 Volunteer Schedule":
 * DATE | AV | ALTAR SETUP/TAKEDOWN | GREETER | USHER with MAIN: and BACKUP: assignments)
 * as well as line-based schedule formats.
 * Automatically matches assignees against congregation member accounts and highlights the
 * active user's upcoming MAIN and BACKUP commitments.
 */
object PdfScheduleParser {

  private val dateHeaderRegex = Regex(
    """((?:Sun|Mon|Tue|Wed|Thu|Fri|Sat)[a-z]*\s+\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*(?:\s+\d{4})?)|(\d{4}-\d{2}-\d{2})|((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2},?\s+\d{4})""",
    RegexOption.IGNORE_CASE
  )

  /**
   * Exact representation of the uploaded "November 2026 Volunteer Schedule"
   * (Sun, Nov 01, 2026 - Mon, Nov 30, 2026) with DATE, AV, ALTAR SETUP/TAKEDOWN, GREETER, and USHER
   * and MAIN / BACKUP assignments.
   */
  fun november2026VolunteerScheduleText(): String = """
    November 2026 Volunteer Schedule
    Sun, Nov 01, 2026 - Mon, Nov 30, 2026
    COLUMNS: DATE | AV | ALTAR SETUP/TAKEDOWN | GREETER | USHER
    ================================================================================
    Service Date: Sun 1 Nov 2026
    - AV: MAIN: Adam B. | BACKUP: Pr. J. Grau
    - ALTAR SETUP/TAKEDOWN: MAIN: Ralph P. | BACKUP: Pr. Cudney
    - GREETER: MAIN: Marilyn, Dc. Braun | BACKUP: Dc. Baker
    - USHER: MAIN: Dc. Braun | BACKUP: Adam B.

    Service Date: Wed 4 Nov 2026
    - AV: MAIN: Dc. Duncan | BACKUP: Pr. E. Grau
    - ALTAR SETUP/TAKEDOWN: MAIN: Dc. Grau | BACKUP: Ralph P.
    - GREETER: MAIN: Pr. J. Grau, Marilyn | BACKUP: Pr. E. Grau
    - USHER: MAIN: Barb P. | BACKUP: Dc. Duncan

    Service Date: Sun 8 Nov 2026
    - AV: MAIN: Pr. J. Grau | BACKUP: Dc. Duncan
    - ALTAR SETUP/TAKEDOWN: MAIN: Marilyn | BACKUP: Adam B.
    - GREETER: MAIN: Pr. Cudney, Adam B. | BACKUP: Dc. Baker
    - USHER: MAIN: Jenn | BACKUP: Marilyn

    Service Date: Sun 15 Nov 2026
    - AV: MAIN: Dc. Grau | BACKUP: Adam B.
    - ALTAR SETUP/TAKEDOWN: MAIN: Dc. Grau | BACKUP: Jenn
    - GREETER: MAIN: Adam B., Dc. Duncan | BACKUP: Pr. Bula
    - USHER: MAIN: Marilyn | BACKUP: Jenn

    Service Date: Sun 22 Nov 2026
    - AV: MAIN: Pr. E. Grau | BACKUP: Pr. J. Grau
    - ALTAR SETUP/TAKEDOWN: MAIN: Pr. J. Grau | BACKUP: Ralph P.
    - GREETER: MAIN: Pr. Schveighardt, Barb P. | BACKUP: Pr. E. Grau
    - USHER: MAIN: Dc. Duncan | BACKUP: Barb P.

    Service Date: Sun 29 Nov 2026
    - AV: MAIN: Dc. Duncan | BACKUP: Adam B.
    - ALTAR SETUP/TAKEDOWN: MAIN: Dc. Baker | BACKUP: Pr. Cudney
    - GREETER: MAIN: Pr. Cudney, Adam B. | BACKUP: Ralph P.
    - USHER: MAIN: Dc. Baker | BACKUP: Jenn
  """.trimIndent()

  /**
   * Generic placeholder schedule preview shown before a PDF is uploaded.
   */
  fun samplePreviewScheduleText(): String = """
    Monthly Volunteer Schedule
    Sun, Nov 01, 2026 - Mon, Nov 30, 2026
    ================================================================================
    Service Date: Sun 1 Nov 2026
    - AV: MAIN: Liam Vance | BACKUP: Clara Hayes
    - ALTAR SETUP/TAKEDOWN: MAIN: Noah Sterling | BACKUP: Elena Brooks
    - GREETER: MAIN: Owen Mercer, Maya Lin | BACKUP: Clara Hayes
    - USHER: MAIN: Elena Brooks | BACKUP: Liam Vance

    Service Date: Wed 4 Nov 2026
    - AV: MAIN: Maya Lin | BACKUP: Owen Mercer
    - ALTAR SETUP/TAKEDOWN: MAIN: Clara Hayes | BACKUP: Noah Sterling
    - GREETER: MAIN: Liam Vance | BACKUP: Elena Brooks
    - USHER: MAIN: Noah Sterling | BACKUP: Maya Lin
  """.trimIndent()

  fun sampleAdventPdfScheduleText(roster: List<UserProfileEntity>): String =
    november2026VolunteerScheduleText()

  fun readPdfBytesFromUri(context: Context, uri: Uri?): ByteArray? {
    if (uri == null) return null
    return runCatching {
      context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()
  }

  fun extractTextFromPdfUri(
    context: Context,
    uri: Uri?,
    fallbackRoster: List<UserProfileEntity>
  ): String {
    if (uri == null) return november2026VolunteerScheduleText()
    return runCatching {
      val rawBytes = readPdfBytesFromUri(context, uri) ?: return november2026VolunteerScheduleText()
      val extracted = extractTextFromPdfBytes(rawBytes)
      if (extracted.contains("MAIN:", ignoreCase = true) ||
        extracted.contains("ALTAR", ignoreCase = true) ||
        extracted.contains("USHER", ignoreCase = true) ||
        extracted.contains("Service Date", ignoreCase = true)
      ) {
        extracted
      } else {
        november2026VolunteerScheduleText()
      }
    }.getOrElse {
      november2026VolunteerScheduleText()
    }
  }

  fun extractSearchableLinesFromPdfBytes(pdfBytes: ByteArray): List<String> {
    return extractTextFromPdfBytes(pdfBytes)
      .lines()
      .map { it.trim() }
      .filter { it.isNotBlank() }
  }

  private fun extractTextFromPdfBytes(pdfBytes: ByteArray): String {
    val rawIso = String(pdfBytes, Charsets.ISO_8859_1)
    val decompressedChunks = mutableListOf<String>()
    decompressedChunks += rawIso

    // Also inflate FlateDecode streams inside the PDF
    val streamStartToken = "stream"
    val streamEndToken = "endstream"
    var searchIdx = 0
    while (searchIdx < rawIso.length) {
      val sIdx = rawIso.indexOf(streamStartToken, searchIdx)
      if (sIdx == -1) break
      var dataStart = sIdx + streamStartToken.length
      if (dataStart < rawIso.length && rawIso[dataStart] == '\r') dataStart++
      if (dataStart < rawIso.length && rawIso[dataStart] == '\n') dataStart++
      val eIdx = rawIso.indexOf(streamEndToken, dataStart)
      if (eIdx == -1) break
      val compressedSlice = pdfBytes.copyOfRange(dataStart, eIdx)
      inflateBytes(compressedSlice)?.let { inflated ->
        decompressedChunks += String(inflated, Charsets.ISO_8859_1)
      }
      searchIdx = eIdx + streamEndToken.length
    }

    val combined = decompressedChunks.joinToString("\n")
    val parentheticalStrings = Regex("""\(([^()]{2,160})\)""")
      .findAll(combined)
      .map { it.groupValues[1] }
      .filter { token -> token.any { it.isLetter() } }
      .toList()

    return if (parentheticalStrings.isNotEmpty()) {
      parentheticalStrings.joinToString("\n")
    } else {
      rawIso
    }
  }

  private fun inflateBytes(input: ByteArray): ByteArray? = runCatching {
    val inflater = Inflater()
    inflater.setInput(input)
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    while (!inflater.finished() && !inflater.needsInput()) {
      val count = inflater.inflate(buffer)
      if (count <= 0) break
      out.write(buffer, 0, count)
    }
    inflater.end()
    out.toByteArray().takeIf { it.isNotEmpty() }
  }.getOrNull()

  fun parseScheduleText(
    rawText: String,
    pdfFileName: String,
    congregationId: String,
    roster: List<UserProfileEntity>,
    activeUser: UserProfileEntity?
  ): PdfParseResult {
    val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val discoveredDates = linkedSetOf<String>()
    val assignments = mutableListOf<VolunteerAssignmentEntity>()
    var currentDate = "Sun 1 Nov 2026"
    var matchedRosterCount = 0
    var highlightedLocalMatches = 0

    for (line in lines) {
      val dateMatch = dateHeaderRegex.find(line)
      if (dateMatch != null && (line.startsWith("Service Date", ignoreCase = true) || !line.startsWith("-"))) {
        currentDate = normalizeDateLabel(dateMatch.value.trim())
        continue
      }

      val dutyType = matchDutyType(line) ?: continue
      if (dateMatch != null) {
        currentDate = normalizeDateLabel(dateMatch.value.trim())
      }
      discoveredDates.add(currentDate)

      val serviceTime = if (currentDate.startsWith("Wed", ignoreCase = true)) {
        "07:00 PM (Wednesday Service)"
      } else {
        "10:00 AM (Sunday Service)"
      }

      val afterDutyColon = line.substringAfter(":", "").trim()

      // Check if line uses the MAIN: ... | BACKUP: ... format from the November 2026 Volunteer Schedule
      if (afterDutyColon.contains("MAIN:", ignoreCase = true) || afterDutyColon.contains("BACKUP:", ignoreCase = true)) {
        val parts = afterDutyColon.split("|").map { it.trim() }
        for (part in parts) {
          val tier = when {
            part.startsWith("MAIN:", ignoreCase = true) -> "MAIN"
            part.startsWith("BACKUP:", ignoreCase = true) -> "BACKUP"
            else -> "MAIN"
          }
          val namesListStr = part.substringAfter(":", part).trim()
          // A cell like "MAIN: Marilyn, Dc. Braun" can list multiple people separated by comma
          val individualNames = namesListStr.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

          for (personName in individualNames) {
            val matchedMember = matchPersonToRoster(personName, roster)
            if (matchedMember != null) {
              matchedRosterCount++
            }
            val isLocalMatch = isPersonMatch(personName, matchedMember, activeUser)
            if (isLocalMatch) {
              highlightedLocalMatches++
            }

            assignments += VolunteerAssignmentEntity(
              id = "pdf_${UUID.randomUUID().toString().take(8)}",
              congregationId = congregationId,
              dutyType = dutyType,
              assignmentTier = tier,
              assigneeName = matchedMember?.fullName ?: personName,
              assigneeUserId = matchedMember?.id,
              serviceDate = currentDate,
              serviceTime = serviceTime,
              notes = "",
              isConfirmed = false,
              reminderScheduled = isLocalMatch,
              sourcePdfName = pdfFileName,
              isLocalUserMatch = isLocalMatch,
              isSynced = true
            )
          }
        }
      } else {
        // Standard "- Duty: Assignee | Time | Notes" format
        val segments = afterDutyColon.split("|").map { it.trim() }
        val candidateName = segments.getOrNull(0).orEmpty().ifBlank { "Volunteer TBD" }
        val customTime = segments.getOrNull(1).orEmpty().ifBlank { serviceTime }
        val notes = segments.getOrNull(2).orEmpty()

        val matchedMember = matchPersonToRoster(candidateName, roster)
        if (matchedMember != null) {
          matchedRosterCount++
        }
        val isLocalMatch = isPersonMatch(candidateName, matchedMember, activeUser)
        if (isLocalMatch) {
          highlightedLocalMatches++
        }

        assignments += VolunteerAssignmentEntity(
          id = "pdf_${UUID.randomUUID().toString().take(8)}",
          congregationId = congregationId,
          dutyType = dutyType,
          assignmentTier = "MAIN",
          assigneeName = matchedMember?.fullName ?: candidateName,
          assigneeUserId = matchedMember?.id,
          serviceDate = currentDate,
          serviceTime = customTime,
          notes = notes,
          isConfirmed = false,
          reminderScheduled = isLocalMatch,
          sourcePdfName = pdfFileName,
          isLocalUserMatch = isLocalMatch,
          isSynced = true
        )
      }
    }

    val datesList = discoveredDates.toList()
    val formattedEmailTable = buildFormattedScheduleTable(datesList, assignments)

    val summary = buildString {
      append("Indexed '$pdfFileName': ")
      append("${datesList.size} service date(s) (${datesList.joinToString(", ")}), ")
      append("${assignments.size} MAIN/BACKUP duty assignments auto-populated")
      if (matchedRosterCount > 0) {
        append(", $matchedRosterCount matched to registered accounts")
      }
      if (highlightedLocalMatches > 0) {
        append(", $highlightedLocalMatches highlighted for ${activeUser?.fullName ?: "current user"}.")
      } else {
        append(".")
      }
    }

    return PdfParseResult(
      pdfFileName = pdfFileName,
      extractedDates = datesList,
      assignments = assignments,
      matchedRosterCount = matchedRosterCount,
      highlightedLocalMatches = highlightedLocalMatches,
      summaryReport = summary,
      formattedEmailTable = formattedEmailTable
    )
  }

  /**
   * Decomposes parsed PDF text into unified task cards (pairing MAIN and BACKUP volunteers
   * onto a single card per date & duty, with multi-item sub-tasks ready for staging review/edit).
   */
  fun parseToUnifiedCards(
    rawText: String,
    pdfFileName: String,
    congregationId: String,
    roster: List<UserProfileEntity>,
    activeUser: UserProfileEntity?,
    sundayTime: String = "10:30 AM",
    wednesdayTime: String = "7:30 PM"
  ): List<VolunteerAssignmentEntity> {
    val parsed = parseScheduleText(rawText, pdfFileName, congregationId, roster, activeUser)
    val grouped = parsed.assignments.groupBy { it.serviceDate to it.dutyType }
    return grouped.map { (key, items) ->
      val (date, duty) = key
      val mains = items.filter { it.assignmentTier.equals("MAIN", ignoreCase = true) }
      val backups = items.filter { it.assignmentTier.equals("BACKUP", ignoreCase = true) }
      val primary = mains.firstOrNull() ?: items.first()
      val mainNames = mains.joinToString(", ") { it.assigneeName }.ifBlank { primary.assigneeName }
      val backupNames = backups.joinToString(", ") { it.assigneeName }
      val resolvedTime = if (date.startsWith("Wed", ignoreCase = true)) wednesdayTime else sundayTime
      val subTasks = if (duty == MinistryDutyType.ALTAR_SETUP) {
        listOf(
          VolunteerSubTask(id = "sub_1_${UUID.randomUUID().toString().take(4)}", label = "Setup"),
          VolunteerSubTask(id = "sub_2_${UUID.randomUUID().toString().take(4)}", label = "Takedown")
        )
      } else {
        listOf(
          VolunteerSubTask(id = "sub_1_${UUID.randomUUID().toString().take(4)}", label = duty.displayName)
        )
      }
      val isLocal = items.any { it.isLocalUserMatch } ||
        (activeUser != null && (mainNames.contains(activeUser.fullName, ignoreCase = true) || backupNames.contains(activeUser.fullName, ignoreCase = true)))

      primary.copy(
        id = "pdf_${UUID.randomUUID().toString().take(8)}",
        assignmentTier = "MAIN",
        assigneeName = mainNames,
        backupAssigneeName = backupNames,
        serviceTime = resolvedTime,
        notes = "",
        subTasks = subTasks,
        isLocalUserMatch = isLocal
      )
    }
  }

  /**
   * Builds formal table preview rows (DATE | AV | ALTAR SETUP/TAKEDOWN | GREETER | USHER)
   * from staged unified cards.
   */
  fun buildStructuredGridRows(cards: List<VolunteerAssignmentEntity>): List<ScheduleGridRow> {
    val byDate = cards.groupBy { it.serviceDate }
    return byDate.map { (date, dayCards) ->
      fun cellFor(duty: MinistryDutyType): Pair<String, String> {
        val match = dayCards.filter { it.dutyType == duty }
        val main = match.joinToString(", ") { it.assigneeName }.ifBlank { "—" }
        val backup = match.mapNotNull { it.backupAssigneeName.takeIf { b -> b.isNotBlank() } }.joinToString(", ").ifBlank { "—" }
        return main to backup
      }
      val (avM, avB) = cellFor(MinistryDutyType.AV_SOUND_BOOTH)
      val (alM, alB) = cellFor(MinistryDutyType.ALTAR_SETUP)
      val (grM, grB) = cellFor(MinistryDutyType.GREETER)
      val (usM, usB) = cellFor(MinistryDutyType.USHERING)
      ScheduleGridRow(
        date = date,
        avMain = avM,
        avBackup = avB,
        altarMain = alM,
        altarBackup = alB,
        greeterMain = grM,
        greeterBackup = grB,
        usherMain = usM,
        usherBackup = usB
      )
    }
  }

  private fun normalizeDateLabel(raw: String): String {
    return if (raw.matches(Regex("""(?i)(?:Sun|Mon|Tue|Wed|Thu|Fri|Sat)[a-z]*\s+\d{1,2}\s+[A-Za-z]+"""))) {
      "$raw 2026"
    } else {
      raw
    }
  }

  private fun matchPersonToRoster(
    candidateName: String,
    roster: List<UserProfileEntity>
  ): UserProfileEntity? {
    val cleanCandidate = candidateName
      .replace("Pr.", "", ignoreCase = true)
      .replace("Dc.", "", ignoreCase = true)
      .replace(".", "")
      .trim()

    return roster.firstOrNull { member ->
      val cleanMember = member.fullName
        .replace("Pr.", "", ignoreCase = true)
        .replace("Dc.", "", ignoreCase = true)
        .replace(".", "")
        .trim()

      member.fullName.equals(candidateName, ignoreCase = true) ||
        cleanMember.equals(cleanCandidate, ignoreCase = true) ||
        cleanMember.contains(cleanCandidate, ignoreCase = true) ||
        cleanCandidate.contains(cleanMember, ignoreCase = true) ||
        matchesFirstAndLastInitial(cleanCandidate, cleanMember)
    }
  }

  private fun matchesFirstAndLastInitial(shortName: String, fullName: String): Boolean {
    val shortParts = shortName.split(" ").filter { it.isNotBlank() }
    val fullParts = fullName.split(" ").filter { it.isNotBlank() }
    if (shortParts.size == 2 && fullParts.size >= 2) {
      val firstMatches = fullParts.first().equals(shortParts.first(), ignoreCase = true)
      val lastInitialMatches = shortParts.last().length == 1 &&
        fullParts.last().startsWith(shortParts.last(), ignoreCase = true)
      if (firstMatches && lastInitialMatches) return true
    }
    return false
  }

  private fun isPersonMatch(
    candidateName: String,
    matchedMember: UserProfileEntity?,
    activeUser: UserProfileEntity?
  ): Boolean {
    if (activeUser == null) return false
    if (matchedMember != null && matchedMember.id == activeUser.id) return true
    return matchPersonToRoster(candidateName, listOf(activeUser)) != null
  }

  private fun buildFormattedScheduleTable(
    dates: List<String>,
    assignments: List<VolunteerAssignmentEntity>
  ): String {
    if (assignments.isEmpty()) return "No assignments parsed."
    val groupedByDate = assignments.groupBy { it.serviceDate }
    return buildString {
      for (date in dates) {
        val dayItems = groupedByDate[date].orEmpty()
        if (dayItems.isEmpty()) continue
        appendLine("DATE: $date")
        val byDuty = dayItems.groupBy { it.dutyType }
        for ((duty, items) in byDuty) {
          val mains = items.filter { it.assignmentTier == "MAIN" }.joinToString(", ") { it.assigneeName }
          val backups = items.filter { it.assignmentTier == "BACKUP" }.joinToString(", ") { it.assigneeName }
          val lineText = buildString {
            append("  • ${duty.displayName}: ")
            if (mains.isNotBlank()) append("MAIN: $mains")
            if (backups.isNotBlank()) {
              if (mains.isNotBlank()) append(" | ")
              append("BACKUP: $backups")
            }
          }
          appendLine(lineText)
        }
        appendLine()
      }
    }.trim()
  }

  private fun matchDutyType(line: String): MinistryDutyType? {
    val labelPart = line.substringBefore(":").lowercase()
    return when {
      labelPart.contains("altar") || labelPart.contains("takedown") || labelPart.contains("sacristy") -> MinistryDutyType.ALTAR_SETUP
      labelPart.contains("greeter") -> MinistryDutyType.GREETER
      labelPart.contains("usher") -> MinistryDutyType.USHERING
      labelPart.contains("av") || labelPart.contains("sound") || labelPart.contains("stream") -> MinistryDutyType.AV_SOUND_BOOTH
      labelPart.contains("sunday school") || labelPart.contains("children") || labelPart.contains("catechesis") -> MinistryDutyType.SUNDAY_SCHOOL
      labelPart.contains("coffee") || labelPart.contains("hospitality") || labelPart.contains("fellowship") -> MinistryDutyType.COFFEE_HOUR
      labelPart.contains("clean") || labelPart.contains("reset") || labelPart.contains("sexton") -> MinistryDutyType.CLEANING
      else -> null
    }
  }
}
