package com.example.parishsync.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.parishsync.data.local.UserProfileEntity
import com.example.parishsync.data.local.VolunteerAssignmentEntity
import com.example.parishsync.data.model.AppRole
import com.example.parishsync.data.model.Congregation
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.MinistryDutyType
import com.example.parishsync.data.model.ParishPolicy
import com.example.parishsync.data.model.UserRole
import com.example.parishsync.data.model.VolunteerSubTask
import com.example.parishsync.workers.PdfScheduleParser
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VolunteerScheduleScreen(
  activeUser: UserProfileEntity,
  activeCongregation: Congregation,
  activeRole: UserRole,
  activeDelegatedRole: DelegatedRole,
  activeRoles: Set<AppRole> = AppRole.fromLegacy(activeRole, activeDelegatedRole),
  congregations: List<Congregation> = emptyList(),
  selectedAdminCongregationId: String = "",
  onSelectAdminCongregation: (String) -> Unit = {},
  navReclickTrigger: Int = 0,
  volunteerAssignments: List<VolunteerAssignmentEntity>,
  rosterMembers: List<UserProfileEntity>,
  onToggleConfirmation: (VolunteerAssignmentEntity) -> Unit,
  onToggleSubTaskCompletion: (VolunteerAssignmentEntity, String) -> Unit = { _, _ -> },
  onTriggerReminder: (VolunteerAssignmentEntity) -> Unit,
  onAddManualAssignment: (
    dutyType: MinistryDutyType,
    customTaskTitle: String,
    assignmentTier: String,
    assigneeName: String,
    backupAssigneeName: String,
    serviceDate: String,
    serviceTime: String,
    notes: String,
    subTasks: List<VolunteerSubTask>
  ) -> Unit,
  onDeleteAssignment: (String) -> Unit,
  onRunPdfScheduleIndexer: (pdfFileName: String, pdfUri: Uri?, customScheduleText: String?) -> Unit,
  onCommitParsedCards: (pdfFileName: String, cards: List<VolunteerAssignmentEntity>) -> Unit = { _, _ -> },
  onEmailScheduleToCongregation: (
    extraEmails: String,
    subject: String,
    message: String,
    fileName: String,
    uri: Uri?,
    summaryText: String
  ) -> Unit,
  modifier: Modifier = Modifier
) {
  var showAddTaskDialog by remember { mutableStateOf(false) }
  var showPdfParserDialog by remember { mutableStateOf(false) }
  var showEmailDialog by remember { mutableStateOf(false) }
  var selectedTabFilter by remember { mutableStateOf(0) } // 0: My Duties (default), 1: All
  var searchQuery by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  val canManageSchedule = ParishPolicy.canManageVolunteerSchedule(activeRoles)
  val canRemind = ParishPolicy.canRemindVolunteer(activeRoles)
  val isGlobalAdmin = ParishPolicy.isGlobalAdmin(activeRoles)

  val today = remember { LocalDate.now() }
  val todayFormatted = remember(today) {
    today.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH))
  }

  fun matchesUserInNamesList(namesCsv: String, user: UserProfileEntity): Boolean {
    if (namesCsv.isBlank()) return false
    return namesCsv.split(",")
      .map { it.trim() }
      .any { it.equals(user.fullName, ignoreCase = true) }
  }

  fun isUserAssignment(a: VolunteerAssignmentEntity): Boolean =
    a.isLocalUserMatch ||
      matchesUserInNamesList(a.assigneeName, activeUser) ||
      matchesUserInNamesList(a.backupAssigneeName, activeUser) ||
      (a.assigneeUserId != null && a.assigneeUserId == activeUser.id)

  fun isPreviousDate(dateStr: String): Boolean {
    val parsed = parseServiceLocalDate(dateStr) ?: return false
    return parsed.isBefore(today)
  }

  val myTotalCount = remember(volunteerAssignments, activeUser) {
    volunteerAssignments.count { isUserAssignment(it) }
  }
  val myUpcomingCount = remember(volunteerAssignments, activeUser, today) {
    volunteerAssignments.count { isUserAssignment(it) && !isPreviousDate(it.serviceDate) }
  }

  // Filter assignments by tab (My Duties, All) + search query, sorted chronologically
  val filteredAssignments = remember(volunteerAssignments, selectedTabFilter, searchQuery, activeUser) {
    var list = when (selectedTabFilter) {
      0 -> volunteerAssignments.filter { isUserAssignment(it) }
      else -> volunteerAssignments
    }
    if (searchQuery.isNotBlank()) {
      val q = searchQuery.trim().lowercase()
      list = list.filter {
        it.assigneeName.lowercase().contains(q) ||
          it.backupAssigneeName.lowercase().contains(q) ||
          it.customTaskTitle.lowercase().contains(q) ||
          it.dutyType.displayName.lowercase().contains(q) ||
          it.serviceDate.lowercase().contains(q) ||
          it.notes.lowercase().contains(q)
      }
    }
    list.sortedWith(compareBy<VolunteerAssignmentEntity> { parseServiceLocalDate(it.serviceDate) ?: LocalDate.MAX }.thenBy { it.serviceDate })
  }

  // Group by date in chronological order
  val assignmentsByDate = remember(filteredAssignments) {
    filteredAssignments.groupBy { it.serviceDate }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Header Card with Quick Action Buttons
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "Volunteer Schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = activeCongregation.displayName.ifBlank { "No Congregation Assigned" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
              )
            }

            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = "$myUpcomingCount Duties",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }

          // Global Admin Congregation Switcher (All Congregations vs specific tenant)
          if (isGlobalAdmin && congregations.isNotEmpty()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val allSelected = selectedAdminCongregationId.isBlank()
              Surface(
                onClick = { onSelectAdminCongregation("") },
                color = if (allSelected) Color.White else Color.White.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (allSelected) 1f else 0.4f)),
                shape = RoundedCornerShape(20.dp)
              ) {
                Text(
                  text = "All Congregations",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = if (allSelected) MaterialTheme.colorScheme.primary else Color.White,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
              congregations.forEach { cong ->
                val selected = selectedAdminCongregationId == cong.id
                Surface(
                  onClick = { onSelectAdminCongregation(cong.id) },
                  color = if (selected) Color.White else Color.White.copy(alpha = 0.14f),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = if (selected) 1f else 0.4f)),
                  shape = RoundedCornerShape(20.dp)
                ) {
                  Text(
                    text = cong.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                  )
                }
              }
            }
          }

          // Role-Gated Action Buttons: strictly visible only to volunteer_scheduler or admin
          if (canManageSchedule) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { showAddTaskDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = Color.White
                )
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Add Task", maxLines = 1, color = Color.White)
              }

              OutlinedButton(
                onClick = { showPdfParserDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = Color.White.copy(alpha = 0.12f),
                  contentColor = Color.White
                )
              ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Parse", maxLines = 1, color = Color.White)
              }

              OutlinedButton(
                onClick = { showEmailDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = Color.White.copy(alpha = 0.12f),
                  contentColor = Color.White
                )
              ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Email", maxLines = 1, color = Color.White)
              }
            }
          }
        }
      }

      // Filter Tabs: 0 = My Duties (default), 1 = All
      TabRow(
        selectedTabIndex = selectedTabFilter,
        containerColor = MaterialTheme.colorScheme.surface
      ) {
        Tab(
          selected = selectedTabFilter == 0,
          onClick = { selectedTabFilter = 0 },
          text = { Text("My Duties ($myTotalCount)", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        Tab(
          selected = selectedTabFilter == 1,
          onClick = { selectedTabFilter = 1 },
          text = { Text("All (${volunteerAssignments.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
      }

      // Search and stats bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search volunteer, role, or date...", style = MaterialTheme.typography.bodySmall) },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodySmall,
          shape = RoundedCornerShape(12.dp)
        )
      }

      val showTodayAnchor = searchQuery.isBlank()
      val pastEntries = remember(assignmentsByDate, today, showTodayAnchor) {
        if (!showTodayAnchor) emptyList()
        else assignmentsByDate.entries.filter {
          val parsed = parseServiceLocalDate(it.key)
          parsed != null && parsed.isBefore(today)
        }
      }
      val todayTasks = remember(assignmentsByDate, today, showTodayAnchor) {
        if (!showTodayAnchor) emptyList()
        else assignmentsByDate.entries
          .filter { parseServiceLocalDate(it.key) == today }
          .flatMap { it.value }
      }
      val upcomingEntries = remember(assignmentsByDate, today, showTodayAnchor) {
        if (!showTodayAnchor) assignmentsByDate.entries.toList()
        else assignmentsByDate.entries.filter {
          val parsed = parseServiceLocalDate(it.key)
          parsed == null || parsed.isAfter(today)
        }
      }

      // Exact item index of Today's Date header in the LazyColumn (1 top spacer + header & cards per past date)
      val todayAnchorIndex = remember(pastEntries, showTodayAnchor) {
        if (!showTodayAnchor) -1
        else 1 + pastEntries.sumOf { 1 + it.value.size }
      }

      // Snap viewport directly to Today's Date on initial load, tab switch, or nav re-click
      LaunchedEffect(selectedTabFilter, navReclickTrigger, todayAnchorIndex, showTodayAnchor) {
        if (showTodayAnchor && todayAnchorIndex >= 0) {
          listState.scrollToItem(index = todayAnchorIndex)
        }
      }

      if (!showTodayAnchor && assignmentsByDate.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Event,
              contentDescription = null,
              modifier = Modifier.size(64.dp),
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Text(
              text = "No tasks matching \"$searchQuery\"",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          item { Spacer(Modifier.height(4.dp)) }

          // 1. Past / Historical Service Dates (scrolled up above Today)
          pastEntries.forEach { (date, tasks) ->
            item(key = "past_header_$date") {
              Box(modifier = Modifier.alpha(0.65f)) {
                DateSectionHeader(date = date, taskCount = tasks.size)
              }
            }
            items(tasks, key = { "past_${it.id}" }) { assignment ->
              val isUserMain = assignment.isLocalUserMatch ||
                matchesUserInNamesList(assignment.assigneeName, activeUser) ||
                (assignment.assigneeUserId != null && assignment.assigneeUserId == activeUser.id)
              val isUserBackup = matchesUserInNamesList(assignment.backupAssigneeName, activeUser)
              Box(modifier = Modifier.alpha(0.68f)) {
                VolunteerTaskCard(
                  assignment = assignment,
                  isUserMain = isUserMain,
                  isUserBackup = isUserBackup,
                  canManageSchedule = canManageSchedule,
                  canRemind = canRemind,
                  onToggleConfirmation = { onToggleConfirmation(assignment) },
                  onToggleSubTask = { subTaskId -> onToggleSubTaskCompletion(assignment, subTaskId) },
                  onTriggerReminder = { onTriggerReminder(assignment) },
                  onDelete = { onDeleteAssignment(assignment.id) }
                )
              }
            }
          }

          // 2. Today's Date Anchor (snaps to top of viewport)
          if (showTodayAnchor) {
            item(key = "header_today_$todayFormatted") {
              DateSectionHeader(date = "Today • $todayFormatted", taskCount = todayTasks.size)
            }
            if (todayTasks.isEmpty()) {
              item(key = "today_empty_divider") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "No service scheduled for today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  HorizontalDivider()
                }
              }
            } else {
              items(todayTasks, key = { it.id }) { assignment ->
                val isUserMain = assignment.isLocalUserMatch ||
                  matchesUserInNamesList(assignment.assigneeName, activeUser) ||
                  (assignment.assigneeUserId != null && assignment.assigneeUserId == activeUser.id)
                val isUserBackup = matchesUserInNamesList(assignment.backupAssigneeName, activeUser)
                VolunteerTaskCard(
                  assignment = assignment,
                  isUserMain = isUserMain,
                  isUserBackup = isUserBackup,
                  canManageSchedule = canManageSchedule,
                  canRemind = canRemind,
                  onToggleConfirmation = { onToggleConfirmation(assignment) },
                  onToggleSubTask = { subTaskId -> onToggleSubTaskCompletion(assignment, subTaskId) },
                  onTriggerReminder = { onTriggerReminder(assignment) },
                  onDelete = { onDeleteAssignment(assignment.id) }
                )
              }
            }
          }

          // 3. Upcoming Future Service Dates (below Today)
          upcomingEntries.forEach { (date, tasks) ->
            item(key = "header_$date") {
              DateSectionHeader(date = date, taskCount = tasks.size)
            }

            items(tasks, key = { it.id }) { assignment ->
              val isUserMain = assignment.isLocalUserMatch ||
                matchesUserInNamesList(assignment.assigneeName, activeUser) ||
                (assignment.assigneeUserId != null && assignment.assigneeUserId == activeUser.id)
              val isUserBackup = matchesUserInNamesList(assignment.backupAssigneeName, activeUser)
              VolunteerTaskCard(
                assignment = assignment,
                isUserMain = isUserMain,
                isUserBackup = isUserBackup,
                canManageSchedule = canManageSchedule,
                canRemind = canRemind,
                onToggleConfirmation = { onToggleConfirmation(assignment) },
                onToggleSubTask = { subTaskId -> onToggleSubTaskCompletion(assignment, subTaskId) },
                onTriggerReminder = { onTriggerReminder(assignment) },
                onDelete = { onDeleteAssignment(assignment.id) }
              )
            }
          }

          item { Spacer(Modifier.height(80.dp)) }
        }
      }
    }
  }

  // --- DIALOG: Add Task Manually (Role + Volunteer Name) ---
  if (showAddTaskDialog) {
    ManualAddTaskDialog(
      activeCongregation = activeCongregation,
      rosterMembers = rosterMembers,
      onDismiss = { showAddTaskDialog = false },
      onSave = { dutyType, customTitle, tier, name, backupName, date, time, notes, subTasks ->
        onAddManualAssignment(dutyType, customTitle, tier, name, backupName, date, time, notes, subTasks)
        showAddTaskDialog = false
      }
    )
  }

  // --- DIALOG: Smart PDF Schedule Parser ---
  if (showPdfParserDialog) {
    PdfScheduleImportDialog(
      activeUser = activeUser,
      activeCongregation = activeCongregation,
      rosterMembers = rosterMembers,
      onDismiss = { showPdfParserDialog = false },
      onCommitParsedCards = { fileName, cards ->
        onCommitParsedCards(fileName, cards)
        showPdfParserDialog = false
      }
    )
  }

  // --- DIALOG: Email Schedule ---
  if (showEmailDialog) {
    EmailScheduleDialog(
      activeCongregation = activeCongregation,
      rosterMembers = rosterMembers,
      volunteerAssignments = volunteerAssignments,
      onDismiss = { showEmailDialog = false },
      onSendEmail = { extraEmails, subject, message, fileName, summary ->
        onEmailScheduleToCongregation(extraEmails, subject, message, fileName, null, summary)
        showEmailDialog = false
      }
    )
  }
}

@Composable
private fun DateSectionHeader(date: String, taskCount: Int) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(
        imageVector = Icons.Default.CalendarMonth,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp)
      )
      Text(
        text = date,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
    }

    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(8.dp)
    ) {
      Text(
        text = "$taskCount tasks",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
      )
    }
  }
}

@Composable
private fun VolunteerTaskCard(
  assignment: VolunteerAssignmentEntity,
  isUserMain: Boolean,
  isUserBackup: Boolean,
  canManageSchedule: Boolean,
  canRemind: Boolean,
  onToggleConfirmation: () -> Unit,
  onToggleSubTask: (String) -> Unit,
  onTriggerReminder: () -> Unit,
  onDelete: () -> Unit
) {
  val isCurrentUser = isUserMain || isUserBackup
  val isMainTier = assignment.assignmentTier.equals("MAIN", ignoreCase = true)
  val taskTitle = assignment.customTaskTitle.ifBlank { assignment.dutyType.displayName }
  val canToggleChecks = isCurrentUser || canManageSchedule

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      }
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Header: Dynamic User Role Badge (MAIN blue / BACKUP orange / hidden) + Duty Title + Completed Pill + Delete
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (isUserMain) {
              Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "MAIN",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            } else if (isUserBackup) {
              Surface(
                color = Color(0xFFF97316),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "BACKUP",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Text(
              text = taskTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            if (assignment.isConfirmed) {
              Surface(
                color = Color(0xFF2E7D32).copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "Completed",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF2E7D32),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          if (canManageSchedule) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete task",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        // Primary Volunteer Name (large, bold, high-contrast) + Service Time
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = assignment.assigneeName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          if (assignment.serviceTime.isNotBlank()) {
            Text(
              text = assignment.serviceTime,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        // Multi-Item Sub-Task Checkboxes
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            if (assignment.subTasks.isNotEmpty()) {
              assignment.subTasks.forEach { subTask ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = canToggleChecks) { onToggleSubTask(subTask.id) }
                    .padding(vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Checkbox(
                    checked = subTask.isCompleted,
                    onCheckedChange = if (canToggleChecks) {
                      { onToggleSubTask(subTask.id) }
                    } else null,
                    modifier = Modifier.size(28.dp)
                  )
                  Spacer(Modifier.width(6.dp))
                  Text(
                    text = subTask.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (subTask.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    color = if (subTask.isCompleted) {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                      MaterialTheme.colorScheme.onSurface
                    }
                  )
                }
              }
            } else {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable(enabled = canToggleChecks) { onToggleConfirmation() }
                  .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = assignment.isConfirmed,
                  onCheckedChange = if (canToggleChecks) {
                    { onToggleConfirmation() }
                  } else null,
                  modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = taskTitle,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        val displayNotes = assignment.notes.takeIf { note ->
          note.isNotBlank() &&
            !note.startsWith("MAIN assignment for ", ignoreCase = true) &&
            !note.startsWith("BACKUP assignment for ", ignoreCase = true) &&
            !note.startsWith("Indexed from ", ignoreCase = true)
        }
        if (displayNotes != null) {
          Text(
            text = displayNotes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Footer: Secondary (BACKUP) Volunteer on left, Role-gated Remind button on right
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          val backupLabel = if (isMainTier) {
            "Backup: ${assignment.backupAssigneeName.ifBlank { "Unassigned" }}"
          } else {
            "Secondary (Backup) Assignment"
          }
          Text(
            text = backupLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          if (canRemind) {
            var isRemindInFlight by remember(assignment.id, assignment.reminderScheduled) { mutableStateOf(false) }
            TextButton(
              onClick = {
                if (!isRemindInFlight) {
                  isRemindInFlight = true
                  onTriggerReminder()
                }
              },
              enabled = !isRemindInFlight,
              modifier = Modifier.height(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
              Spacer(Modifier.width(4.dp))
              Text(
                text = if (assignment.reminderScheduled) "Reminded" else "Remind",
                style = MaterialTheme.typography.labelSmall
              )
            }
          }
        }
      }
    }
  }
}

private fun parseServiceLocalDate(dateStr: String): LocalDate? {
  val clean = dateStr.trim()
  val formatters = listOf(
    DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("EEE MMM d, yyyy", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
  )
  for (fmt in formatters) {
    val parsed = runCatching { LocalDate.parse(clean, fmt) }.getOrNull()
    if (parsed != null) return parsed
  }
  return null
}

private data class ServiceDateOption(
  val formatted: String,
  val dayOfWeek: DayOfWeek
)

private fun sundaysAndWednesdaysInMonth(month: YearMonth): List<ServiceDateOption> {
  val formatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)
  val results = mutableListOf<ServiceDateOption>()
  for (day in 1..month.lengthOfMonth()) {
    val date = month.atDay(day)
    if (date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfWeek == DayOfWeek.WEDNESDAY) {
      results += ServiceDateOption(
        formatted = date.format(formatter),
        dayOfWeek = date.dayOfWeek
      )
    }
  }
  return results
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualAddTaskDialog(
  activeCongregation: Congregation,
  rosterMembers: List<UserProfileEntity>,
  onDismiss: () -> Unit,
  onSave: (
    dutyType: MinistryDutyType,
    customTaskTitle: String,
    tier: String,
    name: String,
    backupName: String,
    date: String,
    time: String,
    notes: String,
    subTasks: List<VolunteerSubTask>
  ) -> Unit
) {
  val quickDuties = listOf(
    MinistryDutyType.AV_SOUND_BOOTH,
    MinistryDutyType.ALTAR_SETUP,
    MinistryDutyType.GREETER,
    MinistryDutyType.USHERING
  )

  var selectedDuty by remember { mutableStateOf(MinistryDutyType.AV_SOUND_BOOTH) }
  var isOtherTask by remember { mutableStateOf(false) }
  var customTaskTitle by remember { mutableStateOf("") }

  var subTaskCount by remember { mutableStateOf(1) }
  var subTaskLabels by remember { mutableStateOf(listOf("AV")) }

  // Outlook-style multi-select volunteer lists for Main and Backup (Optional)
  var mainVolunteers by remember { mutableStateOf<List<String>>(emptyList()) }
  var mainVolunteerQuery by remember { mutableStateOf("") }
  var backupVolunteers by remember { mutableStateOf<List<String>>(emptyList()) }
  var backupVolunteerQuery by remember { mutableStateOf("") }

  // Smart Date & Month Selector Bar
  val currentMonth = remember { YearMonth.now() }
  val nextMonth = remember { currentMonth.plusMonths(1) }
  val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH) }
  var selectedMonthTab by remember { mutableStateOf(0) } // 0: Current Month, 1: Next Month
  var isCustomDateMode by remember { mutableStateOf(false) }

  val monthDates = remember(selectedMonthTab) {
    sundaysAndWednesdaysInMonth(if (selectedMonthTab == 0) currentMonth else nextMonth)
  }

  val defaultSundayTime = activeCongregation.sundayServiceTime.ifBlank { "10:30 AM" }
  val defaultWednesdayTime = activeCongregation.wednesdayServiceTime.ifBlank { "7:30 PM" }

  val initialDate = remember {
    val today = LocalDate.now()
    sundaysAndWednesdaysInMonth(currentMonth).firstOrNull { opt ->
      val parsed = runCatching {
        LocalDate.parse(opt.formatted, DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH))
      }.getOrNull()
      parsed != null && !parsed.isBefore(today)
    } ?: sundaysAndWednesdaysInMonth(currentMonth).first()
  }
  var serviceDate by remember { mutableStateOf(initialDate.formatted) }
  var serviceTime by remember {
    mutableStateOf(if (initialDate.dayOfWeek == DayOfWeek.WEDNESDAY) defaultWednesdayTime else defaultSundayTime)
  }
  var isCustomTimeMode by remember { mutableStateOf(false) }
  var notes by remember { mutableStateOf("") }

  fun syncSubTaskDefaults(duty: MinistryDutyType, isOther: Boolean, customTitle: String) {
    if (!isOther && duty == MinistryDutyType.ALTAR_SETUP) {
      subTaskCount = 2
      subTaskLabels = listOf("Setup", "Takedown")
    } else {
      subTaskCount = 1
      val label = if (isOther) customTitle.ifBlank { "Task Item 1" } else duty.displayName
      subTaskLabels = listOf(label)
    }
  }

  fun updateSubTaskCount(newCount: Int) {
    val clamped = newCount.coerceIn(1, 6)
    subTaskCount = clamped
    val updated = (0 until clamped).map { idx ->
      subTaskLabels.getOrNull(idx) ?: "Item ${idx + 1}"
    }
    subTaskLabels = updated
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Add Volunteer Task")
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // 1. Task Selection (4 presets + Other)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Task / Duty *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            quickDuties.forEach { duty ->
              FilterChip(
                selected = !isOtherTask && selectedDuty == duty,
                onClick = {
                  isOtherTask = false
                  selectedDuty = duty
                  syncSubTaskDefaults(duty, isOther = false, customTitle = "")
                },
                label = { Text(duty.displayName, style = MaterialTheme.typography.labelSmall) }
              )
            }
            FilterChip(
              selected = isOtherTask,
              onClick = {
                isOtherTask = true
                syncSubTaskDefaults(selectedDuty, isOther = true, customTitle = customTaskTitle)
              },
              label = { Text("Other", style = MaterialTheme.typography.labelSmall) }
            )
          }

          if (isOtherTask) {
            OutlinedTextField(
              value = customTaskTitle,
              onValueChange = {
                customTaskTitle = it
                if (subTaskCount == 1) {
                  subTaskLabels = listOf(it.ifBlank { "Task Item 1" })
                }
              },
              label = { Text("Custom Task Title *") },
              placeholder = { Text("e.g. Choir Mic Setup, Fellowship Coffee") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        // 2. Sub-Task Count & Checklist Configuration
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Tasks:",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f)
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              OutlinedButton(
                onClick = { updateSubTaskCount(subTaskCount - 1) },
                enabled = subTaskCount > 1,
                modifier = Modifier.height(32.dp)
              ) {
                Text("-")
              }
              Text(
                text = "$subTaskCount",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
              )
              OutlinedButton(
                onClick = { updateSubTaskCount(subTaskCount + 1) },
                enabled = subTaskCount < 6,
                modifier = Modifier.height(32.dp)
              ) {
                Text("+")
              }
            }
          }

          subTaskLabels.forEachIndexed { idx, label ->
            OutlinedTextField(
              value = label,
              onValueChange = { newVal ->
                subTaskLabels = subTaskLabels.toMutableList().also { it[idx] = newVal }
              },
              label = { Text("Item ${idx + 1}") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        // 3. Outlook-Style Multi-Select Chips for Main & Backup (Optional)
        OutlookMultiChipInput(
          label = "Main *",
          placeholder = "Name",
          selectedChips = mainVolunteers,
          query = mainVolunteerQuery,
          rosterMembers = rosterMembers,
          emailMode = false,
          onQueryChange = { mainVolunteerQuery = it },
          onAddChip = { name ->
            if (mainVolunteers.none { it.equals(name, ignoreCase = true) }) {
              mainVolunteers = mainVolunteers + name
            }
            mainVolunteerQuery = ""
          },
          onRemoveChip = { name ->
            mainVolunteers = mainVolunteers.filterNot { it == name }
          }
        )

        OutlookMultiChipInput(
          label = "Backup (Optional)",
          placeholder = "Name",
          selectedChips = backupVolunteers,
          query = backupVolunteerQuery,
          rosterMembers = rosterMembers,
          emailMode = false,
          onQueryChange = { backupVolunteerQuery = it },
          onAddChip = { name ->
            if (backupVolunteers.none { it.equals(name, ignoreCase = true) }) {
              backupVolunteers = backupVolunteers + name
            }
            backupVolunteerQuery = ""
          },
          onRemoveChip = { name ->
            backupVolunteers = backupVolunteers.filterNot { it == name }
          }
        )

        // 4. Smart Date & Month Selector Bar (Month Tabs on top, Custom Date at end of date grid)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Service Date *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = !isCustomDateMode && selectedMonthTab == 0,
              onClick = {
                isCustomDateMode = false
                selectedMonthTab = 0
              },
              label = { Text(currentMonth.format(monthFormatter), style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
              selected = !isCustomDateMode && selectedMonthTab == 1,
              onClick = {
                isCustomDateMode = false
                selectedMonthTab = 1
              },
              label = { Text(nextMonth.format(monthFormatter), style = MaterialTheme.typography.labelSmall) }
            )
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            monthDates.forEach { dateOpt ->
              val isSelected = !isCustomDateMode && serviceDate == dateOpt.formatted
              FilterChip(
                selected = isSelected,
                onClick = {
                  isCustomDateMode = false
                  serviceDate = dateOpt.formatted
                  isCustomTimeMode = false
                  serviceTime = if (dateOpt.dayOfWeek == DayOfWeek.WEDNESDAY) {
                    defaultWednesdayTime
                  } else {
                    defaultSundayTime
                  }
                },
                label = { Text(dateOpt.formatted, style = MaterialTheme.typography.labelSmall) }
              )
            }
            FilterChip(
              selected = isCustomDateMode,
              onClick = { isCustomDateMode = !isCustomDateMode },
              label = { Text("Custom Date", style = MaterialTheme.typography.labelSmall) }
            )
          }

          if (isCustomDateMode) {
            OutlinedTextField(
              value = serviceDate,
              onValueChange = { serviceDate = it },
              label = { Text("Custom Service Date") },
              placeholder = { Text("e.g. Thu 24 Dec 2026") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        // 5. Clean Service Time Selector (Preset pills + toggleable Custom Time)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Service Time",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = !isCustomTimeMode && serviceTime == defaultSundayTime,
              onClick = {
                isCustomTimeMode = false
                serviceTime = defaultSundayTime
              },
              label = { Text("Sun $defaultSundayTime", style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
              selected = !isCustomTimeMode && serviceTime == defaultWednesdayTime,
              onClick = {
                isCustomTimeMode = false
                serviceTime = defaultWednesdayTime
              },
              label = { Text("Wed $defaultWednesdayTime", style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
              selected = isCustomTimeMode,
              onClick = { isCustomTimeMode = !isCustomTimeMode },
              label = { Text("Custom Time", style = MaterialTheme.typography.labelSmall) }
            )
          }

          if (isCustomTimeMode) {
            OutlinedTextField(
              value = serviceTime,
              onValueChange = { serviceTime = it },
              label = { Text("Custom Service Time") },
              placeholder = { Text("e.g. 9:00 AM") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        // 7. Notes / Instructions
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Notes / Instructions (Optional)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = { Text("e.g. Arrive 20 mins prior for microphone sound check") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      val effectiveMains = if (mainVolunteerQuery.isNotBlank()) {
        (mainVolunteers + mainVolunteerQuery.trim()).distinct()
      } else {
        mainVolunteers
      }
      val effectiveBackups = if (backupVolunteerQuery.isNotBlank()) {
        (backupVolunteers + backupVolunteerQuery.trim()).distinct()
      } else {
        backupVolunteers
      }
      val canSave = effectiveMains.isNotEmpty() &&
        serviceDate.isNotBlank() &&
        (!isOtherTask || customTaskTitle.isNotBlank())
      Button(
        onClick = {
          if (canSave) {
            val builtSubTasks = subTaskLabels.mapIndexed { idx, lbl ->
              VolunteerSubTask(
                id = "sub_${idx + 1}_${UUID.randomUUID().toString().take(4)}",
                label = lbl.trim().ifBlank { "Item ${idx + 1}" },
                isCompleted = false
              )
            }
            onSave(
              selectedDuty,
              if (isOtherTask) customTaskTitle.trim() else "",
              "MAIN",
              effectiveMains.joinToString(", "),
              effectiveBackups.joinToString(", "),
              serviceDate.trim(),
              serviceTime.trim(),
              notes.trim(),
              builtSubTasks
            )
          }
        },
        enabled = canSave
      ) {
        Text("Save Task")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OutlookMultiChipInput(
  label: String,
  placeholder: String,
  selectedChips: List<String>,
  query: String,
  rosterMembers: List<UserProfileEntity>,
  emailMode: Boolean = false,
  onQueryChange: (String) -> Unit,
  onAddChip: (String) -> Unit,
  onRemoveChip: (String) -> Unit
) {
  val filteredMembers = remember(query, rosterMembers, selectedChips, emailMode) {
    val q = query.trim().lowercase()
    rosterMembers.filter { member ->
      val value = if (emailMode) member.email else member.fullName
      val notYetSelected = selectedChips.none { it.equals(value, ignoreCase = true) || (emailMode && it.contains(member.email, ignoreCase = true)) }
      val validForMode = if (emailMode) member.email.contains("@") && !member.email.endsWith("@volunteer.local") else member.fullName.isNotBlank()
      val matchesQuery = q.isBlank() || member.fullName.lowercase().contains(q) || member.email.lowercase().contains(q)
      notYetSelected && validForMode && matchesQuery
    }
  }
  val trimmed = query.trim().trimEnd(',')
  val hasExactMatch = remember(trimmed, rosterMembers, emailMode) {
    trimmed.isNotBlank() && rosterMembers.any {
      if (emailMode) it.email.equals(trimmed, ignoreCase = true) else it.fullName.equals(trimmed, ignoreCase = true)
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold
    )

    Surface(
      shape = RoundedCornerShape(6.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth()
    ) {
      FlowRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        selectedChips.forEach { chip ->
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
            ) {
              Text(
                text = chip,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove $chip",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                  .size(14.dp)
                  .clickable { onRemoveChip(chip) }
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(1f, fill = false)
            .padding(vertical = 4.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          if (query.isEmpty() && selectedChips.isEmpty()) {
            Text(
              text = placeholder,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
          BasicTextField(
            value = query,
            onValueChange = { newVal ->
              if (newVal.endsWith(",") || newVal.endsWith("\n")) {
                val candidate = newVal.trim().trimEnd(',', '\n').trim()
                if (candidate.isNotBlank() && (!emailMode || candidate.contains("@"))) {
                  onAddChip(candidate)
                } else {
                  onQueryChange("")
                }
              } else {
                onQueryChange(newVal)
              }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
              onDone = {
                if (trimmed.isNotBlank() && (!emailMode || trimmed.contains("@"))) {
                  onAddChip(trimmed)
                }
              }
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    if (query.isNotBlank() && (filteredMembers.isNotEmpty() || trimmed.isNotBlank())) {
      ElevatedCard(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          filteredMembers.take(5).forEach { member ->
            val chipValue = if (emailMode) "${member.fullName} <${member.email}>" else member.fullName
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddChip(chipValue) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Column {
                Text(
                  text = member.fullName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium
                )
                if (member.email.isNotBlank() && !member.email.endsWith("@volunteer.local")) {
                  Text(
                    text = member.email,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          if (trimmed.isNotBlank() && !hasExactMatch && (!emailMode || trimmed.contains("@"))) {
            HorizontalDivider()
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddChip(trimmed) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Text(
                text = if (emailMode) "+ Add \"$trimmed\"" else "+ Add \"$trimmed\" as new volunteer",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }
  }
}

private enum class PdfImportStage {
  PREVIEW,
  EDIT_LIST,
  EDIT_CARD
}

@Composable
private fun PdfScheduleImportDialog(
  activeUser: UserProfileEntity,
  activeCongregation: Congregation,
  rosterMembers: List<UserProfileEntity>,
  onDismiss: () -> Unit,
  onCommitParsedCards: (fileName: String, cards: List<VolunteerAssignmentEntity>) -> Unit
) {
  val context = LocalContext.current
  val sundayTime = activeCongregation.sundayServiceTime.ifBlank { "10:30 AM" }
  val wednesdayTime = activeCongregation.wednesdayServiceTime.ifBlank { "7:30 PM" }

  var selectedUri by remember { mutableStateOf<Uri?>(null) }
  var fileName by remember { mutableStateOf("Monthly_Volunteer_Schedule.pdf") }
  var stage by remember { mutableStateOf(PdfImportStage.PREVIEW) }

  var stagedCards by remember {
    mutableStateOf(
      PdfScheduleParser.parseToUnifiedCards(
        rawText = PdfScheduleParser.samplePreviewScheduleText(),
        pdfFileName = "Monthly_Volunteer_Schedule.pdf",
        congregationId = activeCongregation.id,
        roster = rosterMembers,
        activeUser = activeUser,
        sundayTime = sundayTime,
        wednesdayTime = wednesdayTime
      )
    )
  }
  var workingEditCards by remember { mutableStateOf(stagedCards) }
  var editingCardIndex by remember { mutableStateOf<Int?>(null) }

  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      selectedUri = uri
      val path = uri.path.orEmpty()
      val resolvedName = path.substringAfterLast("/").ifBlank { "uploaded_schedule.pdf" }
      fileName = resolvedName
      val extracted = PdfScheduleParser.extractTextFromPdfUri(context, uri, rosterMembers)
      val parsedCards = PdfScheduleParser.parseToUnifiedCards(
        rawText = extracted,
        pdfFileName = resolvedName,
        congregationId = activeCongregation.id,
        roster = rosterMembers,
        activeUser = activeUser,
        sundayTime = sundayTime,
        wednesdayTime = wednesdayTime
      )
      stagedCards = parsedCards
      workingEditCards = parsedCards
      stage = PdfImportStage.PREVIEW
    }
  }

  val gridRows = remember(stagedCards) {
    PdfScheduleParser.buildStructuredGridRows(stagedCards)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
          when (stage) {
            PdfImportStage.PREVIEW -> "Parse PDF Schedule"
            PdfImportStage.EDIT_LIST -> "Edit Parsed Cards (${workingEditCards.size})"
            PdfImportStage.EDIT_CARD -> "Edit Task Card"
          }
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        when (stage) {
          PdfImportStage.PREVIEW -> {
            OutlinedButton(
              onClick = { filePickerLauncher.launch("application/pdf") },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(if (selectedUri != null) "File: $fileName" else "Choose PDF File")
            }

            // Formal Structured Grid Table Preview
            Surface(
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = if (selectedUri != null) fileName.substringBeforeLast(".") else "Monthly Volunteer Schedule",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "${gridRows.firstOrNull()?.date.orEmpty()} – ${gridRows.lastOrNull()?.date.orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                  // Table Header Row
                  Row(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                  ) {
                    ScheduleTableHeaderCell("DATE", 105.dp)
                    ScheduleTableHeaderCell("AV", 125.dp)
                    ScheduleTableHeaderCell("ALTAR SETUP/TAKEDOWN", 145.dp)
                    ScheduleTableHeaderCell("GREETER", 135.dp)
                    ScheduleTableHeaderCell("USHER", 125.dp)
                  }
                  gridRows.forEach { row ->
                    Row {
                      ScheduleTableBodyCell(main = row.date, backup = "", width = 105.dp, isDate = true)
                      ScheduleTableBodyCell(main = row.avMain, backup = row.avBackup, width = 125.dp)
                      ScheduleTableBodyCell(main = row.altarMain, backup = row.altarBackup, width = 145.dp)
                      ScheduleTableBodyCell(main = row.greeterMain, backup = row.greeterBackup, width = 135.dp)
                      ScheduleTableBodyCell(main = row.usherMain, backup = row.usherBackup, width = 125.dp)
                    }
                  }
                }
              }
            }
          }

          PdfImportStage.EDIT_LIST -> {
            Text(
              text = "Tap any card below to edit its details before committing:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            workingEditCards.forEachIndexed { idx, card ->
              OutlinedCard(
                onClick = {
                  editingCardIndex = idx
                  stage = PdfImportStage.EDIT_CARD
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = card.customTaskTitle.ifBlank { card.dutyType.displayName },
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "${card.serviceDate} • ${card.serviceTime}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                  Text(
                    text = "Main: ${card.assigneeName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = "Backup: ${card.backupAssigneeName.ifBlank { "Unassigned" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          PdfImportStage.EDIT_CARD -> {
            val idx = editingCardIndex
            val card = idx?.let { workingEditCards.getOrNull(it) }
            if (idx != null && card != null) {
              var editTitle by remember(idx) {
                mutableStateOf(card.customTaskTitle.ifBlank { card.dutyType.displayName })
              }
              var editDate by remember(idx) { mutableStateOf(card.serviceDate) }
              var editTime by remember(idx) { mutableStateOf(card.serviceTime) }
              var editMainList by remember(idx) {
                mutableStateOf(card.assigneeName.split(",").map { it.trim() }.filter { it.isNotBlank() })
              }
              var editMainQuery by remember(idx) { mutableStateOf("") }
              var editBackupList by remember(idx) {
                mutableStateOf(card.backupAssigneeName.split(",").map { it.trim() }.filter { it.isNotBlank() })
              }
              var editBackupQuery by remember(idx) { mutableStateOf("") }
              var editSubTasksText by remember(idx) {
                mutableStateOf(card.subTasks.joinToString(", ") { it.label })
              }

              OutlinedTextField(
                value = editTitle,
                onValueChange = { editTitle = it },
                label = { Text("Task Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = editDate,
                  onValueChange = { editDate = it },
                  label = { Text("Date") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                  value = editTime,
                  onValueChange = { editTime = it },
                  label = { Text("Time") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
              }
              OutlookMultiChipInput(
                label = "Main Volunteers *",
                placeholder = "Name",
                selectedChips = editMainList,
                query = editMainQuery,
                rosterMembers = rosterMembers,
                emailMode = false,
                onQueryChange = { editMainQuery = it },
                onAddChip = { name ->
                  if (editMainList.none { it.equals(name, ignoreCase = true) }) {
                    editMainList = editMainList + name
                  }
                  editMainQuery = ""
                },
                onRemoveChip = { name -> editMainList = editMainList.filterNot { it == name } }
              )
              OutlookMultiChipInput(
                label = "Backup Volunteers (Optional)",
                placeholder = "Name",
                selectedChips = editBackupList,
                query = editBackupQuery,
                rosterMembers = rosterMembers,
                emailMode = false,
                onQueryChange = { editBackupQuery = it },
                onAddChip = { name ->
                  if (editBackupList.none { it.equals(name, ignoreCase = true) }) {
                    editBackupList = editBackupList + name
                  }
                  editBackupQuery = ""
                },
                onRemoveChip = { name -> editBackupList = editBackupList.filterNot { it == name } }
              )
              OutlinedTextField(
                value = editSubTasksText,
                onValueChange = { editSubTasksText = it },
                label = { Text("Sub-task Items (comma-separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )

              Button(
                onClick = {
                  val finalMains = if (editMainQuery.isNotBlank()) (editMainList + editMainQuery.trim()).distinct() else editMainList
                  val finalBackups = if (editBackupQuery.isNotBlank()) (editBackupList + editBackupQuery.trim()).distinct() else editBackupList
                  val updatedSubTasks = editSubTasksText.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .mapIndexed { sIdx, lbl ->
                      VolunteerSubTask(
                        id = card.subTasks.getOrNull(sIdx)?.id ?: "sub_${sIdx + 1}_${UUID.randomUUID().toString().take(4)}",
                        label = lbl,
                        isCompleted = false
                      )
                    }
                  val updatedCard = card.copy(
                    customTaskTitle = if (editTitle.trim() == card.dutyType.displayName) "" else editTitle.trim(),
                    serviceDate = editDate.trim(),
                    serviceTime = editTime.trim(),
                    assigneeName = finalMains.joinToString(", "),
                    backupAssigneeName = finalBackups.joinToString(", "),
                    subTasks = updatedSubTasks
                  )
                  workingEditCards = workingEditCards.toMutableList().also { it[idx] = updatedCard }
                  editingCardIndex = null
                  stage = PdfImportStage.EDIT_LIST
                },
                enabled = editMainList.isNotEmpty() || editMainQuery.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Save Card")
              }
            }
          }
        }
      }
    },
    confirmButton = {
      when (stage) {
        PdfImportStage.PREVIEW -> {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                workingEditCards = stagedCards
                stage = PdfImportStage.EDIT_LIST
              },
              enabled = selectedUri != null
            ) {
              Text("Edit Cards")
            }
            Button(
              onClick = {
                if (selectedUri != null) {
                  onCommitParsedCards(fileName, stagedCards)
                }
              },
              enabled = selectedUri != null
            ) {
              Text("Confirm & Upload")
            }
          }
        }
        PdfImportStage.EDIT_LIST -> {
          Button(
            onClick = {
              stagedCards = workingEditCards
              stage = PdfImportStage.PREVIEW
            }
          ) {
            Text("Save Edit Session")
          }
        }
        PdfImportStage.EDIT_CARD -> Unit
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          when (stage) {
            PdfImportStage.PREVIEW -> onDismiss()
            PdfImportStage.EDIT_LIST -> stage = PdfImportStage.PREVIEW
            PdfImportStage.EDIT_CARD -> {
              editingCardIndex = null
              stage = PdfImportStage.EDIT_LIST
            }
          }
        }
      ) {
        Text(if (stage == PdfImportStage.PREVIEW) "Cancel" else "Back")
      }
    }
  )
}

@Composable
private fun ScheduleTableHeaderCell(
  text: String,
  width: androidx.compose.ui.unit.Dp
) {
  Box(
    modifier = Modifier
      .width(width)
      .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
      .padding(horizontal = 6.dp, vertical = 6.dp)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun ScheduleTableBodyCell(
  main: String,
  backup: String,
  width: androidx.compose.ui.unit.Dp,
  isDate: Boolean = false
) {
  Column(
    modifier = Modifier
      .width(width)
      .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
      .padding(horizontal = 6.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    if (isDate) {
      Text(
        text = main,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold
      )
    } else {
      Text(
        text = "MAIN: $main",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "BACKUP: $backup",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun EmailScheduleDialog(
  activeCongregation: Congregation,
  rosterMembers: List<UserProfileEntity>,
  volunteerAssignments: List<VolunteerAssignmentEntity>,
  onDismiss: () -> Unit,
  onSendEmail: (extraEmails: String, subject: String, message: String, fileName: String, summary: String) -> Unit
) {
  var selectedRecipients by remember { mutableStateOf<List<String>>(emptyList()) }
  var recipientQuery by remember { mutableStateOf("") }
  var subject by remember {
    mutableStateOf("Volunteer Schedule • ${activeCongregation.displayName.ifBlank { "Congregation" }}")
  }
  var customMessage by remember {
    mutableStateOf("Dear Brothers and Sisters,\n\nPlease find the upcoming volunteer assignments for our congregation below. Please review your scheduled duties (MAIN and BACKUP) and confirm your availability in the NAC Portal app.\n\nWith warm regards,")
  }

  val scheduleSummary = remember(volunteerAssignments) {
    buildString {
      append("VOLUNTEER SCHEDULE SUMMARY\n")
      append("=========================================\n")
      val byDate = volunteerAssignments.groupBy { it.serviceDate }
      byDate.forEach { (date, duties) ->
        append("Service Date: $date\n")
        duties.forEach { d ->
          append("  - ${d.dutyType.displayName} [${d.assignmentTier}]: ${d.assigneeName}\n")
        }
        append("\n")
      }
    }
  }

  fun extractEmailAddress(token: String): String {
    val start = token.indexOf('<')
    val end = token.indexOf('>')
    return if (start in 0 until end) token.substring(start + 1, end).trim() else token.trim()
  }

  val resolvedEmails = remember(selectedRecipients, recipientQuery) {
    val combined = if (recipientQuery.isNotBlank() && recipientQuery.contains("@")) {
      selectedRecipients + recipientQuery.trim()
    } else {
      selectedRecipients
    }
    combined.map { extractEmailAddress(it) }.filter { it.contains("@") }.distinct()
  }
  val canSend = resolvedEmails.isNotEmpty() && subject.isNotBlank() && customMessage.isNotBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Email Schedule")
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlookMultiChipInput(
          label = "To *",
          placeholder = "Search member or type email...",
          selectedChips = selectedRecipients,
          query = recipientQuery,
          rosterMembers = rosterMembers,
          emailMode = true,
          onQueryChange = { recipientQuery = it },
          onAddChip = { chip ->
            if (selectedRecipients.none { it.equals(chip, ignoreCase = true) }) {
              selectedRecipients = selectedRecipients + chip
            }
            recipientQuery = ""
          },
          onRemoveChip = { chip ->
            selectedRecipients = selectedRecipients.filterNot { it == chip }
          }
        )

        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Subject *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = customMessage,
          onValueChange = { customMessage = it },
          label = { Text("Body *") },
          minLines = 4,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (canSend) {
            onSendEmail(
              resolvedEmails.joinToString(", "),
              subject.trim(),
              customMessage.trim(),
              "Volunteer_Schedule.pdf",
              scheduleSummary
            )
          }
        },
        enabled = canSend
      ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Send Email")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
