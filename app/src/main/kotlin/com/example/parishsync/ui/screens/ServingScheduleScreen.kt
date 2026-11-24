package com.example.parishsync.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.parishsync.R
import com.example.parishsync.data.model.AppRole
import com.example.parishsync.data.local.MiniHqAnnouncement
import com.example.parishsync.data.local.MiniHqResourceEntity
import com.example.parishsync.data.local.MiniHqScrapedData
import com.example.parishsync.data.local.MiniHqStagedService
import com.example.parishsync.data.local.ServingScheduleEntity
import com.example.parishsync.data.local.UserProfileEntity
import com.example.parishsync.data.model.Congregation
import com.example.parishsync.data.model.UserRole
import com.example.parishsync.workers.MiniHqScraperService
import com.example.parishsync.workers.ParishNotificationHelper
import com.example.parishsync.workers.PdfPageTextIndex
import com.example.parishsync.workers.PdfScheduleParser
import com.example.parishsync.workers.PdfTextSearchEngine
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// NAC Canada Brand Colors
private val NacNavy = Color(0xFF002D62)
private val NacNavyDark = Color(0xFF001F44)
private val NacGold = Color(0xFFC5A059)
private val NacGoldLight = Color(0xFFF9F5EB)

@Composable
fun ServingScheduleScreen(
  activeUser: UserProfileEntity,
  activeCongregation: Congregation,
  activeRole: UserRole,
  assignedDistrictName: String = "",
  miniHqResources: List<MiniHqResourceEntity> = emptyList(),
  onFeedback: (String) -> Unit = {},
  servingSchedules: List<ServingScheduleEntity>,
  onAddServingSchedule: (
    serviceTitle: String,
    serviceDate: String,
    serviceTime: String,
    officiantMinister: String,
    officiantRank: String,
    assistingMinisters: String,
    choirConductor: String,
    organist: String,
    serviceTheme: String,
    bibleWord: String,
    specialServiceTag: String,
    location: String,
    notes: String
  ) -> Unit,
  onDeleteServingSchedule: (String) -> Unit,
  onLoadSampleSchedule: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var showMiniHqLoginDialog by remember { mutableStateOf(false) }
  var replaceConfirmScope by remember { mutableStateOf<MiniHqScraperService.SyncScope?>(null) }
  var selectedDistrictPdf by remember { mutableStateOf<MiniHqResourceEntity?>(null) }
  var selectedDsgTextResource by remember { mutableStateOf<MiniHqResourceEntity?>(null) }
  var selectedDsgAudioResource by remember { mutableStateOf<MiniHqResourceEntity?>(null) }
  var selectedAnnouncement by remember { mutableStateOf<MiniHqAnnouncement?>(null) }
  var currentSubTab by remember { mutableStateOf(0) } // 0: Schedules, 1: DSG, 2: Announcements
  var selectedMonthYear by remember { mutableStateOf("November 2026") } // "November 2026" (Current) or "December 2026" (Next)
  var announcementCategoryFilter by remember { mutableStateOf("All") } // "All", "Funeral Notice", "Serving Schedules", "Divine Service Prep"

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val syncProgress by MiniHqScraperService.syncProgress.collectAsState()
  var isZipping by remember { mutableStateOf(false) }
  var syncOutcome by remember { mutableStateOf<MiniHqScraperService.SyncOutcome?>(null) }

  val roles = activeUser.effectiveRoles
  val isAdmin = activeRole == UserRole.ADMINISTRATOR || AppRole.ADMIN in roles
  val canManage = isAdmin ||
    activeRole == UserRole.MINISTER ||
    AppRole.MINISTER in roles ||
    AppRole.VOLUNTEER_SCHEDULER in roles

  LaunchedEffect(isAdmin) {
    val last = withContext(Dispatchers.IO) {
      if (isAdmin) MiniHqScraperService.getLastSyncOutcome(context) else null
    }
    if (last != null) {
      syncOutcome = last
    }
  }
  val allAnnouncements by MiniHqScraperService.scrapedAnnouncements.collectAsState()
  val effectiveResources = remember(miniHqResources) {
    val hasDistrictRows = miniHqResources.any { it.resourceCategory == MiniHqScraperService.CATEGORY_DISTRICT_SCHEDULE }
    if (hasDistrictRows) {
      miniHqResources
    } else {
      MiniHqScraperService.discoverDistrictPlaceholdersOnly() + miniHqResources
    }
  }

  // Filtered announcements by Category
  val filteredAnnouncements = remember(allAnnouncements, announcementCategoryFilter) {
    if (announcementCategoryFilter == "All") allAnnouncements
    else allAnnouncements.filter { it.category.equals(announcementCategoryFilter, ignoreCase = true) }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      // NAC Canada Inspired Navy Header Banner
      Surface(
        color = NacNavy,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_nac_emblem),
                contentDescription = null,
                tint = NacGold,
                modifier = Modifier.size(28.dp)
              )
              Column {
                Text(
                  text = "Minister Serving Schedules",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = if (activeCongregation.displayName.isNotBlank()) activeCongregation.displayName else "NAC Canada • MiniHQ Portal",
                  style = MaterialTheme.typography.bodySmall,
                  color = NacGold
                )
              }
            }

            if (canManage) {
              Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = NacGold,
                  contentColor = NacNavyDark
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                  text = "Schedule Service",
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }

          Text(
            text = "District Serving Schedule PDFs, Divine Service Prep (DSG) resources, and MiniHQ announcements.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
          )
        }
      }

      // Three-Tab Navigation: 1. Schedules, 2. DSG, 3. Announcements
      TabRow(
        selectedTabIndex = currentSubTab,
        containerColor = MaterialTheme.colorScheme.surface
      ) {
        Tab(
          selected = currentSubTab == 0,
          onClick = { currentSubTab = 0 },
          text = { Text("Schedules") }
        )
        Tab(
          selected = currentSubTab == 1,
          onClick = { currentSubTab = 1 },
          text = { Text("DSG") }
        )
        Tab(
          selected = currentSubTab == 2,
          onClick = { currentSubTab = 2 },
          text = { Text("Announcements (${allAnnouncements.size})") }
        )
      }

      when (currentSubTab) {
        0 -> {
          // ==========================================
          // TAB 0: Schedules (District Serving Schedule PDFs + Sync)
          // ==========================================
          val districtSchedulesForMonth = remember(effectiveResources, selectedMonthYear, assignedDistrictName) {
            val monthItems = effectiveResources.filter {
              it.resourceCategory == MiniHqScraperService.CATEGORY_DISTRICT_SCHEDULE &&
                it.monthYear.equals(selectedMonthYear, ignoreCase = true)
            }
            val normalizedAssigned = assignedDistrictName
              .replace(" District", "", ignoreCase = true)
              .trim()
            monthItems.sortedWith(
              compareByDescending<MiniHqResourceEntity> {
                normalizedAssigned.isNotBlank() && it.district.equals(normalizedAssigned, ignoreCase = true)
              }.thenByDescending { res ->
                (res.pdfBlob != null && res.pdfBlob.size >= MiniHqScraperService.MIN_PDF_BYTES) ||
                  (res.fileSize >= MiniHqScraperService.MIN_PDF_BYTES && res.localPath.isNotBlank())
              }.thenBy { it.district.lowercase(Locale.ROOT) }
            )
          }

          var expandedDistrict by remember(assignedDistrictName, selectedMonthYear) {
            val normalizedAssigned = assignedDistrictName
              .replace(" District", "", ignoreCase = true)
              .trim()
            mutableStateOf(normalizedAssigned.ifBlank { districtSchedulesForMonth.firstOrNull()?.district.orEmpty() })
          }

          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Tab-Scoped Schedules Sync Panel (Admins only)
            if (isAdmin) {
              item {
                AdminModuleSyncCard(
                  title = "Schedules Sync (MiniHQ)",
                  subtitle = "Current & Next Month District Serving Schedule PDFs",
                  syncButtonLabel = "Sync Schedules",
                  replaceButtonLabel = "Scrape & Replace Schedules",
                  scope = MiniHqScraperService.SyncScope.SCHEDULES,
                  syncProgress = syncProgress,
                  syncOutcome = syncOutcome,
                  onSyncNow = {
                    coroutineScope.launch {
                      val outcome = MiniHqScraperService.performSync(
                        context = context,
                        forceRefresh = true,
                        scope = MiniHqScraperService.SyncScope.SCHEDULES
                      )
                      syncOutcome = outcome
                      onFeedback(outcome.message)
                    }
                  },
                  onScrapeAndReplace = {
                    replaceConfirmScope = MiniHqScraperService.SyncScope.SCHEDULES
                  }
                )
              }
            }

            // Month Selector (Current Month / Next Month)
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                FilterChip(
                  selected = selectedMonthYear == "November 2026",
                  onClick = { selectedMonthYear = "November 2026" },
                  label = { Text("Current Month (Nov 2026)") }
                )
                FilterChip(
                  selected = selectedMonthYear == "December 2026",
                  onClick = { selectedMonthYear = "December 2026" },
                  label = { Text("Next Month (Dec 2026)") }
                )
              }
            }

            // District Serving Schedule PDFs
            items(districtSchedulesForMonth, key = { it.id }) { resource ->
              val normalizedAssigned = assignedDistrictName
                .replace(" District", "", ignoreCase = true)
                .trim()
              val isAssignedDistrict = normalizedAssigned.isNotBlank() &&
                resource.district.equals(normalizedAssigned, ignoreCase = true)
              val isExpanded = expandedDistrict.equals(resource.district, ignoreCase = true)

              DistrictSchedulePdfCard(
                resource = resource,
                isAssignedDistrict = isAssignedDistrict,
                isExpanded = isExpanded,
                onToggleExpand = {
                  selectedDistrictPdf = resource
                },
                onDownloadPdf = {
                  selectedDistrictPdf = resource
                }
              )
            }

            if (servingSchedules.isNotEmpty()) {
              item {
                Text(
                  text = "Congregation Scheduled Services (${servingSchedules.size})",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(top = 8.dp)
                )
              }
              items(servingSchedules, key = { it.id }) { service ->
                NacDivineServiceCard(
                  service = service,
                  canManage = canManage,
                  onDelete = { onDeleteServingSchedule(service.id) }
                )
              }
            }

            item { Spacer(Modifier.height(40.dp)) }
          }
        }

        1 -> {
          // ==========================================
          // TAB 1: DSG (Divine Service Prep Resources)
          // ==========================================
          val today = remember { LocalDate.now() }
          val dsgStoredAssets = remember(effectiveResources, selectedMonthYear) {
            effectiveResources.filter { asset ->
              asset.resourceCategory == MiniHqScraperService.CATEGORY_DSG_ASSET &&
                asset.monthYear.equals(selectedMonthYear, ignoreCase = true) &&
                ((asset.pdfBlob != null && asset.pdfBlob.isNotEmpty()) ||
                  (asset.localPath.isNotBlank() && asset.fileSize > 0L))
            }
          }
          val dsgGroupedByService = remember(dsgStoredAssets) {
            dsgStoredAssets
              .groupBy { "${it.serviceDate}|${it.serviceTitle}" }
              .entries
              .toList()
          }
          val upcomingServices = remember(dsgGroupedByService, today) {
            dsgGroupedByService
              .mapNotNull { entry ->
                val date = MiniHqScraperService.parseServiceDateToLocalDate(entry.value.first().serviceDate)
                if (date != null && !date.isBefore(today)) date to entry else null
              }
              .sortedBy { it.first }
              .map { it.second }
          }
          val specialUndatedServices = remember(dsgGroupedByService) {
            dsgGroupedByService.filter { entry ->
              val rawDate = entry.value.first().serviceDate
              rawDate.isBlank() || MiniHqScraperService.parseServiceDateToLocalDate(rawDate) == null
            }
          }
          val pastServices = remember(dsgGroupedByService, today) {
            dsgGroupedByService
              .mapNotNull { entry ->
                val date = MiniHqScraperService.parseServiceDateToLocalDate(entry.value.first().serviceDate)
                if (date != null && date.isBefore(today)) date to entry else null
              }
              .sortedByDescending { it.first }
              .map { it.second }
          }

          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Tab-Scoped DSG Sync Panel (Admins only)
            if (isAdmin) {
              item {
                AdminModuleSyncCard(
                  title = "DSG Sync (MiniHQ)",
                  subtitle = "Current & Next Month Divine Service Prep + Youth Service",
                  syncButtonLabel = "Sync DSG",
                  replaceButtonLabel = "Scrape & Replace DSG",
                  scope = MiniHqScraperService.SyncScope.DSG,
                  syncProgress = syncProgress,
                  syncOutcome = syncOutcome,
                  onSyncNow = {
                    coroutineScope.launch {
                      val outcome = MiniHqScraperService.performSync(
                        context = context,
                        forceRefresh = true,
                        scope = MiniHqScraperService.SyncScope.DSG
                      )
                      syncOutcome = outcome
                      onFeedback(outcome.message)
                    }
                  },
                  onScrapeAndReplace = {
                    replaceConfirmScope = MiniHqScraperService.SyncScope.DSG
                  }
                )
              }
            }

            // Month Selector + Download All (Month ZIP)
            item {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  FilterChip(
                    selected = selectedMonthYear == "November 2026",
                    onClick = { selectedMonthYear = "November 2026" },
                    label = { Text("Current Month (Nov 2026)") }
                  )
                  FilterChip(
                    selected = selectedMonthYear == "December 2026",
                    onClick = { selectedMonthYear = "December 2026" },
                    label = { Text("Next Month (Dec 2026)") }
                  )
                }

                Button(
                  onClick = {
                    if (!isZipping) {
                      coroutineScope.launch {
                        isZipping = true
                        val zipFileName = "NAC_DSG_${selectedMonthYear.replace(" ", "_")}.zip"
                        val notifId = kotlin.math.abs(zipFileName.hashCode()).coerceAtLeast(1000)
                        ParishNotificationHelper.showDownloadStartedNotification(context, notifId, zipFileName)
                        MiniHqScraperService.createAndSaveMonthlyDsgZip(
                          context = context,
                          monthYear = selectedMonthYear,
                          resources = effectiveResources
                        )
                          .onSuccess { dl ->
                            val posted = ParishNotificationHelper.showDownloadCompleteNotification(
                              context = context,
                              notificationId = notifId,
                              fileName = dl.fileName,
                              contentUri = dl.contentUri,
                              mimeType = dl.mimeType
                            )
                            if (!posted) {
                              onFeedback("Download Ready: ${dl.fileName} (Saved to Downloads)")
                            }
                          }
                          .onFailure { err ->
                            ParishNotificationHelper.cancelDownloadNotification(context, notifId)
                            onFeedback(err.message ?: "Unable to create monthly ZIP.")
                          }
                        isZipping = false
                      }
                    }
                  },
                  enabled = !isZipping && dsgGroupedByService.isNotEmpty(),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = NacNavy,
                    contentColor = Color.White
                  ),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  if (isZipping) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(16.dp),
                      strokeWidth = 2.dp,
                      color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Packaging Monthly ZIP...")
                  } else {
                    Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download All ($selectedMonthYear ZIP)", fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }

            val openDsgAsset: (MiniHqResourceEntity) -> Unit = { asset ->
              when {
                asset.assetType == MiniHqScraperService.ASSET_AUDIO ||
                  asset.fileName.endsWith(".mp3", ignoreCase = true) -> {
                  selectedDsgAudioResource = asset
                }
                asset.fileName.endsWith(".pdf", ignoreCase = true) -> {
                  selectedDistrictPdf = asset
                }
                asset.contentText.isNotBlank() -> {
                  selectedDsgTextResource = asset
                }
                else -> {
                  selectedDistrictPdf = asset
                }
              }
            }
            val downloadDsgAsset: (MiniHqResourceEntity) -> Unit = { asset ->
              coroutineScope.launch {
                val notifId = kotlin.math.abs(asset.id.hashCode()).coerceAtLeast(1000)
                ParishNotificationHelper.showDownloadStartedNotification(context, notifId, asset.fileName)
                MiniHqScraperService.saveResourceToDownloads(context, asset)
                  .onSuccess { dl ->
                    val posted = ParishNotificationHelper.showDownloadCompleteNotification(
                      context = context,
                      notificationId = notifId,
                      fileName = dl.fileName,
                      contentUri = dl.contentUri,
                      mimeType = dl.mimeType
                    )
                    if (!posted) {
                      onFeedback("Download Ready: ${dl.fileName} (Saved to Downloads)")
                    }
                  }
                  .onFailure { err ->
                    ParishNotificationHelper.cancelDownloadNotification(context, notifId)
                    onFeedback(err.message ?: "Failed to download ${asset.fileName}.")
                  }
              }
            }

            if (dsgGroupedByService.isEmpty()) {
              item {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "No DSG files synced for $selectedMonthYear yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                  )
                }
              }
            }

            if (upcomingServices.isNotEmpty()) {
              item {
                Text(
                  text = "UPCOMING SERVICES",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = NacNavy,
                  modifier = Modifier.padding(top = 4.dp)
                )
              }
              items(upcomingServices, key = { "up_${it.key}" }) { (_, assets) ->
                val first = assets.first()
                DsgServiceResourcesCard(
                  serviceDate = first.serviceDate,
                  serviceTitle = first.serviceTitle,
                  assets = assets,
                  onOpenAsset = openDsgAsset,
                  onDownloadAsset = downloadDsgAsset
                )
              }
            }

            if (specialUndatedServices.isNotEmpty()) {
              item {
                Text(
                  text = "SPECIAL",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = NacNavy,
                  modifier = Modifier.padding(top = 6.dp)
                )
              }
              items(specialUndatedServices, key = { "spec_${it.key}" }) { (_, assets) ->
                val first = assets.first()
                DsgServiceResourcesCard(
                  serviceDate = first.serviceDate,
                  serviceTitle = first.serviceTitle,
                  assets = assets,
                  onOpenAsset = openDsgAsset,
                  onDownloadAsset = downloadDsgAsset
                )
              }
            }

            if (pastServices.isNotEmpty()) {
              item {
                Text(
                  text = "PAST SERVICES",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 6.dp)
                )
              }
              items(pastServices, key = { "past_${it.key}" }) { (_, assets) ->
                val first = assets.first()
                DsgServiceResourcesCard(
                  serviceDate = first.serviceDate,
                  serviceTitle = first.serviceTitle,
                  assets = assets,
                  onOpenAsset = openDsgAsset,
                  onDownloadAsset = downloadDsgAsset
                )
              }
            }

            item { Spacer(Modifier.height(40.dp)) }
          }
        }

        2 -> {
          // ==========================================
          // TAB 2: Announcements & Notices (Separate Area)
          // ==========================================
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp, vertical = 8.dp)
          ) {
            // Category Filter Chips
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              FilterChip(
                selected = announcementCategoryFilter == "All",
                onClick = { announcementCategoryFilter = "All" },
                label = { Text("All (${allAnnouncements.size})", style = MaterialTheme.typography.labelSmall) }
              )
              FilterChip(
                selected = announcementCategoryFilter == "Funeral Notice",
                onClick = { announcementCategoryFilter = "Funeral Notice" },
                label = { Text("Funerals", style = MaterialTheme.typography.labelSmall) }
              )
              FilterChip(
                selected = announcementCategoryFilter == "Serving Schedules",
                onClick = { announcementCategoryFilter = "Serving Schedules" },
                label = { Text("Schedules", style = MaterialTheme.typography.labelSmall) }
              )
              FilterChip(
                selected = announcementCategoryFilter == "Divine Service Prep",
                onClick = { announcementCategoryFilter = "Divine Service Prep" },
                label = { Text("Prep/DSG", style = MaterialTheme.typography.labelSmall) }
              )
            }

            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              if (isAdmin) {
                item {
                  AdminModuleSyncCard(
                    title = "Announcements Sync (MiniHQ)",
                    subtitle = "MiniHQ Funeral Notices, Schedule & DSG Announcements",
                    syncButtonLabel = "Sync Announcements",
                    replaceButtonLabel = "Scrape & Replace Announcements",
                    scope = MiniHqScraperService.SyncScope.ANNOUNCEMENTS,
                    syncProgress = syncProgress,
                    syncOutcome = syncOutcome,
                    onSyncNow = {
                      coroutineScope.launch {
                        val outcome = MiniHqScraperService.performSync(
                          context = context,
                          forceRefresh = true,
                          scope = MiniHqScraperService.SyncScope.ANNOUNCEMENTS
                        )
                        syncOutcome = outcome
                        onFeedback(outcome.message)
                      }
                    },
                    onScrapeAndReplace = {
                      replaceConfirmScope = MiniHqScraperService.SyncScope.ANNOUNCEMENTS
                    }
                  )
                }
              }

              items(filteredAnnouncements, key = { it.id }) { ann ->
                MiniHqAnnouncementCard(
                  announcement = ann,
                  onClick = { selectedAnnouncement = ann }
                )
              }

              item { Spacer(Modifier.height(40.dp)) }
            }
          }
        }
      }
    }

    // --- Announcement "More Details" Modal ---
    selectedAnnouncement?.let { ann ->
      AnnouncementDetailsDialog(
        announcement = ann,
        onDismiss = { selectedAnnouncement = null },
        onOpenAttachedPdf = { pdfUrl, pdfName ->
          val syntheticResource = MiniHqResourceEntity(
            id = "ann_pdf_${ann.id}",
            resourceCategory = MiniHqScraperService.CATEGORY_DSG_ASSET,
            monthYear = ann.date,
            isNextMonth = false,
            district = "",
            serviceDate = ann.date,
            serviceTitle = ann.title,
            assetType = "Announcement PDF",
            fileName = pdfName.ifBlank { "Announcement.pdf" },
            sourceUrl = pdfUrl,
            localPath = "",
            fileSize = 0L,
            mimeType = "application/pdf"
          )
          selectedAnnouncement = null
          selectedDistrictPdf = syntheticResource
        }
      )
    }

    // --- DSG Text / References / Transcript Reader Dialog ---
  selectedDsgTextResource?.let { asset ->
    DsgTextReaderDialog(
      resource = asset,
      onDismiss = { selectedDsgTextResource = null },
      onOpenPdfOrInstall = {
        selectedDsgTextResource = null
        selectedDistrictPdf = asset
      }
    )
  }

  // --- DSG Audio Player Dialog ---
  selectedDsgAudioResource?.let { audioAsset ->
    DsgAudioPlayerDialog(
      resource = audioAsset,
      onDismiss = { selectedDsgAudioResource = null },
      onDownloadAudio = {
        coroutineScope.launch {
          val notifId = kotlin.math.abs(audioAsset.id.hashCode()).coerceAtLeast(1000)
          ParishNotificationHelper.showDownloadStartedNotification(context, notifId, audioAsset.fileName)
          MiniHqScraperService.saveResourceToDownloads(context, audioAsset)
            .onSuccess { dl ->
              val posted = ParishNotificationHelper.showDownloadCompleteNotification(
                context = context,
                notificationId = notifId,
                fileName = dl.fileName,
                contentUri = dl.contentUri,
                mimeType = dl.mimeType
              )
              if (!posted) {
                onFeedback("Download Ready: ${dl.fileName} (Saved to Downloads)")
              }
            }
            .onFailure { err ->
              ParishNotificationHelper.cancelDownloadNotification(context, notifId)
              onFeedback(err.message ?: "Failed to download ${audioAsset.fileName}.")
            }
        }
      }
    )
  }

  // --- Scrape & Replace Confirmation Dialog (Admins only, scoped per module) ---
  val pendingReplaceScope = replaceConfirmScope
  if (isAdmin && pendingReplaceScope != null) {
    val scopeTitle = when (pendingReplaceScope) {
      MiniHqScraperService.SyncScope.SCHEDULES -> "Schedules"
      MiniHqScraperService.SyncScope.DSG -> "DSG"
      MiniHqScraperService.SyncScope.ANNOUNCEMENTS -> "Announcements"
      MiniHqScraperService.SyncScope.ALL -> "All MiniHQ"
    }
    InWindowModalOverlay(onDismiss = { replaceConfirmScope = null }) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(0.92f)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Scrape & Replace $scopeTitle",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Purge cached $scopeTitle data and re-scrape fresh from MiniHQ? Other modules will remain untouched.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { replaceConfirmScope = null }) {
              Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            Button(
              onClick = {
                replaceConfirmScope = null
                coroutineScope.launch {
                  MiniHqScraperService.purgeScopeAndResetSyncState(context, pendingReplaceScope)
                  val outcome = MiniHqScraperService.performSync(
                    context = context,
                    forceRefresh = true,
                    scope = pendingReplaceScope
                  )
                  syncOutcome = outcome
                  onFeedback(outcome.message)
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
              Text("Purge & Re-Scrape")
            }
          }
        }
      }
    }
  }

  // --- In-App MiniHQ WebView Login Dialog (Admins only) ---
  if (isAdmin && showMiniHqLoginDialog) {
    MiniHqWebLoginDialog(
      onDismiss = { showMiniHqLoginDialog = false },
      onAuthenticatedAndSync = {
        showMiniHqLoginDialog = false
        MiniHqScraperService.persistWebViewCookies(context)
        coroutineScope.launch {
          val outcome = MiniHqScraperService.performSync(context, forceRefresh = false)
          syncOutcome = outcome
          onFeedback(outcome.message)
        }
      }
    )
  }

  // --- District Schedule PDF Viewer & Manual Install Dialog ---
  selectedDistrictPdf?.let { currentResource ->
    val latestResource = effectiveResources.firstOrNull { it.id == currentResource.id } ?: currentResource
    DistrictPdfViewerDialog(
      resource = latestResource,
      canInstallPdf = isAdmin,
      onDismiss = { selectedDistrictPdf = null },
      onDownloadPdf = {
        coroutineScope.launch {
          val notifId = kotlin.math.abs(latestResource.id.hashCode()).coerceAtLeast(1000)
          ParishNotificationHelper.showDownloadStartedNotification(context, notifId, latestResource.fileName)
          MiniHqScraperService.saveResourceToDownloads(context, latestResource)
            .onSuccess { dl ->
              val posted = ParishNotificationHelper.showDownloadCompleteNotification(
                context = context,
                notificationId = notifId,
                fileName = dl.fileName,
                contentUri = dl.contentUri,
                mimeType = dl.mimeType
              )
              if (!posted) {
                onFeedback("Download Ready: ${dl.fileName} (Saved to Downloads)")
              }
            }
            .onFailure { err ->
              ParishNotificationHelper.cancelDownloadNotification(context, notifId)
              onFeedback(err.message ?: "Failed to download ${latestResource.fileName}.")
            }
        }
      },
      onInstallManualPdf = { uri ->
        coroutineScope.launch {
          MiniHqScraperService.installManualDistrictPdf(context, latestResource, uri)
            .onSuccess { updated ->
              selectedDistrictPdf = updated
              onFeedback("Installed ${updated.fileName} on portal for ministers.")
            }
            .onFailure { err ->
              onFeedback(err.message ?: "Failed to install PDF.")
            }
        }
      }
    )
  }

  // --- Add Divine Service Dialog ---
  if (showAddDialog) {
    AddDivineServiceDialog(
      onDismiss = { showAddDialog = false },
      onSave = { title, date, time, off, offRank, assist, choir, organ, theme, bible, tag, loc, notes ->
        onAddServingSchedule(title, date, time, off, offRank, assist, choir, organ, theme, bible, tag, loc, notes)
        showAddDialog = false
      }
    )
  }
  }
}

@Composable
private fun InWindowModalOverlay(
  onDismiss: () -> Unit,
  content: @Composable () -> Unit
) {
  androidx.activity.compose.BackHandler(onBack = onDismiss)
  Box(
    modifier = Modifier
      .fillMaxSize()
      .zIndex(40f)
      .background(Color.Black.copy(alpha = 0.56f))
      .clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onDismiss
      )
      .padding(horizontal = 12.dp, vertical = 16.dp),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier.clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = {}
      )
    ) {
      content()
    }
  }
}

@Composable
private fun StagedMiniHqServiceCard(
  staged: MiniHqStagedService,
  isAlreadyIntegrated: Boolean,
  onMoveToIntegrated: () -> Unit
) {
  val context = LocalContext.current

  OutlinedCard(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.outlinedCardColors(
      containerColor = if (isAlreadyIntegrated) Color(0xFFF1F8E9) else Color.White
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Header Ribbon: Month-Year + Date/Time in Navy
      Surface(
        color = NacNavy,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              color = NacGold,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = staged.monthYear.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NacNavyDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Text(
              text = "${staged.serviceDate} • ${staged.serviceTime}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          Text(
            text = "${staged.congregation} (${staged.district.take(8)})",
            style = MaterialTheme.typography.labelSmall,
            color = NacGold
          )
        }
      }

      // Card Content
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = staged.serviceTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NacNavy,
            modifier = Modifier.weight(1f)
          )

          if (staged.specialServiceTag.isNotBlank()) {
            Surface(
              color = NacGoldLight,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = staged.specialServiceTag,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NacNavy,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }

        // Theme and Scripture Word Box
        if (staged.bibleWord.isNotBlank() || staged.theme.isNotBlank()) {
          Surface(
            color = NacGoldLight.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              if (staged.theme.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NacGold, modifier = Modifier.size(14.dp))
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = "Theme: ${staged.theme}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = NacNavyDark
                  )
                }
              }

              if (staged.bibleWord.isNotBlank()) {
                Text(
                  text = staged.bibleWord,
                  style = MaterialTheme.typography.bodySmall,
                  fontStyle = FontStyle.Italic,
                  color = NacNavyDark.copy(alpha = 0.9f)
                )
              }
            }
          }
        }

        // Minister details
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Person, contentDescription = null, tint = NacNavy, modifier = Modifier.size(16.dp))
          Text(
            text = "Officiant: ${staged.officiantMinister} (${staged.officiantRank})",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = NacNavy
          )
          if (staged.assistingMinisters.isNotBlank()) {
            Text(
              text = "• Assisting: ${staged.assistingMinisters}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (staged.notes.isNotBlank()) {
          Text(
            text = "Notes: ${staged.notes}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
          )
        }

        HorizontalDivider(color = Color(0xFFEEEEEE))

        // Action Row: Move to Integrated Schedule Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          if (staged.dsgPdfLink.isNotBlank()) {
            TextButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(staged.dsgPdfLink))
                context.startActivity(intent)
              }
            ) {
              Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Open DSG PDF", style = MaterialTheme.typography.labelSmall)
            }
          } else {
            Spacer(Modifier.width(1.dp))
          }

          if (isAlreadyIntegrated) {
            Surface(
              color = Color(0xFFE8F5E9),
              shape = RoundedCornerShape(8.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                Text(
                  text = "In Integrated System",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF2E7D32)
                )
              }
            }
          } else {
            Button(
              onClick = onMoveToIntegrated,
              colors = ButtonDefaults.buttonColors(
                containerColor = NacNavy,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.MoveToInbox, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Move to Integrated Schedule", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MiniHqAnnouncementCard(
  announcement: MiniHqAnnouncement,
  onClick: () -> Unit
) {
  val isFuneral = announcement.category.equals("Funeral Notice", ignoreCase = true)
  val isPrep = announcement.category.equals("Divine Service Prep", ignoreCase = true)

  Card(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isFuneral) Color(0xFFFFF3E0) else if (isPrep) Color(0xFFF3E5F5) else Color(0xFFE3F2FD)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Surface(
          color = if (isFuneral) Color(0xFFE65100) else if (isPrep) Color(0xFF6A1B9A) else NacNavy,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = announcement.category.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        Text(
          text = announcement.date,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Text(
        text = announcement.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      Surface(
        color = Color.White.copy(alpha = 0.78f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = announcement.summary.ifBlank { announcement.content.ifBlank { announcement.title } },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(10.dp)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "View Details",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = if (isFuneral) Color(0xFFE65100) else if (isPrep) Color(0xFF6A1B9A) else NacNavy
        )
        Spacer(Modifier.width(4.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "View Details",
          tint = if (isFuneral) Color(0xFFE65100) else if (isPrep) Color(0xFF6A1B9A) else NacNavy,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

@Composable
private fun AnnouncementDetailsDialog(
  announcement: MiniHqAnnouncement,
  onDismiss: () -> Unit,
  onOpenAttachedPdf: (String, String) -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var urlCopied by remember(announcement.id, announcement.sourceUrl) { mutableStateOf(false) }
  LaunchedEffect(urlCopied) {
    if (urlCopied) {
      delay(2500)
      urlCopied = false
    }
  }

  val isFuneral = announcement.category.equals("Funeral Notice", ignoreCase = true)
  val isPrep = announcement.category.equals("Divine Service Prep", ignoreCase = true)
  val badgeColor = if (isFuneral) Color(0xFFE65100) else if (isPrep) Color(0xFF6A1B9A) else NacNavy

  InWindowModalOverlay(onDismiss = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 12.dp,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Header: Category Badge, Title, Published Date, Close
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                color = badgeColor,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = announcement.category.uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
              Text(
                text = announcement.date,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = announcement.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
          TextButton(onClick = onDismiss) {
            Text("Close")
          }
        }

        HorizontalDivider()

        // Body: Full formatted MiniHQ announcement content
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .height(340.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (announcement.bodyHtml.isNotBlank()) {
            AndroidView(
              factory = { ctx ->
                android.widget.TextView(ctx).apply {
                  textSize = 14.5f
                  setLineSpacing(6f, 1.15f)
                  setTextColor(android.graphics.Color.parseColor("#1E293B"))
                  setLinkTextColor(android.graphics.Color.parseColor("#0B3B60"))
                  movementMethod = android.text.method.LinkMovementMethod.getInstance()
                }
              },
              update = { tv ->
                tv.text = androidx.core.text.HtmlCompat.fromHtml(
                  announcement.bodyHtml,
                  androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
                )
              },
              modifier = Modifier.fillMaxWidth()
            )
          } else {
            Text(
              text = announcement.content.ifBlank { announcement.summary.ifBlank { announcement.title } },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          if (announcement.attachedPdfUrl.isNotBlank()) {
            Button(
              onClick = {
                onOpenAttachedPdf(announcement.attachedPdfUrl, announcement.attachedPdfName)
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = NacNavy,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(
                text = "Preview / Download PDF (${announcement.attachedPdfName.ifBlank { "Document" }})",
                maxLines = 1
              )
            }
          }
        }

        HorizontalDivider()

        // Footer: Full untruncated source_url + Copy URL + Close
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Source URL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = announcement.sourceUrl.ifBlank { "MiniHQ Portal" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
              )
            }
            OutlinedButton(
              onClick = {
                if (announcement.sourceUrl.isNotBlank()) {
                  clipboardManager.setText(AnnotatedString(announcement.sourceUrl))
                  urlCopied = true
                }
              },
              enabled = announcement.sourceUrl.isNotBlank(),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(
                imageVector = if (urlCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
              Spacer(Modifier.width(4.dp))
              Text(
                text = if (urlCopied) "Copied!" else "Copy URL",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          if (announcement.sourceUrl.isNotBlank()) {
            TextButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(announcement.sourceUrl))
                context.startActivity(intent)
              }
            ) {
              Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Open in Browser")
            }
            Spacer(Modifier.width(8.dp))
          }
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = NacNavy, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Close")
          }
        }
      }
    }
  }
}

@Composable
private fun NacDivineServiceCard(
  service: ServingScheduleEntity,
  canManage: Boolean,
  onDelete: () -> Unit
) {
  OutlinedCard(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.outlinedCardColors(
      containerColor = Color.White
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Header Ribbon: Date & Time in Navy Bar
      Surface(
        color = NacNavy,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DateRange,
              contentDescription = null,
              tint = NacGold,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "${service.serviceDate} • ${service.serviceTime}".uppercase(),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              letterSpacing = 0.5.sp
            )
          }

          if (canManage) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete service",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      // Card Body
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = service.serviceTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NacNavy
          )

          if (service.specialServiceTag.isNotBlank()) {
            Surface(
              color = NacGoldLight,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = service.specialServiceTag,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NacNavy,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }

        // Bible Word & Theme Box
        if (service.bibleWord.isNotBlank() || service.serviceTheme.isNotBlank()) {
          Surface(
            color = NacGoldLight.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              if (service.serviceTheme.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NacGold,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = "Theme: ${service.serviceTheme}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = NacNavyDark
                  )
                }
              }

              if (service.bibleWord.isNotBlank()) {
                Text(
                  text = service.bibleWord,
                  style = MaterialTheme.typography.bodySmall,
                  fontStyle = FontStyle.Italic,
                  color = NacNavyDark.copy(alpha = 0.9f)
                )
              }
            }
          }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE))

        // Ministers Section
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "MINISTRY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NacNavy.copy(alpha = 0.6f),
            letterSpacing = 1.sp
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = NacNavy, modifier = Modifier.size(16.dp))
            Text(
              text = "Officiant: ",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "${service.officiantMinister} (${service.officiantRank})",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = NacNavy
            )
          }

          if (service.assistingMinisters.isNotBlank()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Spacer(Modifier.width(16.dp))
              Text(
                text = "Assisting: ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = service.assistingMinisters,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Choir & Music Section
        if (service.choirConductor.isNotBlank() || service.organist.isNotBlank()) {
          HorizontalDivider(color = Color(0xFFEEEEEE))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "MUSIC & CHOIR",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = NacNavy.copy(alpha = 0.6f),
              letterSpacing = 1.sp
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              if (service.choirConductor.isNotBlank()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = NacNavy, modifier = Modifier.size(14.dp))
                  Text(
                    text = "Choir: ${service.choirConductor}",
                    style = MaterialTheme.typography.bodySmall
                  )
                }
              }

              if (service.organist.isNotBlank()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(Icons.Default.MusicNote, contentDescription = null, tint = NacNavy, modifier = Modifier.size(14.dp))
                  Text(
                    text = "Organist: ${service.organist}",
                    style = MaterialTheme.typography.bodySmall
                  )
                }
              }
            }
          }
        }

        // Location & Notes
        if (service.location.isNotBlank() || service.notes.isNotBlank()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (service.location.isNotBlank()) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Text(
                  text = service.location,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.Gray
                )
              }
            }
            if (service.notes.isNotBlank()) {
              Text(
                text = "• ${service.notes}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddDivineServiceDialog(
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    date: String,
    time: String,
    officiant: String,
    officiantRank: String,
    assisting: String,
    choir: String,
    organist: String,
    theme: String,
    bible: String,
    tag: String,
    location: String,
    notes: String
  ) -> Unit
) {
  var title by remember { mutableStateOf("Sunday Divine Service") }
  var date by remember { mutableStateOf("Sun Nov 29, 2026") }
  var time by remember { mutableStateOf("10:30 AM") }
  var officiant by remember { mutableStateOf("Pr. J. Grau") }
  var officiantRank by remember { mutableStateOf("Priest / Rector") }
  var assisting by remember { mutableStateOf("Dc. Duncan, Dc. Baker") }
  var choir by remember { mutableStateOf("Marilyn P.") }
  var organist by remember { mutableStateOf("Sister Sarah") }
  var theme by remember { mutableStateOf("Walking in Faith") }
  var bible by remember { mutableStateOf("Hebrews 11:1 — 'Now faith is confidence in what we hope for and assurance about what we do not see.'") }
  var tag by remember { mutableStateOf("Holy Communion") }
  var location by remember { mutableStateOf("Sanctuary") }
  var notes by remember { mutableStateOf("Sunday School during sermon") }

  val servicePresets = listOf(
    "Sunday Divine Service",
    "Midweek Divine Service",
    "Youth Service",
    "Thanksgiving Service",
    "Confirmation Service"
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(painter = painterResource(id = R.drawable.ic_nac_emblem), contentDescription = null, tint = NacNavy, modifier = Modifier.size(24.dp))
        Text("Schedule Divine Service")
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Service Title & Type *",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          servicePresets.forEach { p ->
            AssistChip(
              onClick = { title = p },
              label = { Text(p, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date *") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Time *") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = officiant,
            onValueChange = { officiant = it },
            label = { Text("Officiant *") },
            singleLine = true,
            modifier = Modifier.weight(1.2f)
          )
          OutlinedTextField(
            value = officiantRank,
            onValueChange = { officiantRank = it },
            label = { Text("Rank *") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = assisting,
          onValueChange = { assisting = it },
          label = { Text("Assisting Ministers") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = choir,
            onValueChange = { choir = it },
            label = { Text("Choir Conductor") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = organist,
            onValueChange = { organist = it },
            label = { Text("Organist") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = theme,
          onValueChange = { theme = it },
          label = { Text("Service Theme / Motto") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = bible,
          onValueChange = { bible = it },
          label = { Text("Bible Word / Scripture") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = tag,
            onValueChange = { tag = it },
            label = { Text("Special Tag") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank() && date.isNotBlank() && officiant.isNotBlank()) {
            onSave(title, date, time, officiant, officiantRank, assisting, choir, organist, theme, bible, tag, location, notes)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = NacNavy),
        enabled = title.isNotBlank() && date.isNotBlank() && officiant.isNotBlank()
      ) {
        Text("Save Service")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun DistrictSchedulePdfCard(
  resource: MiniHqResourceEntity,
  isAssignedDistrict: Boolean,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onDownloadPdf: () -> Unit
) {
  val hasStoredPdf = (resource.pdfBlob != null && resource.pdfBlob.size >= MiniHqScraperService.MIN_PDF_BYTES) ||
    (resource.fileSize >= MiniHqScraperService.MIN_PDF_BYTES && resource.localPath.isNotBlank())
  val missingScheduleYellow = Color(0xFFFACC15)
  val cardShape = RoundedCornerShape(14.dp)

  Surface(
    onClick = onToggleExpand,
    shape = cardShape,
    color = when {
      isAssignedDistrict -> NacGoldLight
      !hasStoredPdf -> Color(0xFFFEFCE8)
      else -> MaterialTheme.colorScheme.surface
    },
    border = BorderStroke(
      width = when {
        !hasStoredPdf -> 2.dp
        isAssignedDistrict -> 1.5.dp
        else -> 1.dp
      },
      color = when {
        !hasStoredPdf -> missingScheduleYellow
        isAssignedDistrict -> NacGold
        else -> MaterialTheme.colorScheme.outlineVariant
      }
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            tint = if (isAssignedDistrict) NacNavy else MaterialTheme.colorScheme.primary
          )
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = resource.district,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              if (isAssignedDistrict) {
                Surface(
                  color = NacNavy,
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text(
                    text = "Your District",
                    style = MaterialTheme.typography.labelSmall,
                    color = NacGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }
              }
              if (!hasStoredPdf) {
                Surface(
                  color = Color(0xFFFEF3C7),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text(
                    text = "Pending Schedule",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF92400E),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }
              }
            }
            Text(
              text = resource.monthYear,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Icon(
          imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      AnimatedVisibility(visible = isExpanded) {
        Button(
          onClick = onDownloadPdf,
          colors = ButtonDefaults.buttonColors(
            containerColor = NacNavy,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(8.dp))
          Text("View Details & Download PDF")
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DsgServiceResourcesCard(
  serviceDate: String,
  serviceTitle: String,
  assets: List<MiniHqResourceEntity>,
  onOpenAsset: (MiniHqResourceEntity) -> Unit,
  onDownloadAsset: (MiniHqResourceEntity) -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (serviceDate.isNotBlank()) {
          Text(
            text = serviceDate,
            style = MaterialTheme.typography.labelMedium,
            color = NacNavy,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = serviceTitle,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        assets.forEach { asset ->
          val icon = when (asset.assetType) {
            MiniHqScraperService.ASSET_AUDIO -> Icons.Default.Headphones
            MiniHqScraperService.ASSET_REFERENCES -> Icons.Default.Description
            MiniHqScraperService.ASSET_EN_BIBLE_READING,
            MiniHqScraperService.ASSET_FR_BIBLE_READING -> Icons.AutoMirrored.Filled.MenuBook
            else -> Icons.Default.PictureAsPdf
          }
          val displayLabel = when {
            asset.assetType.endsWith(" DE", ignoreCase = true) || asset.assetType.equals("DE", ignoreCase = true) -> "German"
            asset.assetType.endsWith(" EN", ignoreCase = true) && !asset.assetType.contains("Bible", ignoreCase = true) && !asset.assetType.contains("DSG", ignoreCase = true) && !asset.assetType.contains("Transcript", ignoreCase = true) -> "English"
            asset.assetType.equals("EN", ignoreCase = true) -> "English"
            asset.assetType.endsWith(" ES", ignoreCase = true) || asset.assetType.equals("ES", ignoreCase = true) -> "Spanish"
            asset.assetType.endsWith(" FR", ignoreCase = true) && !asset.assetType.contains("Bible", ignoreCase = true) && !asset.assetType.contains("DSG", ignoreCase = true) && !asset.assetType.contains("Transcript", ignoreCase = true) -> "French"
            asset.assetType.equals("FR", ignoreCase = true) -> "French"
            else -> asset.assetType
          }
          OutlinedButton(
            onClick = { onOpenAsset(asset) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(
              text = displayLabel,
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DistrictPdfViewerDialog(
  resource: MiniHqResourceEntity,
  canInstallPdf: Boolean,
  onDismiss: () -> Unit,
  onDownloadPdf: () -> Unit,
  onInstallManualPdf: (Uri) -> Unit
) {
  val context = LocalContext.current
  val pdfPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      onInstallManualPdf(uri)
    }
  }

  var renderedPages by remember(resource.id, resource.checksum, resource.localPath) {
    mutableStateOf<List<ImageBitmap>>(emptyList())
  }
  var pageTextIndexes by remember(resource.id, resource.checksum, resource.localPath) {
    mutableStateOf<List<PdfPageTextIndex>>(emptyList())
  }
  var showFullScreenViewer by remember { mutableStateOf(false) }
  var fileSizeKb by remember(resource.id, resource.checksum, resource.localPath) {
    mutableStateOf(0L)
  }
  var isLoadingPages by remember(resource.id, resource.checksum, resource.localPath) {
    mutableStateOf(true)
  }

  LaunchedEffect(resource.id, resource.checksum, resource.localPath) {
    isLoadingPages = true
    val initial = withContext(Dispatchers.IO) {
      val cacheDir = File(context.filesDir, "minihq_cache").apply { mkdirs() }
      val file = File(resource.localPath).takeIf { resource.localPath.isNotBlank() && it.exists() }
        ?: File(cacheDir, resource.fileName).also { target ->
          val blob = resource.pdfBlob
          if (blob != null && MiniHqScraperService.isValidBinaryAsset(resource.fileName, resource.mimeType, blob)) {
            target.writeBytes(blob)
          }
        }
      if (!file.exists() || !MiniHqScraperService.isValidCachedFile(file)) {
        return@withContext Triple<File?, List<ImageBitmap>, Long>(null, emptyList(), 0L)
      }
      val kb = (file.length() / 1024L).coerceAtLeast(1L)
      val firstPage = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
          PdfRenderer(pfd).use { renderer ->
            if (renderer.pageCount > 0) {
              renderer.openPage(0).use { page ->
                val width = (page.width * 2).coerceIn(800, 1400)
                val height = (width * page.height / page.width.coerceAtLeast(1)).coerceAtLeast(400)
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                listOf(bmp.asImageBitmap())
              }
            } else emptyList()
          }
        }
      }.getOrDefault(emptyList())
      Triple(file, firstPage, kb)
    }
    val validFile = initial.first
    renderedPages = initial.second
    fileSizeKb = initial.third
    isLoadingPages = false

    if (validFile != null) {
      val (allPages, extractedIndexes) = withContext(Dispatchers.IO) {
        val restPages = runCatching {
          ParcelFileDescriptor.open(validFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
              val maxPages = renderer.pageCount.coerceAtMost(24)
              if (maxPages <= 1) emptyList()
              else (1 until maxPages).map { pageIndex ->
                renderer.openPage(pageIndex).use { page ->
                  val width = (page.width * 2).coerceIn(800, 1400)
                  val height = (width * page.height / page.width.coerceAtLeast(1)).coerceAtLeast(400)
                  val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                  bmp.eraseColor(android.graphics.Color.WHITE)
                  page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                  bmp.asImageBitmap()
                }
              }
            }
          }
        }.getOrDefault(emptyList())
        val indexes = runCatching {
          PdfTextSearchEngine.extractPageTextIndexes(context, validFile, maxPages = 24)
        }.getOrDefault(emptyList())
        (initial.second + restPages) to indexes
      }
      renderedPages = allPages
      pageTextIndexes = extractedIndexes
    }
  }

  val isManuallyInstalled = resource.checksum.startsWith("manual_")

  InWindowModalOverlay(onDismiss = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 12.dp,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val modalTitle = if (resource.district.isNotBlank()) {
            "${resource.district} Serving Schedule"
          } else {
            listOf(resource.serviceTitle, resource.assetType)
              .filter { it.isNotBlank() }
              .joinToString(" • ")
              .ifBlank { resource.fileName }
          }
          val modalSubtitle = if (resource.district.isNotBlank()) {
            "${resource.monthYear} • ${if (isManuallyInstalled) "Installed PDF" else "Portal PDF"}${if (fileSizeKb > 0) " • ${fileSizeKb} KB" else ""}"
          } else {
            listOf(resource.serviceDate.takeIf { it.isNotBlank() } ?: resource.monthYear, "Portal PDF", if (fileSizeKb > 0) "${fileSizeKb} KB" else "")
              .filter { it.isNotBlank() }
              .joinToString(" • ")
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = modalTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = modalSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          TextButton(onClick = onDismiss) {
            Text("Close")
          }
        }

        HorizontalDivider()

        // Source PDF URL + Copy to Clipboard Bar
        val clipboardManager = LocalClipboardManager.current
        var urlCopied by remember(resource.id, resource.sourceUrl) { mutableStateOf(false) }
        LaunchedEffect(urlCopied) {
          if (urlCopied) {
            delay(2500)
            urlCopied = false
          }
        }
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Source PDF URL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = resource.sourceUrl.ifBlank { "Not synced from MiniHQ yet" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
              )
            }
            OutlinedButton(
              onClick = {
                if (resource.sourceUrl.isNotBlank()) {
                  clipboardManager.setText(AnnotatedString(resource.sourceUrl))
                  urlCopied = true
                }
              },
              enabled = resource.sourceUrl.isNotBlank(),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(
                imageVector = if (urlCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
              Spacer(Modifier.width(4.dp))
              Text(
                text = if (urlCopied) "Copied!" else "Copy URL",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }

        // Inline PDF Pages Preview
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .height(380.dp)
            .background(Color(0xFFF1F3F5), RoundedCornerShape(12.dp))
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          when {
            isLoadingPages -> {
              CircularProgressIndicator()
            }
            renderedPages.isNotEmpty() -> {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                items(renderedPages.size) { index ->
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Page ${index + 1} of ${renderedPages.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      Text(
                        text = "Tap PDF to view full screen & search",
                        style = MaterialTheme.typography.labelSmall,
                        color = NacNavy,
                        fontWeight = FontWeight.Medium
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      shadowElevation = 2.dp,
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFullScreenViewer = true }
                    ) {
                      Image(
                        bitmap = renderedPages[index],
                        contentDescription = "Schedule PDF page ${index + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                      )
                    }
                  }
                }
              }
            }
            else -> {
              Text(
                text = if (canInstallPdf) {
                  "No valid PDF installed yet. Tap 'Install PDF' below to upload this district's schedule PDF."
                } else {
                  "Schedule PDF preview is not yet available."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
              )
            }
          }
        }

        // Action Buttons: Download PDF (all authorized users) + Install / Replace PDF (Admins only)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (canInstallPdf) {
            OutlinedButton(
              onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(
                text = if (isManuallyInstalled) "Replace PDF" else "Install PDF",
                maxLines = 1,
                softWrap = false
              )
            }
          }

          Button(
            onClick = onDownloadPdf,
            colors = ButtonDefaults.buttonColors(
              containerColor = NacNavy,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Download PDF", maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }

  if (showFullScreenViewer && renderedPages.isNotEmpty()) {
    val fullScreenTitle = if (resource.district.isNotBlank()) {
      "${resource.district} • ${resource.monthYear}"
    } else {
      listOf(resource.serviceTitle, resource.assetType, resource.monthYear)
        .filter { it.isNotBlank() }
        .joinToString(" • ")
    }
    FullScreenPdfViewerDialog(
      title = fullScreenTitle,
      pages = renderedPages,
      pageTextIndexes = pageTextIndexes,
      onDismiss = { showFullScreenViewer = false }
    )
  }
}

@Composable
private fun FullScreenPdfViewerDialog(
  title: String,
  pages: List<ImageBitmap>,
  pageTextIndexes: List<PdfPageTextIndex>,
  onDismiss: () -> Unit
) {
  var showSearchBar by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var activeMatchIndex by remember { mutableStateOf(0) }
  var scale by remember { mutableStateOf(1f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }
  val scrollState = rememberScrollState()
  var viewportSize by remember { mutableStateOf(IntSize.Zero) }
  val pageBoundsMap = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
  val pageWidthMap = remember { mutableStateMapOf<Int, Float>() }
  val pageLeftMap = remember { mutableStateMapOf<Int, Float>() }

  val matches = remember(searchQuery, pageTextIndexes) {
    PdfTextSearchEngine.searchMatches(pageTextIndexes, searchQuery)
  }

  LaunchedEffect(matches) {
    activeMatchIndex = 0
  }

  LaunchedEffect(activeMatchIndex, matches, viewportSize) {
    if (matches.isEmpty() || viewportSize.height <= 0) return@LaunchedEffect
    val match = matches[activeMatchIndex.coerceIn(0, matches.lastIndex)]
    val bounds = pageBoundsMap[match.pageIndex] ?: return@LaunchedEffect
    val pageTopY = bounds.first
    val pageHeight = bounds.second
    val pageWidth = pageWidthMap[match.pageIndex] ?: viewportSize.width.toFloat()
    val pageLeft = pageLeftMap[match.pageIndex] ?: 0f

    val matchCenterYNorm = ((match.primaryRect.top + match.primaryRect.bottom) / 2f).coerceIn(0f, 1f)
    val matchCenterXNorm = ((match.primaryRect.left + match.primaryRect.right) / 2f).coerceIn(0f, 1f)
    val matchYInColumn = pageTopY + matchCenterYNorm * pageHeight
    val targetScrollY = (matchYInColumn - viewportSize.height / 2f)
      .toInt()
      .coerceIn(0, scrollState.maxValue.coerceAtLeast(0))

    scrollState.animateScrollTo(targetScrollY)

    if (scale > 1.05f) {
      val centerX = viewportSize.width / 2f
      val centerY = viewportSize.height / 2f
      val unscaledX = pageLeft + matchCenterXNorm * pageWidth
      val unscaledY = matchYInColumn - targetScrollY.toFloat()
      panOffset = Offset(
        x = -(unscaledX - centerX) * scale,
        y = -(unscaledY - centerY) * scale
      )
    }
  }

  androidx.activity.compose.BackHandler(onBack = onDismiss)
  Box(
    modifier = Modifier
      .fillMaxSize()
      .zIndex(60f)
  ) {
    Surface(
      color = Color(0xFF0F172A),
      modifier = Modifier.fillMaxSize()
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Toolbar (safe-area + camera cutout padded, high z-index, 48dp touch targets)
        Surface(
          color = NacNavyDark,
          shadowElevation = 8.dp,
          modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(end = 6.dp)
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { showSearchBar = !showSearchBar },
                  modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Find in PDF",
                    tint = if (showSearchBar) NacGold else Color.White
                  )
                }
                IconButton(
                  onClick = {
                    scale = (scale - 0.5f).coerceAtLeast(1f)
                    if (scale == 1f) panOffset = Offset.Zero
                  },
                  modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                  Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out", tint = Color.White)
                }
                Text(
                  text = "${(scale * 100).toInt()}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = NacGold
                )
                IconButton(
                  onClick = {
                    scale = (scale + 0.5f).coerceAtMost(5f)
                  },
                  modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                  Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = Color.White)
                }
                TextButton(
                  onClick = {
                    scale = 1f
                    panOffset = Offset.Zero
                  },
                  modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                  Text("Reset", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(
                  onClick = onDismiss,
                  modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Close full screen", tint = Color.White)
                }
              }
            }

            // Find / Search Words Bar
            AnimatedVisibility(visible = showSearchBar) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Find word or name (e.g. Grau, London)...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = Color.White,
                      unfocusedContainerColor = Color.White,
                      focusedTextColor = Color.Black,
                      unfocusedTextColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                  )
                  if (matches.isNotEmpty()) {
                    Text(
                      text = "${activeMatchIndex + 1} of ${matches.size}",
                      style = MaterialTheme.typography.labelSmall,
                      color = NacGold,
                      fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                      activeMatchIndex = if (activeMatchIndex - 1 < 0) matches.lastIndex else activeMatchIndex - 1
                    }) {
                      Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match", tint = Color.White)
                    }
                    IconButton(onClick = {
                      activeMatchIndex = (activeMatchIndex + 1) % matches.size
                    }) {
                      Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match", tint = Color.White)
                    }
                  } else if (searchQuery.isNotBlank()) {
                    Text(
                      text = "0 matches",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(0xFFFFCDD2)
                    )
                  }
                }
              }
            }
          }
        }

        // Interactive Zoomable / Pannable / Scrollable PDF Canvas
        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .zIndex(1f)
            .clipToBounds()
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
              detectTapGestures(
                onDoubleTap = {
                  if (scale > 1.2f) {
                    scale = 1f
                    panOffset = Offset.Zero
                  } else {
                    scale = 2.5f
                  }
                }
              )
            }
            .pointerInput(Unit) {
              detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (scale * zoom).coerceIn(1f, 5f)
                scale = newScale
                panOffset = if (newScale > 1f) {
                  panOffset + pan
                } else {
                  Offset.Zero
                }
              }
            }
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState, enabled = scale <= 1.05f)
              .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = panOffset.x,
                translationY = panOffset.y
              )
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            pages.forEachIndexed { idx, pageBitmap ->
              val pageMatches = matches.filter { it.pageIndex == idx }
              val activeMatch = matches.getOrNull(activeMatchIndex)
              Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.onGloballyPositioned { coords ->
                  // Track page column Y offset for auto-scrolling
                  pageBoundsMap[idx] = coords.positionInParent().y to coords.size.height.toFloat()
                  pageWidthMap[idx] = coords.size.width.toFloat()
                  pageLeftMap[idx] = coords.positionInParent().x
                }
              ) {
                Text(
                  text = "Page ${idx + 1} of ${pages.size}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.7f)
                )
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color.White,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                      bitmap = pageBitmap,
                      contentDescription = "Full screen PDF page ${idx + 1}",
                      contentScale = ContentScale.FillWidth,
                      modifier = Modifier.fillMaxWidth()
                    )
                    if (pageMatches.isNotEmpty()) {
                      Canvas(modifier = Modifier.matchParentSize()) {
                        val padX = 1.dp.toPx()
                        val padY = 1.dp.toPx()
                        val borderStroke = 1.5.dp.toPx()
                        for (match in pageMatches) {
                          val isActive = (match === activeMatch)
                          for (rect in match.rects) {
                            val left = (rect.left * size.width - padX).coerceAtLeast(0f)
                            val top = (rect.top * size.height - padY).coerceAtLeast(0f)
                            val right = (rect.right * size.width + padX).coerceAtMost(size.width)
                            val bottom = (rect.bottom * size.height + padY).coerceAtMost(size.height)
                            val rectSize = Size(
                              width = (right - left).coerceAtLeast(4f),
                              height = (bottom - top).coerceAtLeast(4f)
                            )
                            drawRect(
                              color = Color(0xFFFACC15).copy(alpha = if (isActive) 0.55f else 0.45f),
                              topLeft = Offset(left, top),
                              size = rectSize
                            )
                            if (isActive) {
                              drawRect(
                                color = Color(0xFFEA580C),
                                topLeft = Offset(left, top),
                                size = rectSize,
                                style = Stroke(width = borderStroke)
                              )
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MiniHqWebLoginDialog(
  onDismiss: () -> Unit,
  onAuthenticatedAndSync: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var isRunningBrowserSync by remember { mutableStateOf(false) }
  var browserSyncStatus by remember { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Sign in to MiniHQ",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Authenticate on naccanada.org, then tap Sync Live PDFs.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          TextButton(onClick = onDismiss) {
            Text("Close")
          }
        }

        if (browserSyncStatus.isNotBlank()) {
          Text(
            text = browserSyncStatus,
            style = MaterialTheme.typography.labelMedium,
            color = NacNavy,
            fontWeight = FontWeight.Medium
          )
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clip(RoundedCornerShape(12.dp))
        ) {
          AndroidView(
            factory = { ctx ->
              val pendingSaveJobs = java.util.Collections.synchronizedList(mutableListOf<kotlinx.coroutines.Job>())
              val bridge = object {
                @android.webkit.JavascriptInterface
                fun onProgress(percent: Int, message: String) {
                  MiniHqScraperService.updateSyncProgress(
                    isSyncing = percent < 100,
                    percent = percent,
                    stepMessage = message,
                    requiresAuth = false
                  )
                  browserSyncStatus = "$percent% — $message"
                }

                @android.webkit.JavascriptInterface
                fun onPdfFetched(
                  resourceId: String,
                  realUrl: String,
                  contentType: String,
                  base64Data: String
                ) {
                  val job = coroutineScope.launch {
                    MiniHqScraperService.saveWebViewScrapedBinary(
                      context = ctx,
                      resourceId = resourceId,
                      realUrl = realUrl,
                      contentType = contentType,
                      base64Payload = base64Data
                    )
                  }
                  pendingSaveJobs.add(job)
                }

                @android.webkit.JavascriptInterface
                fun onComplete(savedCount: Int, errorMsg: String) {
                  coroutineScope.launch {
                    val jobsSnapshot = synchronized(pendingSaveJobs) { pendingSaveJobs.toList() }
                    jobsSnapshot.forEach { it.join() }
                    synchronized(pendingSaveJobs) { pendingSaveJobs.clear() }
                    MiniHqScraperService.persistWebViewCookies(ctx)
                    isRunningBrowserSync = false
                    if (errorMsg.isNotBlank()) {
                      browserSyncStatus = errorMsg
                      MiniHqScraperService.updateSyncProgress(
                        isSyncing = false,
                        percent = 0,
                        stepMessage = "Sync failed: $savedCount/18 Districts Saved",
                        requiresAuth = true,
                        hasError = true,
                        savedDistrictCount = savedCount,
                        totalDistrictCount = 18,
                        diagnosticLog = errorMsg
                      )
                    } else {
                      MiniHqScraperService.updateSyncProgress(
                        isSyncing = false,
                        percent = 100,
                        stepMessage = "Sync Complete! ($savedCount/18 Districts Saved)",
                        requiresAuth = false,
                        hasError = false,
                        savedDistrictCount = savedCount,
                        totalDistrictCount = 18
                      )
                      onAuthenticatedAndSync()
                    }
                  }
                }
              }

              WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(bridge, "MiniHqBridge")
                webViewClient = object : WebViewClient() {
                  override fun onPageFinished(view: WebView?, url: String?) {
                    CookieManager.getInstance().flush()
                  }
                }
                loadUrl(MiniHqScraperService.MINIHQ_URL)
                webViewRef = this
              }
            },
            modifier = Modifier.fillMaxSize()
          )
        }

        Button(
          onClick = {
            val wv = webViewRef ?: return@Button
            if (isRunningBrowserSync) return@Button
            isRunningBrowserSync = true
            browserSyncStatus = "10% — Starting in-browser MiniHQ extraction..."
            CookieManager.getInstance().flush()

            val jsScraper = """
              (async function() {
                try {
                  MiniHqBridge.onProgress(15, 'Fetching MiniHQ landing page...');
                  const homeRes = await fetch('${MiniHqScraperService.MINIHQ_URL}', { credentials: 'include' });
                  const homeHtml = await homeRes.text();
                  const parser = new DOMParser();
                  const homeDoc = parser.parseFromString(homeHtml, 'text/html');
                  const allLinks = Array.from(homeDoc.querySelectorAll('a'));
                  const monthLinks = allLinks.filter(a => {
                    const href = a.getAttribute('href') || '';
                    return href.toLowerCase().includes('serving_schedules_');
                  }).map(a => ({
                    label: (a.textContent || '').trim(),
                    url: new URL(a.getAttribute('href'), '${MiniHqScraperService.MINIHQ_URL}').href
                  }));
                  if (monthLinks.length === 0) {
                    MiniHqBridge.onComplete(0, 'Please sign in to MiniHQ first, then tap Sync Live PDFs.');
                    return;
                  }
                  const districts = ['British Columbia','Alberta','Saskatchewan','Manitoba','Northern Ontario','Kitchener','Hamilton','Toronto','Eastern Canada'];
                  let tasks = [];
                  for (const m of monthLinks) {
                    const mRes = await fetch(m.url, { credentials: 'include' });
                    const mHtml = await mRes.text();
                    const lowerHtml = mHtml.toLowerCase();
                    const startIdx = lowerHtml.indexOf('district serving schedules');
                    let scopedHtml = startIdx >= 0 ? mHtml.substring(startIdx) : mHtml;
                    const stopWords = ['nacc calendars', 'youth schedules', 'seniors schedules'];
                    const stopIndices = stopWords.map(w => scopedHtml.toLowerCase().indexOf(w)).filter(idx => idx > 0);
                    if (stopIndices.length > 0) {
                      scopedHtml = scopedHtml.substring(0, Math.min(...stopIndices));
                    }
                    const mDoc = parser.parseFromString(scopedHtml, 'text/html');
                    const mLinks = Array.from(mDoc.querySelectorAll('a'));
                    for (const d of districts) {
                      const match = mLinks.find(a => {
                        const txt = (a.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
                        return txt === d.toLowerCase() || txt.includes(d.toLowerCase());
                      });
                      if (match && match.getAttribute('href')) {
                        const realUrl = new URL(match.getAttribute('href'), m.url).href;
                        const normMonth = (m.label || '').replace(/\s+/g, ' ').trim();
                        const monthSlug = normMonth.replace(/\s+/g, '_').toLowerCase();
                        const distSlug = d.replace(/\s+/g, '_').toLowerCase();
                        tasks.push({ id: 'sched_' + monthSlug + '_' + distSlug, district: d, month: normMonth, referrer: m.url, url: realUrl });
                      }
                    }
                  }
                  let saved = 0;
                  for (let i = 0; i < tasks.length; i++) {
                    const t = tasks[i];
                    const pct = 35 + Math.floor(((i + 1) * 55) / Math.max(tasks.length, 1));
                    MiniHqBridge.onProgress(pct, 'Downloading ' + (i + 1) + '/' + tasks.length + ': ' + t.district + ' - ' + t.month);
                    const pdfRes = await fetch(t.url, { credentials: 'include', referrer: t.referrer });
                    const ct = pdfRes.headers.get('content-type') || '';
                    const buf = await pdfRes.arrayBuffer();
                    const bytes = new Uint8Array(buf);
                    let bin = '';
                    const chunk = 8192;
                    for (let j = 0; j < bytes.length; j += chunk) {
                      bin += String.fromCharCode.apply(null, bytes.subarray(j, j + chunk));
                    }
                    const b64 = btoa(bin);
                    MiniHqBridge.onPdfFetched(t.id, t.url, ct, b64);
                    if (bytes.length > 5000 && bytes[0] === 0x25 && bytes[1] === 0x50) saved++;
                  }
                  MiniHqBridge.onComplete(saved, '');
                } catch (err) {
                  MiniHqBridge.onComplete(0, 'Sync error: ' + err.message);
                }
              })();
            """.trimIndent()
            wv.evaluateJavascript(jsScraper, null)
          },
          enabled = !isRunningBrowserSync,
          colors = ButtonDefaults.buttonColors(
            containerColor = NacNavy,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(8.dp))
          Text(
            text = if (isRunningBrowserSync) "Syncing Live PDFs..." else "Sync Live PDFs from MiniHQ",
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Composable
private fun DsgTextReaderDialog(
  resource: MiniHqResourceEntity,
  onDismiss: () -> Unit,
  onOpenPdfOrInstall: () -> Unit
) {
  InWindowModalOverlay(onDismiss = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 12.dp,
      modifier = Modifier.fillMaxWidth(0.94f)
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Column {
          Text(resource.assetType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            text = "${resource.serviceDate} • ${resource.serviceTitle}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = resource.contentText.ifBlank { "No text preview available yet." },
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "Source URL:\n${resource.sourceUrl}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("Close")
          }
          Spacer(Modifier.width(8.dp))
          Button(
            onClick = onOpenPdfOrInstall,
            colors = ButtonDefaults.buttonColors(containerColor = NacNavy)
          ) {
            Text("PDF Viewer / Install")
          }
        }
      }
    }
  }
}

private fun formatAudioMillis(ms: Int): String {
  val totalSec = (ms / 1000).coerceAtLeast(0)
  val mins = totalSec / 60
  val secs = totalSec % 60
  return "%d:%02d".format(mins, secs)
}

@Composable
private fun DsgAudioPlayerDialog(
  resource: MiniHqResourceEntity,
  onDismiss: () -> Unit,
  onDownloadAudio: () -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var isPlaying by remember(resource.id) { mutableStateOf(false) }
  var isPrepared by remember(resource.id) { mutableStateOf(false) }
  var isPreparing by remember(resource.id) { mutableStateOf(false) }
  var currentPosMs by remember(resource.id) { mutableStateOf(0) }
  var durationMs by remember(resource.id) { mutableStateOf(0) }
  var isSeeking by remember(resource.id) { mutableStateOf(false) }
  var urlCopied by remember(resource.id, resource.sourceUrl) { mutableStateOf(false) }
  var audioError by remember(resource.id) { mutableStateOf<String?>(null) }
  val mediaPlayer = remember(resource.id) { MediaPlayer() }

  LaunchedEffect(urlCopied) {
    if (urlCopied) {
      delay(2500)
      urlCopied = false
    }
  }

  LaunchedEffect(isPlaying, isPrepared, isSeeking) {
    while (isPlaying && isPrepared) {
      if (!isSeeking) {
        runCatching {
          currentPosMs = mediaPlayer.currentPosition.coerceAtLeast(0)
          if (mediaPlayer.duration > 0) durationMs = mediaPlayer.duration
        }
      }
      delay(250)
    }
  }

  DisposableEffect(resource.id) {
    onDispose {
      runCatching {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        mediaPlayer.release()
      }
    }
  }

  InWindowModalOverlay(onDismiss = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 12.dp,
      modifier = Modifier.fillMaxWidth(0.94f)
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Divine Service Audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              text = listOf(resource.serviceDate, resource.serviceTitle).filter { it.isNotBlank() }.joinToString(" • "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          TextButton(onClick = onDismiss) {
            Text("Close")
          }
        }
        Text(
          text = resource.fileName,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold
        )
        if (resource.contentText.isNotBlank()) {
          Text(
            text = resource.contentText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Source Audio URL + Copy URL Bar
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Source Audio URL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = resource.sourceUrl.ifBlank { "Not synced from MiniHQ yet" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
              )
            }
            OutlinedButton(
              onClick = {
                if (resource.sourceUrl.isNotBlank()) {
                  clipboardManager.setText(AnnotatedString(resource.sourceUrl))
                  urlCopied = true
                }
              },
              enabled = resource.sourceUrl.isNotBlank(),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(
                imageVector = if (urlCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
              Spacer(Modifier.width(4.dp))
              Text(
                text = if (urlCopied) "Copied!" else "Copy URL",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }

        // Scrub Timeline + Duration
        Column(modifier = Modifier.fillMaxWidth()) {
          Slider(
            value = if (durationMs > 0) currentPosMs.toFloat().coerceIn(0f, durationMs.toFloat()) else 0f,
            onValueChange = { newPos ->
              isSeeking = true
              currentPosMs = newPos.toInt()
            },
            onValueChangeFinished = {
              if (isPrepared) {
                runCatching { mediaPlayer.seekTo(currentPosMs) }
              }
              isSeeking = false
            },
            valueRange = 0f..(durationMs.coerceAtLeast(1).toFloat()),
            enabled = isPrepared && durationMs > 0
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = formatAudioMillis(currentPosMs),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = if (durationMs > 0) formatAudioMillis(durationMs) else "--:--",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        audioError?.let { err ->
          Text(
            text = err,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
        Button(
          onClick = {
            if (isPlaying) {
              runCatching { mediaPlayer.pause() }
              isPlaying = false
            } else if (isPrepared) {
              runCatching {
                mediaPlayer.start()
                isPlaying = true
                audioError = null
              }.onFailure { err ->
                audioError = err.message ?: "Unable to resume audio."
                isPlaying = false
              }
            } else if (!isPreparing) {
              runCatching {
                isPreparing = true
                audioError = null
                mediaPlayer.reset()
                val cacheFile = File(File(context.filesDir, "minihq_cache"), resource.fileName)
                val localFile = File(resource.localPath).takeIf { resource.localPath.isNotBlank() && it.exists() }
                  ?: cacheFile.takeIf { it.exists() && it.length() >= MiniHqScraperService.MIN_AUDIO_BYTES }
                if (localFile != null) {
                  mediaPlayer.setDataSource(localFile.absolutePath)
                  mediaPlayer.prepare()
                  durationMs = mediaPlayer.duration.coerceAtLeast(0)
                  isPrepared = true
                  isPreparing = false
                  mediaPlayer.start()
                  mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    currentPosMs = 0
                  }
                  isPlaying = true
                } else {
                  val blob = resource.pdfBlob
                  if (blob != null && MiniHqScraperService.isValidBinaryAsset(resource.fileName, "audio/mpeg", blob)) {
                    val tempFile = File(context.cacheDir, resource.fileName).apply { writeBytes(blob) }
                    mediaPlayer.setDataSource(tempFile.absolutePath)
                    mediaPlayer.prepare()
                    durationMs = mediaPlayer.duration.coerceAtLeast(0)
                    isPrepared = true
                    isPreparing = false
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener {
                      isPlaying = false
                      currentPosMs = 0
                    }
                    isPlaying = true
                  } else if (resource.sourceUrl.startsWith("http", ignoreCase = true)) {
                    val cookie = context.getSharedPreferences("minihq_sync_prefs", android.content.Context.MODE_PRIVATE)
                      .getString("key_minihq_session_cookie", "").orEmpty()
                    val headers = if (cookie.isNotBlank()) mapOf("Cookie" to cookie) else emptyMap()
                    mediaPlayer.setDataSource(context, Uri.parse(resource.sourceUrl), headers)
                    mediaPlayer.setOnPreparedListener { mp ->
                      durationMs = mp.duration.coerceAtLeast(0)
                      isPrepared = true
                      isPreparing = false
                      mp.start()
                      isPlaying = true
                    }
                    mediaPlayer.setOnCompletionListener {
                      isPlaying = false
                      currentPosMs = 0
                    }
                    mediaPlayer.setOnErrorListener { _, what, extra ->
                      isPreparing = false
                      isPlaying = false
                      audioError = "Unable to stream audio (code $what/$extra). Try Sync Now or Download MP3."
                      true
                    }
                    mediaPlayer.prepareAsync()
                  } else {
                    isPreparing = false
                    error("No cached MP3 audio file yet. Tap Sync DSG to fetch the live recording.")
                  }
                }
              }.onFailure { err ->
                isPreparing = false
                audioError = err.message ?: "Unable to play audio."
                isPlaying = false
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = NacNavy),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(Modifier.width(8.dp))
          Text(
            when {
              isPreparing -> "Loading Audio..."
              isPlaying -> "Pause Audio"
              else -> "Play Recording"
            }
          )
        }
        OutlinedButton(
          onClick = onDownloadAudio,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Download Audio (.mp3)")
        }
      }
    }
  }
}

@Composable
private fun AdminModuleSyncCard(
  title: String,
  subtitle: String,
  syncButtonLabel: String,
  replaceButtonLabel: String,
  scope: MiniHqScraperService.SyncScope,
  syncProgress: MiniHqScraperService.SyncProgressState,
  syncOutcome: MiniHqScraperService.SyncOutcome?,
  onSyncNow: () -> Unit,
  onScrapeAndReplace: () -> Unit
) {
  val isThisScopeActive = syncProgress.activeScope == scope || syncProgress.activeScope == MiniHqScraperService.SyncScope.ALL
  var showTechnicalDetails by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = NacNavyDark),
    shape = RoundedCornerShape(14.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Default.Sync,
          contentDescription = null,
          tint = NacGold,
          modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = NacGold
          )
        }
      }

      // Full-width button row so both Sync and Scrape & Replace have spacious, reliable touch targets
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { if (!syncProgress.isSyncing) onSyncNow() },
          enabled = !syncProgress.isSyncing,
          colors = ButtonDefaults.buttonColors(
            containerColor = NacGold,
            contentColor = NacNavyDark
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 44.dp)
        ) {
          if (syncProgress.isSyncing && isThisScopeActive) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              strokeWidth = 2.dp,
              color = NacNavyDark
            )
            Spacer(Modifier.width(6.dp))
            Text("${syncProgress.percent}%", style = MaterialTheme.typography.labelSmall, maxLines = 1)
          } else {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(syncButtonLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
          }
        }

        OutlinedButton(
          onClick = { onScrapeAndReplace() },
          enabled = true,
          border = BorderStroke(1.dp, NacGold.copy(alpha = 0.85f)),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 44.dp)
        ) {
          Text(
            text = replaceButtonLabel,
            color = NacGold,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
          )
        }
      }

      if (isThisScopeActive && (syncProgress.isSyncing || syncProgress.hasError || (syncProgress.percent > 0 && syncProgress.stepMessage.isNotBlank()))) {
        val barColor = if (syncProgress.hasError) Color(0xFFEF5350) else NacGold
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          LinearProgressIndicator(
            progress = { (syncProgress.percent / 100f).coerceIn(0f, 1f) },
            color = barColor,
            trackColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(4.dp))
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${syncProgress.percent}% — ${syncProgress.stepMessage}",
              style = MaterialTheme.typography.labelSmall,
              color = if (syncProgress.hasError) Color(0xFFFFCDD2) else Color.White.copy(alpha = 0.92f),
              modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            if (scope == MiniHqScraperService.SyncScope.SCHEDULES || scope == MiniHqScraperService.SyncScope.ALL) {
              Text(
                text = "${syncProgress.savedDistrictCount}/${syncProgress.totalDistrictCount} Saved",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = barColor
              )
            }
          }
          if (syncProgress.hasError && syncProgress.diagnosticLog.isNotBlank()) {
            TextButton(
              onClick = { showTechnicalDetails = !showTechnicalDetails },
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = if (showTechnicalDetails) "Hide Technical Details" else "Technical Details",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFEE2E2)
              )
              Spacer(Modifier.width(4.dp))
              Icon(
                imageVector = if (showTechnicalDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFFFEE2E2),
                modifier = Modifier.size(14.dp)
              )
            }
            AnimatedVisibility(visible = showTechnicalDetails) {
              Surface(
                color = Color(0xFF7F1D1D).copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = syncProgress.diagnosticLog,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFFEE2E2),
                  modifier = Modifier.padding(8.dp)
                )
              }
            }
          }
        }
      }

      syncOutcome?.takeIf { it.scope == scope || it.scope == MiniHqScraperService.SyncScope.ALL }?.let { outcome ->
        val formattedTime = if (outcome.checkedTimestamp > 0) {
          SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(outcome.checkedTimestamp))
        } else "Just now"
        Text(
          text = "Last checked: $formattedTime • ${outcome.status.name}",
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.78f)
        )
      }
    }
  }
}
