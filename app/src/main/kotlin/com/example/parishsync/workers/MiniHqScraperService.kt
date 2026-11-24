package com.example.parishsync.workers

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.parishsync.BuildConfig
import com.example.parishsync.data.local.MiniHqAnnouncement
import com.example.parishsync.data.local.MiniHqResourceEntity
import com.example.parishsync.data.local.MiniHqScrapedData
import com.example.parishsync.data.local.MiniHqStagedService
import com.example.parishsync.data.local.ParishDatabase
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.coroutines.resume

/**
 * Service responsible for:
 * 1. Connecting to the NAC Canada MiniHQ Portal (https://www.naccanada.org/imis_prod/MiniHQ).
 * 2. Hashing the Divine Service Schedule and Divine Service Guide (DSG) content.
 * 3. Comparing hashes against the previous scrape.
 * 4. Skipping updates if schedule and DSG guide remain unchanged.
 */
object MiniHqScraperService {
  private const val TAG = "MiniHqScraperService"
  private const val PREFS_NAME = "nac_minihq_scraper_prefs"
  private const val KEY_SCHEDULE_HASH = "key_schedule_hash"
  private const val KEY_DSG_HASH = "key_dsg_hash"
  private const val KEY_LAST_CHECK_TIME = "key_last_check_time"
  private const val KEY_LAST_OUTCOME = "key_last_outcome"
  private const val KEY_LAST_MESSAGE = "key_last_message"
  private const val KEY_SCRAPED_ANNOUNCEMENTS_JSON = "key_scraped_announcements_json"
  private val announcementJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }

  const val MINIHQ_URL = "https://www.naccanada.org/imis_prod/MiniHQ"
  const val MINIHQ_USERNAME = "MAGRAU"

  const val CATEGORY_DISTRICT_SCHEDULE = "DISTRICT_SCHEDULE"
  const val CATEGORY_DSG_ASSET = "DSG_ASSET"

  const val ASSET_SCHEDULE_PDF = "Schedule PDF"
  const val ASSET_EN_DSG = "English DSG"
  const val ASSET_FR_DSG = "French DSG"
  const val ASSET_AUDIO = "Audio"
  const val ASSET_REFERENCES = "References"
  const val ASSET_EN_TRANSCRIPT = "English Transcript"
  const val ASSET_FR_TRANSCRIPT = "French Transcript"
  const val ASSET_EN_BIBLE_READING = "Bible Reading EN"
  const val ASSET_FR_BIBLE_READING = "Bible Reading FR"

  val CANADIAN_DISTRICTS = listOf(
    "British Columbia",
    "Alberta",
    "Saskatchewan",
    "Manitoba",
    "Northern Ontario",
    "Kitchener",
    "Hamilton",
    "Toronto",
    "Eastern Canada"
  )

  val TARGET_MONTHS = listOf(
    "November 2026" to false,
    "December 2026" to true
  )

  enum class SyncScope {
    SCHEDULES,
    DSG,
    ANNOUNCEMENTS,
    ALL
  }

  enum class SyncStatus {
    SKIPPED_UNCHANGED,
    UPDATED,
    FAILED
  }

  data class SyncOutcome(
    val status: SyncStatus,
    val message: String,
    val scheduleHash: String,
    val dsgHash: String,
    val checkedTimestamp: Long = System.currentTimeMillis(),
    val stagedServicesCount: Int = 0,
    val announcementsCount: Int = 0,
    val scope: SyncScope = SyncScope.ALL
  )

  data class SyncProgressState(
    val isSyncing: Boolean = false,
    val activeScope: SyncScope = SyncScope.ALL,
    val percent: Int = 0,
    val stepMessage: String = "",
    val requiresAuth: Boolean = false,
    val hasError: Boolean = false,
    val savedDistrictCount: Int = 0,
    val totalDistrictCount: Int = 18,
    val diagnosticLog: String = ""
  )

  private const val STALE_SYNC_LOCK_TIMEOUT_MS = 120_000L

  @Volatile
  private var syncStartedAtMs: Long = 0L

  internal fun isSyncLockActive(
    isSyncing: Boolean,
    startedAtMs: Long,
    nowMs: Long = System.currentTimeMillis(),
    forceRefresh: Boolean = false
  ): Boolean {
    if (!isSyncing || forceRefresh) return false
    if (startedAtMs <= 0L) return false
    return (nowMs - startedAtMs) < STALE_SYNC_LOCK_TIMEOUT_MS
  }

  private val _syncProgress = MutableStateFlow(SyncProgressState())
  val syncProgress: StateFlow<SyncProgressState> = _syncProgress.asStateFlow()

  private val _scrapedAnnouncements = MutableStateFlow(MiniHqScrapedData.getMiniHqAnnouncements())
  val scrapedAnnouncements: StateFlow<List<MiniHqAnnouncement>> = _scrapedAnnouncements.asStateFlow()

  /**
   * Performs the scrape/sync routine:
   * 1. Evaluates candidate Divine Service schedule & DSG data.
   * 2. Computes SHA-256 fingerprint for schedule items and DSG prep notes.
   * 3. Compares with previously stored hashes.
   * 4. If identical, skips update and logs the skip.
   * 5. If changed, commits new hashes and stages updated services.
   */
  suspend fun performSync(
    context: Context,
    forceRefresh: Boolean = false,
    scope: SyncScope = SyncScope.ALL
  ): SyncOutcome = withContext(Dispatchers.IO) {
    if (isSyncLockActive(_syncProgress.value.isSyncing, syncStartedAtMs, forceRefresh = forceRefresh)) {
      return@withContext getLastSyncOutcome(context) ?: SyncOutcome(
        status = SyncStatus.SKIPPED_UNCHANGED,
        message = "MiniHQ sync is already in progress.",
        scheduleHash = "",
        dsgHash = "",
        scope = scope
      )
    }

    syncStartedAtMs = System.currentTimeMillis()
    _syncProgress.value = SyncProgressState(
      isSyncing = true,
      activeScope = scope,
      percent = 10,
      stepMessage = "Authenticating with MiniHQ..."
    )

    try {
      purgeLegacyMockCache(context)

      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val lastScheduleHash = prefs.getString(KEY_SCHEDULE_HASH, "") ?: ""
      val lastDsgHash = prefs.getString(KEY_DSG_HASH, "") ?: ""

      Log.i(TAG, "Starting MiniHQ sync check at $MINIHQ_URL")

      var sessionCookie = getSessionCookie(context)
      var landingHtml = fetchHtmlWithCookie(MINIHQ_URL, sessionCookie)
      var discoveredMonthUrls = if (!landingHtml.isNullOrBlank()) {
        extractSchedulesMonthUrls(MINIHQ_URL, landingHtml)
      } else {
        emptyList()
      }
      var authRequired = isLandingPageAuthRequired(landingHtml, discoveredMonthUrls)

      if (authRequired && BuildConfig.NAC_USERNAME.isNotBlank() && BuildConfig.NAC_PASSWORD.isNotBlank()) {
        _syncProgress.value = SyncProgressState(
          isSyncing = true,
          activeScope = scope,
          percent = 18,
          stepMessage = "Authenticating with MiniHQ...",
          requiresAuth = false
        )
        val (autoCookie, autoHtml) = attemptAutomatedMiniHqLogin(context)
        if (autoCookie.isNotBlank()) {
          sessionCookie = autoCookie
        }
        if (!autoHtml.isNullOrBlank()) {
          landingHtml = autoHtml
          discoveredMonthUrls = extractSchedulesMonthUrls(MINIHQ_URL, autoHtml)
          authRequired = isLandingPageAuthRequired(landingHtml, discoveredMonthUrls)
        }
      }

      _syncProgress.value = SyncProgressState(
        isSyncing = true,
        activeScope = scope,
        percent = 25,
        stepMessage = when (scope) {
          SyncScope.SCHEDULES -> "Locating Current & Next Month Schedule pages..."
          SyncScope.DSG -> "Locating Current & Next Month DSG pages..."
          SyncScope.ANNOUNCEMENTS -> "Scanning MiniHQ Announcements..."
          SyncScope.ALL -> "Locating Current & Next Month Schedule & DSG pages..."
        },
        requiresAuth = authRequired
      )

      val stagedServices = MiniHqScrapedData.getStagedDivineServices()
      if (_scrapedAnnouncements.value == MiniHqScrapedData.getMiniHqAnnouncements()) {
        loadPersistedAnnouncements(context)?.let { persisted ->
          if (persisted.isNotEmpty()) _scrapedAnnouncements.value = persisted
        }
      }
      val announcements = if (scope == SyncScope.ANNOUNCEMENTS || scope == SyncScope.ALL) {
        val liveAnnouncements = if (!landingHtml.isNullOrBlank() && !authRequired) {
          scrapeAnnouncementsWithDetails(MINIHQ_URL, landingHtml, sessionCookie)
        } else {
          emptyList()
        }
        if (liveAnnouncements.isNotEmpty()) {
          _scrapedAnnouncements.value = liveAnnouncements
          persistAnnouncements(context, liveAnnouncements)
          liveAnnouncements
        } else {
          _scrapedAnnouncements.value
        }
      } else {
        _scrapedAnnouncements.value
      }

      if (scope == SyncScope.ANNOUNCEMENTS) {
        val annCount = announcements.size
        val annMsg = if (authRequired && annCount == 0) {
          "MiniHQ authentication required for Announcements."
        } else {
          "Announcements Sync Complete! $annCount items loaded."
        }
        _syncProgress.value = SyncProgressState(
          isSyncing = false,
          activeScope = SyncScope.ANNOUNCEMENTS,
          percent = 100,
          stepMessage = annMsg,
          requiresAuth = authRequired,
          hasError = authRequired && annCount == 0
        )
        return@withContext SyncOutcome(
          status = if (authRequired && annCount == 0) SyncStatus.FAILED else SyncStatus.UPDATED,
          message = annMsg,
          scheduleHash = lastScheduleHash,
          dsgHash = lastDsgHash,
          checkedTimestamp = System.currentTimeMillis(),
          stagedServicesCount = stagedServices.size,
          announcementsCount = annCount,
          scope = SyncScope.ANNOUNCEMENTS
        )
      }

      val currentScheduleHash = computeScheduleHash(stagedServices)
      val currentDsgHash = computeDsgHash(stagedServices)

      val skipUpdate = !forceRefresh && shouldSkipUpdate(
        currentScheduleHash = currentScheduleHash,
        lastScheduleHash = lastScheduleHash,
        currentDsgHash = currentDsgHash,
        lastDsgHash = lastDsgHash
      )

      val dao = ParishDatabase.getInstance(context).parishDao()
      val existingResources = runCatching { dao.getMiniHqResourcesOnce() }.getOrElse { err ->
        if (err is android.database.sqlite.SQLiteBlobTooBigException ||
          err.cause is android.database.sqlite.SQLiteBlobTooBigException
        ) {
          Log.w(TAG, "Purging oversized SQLite blob cache on sync startup")
          dao.deleteAllMiniHqResources()
          emptyList()
        } else {
          throw err
        }
      }
      var syncDiagnostic = ""
      if (!skipUpdate || existingResources.isEmpty() || forceRefresh) {
        syncDiagnostic = syncResourcesToLocalCacheAndDb(
          context = context,
          dao = dao,
          stagedServices = stagedServices,
          existingById = existingResources.associateBy { it.id },
          landingHtml = landingHtml,
          sessionCookie = sessionCookie,
          authRequired = authRequired,
          scope = scope
        )
      }

      _syncProgress.value = SyncProgressState(
        isSyncing = true,
        activeScope = scope,
        percent = 95,
        stepMessage = "Updating database & parsing entries...",
        requiresAuth = authRequired
      )

      val updatedList = dao.getMiniHqResourcesOnce()
      val savedDistricts = updatedList.count {
        it.resourceCategory == CATEGORY_DISTRICT_SCHEDULE &&
          it.localPath.isNotBlank() &&
          it.fileSize > 5000L &&
          File(it.localPath).exists()
      }
      val savedDsgCount = updatedList.count {
        it.resourceCategory == CATEGORY_DSG_ASSET &&
          it.localPath.isNotBlank() &&
          it.fileSize > 0L
      }

      val targetSavedCount = if (scope == SyncScope.DSG) savedDsgCount else savedDistricts
      val outcome: SyncOutcome
      if ((authRequired && targetSavedCount == 0) || (!skipUpdate && targetSavedCount == 0)) {
        val failMsg = if (scope == SyncScope.DSG) {
          if (authRequired) "MiniHQ authentication required — 0 DSG files saved." else "DSG sync failed — 0 DSG files saved."
        } else {
          if (authRequired) "MiniHQ authentication required — 0/18 Districts Saved." else "MiniHQ sync failed — 0/18 Districts Saved."
        }
        val diag = syncDiagnostic.ifBlank {
          "HTTP fetch of $MINIHQ_URL returned unauthenticated Sign-In HTML or empty response. " +
            "Tap 'Sign in to MiniHQ' to authenticate and run the live in-browser scraper."
        }
        prefs.edit()
          .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
          .putString(KEY_LAST_OUTCOME, SyncStatus.FAILED.name)
          .putString(KEY_LAST_MESSAGE, failMsg)
          .apply()

        _syncProgress.value = SyncProgressState(
          isSyncing = false,
          activeScope = scope,
          percent = if (authRequired) 15 else 0,
          stepMessage = failMsg,
          requiresAuth = true,
          hasError = true,
          savedDistrictCount = savedDistricts,
          totalDistrictCount = 18,
          diagnosticLog = diag
        )
        outcome = SyncOutcome(
          status = SyncStatus.FAILED,
          message = failMsg,
          scheduleHash = currentScheduleHash,
          dsgHash = currentDsgHash,
          checkedTimestamp = System.currentTimeMillis(),
          stagedServicesCount = stagedServices.size,
          announcementsCount = announcements.size,
          scope = scope
        )
      } else if (skipUpdate) {
        val skipMsg = if (scope == SyncScope.DSG) {
          "DSG Sync: Up to date ($savedDsgCount DSG assets saved)."
        } else {
          "MiniHQ Sync: Schedule and Divine Service Guide are unchanged ($savedDistricts/18 Districts Saved)."
        }
        prefs.edit()
          .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
          .putString(KEY_LAST_OUTCOME, SyncStatus.SKIPPED_UNCHANGED.name)
          .putString(KEY_LAST_MESSAGE, skipMsg)
          .apply()

        _syncProgress.value = SyncProgressState(
          isSyncing = false,
          activeScope = scope,
          percent = 100,
          stepMessage = skipMsg,
          requiresAuth = authRequired,
          hasError = false,
          savedDistrictCount = savedDistricts,
          totalDistrictCount = 18
        )
        outcome = SyncOutcome(
          status = SyncStatus.SKIPPED_UNCHANGED,
          message = skipMsg,
          scheduleHash = currentScheduleHash,
          dsgHash = currentDsgHash,
          checkedTimestamp = System.currentTimeMillis(),
          stagedServicesCount = stagedServices.size,
          announcementsCount = announcements.size,
          scope = scope
        )
      } else {
        val updateMsg = if (scope == SyncScope.DSG) {
          "DSG Sync Complete! $savedDsgCount DSG assets saved."
        } else {
          "Sync Complete! $savedDistricts/18 Districts Saved."
        }
        prefs.edit()
          .putString(KEY_SCHEDULE_HASH, currentScheduleHash)
          .putString(KEY_DSG_HASH, currentDsgHash)
          .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
          .putString(KEY_LAST_OUTCOME, SyncStatus.UPDATED.name)
          .putString(KEY_LAST_MESSAGE, updateMsg)
          .apply()

        _syncProgress.value = SyncProgressState(
          isSyncing = false,
          activeScope = scope,
          percent = 100,
          stepMessage = updateMsg,
          requiresAuth = false,
          hasError = false,
          savedDistrictCount = savedDistricts,
          totalDistrictCount = 18
        )
        outcome = SyncOutcome(
          status = SyncStatus.UPDATED,
          message = updateMsg,
          scheduleHash = currentScheduleHash,
          dsgHash = currentDsgHash,
          checkedTimestamp = System.currentTimeMillis(),
          stagedServicesCount = stagedServices.size,
          announcementsCount = announcements.size,
          scope = scope
        )
      }
      outcome
    } catch (e: Exception) {
      Log.e(TAG, "MiniHQ sync failed: ${e.message}", e)
      _syncProgress.value = SyncProgressState(
        isSyncing = false,
        percent = 0,
        stepMessage = "Sync failed: ${e.message}",
        hasError = true,
        diagnosticLog = e.stackTraceToString().take(800)
      )
      SyncOutcome(
        status = SyncStatus.FAILED,
        message = e.message ?: "MiniHQ sync failed.",
        scheduleHash = "",
        dsgHash = ""
      )
    } finally {
      syncStartedAtMs = 0L
      if (_syncProgress.value.isSyncing) {
        _syncProgress.value = _syncProgress.value.copy(isSyncing = false)
      }
    }
  }

  fun getLastSyncOutcome(context: Context): SyncOutcome? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val statusName = prefs.getString(KEY_LAST_OUTCOME, null) ?: return null
    val status = runCatching { SyncStatus.valueOf(statusName) }.getOrDefault(SyncStatus.SKIPPED_UNCHANGED)
    return SyncOutcome(
      status = status,
      message = prefs.getString(KEY_LAST_MESSAGE, "Up to date") ?: "Up to date",
      scheduleHash = prefs.getString(KEY_SCHEDULE_HASH, "") ?: "",
      dsgHash = prefs.getString(KEY_DSG_HASH, "") ?: "",
      checkedTimestamp = prefs.getLong(KEY_LAST_CHECK_TIME, 0L),
      stagedServicesCount = MiniHqScrapedData.getStagedDivineServices().size,
      announcementsCount = MiniHqScrapedData.getMiniHqAnnouncements().size
    )
  }

  fun computeScheduleHash(services: List<MiniHqStagedService>): String {
    val scheduleRaw = buildString {
      services.sortedBy { it.id }.forEach {
        append("${it.id}|${it.monthYear}|${it.serviceDate}|${it.serviceTime}|${it.serviceTitle}|${it.officiantMinister}|${it.officiantRank}|${it.location}\n")
      }
    }
    return sha256(scheduleRaw)
  }

  fun computeDsgHash(services: List<MiniHqStagedService>): String {
    val dsgRaw = buildString {
      services.sortedBy { it.id }.forEach {
        append("${it.id}|${it.theme}|${it.bibleWord}|${it.dsgPdfLink}|${it.audioLink}|${it.specialServiceTag}\n")
      }
    }
    return sha256(dsgRaw)
  }

  fun shouldSkipUpdate(
    currentScheduleHash: String,
    lastScheduleHash: String,
    currentDsgHash: String,
    lastDsgHash: String
  ): Boolean {
    return lastScheduleHash.isNotBlank() &&
      currentScheduleHash.equals(lastScheduleHash, ignoreCase = true) &&
      currentDsgHash.equals(lastDsgHash, ignoreCase = true)
  }

  fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  /**
   * Lightweight 18-district placeholder list for Schedules UI before first sync
   * (avoids generating 60+ DSG rows and SHA-256 hashes on the Main Thread).
   */
  fun discoverDistrictPlaceholdersOnly(nowMillis: Long = System.currentTimeMillis()): List<MiniHqResourceEntity> {
    val results = mutableListOf<MiniHqResourceEntity>()
    TARGET_MONTHS.forEach { (monthYear, isNextMonth) ->
      val monthSlug = monthYear.replace(" ", "_")
      CANADIAN_DISTRICTS.forEach { district ->
        val districtSlug = district.replace(" ", "_")
        results += MiniHqResourceEntity(
          id = "sched_${monthSlug.lowercase()}_${districtSlug.lowercase()}",
          resourceCategory = CATEGORY_DISTRICT_SCHEDULE,
          monthYear = monthYear,
          isNextMonth = isNextMonth,
          district = district,
          serviceDate = "",
          serviceTitle = "$district District Serving Schedule",
          assetType = ASSET_SCHEDULE_PDF,
          fileName = "${districtSlug}_${monthSlug}_Serving_Schedule.pdf",
          sourceUrl = "",
          checksum = "pending",
          lastCheckedAt = nowMillis
        )
      }
    }
    return results
  }

  /**
   * Discovers all District Serving Schedule PDFs (Current & Next Month across all 9 Canadian districts)
   * and all Divine Service Prep (DSG) assets (EN/FR DSGs, Audio, References, EN/FR Transcripts).
   */
  fun discoverCatalogResources(
    stagedServices: List<MiniHqStagedService> = MiniHqScrapedData.getStagedServices(),
    nowMillis: Long = System.currentTimeMillis()
  ): List<MiniHqResourceEntity> {
    val results = mutableListOf<MiniHqResourceEntity>()

    // 1. District Serving Schedule PDFs for Current Month & Next Month
    // Note: `sourceUrl` starts blank until extracted from the live authenticated MiniHQ DOM (`el.href`).
    TARGET_MONTHS.forEach { (monthYear, isNextMonth) ->
      val monthSlug = monthYear.replace(" ", "_")
      CANADIAN_DISTRICTS.forEach { district ->
        val districtSlug = district.replace(" ", "_")
        val fileName = "${districtSlug}_${monthSlug}_Serving_Schedule.pdf"
        results += MiniHqResourceEntity(
          id = "sched_${monthSlug.lowercase()}_${districtSlug.lowercase()}",
          resourceCategory = CATEGORY_DISTRICT_SCHEDULE,
          monthYear = monthYear,
          isNextMonth = isNextMonth,
          district = district,
          serviceDate = "",
          serviceTitle = "$district District Serving Schedule",
          assetType = ASSET_SCHEDULE_PDF,
          fileName = fileName,
          sourceUrl = "",
          checksum = sha256("pending|$monthYear|$district"),
          lastCheckedAt = nowMillis
        )
      }
    }

    // 2. Divine Service Prep (DSG) resources per service date
    val uniqueServicesByDateAndMonth = stagedServices.distinctBy { "${it.monthYear}|${it.serviceDate}|${it.serviceTitle}" }
    uniqueServicesByDateAndMonth.forEach { svc ->
      val isNext = svc.monthYear.equals("December 2026", ignoreCase = true)
      val monthUrlPath = svc.monthYear.replace(" ", "")
      val dateSlug = svc.serviceDate.replace(",", "").replace(" ", "_")
      val baseId = "dsg_${svc.id}"

      val enDsgUrl = svc.dsgPdfLink.ifBlank {
        "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/$monthUrlPath/English/$dateSlug-dsg-en.pdf"
      }
      val frDsgUrl = enDsgUrl
        .replace("/English/", "/French/")
        .replace("-dsg-en.pdf", "-dsg-fr.pdf")
      val audioUrl = svc.audioLink.ifBlank {
        "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/$monthUrlPath/Audio/$dateSlug-recording.mp3"
      }
      val refUrl =
        "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/$monthUrlPath/References/$dateSlug-references.pdf"
      val enTranscriptUrl =
        "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/$monthUrlPath/Transcripts/$dateSlug-transcript-en.pdf"
      val frTranscriptUrl =
        "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/$monthUrlPath/Transcripts/$dateSlug-transcript-fr.pdf"

      val assetSpecs = listOf(
        Triple(ASSET_EN_DSG, enDsgUrl, "${dateSlug}_DSG_EN.pdf"),
        Triple(ASSET_FR_DSG, frDsgUrl, "${dateSlug}_DSG_FR.pdf"),
        Triple(ASSET_AUDIO, audioUrl, "${dateSlug}_Audio.mp3"),
        Triple(ASSET_REFERENCES, refUrl, "${dateSlug}_References.pdf"),
        Triple(ASSET_EN_TRANSCRIPT, enTranscriptUrl, "${dateSlug}_Transcript_EN.pdf"),
        Triple(ASSET_FR_TRANSCRIPT, frTranscriptUrl, "${dateSlug}_Transcript_FR.pdf")
      )

      assetSpecs.forEach { (assetType, url, fileName) ->
        val typeKey = assetType.lowercase().replace(" ", "_")
        val readableText = buildString {
          appendLine("${svc.serviceDate} • ${svc.serviceTime}")
          appendLine(svc.serviceTitle)
          if (svc.theme.isNotBlank()) appendLine("\nTheme: ${svc.theme}")
          if (svc.bibleWord.isNotBlank()) appendLine("Bible Word: ${svc.bibleWord}")
          if (svc.officiantMinister.isNotBlank()) appendLine("Officiant: ${svc.officiantMinister} (${svc.officiantRank})")
          if (svc.assistingMinisters.isNotBlank()) appendLine("Assisting: ${svc.assistingMinisters}")
          if (svc.notes.isNotBlank()) appendLine("\nPrep Notes:\n${svc.notes}")
        }.trim()
        results += MiniHqResourceEntity(
          id = "${baseId}_$typeKey",
          resourceCategory = CATEGORY_DSG_ASSET,
          monthYear = svc.monthYear,
          isNextMonth = isNext,
          district = svc.district,
          serviceDate = svc.serviceDate,
          serviceTitle = svc.serviceTitle,
          assetType = assetType,
          fileName = fileName,
          sourceUrl = url,
          contentText = readableText,
          checksum = sha256("$url|${svc.theme}|${svc.bibleWord}"),
          lastCheckedAt = nowMillis
        )
      }
    }

    return results
  }

  private fun storageDirForCategory(context: Context, category: String): File {
    val subDir = if (category == CATEGORY_DISTRICT_SCHEDULE) "schedules" else "dsg"
    return File(context.filesDir, subDir).apply { mkdirs() }
  }

  fun purgeLegacyMockCache(context: Context) {
    listOf("minihq_cache", "schedules", "dsg").forEach { dirName ->
      val dir = File(context.filesDir, dirName).apply { mkdirs() }
      dir.listFiles()?.forEach { file ->
        val bytes = runCatching {
          if (file.length() <= 64 * 1024L) file.readBytes() else file.inputStream().use { ins ->
            ByteArray(1024).also { buf -> ins.read(buf) }
          }
        }.getOrNull()
        if (bytes != null && isMockPlaceholderPdf(bytes)) {
          Log.i(TAG, "Purging legacy/invalid cached file: ${file.name}")
          file.delete()
        }
      }
    }
  }

  /**
   * Navigates the MiniHQ landing HTML strictly under the `Schedules` section
   * (distinct from `Divine Service Prep` and `Announcements`) to extract Current & Next Month URLs.
   */
  fun extractDsgMonthUrls(landingUrl: String, html: String): List<Pair<String, String>> {
    val dsgStart = Regex(""">\s*Divine\s+Service\s+Prep\s*<""", RegexOption.IGNORE_CASE).find(html)?.range?.first
      ?: html.indexOf("Divine Service Prep", ignoreCase = true)
    if (dsgStart < 0) return emptyList()
    val afterDsg = html.substring(dsgStart)
    val schedulesIdx = Regex(""">\s*Schedules\s*<""", RegexOption.IGNORE_CASE).find(afterDsg)?.range?.first
      ?: afterDsg.indexOf("Schedules", ignoreCase = true)
    val dsgSection = if (schedulesIdx > 0) afterDsg.substring(0, schedulesIdx) else afterDsg

    val yearRegex = Regex("""\b20\d{2}\b""")
    val anchorRegex = Regex("""<a\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val scoped = anchorRegex.findAll(dsgSection).mapNotNull { match ->
      val fullUrl = resolveFullHrefUrl(landingUrl, match.groupValues[1])
      val label = normalizeHtmlLabel(match.groupValues[2])
      if (label.isNotBlank() && (fullUrl.contains("Divine", ignoreCase = true) || fullUrl.contains("DSG", ignoreCase = true) || yearRegex.containsMatchIn(label))) {
        label to fullUrl
      } else {
        null
      }
    }.distinctBy { it.second }.toList()
    if (scoped.isNotEmpty()) return scoped

    return anchorRegex.findAll(html).mapNotNull { match ->
      val fullUrl = resolveFullHrefUrl(landingUrl, match.groupValues[1])
      val label = normalizeHtmlLabel(match.groupValues[2])
      if (label.isNotBlank() && (fullUrl.contains("Divine_Service_Prep", ignoreCase = true) || fullUrl.contains("DSG", ignoreCase = true))) {
        label to fullUrl
      } else {
        null
      }
    }.distinctBy { it.second }.toList()
  }

  private val DSG_MONTH_ABBR = mapOf(
    "january" to "Jan", "february" to "Feb", "march" to "Mar", "april" to "Apr",
    "may" to "May", "june" to "Jun", "july" to "Jul", "august" to "Aug",
    "september" to "Sep", "october" to "Oct", "november" to "Nov", "december" to "Dec"
  )

  private val DSG_DATE_HEADER_REGEX = Regex(
    """(Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday),?\s+(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{1,2}),?\s+(\d{4})(?:\s*[—–\-]+\s*([^<\r\n]+))?""",
    RegexOption.IGNORE_CASE
  )

  private val DSG_SPECIAL_HEADER_REGEX = Regex(
    """>(?:[\s▸►▼\u25B8\u25BC]|&nbsp;|&#9656;|&#9662;|&#9658;)*(Confirmation\s+DSG|Confirmation\s+Letter|Foreword|Foreward|Youth\s+Service|Full\s+DSG|Special\s+Edition\s+DSG)(?:[\s▸►▼]|&nbsp;)*<""",
    RegexOption.IGNORE_CASE
  )

  fun parseServiceDateToLocalDate(rawDate: String): LocalDate? {
    val cleaned = rawDate
      .replace(Regex("""^[A-Za-z]+,?\s+"""), "")
      .replace(",", "")
      .replace(Regex("\\s+"), " ")
      .trim()
    if (cleaned.isBlank()) return null
    val patterns = listOf("MMM d yyyy", "MMMM d yyyy", "MMM dd yyyy", "MMMM dd yyyy", "yyyy-MM-dd")
    for (pattern in patterns) {
      val parsed = runCatching {
        LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
      }.getOrNull()
      if (parsed != null) return parsed
    }
    return null
  }

  /**
   * Parses a live MiniHQ Divine Service Prep month page, scanning both dated service accordions
   * and undated/special sections (Foreword, Confirmation DSG/Letter, Youth Service, Full DSG, Special Edition DSG).
   * Enforces the canonical naming schemas:
   * - Dated: `[Category]_[EventDay]_[Month]_[Date]_[Year]_[Lang].[ext]` (e.g. `DSG_EN_Sunday_Nov_01_2026.pdf`, `Audio_Sunday_Nov_01_2026.mp3`)
   * - Special/Undated: `[Category]_[Month]_[Year]_[Lang].[ext]` (e.g. `SpecialEdition_DSG_Feb_2026_EN.pdf`)
   */
  private fun classifyStructuredDsgLink(
    sectionHtml: String,
    label: String,
    fullUrl: String
  ): String {
    val lowerSection = sectionHtml.lowercase(Locale.ROOT)
    val lowerUrl = fullUrl.lowercase(Locale.ROOT)
    val lowerLabel = label.lowercase(Locale.ROOT)
    if (lowerUrl.contains(".mp3")) return ASSET_AUDIO

    val fileNamePart = fullUrl.substringAfterLast("/").substringBefore("?")
    val linkPos = sectionHtml.indexOf(fileNamePart, ignoreCase = true)
    val localContext = if (linkPos >= 0) {
      sectionHtml
        .substring((linkPos - 140).coerceAtLeast(0), linkPos)
        .substringAfterLast("</a>")
        .lowercase(Locale.ROOT)
    } else {
      ""
    }

    val isRef = lowerUrl.contains("ref") ||
      lowerLabel.contains("ref") ||
      localContext.contains("references")
    val sectionHasBibleReading = lowerSection.contains("bible reading") ||
      (lowerUrl.contains("bible") && !isRef) ||
      (lowerLabel.contains("bible") && !isRef)
    val isBibleReading = !isRef && sectionHasBibleReading && (
      lowerUrl.contains("bible") ||
        lowerUrl.contains("reading") ||
        lowerLabel.contains("bible") ||
        lowerLabel.contains("reading") ||
        localContext.contains("bible reading")
      )
    val isTranscript = !isRef && !isBibleReading && (
      lowerUrl.contains("transcript") ||
        lowerLabel.contains("transcript") ||
        localContext.contains("transcript")
      )
    val isFrench = lowerUrl.contains("-fr") ||
      lowerUrl.contains("_fr") ||
      lowerUrl.contains("/french/") ||
      lowerLabel == "fr" ||
      lowerLabel.contains("french") ||
      lowerLabel.contains("français")

    return when {
      isBibleReading && isFrench -> ASSET_FR_BIBLE_READING
      isBibleReading -> ASSET_EN_BIBLE_READING
      isTranscript && isFrench -> ASSET_FR_TRANSCRIPT
      isTranscript -> ASSET_EN_TRANSCRIPT
      isRef -> ASSET_REFERENCES
      isFrench -> ASSET_FR_DSG
      else -> ASSET_EN_DSG
    }
  }

  fun extractDsgResourcesFromHtml(
    dsgPageUrl: String,
    monthYear: String,
    html: String,
    nowMillis: Long = System.currentTimeMillis()
  ): List<MiniHqResourceEntity> {
    val startIdx = Regex("""DIVINE\s+SERVICE\s+PREP""", RegexOption.IGNORE_CASE).find(html)?.range?.first
      ?: DSG_DATE_HEADER_REGEX.find(html)?.range?.first
      ?: DSG_SPECIAL_HEADER_REGEX.find(html)?.range?.first
      ?: 0
    val afterStart = html.substring(startIdx)
    val endMarkers = listOf("Divine Service Recording", "Copyright ©", "Copyright &copy;")
    val endIdx = endMarkers
      .map { afterStart.indexOf(it, ignoreCase = true) }
      .filter { it > 0 }
      .minOrNull() ?: afterStart.length
    val scopedHtml = afterStart.substring(0, endIdx)

    data class SectionMarker(
      val start: Int,
      val bodyStart: Int,
      val isDated: Boolean,
      val title: String,
      val eventDay: String = "",
      val monthFull: String = "",
      val dayNum: Int = 0,
      val year: String = ""
    )

    val markers = mutableListOf<SectionMarker>()
    DSG_DATE_HEADER_REGEX.findAll(scopedHtml).forEach { m ->
      val day = m.groupValues[1].replaceFirstChar { it.uppercaseChar() }
      val month = m.groupValues[2].replaceFirstChar { it.uppercaseChar() }
      val dateNum = m.groupValues[3].toIntOrNull() ?: 1
      val year = m.groupValues[4]
      val subtitle = normalizeHtmlLabel(m.groupValues[5]).ifBlank { "$day Divine Service" }
      markers += SectionMarker(
        start = m.range.first,
        bodyStart = m.range.last + 1,
        isDated = true,
        title = subtitle,
        eventDay = day,
        monthFull = month,
        dayNum = dateNum,
        year = year
      )
    }
    DSG_SPECIAL_HEADER_REGEX.findAll(scopedHtml).forEach { m ->
      val rawTitle = normalizeHtmlLabel(m.groupValues[1])
      val canonicalTitle = if (rawTitle.equals("Foreward", ignoreCase = true)) "Foreword" else rawTitle
      markers += SectionMarker(
        start = m.range.first,
        bodyStart = m.range.last + 1,
        isDated = false,
        title = canonicalTitle
      )
    }

    val sortedMarkers = markers.sortedBy { it.start }
    if (sortedMarkers.isEmpty()) return emptyList()

    val isNext = monthYear.equals("December 2026", ignoreCase = true)
    val anchorRegex = Regex(
      """<a\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val audioSourceRegex = Regex(
      """<source\s+[^>]*src\s*=\s*["']([^"']+)["']""",
      RegexOption.IGNORE_CASE
    )
    val results = mutableListOf<MiniHqResourceEntity>()
    val seenIds = mutableSetOf<String>()

    sortedMarkers.forEachIndexed { idx, marker ->
      val bodyEnd = if (idx + 1 < sortedMarkers.size) sortedMarkers[idx + 1].start else scopedHtml.length
      if (marker.bodyStart >= bodyEnd) return@forEachIndexed
      val sectionHtml = scopedHtml.substring(marker.bodyStart, bodyEnd)

      val rawLinks = mutableListOf<Pair<String, String>>()
      anchorRegex.findAll(sectionHtml).forEach { am ->
        val rawHref = am.groupValues[1].trim()
        if (rawHref.isBlank() || rawHref.startsWith("#") || rawHref.startsWith("javascript:", ignoreCase = true)) return@forEach
        val fullUrl = resolveFullHrefUrl(dsgPageUrl, rawHref)
        val label = normalizeHtmlLabel(am.groupValues[2])
        if (fullUrl.contains(".pdf", ignoreCase = true) || fullUrl.contains(".mp3", ignoreCase = true)) {
          rawLinks += label to fullUrl
        }
      }
      audioSourceRegex.findAll(sectionHtml).forEach { sm ->
        val fullUrl = resolveFullHrefUrl(dsgPageUrl, sm.groupValues[1].trim())
        if (fullUrl.contains(".mp3", ignoreCase = true)) {
          rawLinks += "Audio" to fullUrl
        }
      }

      if (marker.isDated) {
        val monthAbbr = DSG_MONTH_ABBR[marker.monthFull.lowercase(Locale.ROOT)] ?: marker.monthFull.take(3)
        val dayPadded = marker.dayNum.toString().padStart(2, '0')
        val dayShort = marker.eventDay.take(3)
        val displayDate = "$dayShort $monthAbbr ${marker.dayNum}, ${marker.year}"
        val dateKey = "${marker.year}_${monthAbbr.lowercase()}_$dayPadded"

        rawLinks.distinctBy { it.second }.forEach { (label, fullUrl) ->
          val assetType = classifyStructuredDsgLink(sectionHtml, label, fullUrl)
          val fileName = when (assetType) {
            ASSET_AUDIO -> "Audio_${marker.eventDay}_${monthAbbr}_${dayPadded}_${marker.year}.mp3"
            ASSET_FR_BIBLE_READING -> "BibleReading_${marker.eventDay}_${monthAbbr}_${dayPadded}_${marker.year}_FR.pdf"
            ASSET_EN_BIBLE_READING -> "BibleReading_${marker.eventDay}_${monthAbbr}_${dayPadded}_${marker.year}_EN.pdf"
            ASSET_FR_TRANSCRIPT -> "Transcript_${marker.eventDay}_${monthAbbr}_${dayPadded}_${marker.year}_FR.pdf"
            ASSET_EN_TRANSCRIPT -> "Transcript_${marker.eventDay}_${monthAbbr}_${dayPadded}_${marker.year}_EN.pdf"
            ASSET_REFERENCES -> "References_${marker.eventDay}_${monthAbbr}_${dayPadded}_${marker.year}.pdf"
            ASSET_FR_DSG -> "DSG_FR_${marker.eventDay}_${monthAbbr}_${dayPadded}_${marker.year}.pdf"
            else -> "DSG_EN_${marker.eventDay}_${monthAbbr}_${dayPadded}_${marker.year}.pdf"
          }

          val typeKey = assetType.lowercase(Locale.ROOT).replace(" ", "_")
          val id = "dsg_live_${dateKey}_$typeKey"
          if (seenIds.add(id)) {
            results += MiniHqResourceEntity(
              id = id,
              resourceCategory = CATEGORY_DSG_ASSET,
              monthYear = monthYear,
              isNextMonth = isNext,
              district = "",
              serviceDate = displayDate,
              serviceTitle = marker.title,
              assetType = assetType,
              fileName = fileName,
              sourceUrl = fullUrl,
              contentText = "$displayDate • ${marker.title}",
              checksum = sha256("$fullUrl|$displayDate|${marker.title}"),
              lastCheckedAt = nowMillis
            )
          }
        }
      } else {
        // Special / Undated item (e.g. Youth Service, Special Edition DSG, Foreword, Full DSG, Confirmation DSG/Letter)
        val catSlug = when {
          marker.title.contains("Special Edition", ignoreCase = true) -> "SpecialEdition_DSG"
          marker.title.contains("Youth", ignoreCase = true) -> "YouthService_DSG"
          marker.title.contains("Full DSG", ignoreCase = true) -> "Full_DSG"
          marker.title.contains("Confirmation DSG", ignoreCase = true) -> "Confirmation_DSG"
          marker.title.contains("Confirmation Letter", ignoreCase = true) -> "Confirmation_Letter"
          marker.title.contains("Foreword", ignoreCase = true) -> "Foreword_DSG"
          else -> marker.title.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_')
        }
        val defaultMonthParts = monthYear.split(" ")
        val defaultMonthAbbr = DSG_MONTH_ABBR[defaultMonthParts.getOrNull(0)?.lowercase(Locale.ROOT).orEmpty()] ?: "Nov"
        val defaultYear = defaultMonthParts.getOrNull(1) ?: "2026"

        val distinctLinks = rawLinks.distinctBy { it.second }
        val isMultiAssetSpecialService = marker.title.contains("Youth", ignoreCase = true) ||
          distinctLinks.any { (lbl, u) ->
            val lu = u.lowercase(Locale.ROOT)
            val ll = lbl.lowercase(Locale.ROOT)
            lu.contains(".mp3") || lu.contains("transcript") || ll.contains("audio") || ll.contains("transcript")
          }

        if (isMultiAssetSpecialService) {
          // Case A: Full-featured special service (e.g. Youth Service) -> structured asset labels
          distinctLinks.forEach { (label, fullUrl) ->
            val assetType = classifyStructuredDsgLink(sectionHtml, label, fullUrl)
            val fileName = when (assetType) {
              ASSET_AUDIO -> "${catSlug}_Audio_${defaultMonthAbbr}_${defaultYear}.mp3"
              ASSET_FR_BIBLE_READING -> "${catSlug}_BibleReading_${defaultMonthAbbr}_${defaultYear}_FR.pdf"
              ASSET_EN_BIBLE_READING -> "${catSlug}_BibleReading_${defaultMonthAbbr}_${defaultYear}_EN.pdf"
              ASSET_FR_TRANSCRIPT -> "${catSlug}_Transcript_${defaultMonthAbbr}_${defaultYear}_FR.pdf"
              ASSET_EN_TRANSCRIPT -> "${catSlug}_Transcript_${defaultMonthAbbr}_${defaultYear}_EN.pdf"
              ASSET_REFERENCES -> "${catSlug}_References_${defaultMonthAbbr}_${defaultYear}.pdf"
              ASSET_FR_DSG -> "${catSlug}_FR_${defaultMonthAbbr}_${defaultYear}.pdf"
              else -> "${catSlug}_EN_${defaultMonthAbbr}_${defaultYear}.pdf"
            }

            val typeKey = assetType.lowercase(Locale.ROOT).replace(" ", "_")
            val id = "dsg_special_${catSlug.lowercase()}_${defaultMonthAbbr.lowercase()}_${defaultYear}_$typeKey"
            if (seenIds.add(id)) {
              results += MiniHqResourceEntity(
                id = id,
                resourceCategory = CATEGORY_DSG_ASSET,
                monthYear = monthYear,
                isNextMonth = isNext,
                district = "",
                serviceDate = "",
                serviceTitle = marker.title,
                assetType = assetType,
                fileName = fileName,
                sourceUrl = fullUrl,
                contentText = "${marker.title} • $assetType",
                checksum = sha256("$fullUrl|${marker.title}|$assetType"),
                lastCheckedAt = nowMillis
              )
            }
          }
        } else {
          // Case B: Document-only special items (Full DSG, Foreword, Special Edition DSG, Confirmation)
          val detectedMonthsCount = distinctLinks.mapNotNull { (lbl, u) ->
            DSG_MONTH_ABBR.entries.firstOrNull { (full, _) ->
              lbl.lowercase(Locale.ROOT).contains(full) || u.lowercase(Locale.ROOT).contains(full)
            }?.value
          }.distinct().size

          distinctLinks.forEach { (label, fullUrl) ->
            val lowerLabel = label.lowercase(Locale.ROOT)
            val lowerUrl = fullUrl.lowercase(Locale.ROOT)
            val detectedMonthPair = DSG_MONTH_ABBR.entries.firstOrNull { (full, _) ->
              lowerLabel.contains(full) || lowerUrl.contains(full)
            }
            val variantMonthAbbr = detectedMonthPair?.value ?: defaultMonthAbbr
            val variantYear = Regex("""\b(20\d{2})\b""").find(label)?.groupValues?.get(1)
              ?: Regex("""\b(20\d{2})\b""").find(fullUrl)?.groupValues?.get(1)
              ?: defaultYear
            val (langCode, fullLangName) = when {
              lowerLabel.contains("ref") || lowerUrl.contains("ref") -> "REF" to "Bible References"
              lowerLabel.contains("french") || lowerLabel == "fr" || lowerUrl.contains("-fr") || lowerUrl.contains("_fr") -> "FR" to "French"
              lowerLabel.contains("german") || lowerLabel == "de" || lowerUrl.contains("-de") || lowerUrl.contains("_de") -> "DE" to "German"
              lowerLabel.contains("spanish") || lowerLabel == "es" || lowerUrl.contains("-es") || lowerUrl.contains("_es") -> "ES" to "Spanish"
              else -> "EN" to "English"
            }
            val ext = if (lowerUrl.contains(".mp3")) "mp3" else "pdf"
            val fileName = "${catSlug}_${variantMonthAbbr}_${variantYear}_${langCode}.$ext"
            val chipLabel = if (detectedMonthsCount > 1 && detectedMonthPair != null) {
              "$fullLangName ($variantMonthAbbr)"
            } else {
              fullLangName
            }
            val id = "dsg_special_${catSlug.lowercase()}_${variantMonthAbbr.lowercase()}_${variantYear}_${langCode.lowercase()}"
            if (seenIds.add(id)) {
              results += MiniHqResourceEntity(
                id = id,
                resourceCategory = CATEGORY_DSG_ASSET,
                monthYear = monthYear,
                isNextMonth = isNext,
                district = "",
                serviceDate = "",
                serviceTitle = marker.title,
                assetType = chipLabel,
                fileName = fileName,
                sourceUrl = fullUrl,
                contentText = "${marker.title} ($chipLabel)",
                checksum = sha256("$fullUrl|${marker.title}|$chipLabel"),
                lastCheckedAt = nowMillis
              )
            }
          }
        }
      }
    }

    return results
  }

  private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

  private fun normalizeHtmlLabel(raw: String): String {
    return raw
      .replace(Regex("<[^>]+>"), " ")
      .replace("&nbsp;", " ", ignoreCase = true)
      .replace("&amp;", "&", ignoreCase = true)
      .replace(Regex("\\s+"), " ")
      .trim()
  }

  /**
   * Determines whether the fetched MiniHQ landing page requires authentication.
   * Simply checking for the word "Schedules" is unsafe because iMIS templates/nav menus
   * can contain "Schedules" even on the unauthenticated sign-in page.
   */
  fun isLandingPageAuthRequired(
    landingHtml: String?,
    discoveredMonthUrls: List<Pair<String, String>> = if (landingHtml.isNullOrBlank()) emptyList() else extractSchedulesMonthUrls(MINIHQ_URL, landingHtml)
  ): Boolean {
    if (landingHtml.isNullOrBlank()) return true
    val hasPasswordInput =
      landingHtml.contains("type=\"password\"", ignoreCase = true) ||
        landingHtml.contains("type='password'", ignoreCase = true)
    if (hasPasswordInput) return true

    val hasSignOut = landingHtml.contains("Sign Out", ignoreCase = true)
    if (hasSignOut) return false

    val hasSignIn =
      landingHtml.contains("Sign In", ignoreCase = true) ||
        landingHtml.contains("Log On", ignoreCase = true) ||
        landingHtml.contains("Sign_In.aspx", ignoreCase = true)
    if (hasSignIn) return true

    if (discoveredMonthUrls.isNotEmpty() || landingHtml.contains("District Serving Schedules", ignoreCase = true)) {
      return false
    }
    return true
  }

  fun extractSchedulesMonthUrls(landingUrl: String, html: String): List<Pair<String, String>> {
    val anchorRegex = Regex(
      """<a\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val yearRegex = Regex("""(?:^|\D)(20\d{2})(?:\D|$)""")

    // Prefer the `Schedules` section after `Divine Service Prep` to avoid matching nav menus.
    val dsgIdx = Regex(""">\s*Divine\s+Service\s+Prep\s*<""", RegexOption.IGNORE_CASE).find(html)?.range?.first
      ?: html.indexOf("Divine Service Prep", ignoreCase = true)
    val searchStart = if (dsgIdx >= 0) dsgIdx else 0
    val schedulesMatch = Regex(""">\s*Schedules\s*<""", RegexOption.IGNORE_CASE).find(html, searchStart)
    val schedulesStart = schedulesMatch?.range?.first ?: html.indexOf("Schedules", startIndex = searchStart, ignoreCase = true)

    fun canonicalMonthLabel(rawLabel: String, url: String): String {
      if (yearRegex.containsMatchIn(rawLabel) &&
        DSG_MONTH_ABBR.keys.any { rawLabel.contains(it, ignoreCase = true) }
      ) {
        return rawLabel
      }
      val urlMonth = DSG_MONTH_ABBR.keys.firstOrNull { url.contains(it, ignoreCase = true) }
        ?.replaceFirstChar { it.uppercaseChar() }
      val urlYear = yearRegex.find(url)?.groupValues?.getOrNull(1)
      return if (urlMonth != null && urlYear != null) "$urlMonth $urlYear" else rawLabel
    }

    val scopedResults = if (schedulesStart >= 0) {
      val afterSchedules = html.substring(schedulesStart)
      val announcementsStart = Regex(""">\s*Announcements\s*<""", RegexOption.IGNORE_CASE).find(afterSchedules)?.range?.first
        ?: afterSchedules.indexOf("Announcements", ignoreCase = true)
      val schedulesSection = if (announcementsStart > 0) {
        afterSchedules.substring(0, announcementsStart)
      } else {
        afterSchedules
      }
      anchorRegex.findAll(schedulesSection).mapNotNull { match ->
        val fullUrl = resolveFullHrefUrl(landingUrl, match.groupValues[1])
        val rawLabel = normalizeHtmlLabel(match.groupValues[2])
        if (fullUrl.contains("Serving_Schedules", ignoreCase = true) ||
          (rawLabel.isNotBlank() && yearRegex.containsMatchIn(rawLabel) && !fullUrl.contains("DSG", ignoreCase = true) && !fullUrl.contains("Divine", ignoreCase = true))
        ) {
          canonicalMonthLabel(rawLabel, fullUrl) to fullUrl
        } else {
          null
        }
      }.distinctBy { it.second }.toList()
    } else {
      emptyList()
    }

    if (scopedResults.isNotEmpty()) return scopedResults

    // Fallback: scan the full document for explicit `Serving_Schedules_` month links
    return anchorRegex.findAll(html).mapNotNull { match ->
      val fullUrl = resolveFullHrefUrl(landingUrl, match.groupValues[1])
      val rawLabel = normalizeHtmlLabel(match.groupValues[2])
      if (fullUrl.contains("Serving_Schedules", ignoreCase = true) && rawLabel.isNotBlank()) {
        canonicalMonthLabel(rawLabel, fullUrl) to fullUrl
      } else {
        null
      }
    }.distinctBy { it.second }.toList()
  }

  /**
   * Scopes extraction strictly to the `District Serving Schedules` heading/column,
   * ignoring `NACC Calendars 2026`, `Youth Schedules`, and `Seniors Schedules`.
   */
  fun extractDistrictSchedulePdfLinks(monthPageUrl: String, html: String): Map<String, String> {
    val startIdx = html.indexOf("District Serving Schedules", ignoreCase = true)
    if (startIdx < 0) return emptyMap()
    val afterHeading = html.substring(startIdx)
    val stopMarkers = listOf("NACC Calendars", "Youth Schedules", "Seniors Schedules")
    val endIdx = stopMarkers
      .map { afterHeading.indexOf(it, ignoreCase = true) }
      .filter { it > 0 }
      .minOrNull() ?: afterHeading.length
    val scopedSection = afterHeading.substring(0, endIdx)

    val anchorRegex = Regex("""<a\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val result = linkedMapOf<String, String>()
    anchorRegex.findAll(scopedSection).forEach { match ->
      val rawHref = match.groupValues[1].trim()
      if (rawHref.isBlank() || rawHref.startsWith("#") || rawHref.startsWith("javascript:", ignoreCase = true)) {
        return@forEach
      }
      val fullUrl = resolveFullHrefUrl(monthPageUrl, rawHref)
      val label = normalizeHtmlLabel(match.groupValues[2])
      val matchedDistrict = CANADIAN_DISTRICTS.firstOrNull { district ->
        label.equals(district, ignoreCase = true) || label.contains(district, ignoreCase = true)
      }
      if (matchedDistrict != null && fullUrl.startsWith("http", ignoreCase = true)) {
        result[matchedDistrict] = fullUrl
      }
    }
    return result
  }

  /**
   * Attempts automated headless iMIS authentication using `BuildConfig.NAC_USERNAME` and
   * `BuildConfig.NAC_PASSWORD` (loaded from `local.properties` at build time).
   * Credentials are never logged or exposed in any UI/API state.
   */
  private suspend fun attemptAutomatedMiniHqLogin(context: Context): Pair<String, String?> {
    val username = BuildConfig.NAC_USERNAME
    val password = BuildConfig.NAC_PASSWORD
    if (username.isBlank() || password.isBlank()) return "" to null

    val httpAttempt = runCatching {
      var currentUrl = MINIHQ_URL
      var activeCookie = getSessionCookie(context)
      var loginHtml: String? = null
      var redirects = 0

      while (redirects < 6) {
        val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          instanceFollowRedirects = false
          connectTimeout = 8000
          readTimeout = 10000
          setRequestProperty("User-Agent", BROWSER_USER_AGENT)
          setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
          if (activeCookie.isNotBlank()) {
            setRequestProperty("Cookie", activeCookie)
          }
        }
        val code = conn.responseCode
        val setCookies = conn.headerFields["Set-Cookie"] ?: conn.headerFields["set-cookie"]
        if (!setCookies.isNullOrEmpty()) {
          activeCookie = mergeCookies(activeCookie, setCookies)
        }
        if (code in 300..399) {
          val location = conn.getHeaderField("Location").orEmpty()
          conn.disconnect()
          if (location.isBlank()) break
          currentUrl = resolveFullHrefUrl(currentUrl, location)
          redirects++
          continue
        }
        if (code in 200..299) {
          loginHtml = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        conn.disconnect()
        break
      }

      val html = loginHtml ?: return@runCatching activeCookie to null
      if (!isLandingPageAuthRequired(html)) {
        saveSessionCookie(context, activeCookie)
        return@runCatching activeCookie to html
      }

      // Parse form action and input fields from the iMIS / ASP.NET login page
      val formActionMatch = Regex(
        """<form\b[^>]*\baction\s*=\s*["']([^"']*)["'][^>]*>""",
        RegexOption.IGNORE_CASE
      ).find(html)
      val actionUrl = formActionMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
        ?.let { resolveFullHrefUrl(currentUrl, it.replace("&amp;", "&")) }
        ?: currentUrl

      val formParams = linkedMapOf<String, String>()
      val inputTagRegex = Regex("""<input\b[^>]*>""", RegexOption.IGNORE_CASE)
      val attrRegex = Regex("""(\w[\w:-]*)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
      var userFieldKey: String? = null
      var passFieldKey: String? = null
      var submitFieldKey: String? = null
      var submitFieldVal = "Sign In"

      inputTagRegex.findAll(html).forEach { tagMatch ->
        val attrs = mutableMapOf<String, String>()
        attrRegex.findAll(tagMatch.value).forEach { attr ->
          val k = attr.groupValues[1].lowercase(Locale.ROOT)
          val v = attr.groupValues[2].ifEmpty { attr.groupValues[3] }
          attrs[k] = v
        }
        val name = attrs["name"] ?: return@forEach
        val type = attrs["type"]?.lowercase(Locale.ROOT) ?: "text"
        val value = attrs["value"].orEmpty()
        when {
          type == "hidden" -> formParams[name] = value
          type == "password" -> passFieldKey = name
          type == "submit" || type == "image" -> {
            if (submitFieldKey == null && (value.contains("sign", true) || value.contains("log", true) || name.contains("sign", true) || name.contains("login", true) || name.contains("submit", true))) {
              submitFieldKey = name
              if (value.isNotBlank()) submitFieldVal = value
            }
          }
          type == "text" || type == "email" -> {
            if (userFieldKey == null && (name.contains("user", true) || name.contains("login", true) || name.contains("email", true) || name.contains("name", true))) {
              userFieldKey = name
            }
          }
        }
      }

      if (userFieldKey == null || passFieldKey == null) {
        return@runCatching activeCookie to null
      }
      formParams[userFieldKey!!] = username
      formParams[passFieldKey!!] = password
      if (submitFieldKey != null) {
        formParams[submitFieldKey!!] = submitFieldVal
      }

      val encodedBody = formParams.entries.joinToString("&") { (k, v) ->
        "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
      }

      var postUrl = actionUrl
      var postMethod = "POST"
      var postRedirects = 0
      var resultHtml: String? = null
      while (postRedirects < 8) {
        val conn = (URL(postUrl).openConnection() as HttpURLConnection).apply {
          requestMethod = postMethod
          instanceFollowRedirects = false
          connectTimeout = 8000
          readTimeout = 10000
          setRequestProperty("User-Agent", BROWSER_USER_AGENT)
          setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
          setRequestProperty("Referer", currentUrl)
          if (activeCookie.isNotBlank()) {
            setRequestProperty("Cookie", activeCookie)
          }
          if (postMethod == "POST") {
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            outputStream.use { it.write(encodedBody.toByteArray(Charsets.UTF_8)) }
          }
        }
        val code = conn.responseCode
        val setCookies = conn.headerFields["Set-Cookie"] ?: conn.headerFields["set-cookie"]
        if (!setCookies.isNullOrEmpty()) {
          activeCookie = mergeCookies(activeCookie, setCookies)
        }
        if (code in 300..399) {
          val location = conn.getHeaderField("Location").orEmpty()
          conn.disconnect()
          if (location.isBlank()) break
          postUrl = resolveFullHrefUrl(postUrl, location)
          postMethod = "GET"
          postRedirects++
          continue
        }
        if (code in 200..299) {
          resultHtml = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        conn.disconnect()
        break
      }

      if (activeCookie.isNotBlank()) {
        saveSessionCookie(context, activeCookie)
      }
      val finalHtml = if (!resultHtml.isNullOrBlank() && !isLandingPageAuthRequired(resultHtml)) {
        resultHtml
      } else {
        fetchHtmlWithCookie(MINIHQ_URL, activeCookie)
      }
      activeCookie to finalHtml
    }.getOrElse {
      Log.w(TAG, "Automated HTTP login did not complete: ${it.message}")
      "" to null
    }

    if (!httpAttempt.second.isNullOrBlank() && !isLandingPageAuthRequired(httpAttempt.second)) {
      return httpAttempt
    }
    // Fallback to headless off-screen WebView login for iMIS ASP.NET __doPostBack / JS auth flows
    return attemptHeadlessWebViewLogin(context, username, password)
  }

  private val webViewMutex = kotlinx.coroutines.sync.Mutex()

  private fun disposeWebViewSafely(webView: WebView) {
    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    mainHandler.post {
      runCatching {
        webView.stopLoading()
        webView.webViewClient = WebViewClient()
        webView.loadUrl("about:blank")
      }
      mainHandler.postDelayed({
        runCatching { webView.destroy() }
      }, 450L)
    }
  }

  private suspend fun attemptHeadlessWebViewLogin(
    context: Context,
    username: String,
    password: String
  ): Pair<String, String?> = webViewMutex.withLock {
    val result = withTimeoutOrNull(22_000L) {
      withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Pair<String, String?>> { cont ->
          var completed = false
          var loginSubmitted = false
          var signInLinkClicked = false
          val webView = WebView(context.applicationContext)
          webView.settings.javaScriptEnabled = true
          webView.settings.domStorageEnabled = true
          CookieManager.getInstance().setAcceptCookie(true)
          CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

          fun finish(cookie: String, html: String?) {
            if (!completed) {
              completed = true
              disposeWebViewSafely(webView)
              if (cont.isActive) cont.resume(cookie to html)
            }
          }

          val escapedUser = username.replace("\\", "\\\\").replace("'", "\\'")
          val escapedPass = password.replace("\\", "\\\\").replace("'", "\\'")

          webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
              if (completed || view == null) return
              CookieManager.getInstance().flush()
              view.evaluateJavascript(
                "(function(){ return document.documentElement ? document.documentElement.outerHTML : ''; })();"
              ) { rawJson ->
                if (completed) return@evaluateJavascript
                val html = runCatching {
                  org.json.JSONTokener(rawJson).nextValue()?.toString()
                }.getOrNull().orEmpty()
                if (html.isNotBlank() && !isLandingPageAuthRequired(html)) {
                  val cookie = persistWebViewCookies(context)
                  finish(cookie, html)
                } else if (html.isNotBlank()) {
                  val jsFillOrClickSignIn = """
                    (function() {
                      var p = document.querySelector('input[type="password"]');
                      var u = document.querySelector('input[type="text"], input[type="email"], input[id*="User" i], input[name*="User" i], input[id*="Login" i]');
                      if (u && p && !$loginSubmitted) {
                        u.value = '$escapedUser';
                        u.dispatchEvent(new Event('input', { bubbles: true }));
                        u.dispatchEvent(new Event('change', { bubbles: true }));
                        p.value = '$escapedPass';
                        p.dispatchEvent(new Event('input', { bubbles: true }));
                        p.dispatchEvent(new Event('change', { bubbles: true }));
                        var btn = document.querySelector('input[type="submit"], button[type="submit"], a[id*="SignIn" i], a[id*="Login" i], input[id*="SignIn" i], button');
                        if (btn) { btn.click(); return 'submitted'; }
                        var form = p.closest('form');
                        if (form) { form.submit(); return 'form_submitted'; }
                      }
                      if (!p && !$signInLinkClicked) {
                        var signInLink = Array.from(document.querySelectorAll('a, button, input[type="button"], input[type="submit"]')).find(function(el) {
                          var txt = (el.innerText || el.value || '').trim().toLowerCase();
                          return txt === 'sign in' || txt === 'log in' || txt === 'log on' || (el.href && el.href.toLowerCase().indexOf('sign_in') >= 0);
                        });
                        if (signInLink) { signInLink.click(); return 'clicked_signin_link'; }
                      }
                      return 'waiting';
                    })();
                  """.trimIndent()
                  view.evaluateJavascript(jsFillOrClickSignIn) { actionRaw ->
                    val action = actionRaw.orEmpty()
                    if (action.contains("submitted")) {
                      loginSubmitted = true
                    } else if (action.contains("clicked_signin_link")) {
                      signInLinkClicked = true
                    }
                  }
                }
              }
            }
          }

          cont.invokeOnCancellation {
            disposeWebViewSafely(webView)
          }
          webView.loadUrl(MINIHQ_URL)
        }
      }
    }
    return@withLock result ?: ("" to null)
  }

  private suspend fun fetchHtmlViaWebView(
    context: Context,
    targetUrl: String,
    expectedMarker: String? = null
  ): String? = webViewMutex.withLock {
    val result = withTimeoutOrNull(18_000L) {
      withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<String?> { cont ->
          var completed = false
          var loginSubmitted = false
          var signInLinkClicked = false
          val webView = WebView(context.applicationContext)
          webView.settings.javaScriptEnabled = true
          webView.settings.domStorageEnabled = true
          CookieManager.getInstance().setAcceptCookie(true)
          CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

          fun finish(html: String?) {
            if (!completed) {
              completed = true
              disposeWebViewSafely(webView)
              if (cont.isActive) cont.resume(html)
            }
          }

          val escapedUser = BuildConfig.NAC_USERNAME.replace("\\", "\\\\").replace("'", "\\'")
          val escapedPass = BuildConfig.NAC_PASSWORD.replace("\\", "\\\\").replace("'", "\\'")

          fun readDomAndCheck(view: WebView, allowRetry: Boolean) {
            if (completed) return
            CookieManager.getInstance().flush()
            view.evaluateJavascript(
              "(function(){ return document.documentElement ? document.documentElement.outerHTML : ''; })();"
            ) { rawJson ->
              if (completed) return@evaluateJavascript
              val html = runCatching {
                org.json.JSONTokener(rawJson).nextValue()?.toString()
              }.getOrNull().orEmpty()

              if (html.isNotBlank() && isLandingPageAuthRequired(html) && escapedUser.isNotBlank() && escapedPass.isNotBlank()) {
                val jsFillOrClickSignIn = """
                  (function() {
                    var p = document.querySelector('input[type="password"]');
                    var u = document.querySelector('input[type="text"], input[type="email"], input[id*="User" i], input[name*="User" i], input[id*="Login" i]');
                    if (u && p && !$loginSubmitted) {
                      u.value = '$escapedUser';
                      u.dispatchEvent(new Event('input', { bubbles: true }));
                      u.dispatchEvent(new Event('change', { bubbles: true }));
                      p.value = '$escapedPass';
                      p.dispatchEvent(new Event('input', { bubbles: true }));
                      p.dispatchEvent(new Event('change', { bubbles: true }));
                      var btn = document.querySelector('input[type="submit"], button[type="submit"], a[id*="SignIn" i], a[id*="Login" i], input[id*="SignIn" i], button');
                      if (btn) { btn.click(); return 'submitted'; }
                      var form = p.closest('form');
                      if (form) { form.submit(); return 'form_submitted'; }
                    }
                    if (!p && !$signInLinkClicked) {
                      var signInLink = Array.from(document.querySelectorAll('a, button, input[type="button"], input[type="submit"]')).find(function(el) {
                        var txt = (el.innerText || el.value || '').trim().toLowerCase();
                        return txt === 'sign in' || txt === 'log in' || txt === 'log on' || (el.href && el.href.toLowerCase().indexOf('sign_in') >= 0);
                      });
                      if (signInLink) { signInLink.click(); return 'clicked_signin_link'; }
                    }
                    return 'waiting';
                  })();
                """.trimIndent()
                view.evaluateJavascript(jsFillOrClickSignIn) { actionRaw ->
                  val action = actionRaw.orEmpty()
                  if (action.contains("submitted")) loginSubmitted = true
                  else if (action.contains("clicked_signin_link")) signInLinkClicked = true
                }
                return@evaluateJavascript
              }

              val markerFound = expectedMarker.isNullOrBlank() || html.contains(expectedMarker, ignoreCase = true)
              if (html.isNotBlank() && !isLandingPageAuthRequired(html) && (markerFound || !allowRetry)) {
                persistWebViewCookies(context)
                finish(html)
              } else if (allowRetry) {
                view.postDelayed({ readDomAndCheck(view, allowRetry = false) }, 650L)
              }
            }
          }

          webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
              if (completed || view == null) return
              readDomAndCheck(view, allowRetry = true)
            }
          }

          cont.invokeOnCancellation {
            disposeWebViewSafely(webView)
          }
          webView.loadUrl(targetUrl)
        }
      }
    }
    return@withLock result
  }

  private fun saveSessionCookie(context: Context, cookieHeader: String) {
    if (cookieHeader.isBlank()) return
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString("key_minihq_session_cookie", cookieHeader)
      .apply()
  }

  private fun mergeCookies(existingCookieHeader: String, setCookieHeaders: List<String>?): String {
    val cookieMap = linkedMapOf<String, String>()
    existingCookieHeader.split(";")
      .map { it.trim() }
      .filter { it.contains("=") }
      .forEach { pair ->
        val idx = pair.indexOf('=')
        cookieMap[pair.substring(0, idx).trim()] = pair.substring(idx + 1).trim()
      }
    setCookieHeaders.orEmpty().forEach { header ->
      val firstPart = header.substringBefore(";").trim()
      val idx = firstPart.indexOf('=')
      if (idx > 0) {
        cookieMap[firstPart.substring(0, idx).trim()] = firstPart.substring(idx + 1).trim()
      }
    }
    return cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
  }

  private fun fetchHtmlWithCookie(urlString: String, sessionCookie: String): String? {
    return runCatching {
      var currentUrl = urlString
      var activeCookie = sessionCookie
      var redirects = 0
      while (redirects < 6) {
        val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          instanceFollowRedirects = false
          connectTimeout = 8000
          readTimeout = 10000
          setRequestProperty("User-Agent", BROWSER_USER_AGENT)
          setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
          if (activeCookie.isNotBlank()) {
            setRequestProperty("Cookie", activeCookie)
          }
        }
        val code = conn.responseCode
        val setCookies = conn.headerFields["Set-Cookie"] ?: conn.headerFields["set-cookie"]
        if (!setCookies.isNullOrEmpty()) {
          activeCookie = mergeCookies(activeCookie, setCookies)
        }
        if (code in 300..399) {
          val location = conn.getHeaderField("Location").orEmpty()
          conn.disconnect()
          if (location.isBlank()) return@runCatching null
          currentUrl = resolveFullHrefUrl(currentUrl, location)
          redirects++
          continue
        }
        val html = if (code in 200..299) {
          conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
          null
        }
        conn.disconnect()
        return@runCatching html
      }
      null
    }.getOrNull()
  }

  fun extractAnnouncementsFromLandingHtml(landingUrl: String, html: String): List<MiniHqAnnouncement> {
    val annIdx = Regex(""">\s*Announcements\s*<""", RegexOption.IGNORE_CASE).find(html)?.range?.first
      ?: html.indexOf("Announcements", ignoreCase = true)
    if (annIdx < 0) return emptyList()
    val section = html.substring(annIdx)
    val anchorRegex = Regex(
      """<a\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    return anchorRegex.findAll(section).mapIndexedNotNull { idx, match ->
      val fullUrl = resolveFullHrefUrl(landingUrl, match.groupValues[1])
      val title = normalizeHtmlLabel(match.groupValues[2])
      if (title.isBlank() || title.equals("Sign Out", ignoreCase = true)) return@mapIndexedNotNull null
      val preContext = section.substring((match.range.first - 140).coerceAtLeast(0), match.range.first)
      val dateText = Regex("""(?:January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2},\s+\d{4}""", RegexOption.IGNORE_CASE)
        .findAll(preContext)
        .lastOrNull()
        ?.value
        .orEmpty()
      val category = when {
        title.contains("Funeral", ignoreCase = true) -> "Funeral Notice"
        title.contains("Serving Schedule", ignoreCase = true) -> "Serving Schedules"
        title.contains("Divine Service", ignoreCase = true) -> "Divine Service Prep"
        else -> "Announcements"
      }
      MiniHqAnnouncement(
        id = "minihq_ann_$idx",
        title = title,
        date = dateText.ifBlank { "MiniHQ" },
        category = category,
        summary = title,
        content = "",
        sourceUrl = fullUrl
      )
    }.distinctBy { it.sourceUrl }.toList()
  }

  suspend fun scrapeAnnouncementsWithDetails(
    landingUrl: String,
    landingHtml: String,
    sessionCookie: String
  ): List<MiniHqAnnouncement> = coroutineScope {
    val baseList = extractAnnouncementsFromLandingHtml(landingUrl, landingHtml)
    if (baseList.isEmpty()) return@coroutineScope emptyList()

    baseList.mapIndexed { idx, ann ->
      async(Dispatchers.IO) {
        if (ann.sourceUrl.isBlank()) return@async ann
        val detailHtml = fetchHtmlWithCookie(ann.sourceUrl, sessionCookie)
        if (detailHtml.isNullOrBlank() || isLandingPageAuthRequired(detailHtml)) {
          ann.copy(
            summary = buildConciseAnnouncementSummary(ann.title, ann.content),
            content = ann.content.ifBlank { ann.title }
          )
        } else {
          parseAnnouncementDetailHtml(ann, detailHtml)
        }
      }
    }.awaitAll()
  }

  fun parseAnnouncementDetailHtml(
    base: MiniHqAnnouncement,
    detailHtml: String
  ): MiniHqAnnouncement {
    // 1. Strip scripts, styles, noscripts, and site-wide nav/header/footer chrome
    var cleaned = detailHtml
      .replace(Regex("""<script\b[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
      .replace(Regex("""<style\b[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
      .replace(Regex("""<noscript\b[^>]*>.*?</noscript>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
      .replace(Regex("""<header\b[^>]*>.*?</header>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
      .replace(Regex("""<nav\b[^>]*>.*?</nav>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
      .replace(Regex("""<footer\b[^>]*>.*?</footer>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")

    // 2. Isolate the main announcement content container from iMIS page layout
    val mainContentPatterns = listOf(
      Regex("""<div[^>]+id\s*=\s*["'][^"']*(?:MainContent|ContentPanel|BodyContent|TemplateBody|ContentItem)[^"']*["'][^>]*>(.*?)</div>\s*(?:</div>\s*)*(?:<div[^>]+id\s*=\s*["'][^"']*Footer|<footer|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
      Regex("""<article\b[^>]*>(.*?)</article>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
      Regex("""<main\b[^>]*>(.*?)</main>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    )
    var bodyHtml = mainContentPatterns.firstNotNullOfOrNull { regex ->
      regex.find(cleaned)?.groupValues?.getOrNull(1)?.takeIf { it.length > 40 }
    } ?: run {
      // Fallback: slice from the first main heading after MiniHQ nav ("Sign Out" / "VIDEO")
      val navEndIdx = Regex("""(?:Sign Out|MEMBER MANAGEMENT|TRAINING|VIDEO)\s*</a>""", RegexOption.IGNORE_CASE)
        .findAll(cleaned)
        .lastOrNull()
        ?.range
        ?.last ?: 0
      val afterNav = if (navEndIdx > 0 && navEndIdx < cleaned.length) cleaned.substring(navEndIdx) else cleaned
      val footerStart = Regex("""<div[^>]+id\s*=\s*["'][^"']*Footer[^"']*["']""", RegexOption.IGNORE_CASE)
        .find(afterNav)?.range?.first ?: afterNav.length
      afterNav.substring(0, footerStart)
    }

    // Remove residual breadcrumb/search/form controls
    bodyHtml = bodyHtml
      .replace(Regex("""<form\b[^>]*>""", RegexOption.IGNORE_CASE), "")
      .replace(Regex("""</form>""", RegexOption.IGNORE_CASE), "")
      .replace(Regex("""<input\b[^>]*>""", RegexOption.IGNORE_CASE), "")
      .replace(Regex("""<button\b[^>]*>.*?</button>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
      .trim()

    // 3. Detect any attached/linked PDF inside the announcement body
    val pdfLinkMatch = Regex(
      """<a\s+[^>]*href\s*=\s*["']([^"']*\.pdf[^"']*)["'][^>]*>(.*?)</a>""",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).find(bodyHtml)
    val attachedPdfUrl = pdfLinkMatch?.let { resolveFullHrefUrl(base.sourceUrl, it.groupValues[1]) }.orEmpty()
    val attachedPdfName = pdfLinkMatch?.let {
      normalizeHtmlLabel(it.groupValues[2]).ifBlank {
        attachedPdfUrl.substringAfterLast('/').substringBefore('?').ifBlank { "Announcement.pdf" }
      }
    }.orEmpty()

    // 4. Convert HTML to clean structured plain text (body_text)
    val bodyText = htmlToStructuredPlainText(bodyHtml)
      .ifBlank { base.title }

    // 5. Generate concise 1-2 sentence summary (~140-180 chars, no mid-word cutoffs)
    val summaryText = buildConciseAnnouncementSummary(base.title, bodyText)

    return base.copy(
      summary = summaryText,
      content = bodyText,
      bodyHtml = bodyHtml,
      attachedPdfUrl = attachedPdfUrl,
      attachedPdfName = attachedPdfName
    )
  }

  private fun htmlToStructuredPlainText(html: String): String {
    if (html.isBlank()) return ""
    var text = html
      .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
      .replace(Regex("""</(?:p|div|h[1-6]|li|tr)>""", RegexOption.IGNORE_CASE), "\n")
      .replace(Regex("""<li\b[^>]*>""", RegexOption.IGNORE_CASE), "• ")
      .replace(Regex("""<td\b[^>]*>""", RegexOption.IGNORE_CASE), " ")
      .replace(Regex("""<[^>]+>"""), "")
    text = decodeBasicHtmlEntities(text)
    return text.lines()
      .map { it.trim().replace(Regex("""\s{2,}"""), " ") }
      .fold(mutableListOf<String>()) { acc, line ->
        if (line.isBlank()) {
          if (acc.isNotEmpty() && acc.last().isNotBlank()) acc.add("")
        } else {
          acc.add(line)
        }
        acc
      }
      .joinToString("\n")
      .trim()
  }

  private fun decodeBasicHtmlEntities(input: String): String {
    var s = input
      .replace("&nbsp;", " ", ignoreCase = true)
      .replace("&amp;", "&", ignoreCase = true)
      .replace("&lt;", "<", ignoreCase = true)
      .replace("&gt;", ">", ignoreCase = true)
      .replace("&quot;", "\"", ignoreCase = true)
      .replace("&#39;", "'")
      .replace("&apos;", "'", ignoreCase = true)
      .replace("&ndash;", "–", ignoreCase = true)
      .replace("&mdash;", "—", ignoreCase = true)
    s = Regex("""&#(\d+);""").replace(s) { m ->
      m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
    }
    s = Regex("""&#x([0-9a-fA-F]+);""").replace(s) { m ->
      m.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: m.value
    }
    return s
  }

  fun buildConciseAnnouncementSummary(title: String, bodyText: String): String {
    val cleanLines = bodyText.lines()
      .map { it.trim() }
      .filter { it.isNotBlank() && !it.equals(title, ignoreCase = true) && !it.equals("Funeral Notice", ignoreCase = true) }

    if (cleanLines.isEmpty()) return title

    val joined = cleanLines.joinToString(" ")
      .replace(Regex("""\s+"""), " ")
      .trim()

    if (joined.length <= 175) return joined

    // Try to take 1-2 complete sentences within ~175 chars
    val sentenceMatches = Regex("""[^.!?]+[.!?]""").findAll(joined).map { it.value.trim() }.toList()
    if (sentenceMatches.isNotEmpty()) {
      val sb = StringBuilder()
      for (sentence in sentenceMatches) {
        val candidate = if (sb.isEmpty()) sentence else "${sb} $sentence"
        if (candidate.length <= 180) {
          sb.clear()
          sb.append(candidate)
        } else {
          break
        }
      }
      if (sb.isNotEmpty()) return sb.toString()
    }

    // Extractive fallback without mid-word cutoff
    val slice = joined.substring(0, 170.coerceAtMost(joined.length))
    val lastSpace = slice.lastIndexOf(' ')
    val cleanCut = if (lastSpace > 100) slice.substring(0, lastSpace) else slice
    return "$cleanCut…"
  }

  fun loadPersistedAnnouncements(context: Context): List<MiniHqAnnouncement>? = runCatching {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_SCRAPED_ANNOUNCEMENTS_JSON, null)
      ?.takeIf { it.isNotBlank() } ?: return null
    announcementJson.decodeFromString<List<MiniHqAnnouncement>>(raw)
  }.getOrNull()

  private fun persistAnnouncements(context: Context, list: List<MiniHqAnnouncement>) {
    runCatching {
      val encoded = announcementJson.encodeToString(list)
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putString(KEY_SCRAPED_ANNOUNCEMENTS_JSON, encoded)
        .apply()
    }
  }

  private suspend fun syncResourcesToLocalCacheAndDb(
    context: Context,
    dao: com.example.parishsync.data.local.ParishDao,
    stagedServices: List<MiniHqStagedService>,
    existingById: Map<String, MiniHqResourceEntity>,
    landingHtml: String? = null,
    sessionCookie: String = "",
    authRequired: Boolean = false,
    scope: SyncScope = SyncScope.ALL
  ): String {
    val cacheDir = File(context.filesDir, "minihq_cache").apply { mkdirs() }
    purgeLegacyMockCache(context)

    val syncSchedules = scope == SyncScope.SCHEDULES || scope == SyncScope.ALL
    val syncDsg = scope == SyncScope.DSG || scope == SyncScope.ALL

    // Discover live month -> district PDF URLs and DSG resources if authenticated MiniHQ HTML was retrieved
    val liveDistrictUrlsByMonthAndDistrict = mutableMapOf<Pair<String, String>, String>()
    val liveDsgResourcesByMonth = mutableMapOf<String, List<MiniHqResourceEntity>>()
    val diagNotes = java.util.Collections.synchronizedList(mutableListOf<String>())
    var activeCookie = sessionCookie
    if (!landingHtml.isNullOrBlank() && !authRequired) {
      if (syncSchedules) {
        val monthUrls = extractSchedulesMonthUrls(MINIHQ_URL, landingHtml)
        if (monthUrls.isEmpty()) {
          diagNotes += "No schedule month links found on MiniHQ landing page."
        }
        monthUrls.forEach { (monthLabel, monthUrl) ->
          val httpHtml = fetchHtmlWithCookie(monthUrl, activeCookie)
          val httpLinks = if (!httpHtml.isNullOrBlank() && !isLandingPageAuthRequired(httpHtml)) {
            extractDistrictSchedulePdfLinks(monthUrl, httpHtml)
          } else {
            emptyMap()
          }
          val monthHtml = if (httpLinks.isNotEmpty()) {
            httpHtml
          } else {
            fetchHtmlViaWebView(context, monthUrl, expectedMarker = "District Serving Schedules")?.also {
              val refreshed = getSessionCookie(context)
              if (refreshed.isNotBlank()) activeCookie = refreshed
            } ?: httpHtml
          }
          if (!monthHtml.isNullOrBlank()) {
            val links = if (httpLinks.isNotEmpty()) httpLinks else extractDistrictSchedulePdfLinks(monthUrl, monthHtml)
            if (links.isEmpty()) {
              diagNotes += "No district PDF links found on $monthLabel ($monthUrl)."
            }
            links.forEach { (district, pdfUrl) ->
              liveDistrictUrlsByMonthAndDistrict[monthLabel.lowercase() to district.lowercase()] = pdfUrl
            }
          } else {
            diagNotes += "Failed to fetch month schedule page $monthLabel ($monthUrl)."
          }
        }
      }
      if (syncDsg) {
        extractDsgMonthUrls(MINIHQ_URL, landingHtml).forEach { (monthLabel, dsgMonthUrl) ->
          val httpDsgHtml = fetchHtmlWithCookie(dsgMonthUrl, activeCookie)
          val httpParsed = if (!httpDsgHtml.isNullOrBlank() && !isLandingPageAuthRequired(httpDsgHtml)) {
            extractDsgResourcesFromHtml(dsgMonthUrl, monthLabel, httpDsgHtml)
          } else {
            emptyList()
          }
          val dsgHtml = if (httpParsed.isNotEmpty()) {
            httpDsgHtml
          } else {
            fetchHtmlViaWebView(context, dsgMonthUrl, expectedMarker = "DIVINE SERVICE PREP")?.also {
              val refreshed = getSessionCookie(context)
              if (refreshed.isNotBlank()) activeCookie = refreshed
            } ?: httpDsgHtml
          }
          if (!dsgHtml.isNullOrBlank()) {
            val parsedDsg = if (httpParsed.isNotEmpty()) httpParsed else extractDsgResourcesFromHtml(dsgMonthUrl, monthLabel, dsgHtml)
            if (parsedDsg.isNotEmpty()) {
              liveDsgResourcesByMonth[monthLabel.lowercase()] = parsedDsg
            }
          }
        }
      }
    }

    val baseCatalog = if (syncSchedules) discoverDistrictPlaceholdersOnly() else emptyList()
    val districtCatalog = if (syncSchedules) {
      baseCatalog.map { res ->
        val liveUrl = liveDistrictUrlsByMonthAndDistrict[res.monthYear.lowercase() to res.district.lowercase()]
        if (!liveUrl.isNullOrBlank()) res.copy(sourceUrl = liveUrl) else res
      }
    } else {
      emptyList()
    }
    val dsgCatalog = if (syncDsg) {
      liveDsgResourcesByMonth.values.flatten()
    } else {
      emptyList()
    }
    val discovered = districtCatalog + dsgCatalog

    val districtResources = discovered.filter { it.resourceCategory == CATEGORY_DISTRICT_SCHEDULE }
    val totalDistricts = districtResources.size.coerceAtLeast(1)
    val processedCounter = java.util.concurrent.atomic.AtomicInteger(0)

    val updatedResources = coroutineScope {
      discovered.chunked(4).flatMap { batch ->
        batch.map { res ->
          async(Dispatchers.IO) {
            if (res.resourceCategory == CATEGORY_DISTRICT_SCHEDULE) {
              val processed = processedCounter.incrementAndGet()
              val pct = 35 + ((processed * 50) / totalDistricts)
              _syncProgress.value = SyncProgressState(
                isSyncing = true,
                activeScope = scope,
                percent = pct.coerceIn(35, 85),
                stepMessage = "Downloading & verifying District PDFs ($processed/$totalDistricts: ${res.district} - ${res.monthYear})...",
                requiresAuth = authRequired
              )
            }

            Log.i(TAG, "Scraping MiniHQ resource URL: ${res.sourceUrl}")
            val existing = existingById[res.id]
            val targetDir = storageDirForCategory(context, res.resourceCategory)
            val targetFile = File(targetDir, res.fileName)
            val existingFile = existing?.localPath?.takeIf { it.isNotBlank() }?.let { File(it) }
            val hasPreservedCache = existing != null &&
              (existing.checksum.startsWith("manual_") || existing.checksum.startsWith("live_")) &&
              existingFile != null &&
              isValidCachedFile(existingFile)

            if (existing != null && existing.checksum.startsWith("manual_") && existingFile != null && isValidCachedFile(existingFile)) {
              existing.copy(pdfBlob = null)
            } else {
              val alreadyCached = existing != null &&
                existing.checksum == res.checksum &&
                existingFile != null &&
                isValidCachedFile(existingFile)

              if (alreadyCached) {
                res.copy(
                  localPath = existingFile!!.absolutePath,
                  fileSize = existingFile.length(),
                  mimeType = mimeTypeFor(res.fileName),
                  pdfBlob = null
                )
              } else if (res.sourceUrl.isNotBlank()) {
                val remoteResult = fetchRemoteBinaryWithValidation(
                  urlString = res.sourceUrl,
                  fileName = res.fileName,
                  sessionCookie = sessionCookie
                )
                val remoteBytes = remoteResult.getOrNull()
                if (remoteBytes != null) {
                  targetFile.writeBytes(remoteBytes)
                  val digest = MessageDigest.getInstance("SHA-256").digest(remoteBytes)
                  val liveHash = "live_" + digest.joinToString("") { "%02x".format(it) }
                  res.copy(
                    localPath = targetFile.absolutePath,
                    fileSize = remoteBytes.size.toLong(),
                    mimeType = mimeTypeFor(res.fileName),
                    pdfBlob = null,
                    checksum = liveHash
                  )
                } else if (hasPreservedCache) {
                  existing!!.copy(pdfBlob = null)
                } else {
                  if (res.resourceCategory == CATEGORY_DISTRICT_SCHEDULE) {
                    diagNotes += "${res.district} (${res.monthYear}): ${remoteResult.exceptionOrNull()?.message ?: "download failed"}"
                  }
                  if (targetFile.exists()) targetFile.delete()
                  res.copy(localPath = "", fileSize = 0L, pdfBlob = null)
                }
              } else if (hasPreservedCache) {
                existing!!.copy(pdfBlob = null)
              } else {
                if (targetFile.exists()) targetFile.delete()
                res.copy(localPath = "", fileSize = 0L, pdfBlob = null)
              }
            }
          }
        }.awaitAll()
      }
    }

    val persistedResources = if (liveDsgResourcesByMonth.isNotEmpty()) {
      updatedResources.filter { res ->
        res.resourceCategory == CATEGORY_DISTRICT_SCHEDULE ||
          (res.localPath.isNotBlank() && res.fileSize > 0L)
      }
    } else {
      updatedResources
    }

    if (persistedResources.isNotEmpty()) {
      dao.upsertMiniHqResources(persistedResources)
    }
    if (scope == SyncScope.ALL) {
      val activeIds = persistedResources.map { it.id }
      if (activeIds.isNotEmpty()) {
        dao.deleteStaleMiniHqResources(activeIds)
      }
    } else if (scope == SyncScope.DSG && liveDsgResourcesByMonth.isNotEmpty()) {
      val keepIds = (existingById.values
        .filter { it.resourceCategory != CATEGORY_DSG_ASSET }
        .map { it.id } + persistedResources.map { it.id }).distinct()
      if (keepIds.isNotEmpty()) {
        dao.deleteStaleMiniHqResources(keepIds)
      }
    }
    val activeFileNames = persistedResources.map { it.fileName }.toSet()
    cacheDir.listFiles()?.forEach { file ->
      if (file.name !in activeFileNames) {
        file.delete()
      }
    }
    return diagNotes.take(5).joinToString("\n")
  }

  const val MIN_PDF_BYTES = 5 * 1024 + 1
  const val MIN_AUDIO_BYTES = 50 * 1024 + 1

  data class DownloadResult(
    val fileName: String,
    val contentUri: Uri?,
    val mimeType: String
  )

  fun isMockPlaceholderPdf(bytes: ByteArray): Boolean {
    if (bytes.isEmpty()) return false
    val text = String(bytes, Charsets.ISO_8859_1)
    return text.contains("NEW APOSTOLIC CHURCH CANADA - MINI HQ") ||
      text.contains("NAC Portal Verified PDF Padding")
  }

  fun resolveFullHrefUrl(baseUrl: String, href: String): String {
    val cleanHref = href.trim().replace("&amp;", "&")
    if (cleanHref.isBlank()) return ""
    val resolved = runCatching {
      val effectiveBase = if (cleanHref.startsWith("/")) {
        URL("https://www.naccanada.org")
      } else {
        URL(baseUrl)
      }
      URL(effectiveBase, cleanHref).toString()
    }.getOrDefault(cleanHref)

    // Upgrade http:// -> https:// for naccanada.org
    var normalized = resolved
      .replace(Regex("""^http://(www\.)?naccanada\.org""", RegexOption.IGNORE_CASE), "https://www.naccanada.org")

    // Prevent duplicated /MiniHQ/ path segments (e.g. /IMIS_PROD/MiniHQ/Resources/Schedules/MiniHQ/Schedules/...)
    val miniHqRegex = Regex("""/MiniHQ/""", RegexOption.IGNORE_CASE)
    val matches = miniHqRegex.findAll(normalized).toList()
    if (matches.size > 1) {
      val tailFromLastMiniHq = normalized.substring(matches.last().range.first)
      normalized = "https://www.naccanada.org/imis_prod$tailFromLastMiniHq"
    }
    return normalized
  }

  fun extractFullHrefLinks(baseUrl: String, html: String): List<String> {
    val anchorRegex = Regex("""<a\s+[^>]*href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    return anchorRegex.findAll(html)
      .map { match -> resolveFullHrefUrl(baseUrl, match.groupValues[1]) }
      .filter { it.isNotBlank() }
      .distinct()
      .toList()
  }

  fun persistWebViewCookies(context: Context): String {
    val cookieMap = linkedMapOf<String, String>()
    runCatching {
      val cm = CookieManager.getInstance()
      cm.flush()
      listOf(
        cm.getCookie(MINIHQ_URL).orEmpty(),
        cm.getCookie("https://www.naccanada.org").orEmpty(),
        cm.getCookie("https://naccanada.org").orEmpty()
      ).forEach { raw ->
        raw.split(";").map { it.trim() }.filter { it.contains("=") }.forEach { pair ->
          val idx = pair.indexOf('=')
          cookieMap[pair.substring(0, idx).trim()] = pair.substring(idx + 1).trim()
        }
      }
    }
    val merged = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
    if (merged.isNotBlank()) {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("key_minihq_session_cookie", merged)
        .apply()
    }
    return merged
  }

  private fun getSessionCookie(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedCookie = prefs.getString("key_minihq_session_cookie", "").orEmpty()
    val webViewCookies = runCatching {
      val cm = CookieManager.getInstance()
      listOf(
        cm.getCookie(MINIHQ_URL).orEmpty(),
        cm.getCookie("https://www.naccanada.org").orEmpty(),
        cm.getCookie("https://naccanada.org").orEmpty()
      )
    }.getOrDefault(emptyList())
    val cookieMap = linkedMapOf<String, String>()
    (listOf(savedCookie) + webViewCookies).forEach { raw ->
      raw.split(";").map { it.trim() }.filter { it.contains("=") }.forEach { pair ->
        val idx = pair.indexOf('=')
        cookieMap[pair.substring(0, idx).trim()] = pair.substring(idx + 1).trim()
      }
    }
    return cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
  }

  fun isValidBinaryAsset(
    fileName: String,
    contentType: String?,
    bytes: ByteArray
  ): Boolean {
    val ct = contentType?.lowercase().orEmpty()
    if (ct.contains("text/html") || ct.contains("application/json")) {
      return false
    }
    if (isMockPlaceholderPdf(bytes)) {
      return false
    }
    return when {
      fileName.endsWith(".pdf", ignoreCase = true) -> {
        bytes.size >= MIN_PDF_BYTES &&
          bytes.size >= 5 &&
          bytes[0] == '%'.code.toByte() &&
          bytes[1] == 'P'.code.toByte() &&
          bytes[2] == 'D'.code.toByte() &&
          bytes[3] == 'F'.code.toByte() &&
          bytes[4] == '-'.code.toByte()
      }
      fileName.endsWith(".mp3", ignoreCase = true) -> {
        if (bytes.size < MIN_AUDIO_BYTES || bytes.size < 3) return false
        val isId3 = bytes[0] == 'I'.code.toByte() &&
          bytes[1] == 'D'.code.toByte() &&
          bytes[2] == '3'.code.toByte()
        val isMpegSync = bytes[0] == 0xFF.toByte() && (bytes[1].toInt() and 0xE0) == 0xE0
        isId3 || isMpegSync
      }
      else -> bytes.isNotEmpty()
    }
  }

  fun isValidCachedFile(file: File): Boolean {
    if (!file.exists() || !file.isFile) return false
    val bytes = runCatching { file.readBytes() }.getOrNull() ?: return false
    return isValidBinaryAsset(file.name, null, bytes)
  }

  private fun fetchRemoteBinaryWithValidation(
    urlString: String,
    fileName: String,
    sessionCookie: String = ""
  ): Result<ByteArray> {
    return runCatching {
      var currentUrl = urlString
      var activeCookie = sessionCookie
      var redirects = 0
      while (redirects < 6) {
        val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          instanceFollowRedirects = false
          connectTimeout = 8000
          readTimeout = 15000
          setRequestProperty("User-Agent", BROWSER_USER_AGENT)
          setRequestProperty("Accept", "application/pdf, audio/mpeg, application/octet-stream, */*")
          if (activeCookie.isNotBlank()) {
            setRequestProperty("Cookie", activeCookie)
          }
        }
        val code = conn.responseCode
        val setCookies = conn.headerFields["Set-Cookie"] ?: conn.headerFields["set-cookie"]
        if (!setCookies.isNullOrEmpty()) {
          activeCookie = mergeCookies(activeCookie, setCookies)
        }
        if (code in 300..399) {
          val location = conn.getHeaderField("Location").orEmpty()
          conn.disconnect()
          if (location.isBlank()) {
            error("HTTP $code redirect missing Location header for $currentUrl")
          }
          currentUrl = resolveFullHrefUrl(currentUrl, location)
          redirects++
          continue
        }
        if (code in 200..299) {
          val contentType = conn.contentType
          val bytes = conn.inputStream.use { it.readBytes() }
          conn.disconnect()
          Log.i(TAG, "Fetched $fileName: HTTP=$code, Content-Type=$contentType, bytes=${bytes.size}, URL=$currentUrl")
          if (!isValidBinaryAsset(fileName, contentType, bytes)) {
            error("Invalid binary response (Content-Type=$contentType, bytes=${bytes.size}) from $currentUrl")
          }
          return@runCatching bytes
        }
        conn.disconnect()
        error("HTTP $code downloading $currentUrl")
      }
      error("Too many redirects downloading $urlString")
    }.onFailure { err ->
      Log.e(TAG, "MiniHQ download failed for $fileName ($urlString): ${err.message}")
    }
  }

  suspend fun purgeScopeAndResetSyncState(
    context: Context,
    scope: SyncScope
  ) = withContext(Dispatchers.IO) {
    if (scope == SyncScope.ALL) {
      purgeAllAndResetSyncState(context)
      return@withContext
    }
    val label = when (scope) {
      SyncScope.SCHEDULES -> "Schedules"
      SyncScope.DSG -> "DSG"
      SyncScope.ANNOUNCEMENTS -> "Announcements"
      SyncScope.ALL -> "All"
    }
    syncStartedAtMs = 0L
    _syncProgress.value = SyncProgressState(
      isSyncing = true,
      activeScope = scope,
      percent = 5,
      stepMessage = "Purging cached $label data..."
    )
    val dao = ParishDatabase.getInstance(context).parishDao()
    val cacheDir = File(context.filesDir, "minihq_cache")
    when (scope) {
      SyncScope.SCHEDULES -> {
        dao.deleteMiniHqResourcesByCategory(CATEGORY_DISTRICT_SCHEDULE)
        File(context.filesDir, "schedules").listFiles()?.forEach { it.delete() }
        cacheDir.listFiles()?.filter { it.name.contains("Serving_Schedule", ignoreCase = true) }?.forEach { it.delete() }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_SCHEDULE_HASH).apply()
      }
      SyncScope.DSG -> {
        dao.deleteMiniHqResourcesByCategory(CATEGORY_DSG_ASSET)
        File(context.filesDir, "dsg").listFiles()?.forEach { it.delete() }
        cacheDir.listFiles()?.filter { !it.name.contains("Serving_Schedule", ignoreCase = true) }?.forEach { it.delete() }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_DSG_HASH).apply()
      }
      SyncScope.ANNOUNCEMENTS -> {
        _scrapedAnnouncements.value = emptyList()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
          .remove(KEY_SCRAPED_ANNOUNCEMENTS_JSON)
          .apply()
      }
      SyncScope.ALL -> Unit
    }
    syncStartedAtMs = 0L
    _syncProgress.value = SyncProgressState(
      isSyncing = false,
      activeScope = scope,
      percent = 0,
      stepMessage = "$label cache purged. Re-scraping fresh from MiniHQ..."
    )
  }

  suspend fun purgeAllAndResetSyncState(context: Context) = withContext(Dispatchers.IO) {
    _syncProgress.value = SyncProgressState(
      isSyncing = true,
      percent = 5,
      stepMessage = "Purging cached PDFs, DSG assets, and stale database records..."
    )
    ParishDatabase.getInstance(context).parishDao().deleteAllMiniHqResources()
    val cacheDir = File(context.filesDir, "minihq_cache").apply { mkdirs() }
    cacheDir.listFiles()?.forEach { it.delete() }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .clear()
      .apply()
    _syncProgress.value = SyncProgressState(
      isSyncing = false,
      percent = 10,
      stepMessage = "Cache purged. Ready for fresh MiniHQ scrape.",
      savedDistrictCount = 0,
      totalDistrictCount = 18
    )
  }

  fun updateSyncProgress(
    isSyncing: Boolean,
    percent: Int,
    stepMessage: String,
    requiresAuth: Boolean = false,
    hasError: Boolean = false,
    savedDistrictCount: Int = _syncProgress.value.savedDistrictCount,
    totalDistrictCount: Int = 18,
    diagnosticLog: String = ""
  ) {
    _syncProgress.value = SyncProgressState(
      isSyncing = isSyncing,
      percent = percent,
      stepMessage = stepMessage,
      requiresAuth = requiresAuth,
      hasError = hasError,
      savedDistrictCount = savedDistrictCount,
      totalDistrictCount = totalDistrictCount,
      diagnosticLog = diagnosticLog
    )
  }

  fun extractFileNameFromUrl(realUrl: String, fallbackName: String): String {
    val cleanPath = realUrl.substringBefore("?").substringBefore("#").trim()
    val rawName = cleanPath.substringAfterLast("/", "").trim()
    val decoded = runCatching { java.net.URLDecoder.decode(rawName, "UTF-8") }.getOrDefault(rawName)
    return if (decoded.contains(".")) decoded else fallbackName
  }

  /**
   * Saves a genuine binary asset fetched directly inside the authenticated WebView context
   * (`fetch(realUrl, { credentials: 'include' })`), enforcing strict magic-byte & size checks.
   */
  suspend fun saveWebViewScrapedBinary(
    context: Context,
    resourceId: String,
    realUrl: String,
    contentType: String,
    base64Payload: String
  ): Result<MiniHqResourceEntity> = withContext(Dispatchers.IO) {
    runCatching {
      val dao = ParishDatabase.getInstance(context).parishDao()
      val existing = dao.getMiniHqResourcesOnce().firstOrNull { it.id == resourceId }
        ?: discoverCatalogResources().firstOrNull { it.id == resourceId }
        ?: error("Unknown resource ID: $resourceId")

      // Preserve the canonical schema filename (e.g. Kitchener_November_2026_Serving_Schedule.pdf)
      val canonicalFileName = existing.fileName.ifBlank {
        extractFileNameFromUrl(realUrl, "District_Serving_Schedule.pdf")
      }
      val bytes = Base64.decode(base64Payload, Base64.DEFAULT)
      Log.i(TAG, "WebView fetched $canonicalFileName (from $realUrl): Content-Type=$contentType, bytes=${bytes.size}")
      require(isValidBinaryAsset(canonicalFileName, contentType, bytes)) {
        "Invalid binary response from $realUrl (Content-Type=$contentType, bytes=${bytes.size})"
      }

      val targetDir = storageDirForCategory(context, existing.resourceCategory)
      val targetFile = File(targetDir, canonicalFileName)
      targetFile.writeBytes(bytes)

      val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
      val hash = "live_" + digest.joinToString("") { "%02x".format(it) }
      val updated = existing.copy(
        fileName = canonicalFileName,
        sourceUrl = realUrl,
        localPath = targetFile.absolutePath,
        fileSize = bytes.size.toLong(),
        mimeType = mimeTypeFor(canonicalFileName),
        pdfBlob = null,
        checksum = hash,
        lastCheckedAt = System.currentTimeMillis()
      )
      dao.upsertMiniHqResources(listOf(updated))
      updated
    }
  }

  suspend fun installManualDistrictPdf(
    context: Context,
    resource: MiniHqResourceEntity,
    sourceUri: Uri
  ): Result<MiniHqResourceEntity> = withContext(Dispatchers.IO) {
    runCatching {
      val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
        ?: error("Unable to read the selected PDF file.")
      require(isValidBinaryAsset(resource.fileName, "application/pdf", bytes)) {
        "Invalid PDF file. Please choose a valid PDF (> 5 KB with %PDF- header)."
      }

      val targetDir = storageDirForCategory(context, resource.resourceCategory)
      val targetFile = File(targetDir, resource.fileName)
      targetFile.writeBytes(bytes)

      val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
      val hash = "manual_" + digest.joinToString("") { "%02x".format(it) }
      val updated = resource.copy(
        localPath = targetFile.absolutePath,
        fileSize = bytes.size.toLong(),
        mimeType = "application/pdf",
        pdfBlob = null,
        checksum = hash,
        lastCheckedAt = System.currentTimeMillis()
      )
      ParishDatabase.getInstance(context).parishDao().upsertMiniHqResources(listOf(updated))
      updated
    }
  }

  suspend fun saveResourceToDownloads(
    context: Context,
    resource: MiniHqResourceEntity
  ): Result<DownloadResult> = withContext(Dispatchers.IO) {
    runCatching {
      val cacheDir = File(context.filesDir, "minihq_cache").apply { mkdirs() }
      val cachedFile = File(resource.localPath).takeIf { resource.localPath.isNotBlank() }
        ?: File(cacheDir, resource.fileName)
      if (!isValidCachedFile(cachedFile)) {
        val blob = resource.pdfBlob
        if (blob != null && isValidBinaryAsset(resource.fileName, resource.mimeType, blob)) {
          cachedFile.writeBytes(blob)
        } else {
          require(resource.sourceUrl.isNotBlank()) {
            "File not synced from MiniHQ yet. Tap 'Sign in to MiniHQ' and 'Sync Live PDFs', or install the PDF manually."
          }
          val remoteBytes = fetchRemoteBinaryWithValidation(
            urlString = resource.sourceUrl,
            fileName = resource.fileName,
            sessionCookie = getSessionCookie(context)
          ).getOrThrow()
          cachedFile.writeBytes(remoteBytes)
        }
      }
      val mimeType = mimeTypeFor(resource.fileName)
      val uri = writeFileToDownloads(context, cachedFile, resource.fileName, mimeType)
      DownloadResult(fileName = resource.fileName, contentUri = uri, mimeType = mimeType)
    }
  }

  suspend fun createAndSaveMonthlyDsgZip(
    context: Context,
    monthYear: String,
    resources: List<MiniHqResourceEntity>
  ): Result<DownloadResult> = withContext(Dispatchers.IO) {
    runCatching {
      val monthAssets = resources.filter {
        it.resourceCategory == CATEGORY_DSG_ASSET && it.monthYear.equals(monthYear, ignoreCase = true)
      }
      require(monthAssets.isNotEmpty()) { "No DSG assets found for $monthYear." }

      val cacheDir = File(context.filesDir, "minihq_cache").apply { mkdirs() }
      val zipFileName = "NAC_DSG_${monthYear.replace(" ", "_")}.zip"
      val tempZip = File(cacheDir, zipFileName)
      val sessionCookie = getSessionCookie(context)

      var addedCount = 0
      ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
        val seenNames = mutableSetOf<String>()
        monthAssets.forEach { asset ->
          val localFile = File(asset.localPath).takeIf { asset.localPath.isNotBlank() }
            ?: File(cacheDir, asset.fileName)
          if (!isValidCachedFile(localFile)) {
            val remoteBytes = fetchRemoteBinaryWithValidation(
              urlString = asset.sourceUrl,
              fileName = asset.fileName,
              sessionCookie = sessionCookie
            ).getOrNull() ?: return@forEach
            localFile.writeBytes(remoteBytes)
          }
          val entryName = if (seenNames.add(asset.fileName)) asset.fileName else "${asset.id}_${asset.fileName}"
          zos.putNextEntry(ZipEntry(entryName))
          localFile.inputStream().use { it.copyTo(zos) }
          zos.closeEntry()
          addedCount++
        }
      }

      require(addedCount > 0) {
        "No authenticated binary DSG files are cached or downloadable for $monthYear yet."
      }
      val uri = writeFileToDownloads(context, tempZip, zipFileName, "application/zip")
      DownloadResult(fileName = zipFileName, contentUri = uri, mimeType = "application/zip")
    }
  }

  private fun mimeTypeFor(fileName: String): String = when {
    fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
    fileName.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
    fileName.endsWith(".zip", ignoreCase = true) -> "application/zip"
    else -> "application/octet-stream"
  }

  private fun writeFileToDownloads(
    context: Context,
    sourceFile: File,
    displayName: String,
    mimeType: String
  ): Uri? {
    require(sourceFile.exists() && sourceFile.length() > 0L) {
      "Download aborted: $displayName is empty or missing."
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val resolver = context.contentResolver
      val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, displayName)
        put(MediaStore.Downloads.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.IS_PENDING, 1)
      }
      val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: error("Unable to create download entry for $displayName")
      resolver.openOutputStream(uri)?.use { out ->
        sourceFile.inputStream().use { input -> input.copyTo(out) }
      }
      values.clear()
      values.put(MediaStore.Downloads.IS_PENDING, 0)
      resolver.update(uri, values, null, null)
      return uri
    } else {
      @Suppress("DEPRECATION")
      val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply {
        mkdirs()
      }
      val destFile = File(downloadsDir, displayName)
      sourceFile.inputStream().use { input ->
        FileOutputStream(destFile).use { out -> input.copyTo(out) }
      }
      return runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
      }.getOrElse { Uri.fromFile(destFile) }
    }
  }
}
