package com.example.parishsync.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parishsync.data.local.HymnSelectionEntity
import com.example.parishsync.data.local.VolunteerAssignmentEntity
import com.example.parishsync.data.model.MinistryDutyType

@Composable
fun RosterAndChoirScreen(
  activeUserName: String,
  volunteerAssignments: List<VolunteerAssignmentEntity>,
  hymnSelections: List<HymnSelectionEntity>,
  onToggleConfirmation: (VolunteerAssignmentEntity) -> Unit,
  onTriggerReminder: (VolunteerAssignmentEntity) -> Unit,
  onAddManualAssignment: (
    dutyType: MinistryDutyType,
    assigneeName: String,
    serviceDate: String,
    serviceTime: String,
    notes: String
  ) -> Unit,
  initialSubTab: Int = 0,
  modifier: Modifier = Modifier
) {
  var selectedSubTab by rememberSaveable { mutableIntStateOf(initialSubTab) }

  Column(modifier = modifier.fillMaxSize()) {
    TabRow(selectedTabIndex = selectedSubTab) {
      Tab(
        selected = selectedSubTab == 0,
        onClick = { selectedSubTab = 0 },
        text = { Text("Volunteer Roster (${volunteerAssignments.size})") },
        icon = { Icon(Icons.Default.EventAvailable, contentDescription = null) }
      )
      Tab(
        selected = selectedSubTab == 1,
        onClick = { selectedSubTab = 1 },
        text = { Text("Choir & Hymns (${hymnSelections.size})") },
        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) }
      )
    }

    when (selectedSubTab) {
      0 -> VolunteerRosterContent(
        activeUserName = activeUserName,
        assignments = volunteerAssignments,
        onToggleConfirmation = onToggleConfirmation,
        onTriggerReminder = onTriggerReminder,
        onAddManualAssignment = onAddManualAssignment
      )
      1 -> ChoirHymnHubContent(
        hymns = hymnSelections
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolunteerRosterContent(
  activeUserName: String,
  assignments: List<VolunteerAssignmentEntity>,
  onToggleConfirmation: (VolunteerAssignmentEntity) -> Unit,
  onTriggerReminder: (VolunteerAssignmentEntity) -> Unit,
  onAddManualAssignment: (
    dutyType: MinistryDutyType,
    assigneeName: String,
    serviceDate: String,
    serviceTime: String,
    notes: String
  ) -> Unit
) {
  var selectedDutyFilter by rememberSaveable { mutableStateOf<MinistryDutyType?>(null) }
  var onlyMyDuties by rememberSaveable { mutableStateOf(false) }
  var showAddDutyDialog by rememberSaveable { mutableStateOf(false) }

  val filteredList = remember(assignments, selectedDutyFilter, onlyMyDuties, activeUserName) {
    assignments.filter { item ->
      val matchesDuty = selectedDutyFilter == null || item.dutyType == selectedDutyFilter
      val matchesUser = !onlyMyDuties ||
        item.isLocalUserMatch ||
        item.assigneeName.contains(activeUserName, ignoreCase = true) ||
        activeUserName.contains(item.assigneeName.replace("Pr.", "").replace("Dc.", "").trim(), ignoreCase = true)
      matchesDuty && matchesUser
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Auto-Populated Volunteer Commitments",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "AV • Altar Setup/Takedown • Greeter • Usher (MAIN & BACKUP)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Button(onClick = { showAddDutyDialog = true }) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Duty")
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            FilterChip(
              selected = selectedDutyFilter == null && !onlyMyDuties,
              onClick = {
                selectedDutyFilter = null
                onlyMyDuties = false
              },
              label = { Text("All Duties") }
            )
            FilterChip(
              selected = onlyMyDuties,
              onClick = { onlyMyDuties = !onlyMyDuties },
              label = { Text("My Commitments ($activeUserName)") }
            )
            listOf(
              MinistryDutyType.AV_SOUND_BOOTH,
              MinistryDutyType.ALTAR_SETUP,
              MinistryDutyType.GREETER,
              MinistryDutyType.USHERING
            ).forEach { dutyType ->
              FilterChip(
                selected = selectedDutyFilter == dutyType,
                onClick = {
                  selectedDutyFilter = if (selectedDutyFilter == dutyType) null else dutyType
                },
                label = { Text(dutyType.displayName) }
              )
            }
          }
        }
      }
    }

    if (filteredList.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "No Volunteer Schedule Uploaded Yet",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "When your Rector or delegated Volunteer Scheduler uploads the monthly Volunteer Schedule PDF (e.g., November 2026 Volunteer Schedule), all MAIN and BACKUP commitments will auto-populate here.",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }

    items(filteredList, key = { it.id }) { assignment ->
      val isUserMatch = assignment.isLocalUserMatch ||
        assignment.assigneeName.equals(activeUserName, ignoreCase = true) ||
        (activeUserName.isNotBlank() && assignment.assigneeName.contains(activeUserName, ignoreCase = true))

      VolunteerAssignmentCard(
        assignment = assignment,
        isCurrentUser = isUserMatch,
        onToggleConfirmation = { onToggleConfirmation(assignment) },
        onTriggerReminder = { onTriggerReminder(assignment) }
      )
    }
  }

  if (showAddDutyDialog) {
    AddVolunteerDutyDialog(
      defaultAssignee = activeUserName,
      onDismiss = { showAddDutyDialog = false },
      onAdd = { duty, assignee, date, time, notes ->
        onAddManualAssignment(duty, assignee, date, time, notes)
        showAddDutyDialog = false
      }
    )
  }
}

@Composable
private fun VolunteerAssignmentCard(
  assignment: VolunteerAssignmentEntity,
  isCurrentUser: Boolean,
  onToggleConfirmation: () -> Unit,
  onTriggerReminder: () -> Unit
) {
  val icon = when (assignment.dutyType) {
    MinistryDutyType.ALTAR_SETUP -> Icons.Default.Spa
    MinistryDutyType.GREETER -> Icons.Default.Handshake
    MinistryDutyType.USHERING -> Icons.Default.Groups
    MinistryDutyType.AV_SOUND_BOOTH -> Icons.Default.SettingsVoice
    MinistryDutyType.SUNDAY_SCHOOL -> Icons.Default.School
    MinistryDutyType.COFFEE_HOUR -> Icons.Default.Coffee
    MinistryDutyType.CLEANING -> Icons.Default.CleaningServices
  }

  val isMainTier = assignment.assignmentTier.equals("MAIN", ignoreCase = true)

  OutlinedCard(
    border = BorderStroke(
      width = if (isCurrentUser) 2.dp else 1.dp,
      color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    ),
    colors = CardDefaults.outlinedCardColors(
      containerColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
      } else {
        MaterialTheme.colorScheme.surface
      }
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = assignment.dutyType.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Surface(
                color = if (isMainTier) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.tertiaryContainer
                },
                shape = MaterialTheme.shapes.small
              ) {
                Text(
                  text = assignment.assignmentTier.uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "${assignment.serviceDate} • ${assignment.serviceTime}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        if (isCurrentUser) {
          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.small
          ) {
            Text(
              text = "YOUR COMMITMENT",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "${assignment.assignmentTier}: ${assignment.assigneeName}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold
        )
        if (!assignment.sourcePdfName.isNullOrBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PictureAsPdf,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.secondary
            )
            Text(
              text = assignment.sourcePdfName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary
            )
          }
        }
      }

      if (assignment.notes.isNotBlank()) {
        Text(
          text = assignment.notes,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      HorizontalDivider()

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(onClick = onTriggerReminder) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (assignment.reminderScheduled) "Send Reminder Now" else "Set Reminder")
        }

        Button(onClick = onToggleConfirmation) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (assignment.isConfirmed) "Confirmed ✓" else "Confirm Duty")
        }
      }
    }
  }
}

@Composable
private fun ChoirHymnHubContent(
  hymns: List<HymnSelectionEntity>
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.LibraryMusic,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
              text = "Choir & Liturgical Hymn Selection Hub",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onTertiaryContainer
            )
          }
          Text(
            text = "Upcoming service musical selections, SATB organ keys, and choirmaster rehearsal notes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
          )
        }
      }
    }

    if (hymns.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "No choir hymn selections posted yet for this congregation.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
          )
        }
      }
    }

    items(hymns, key = { it.id }) { hymn ->
      ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = MaterialTheme.shapes.small
            ) {
              Text(
                text = "${hymn.liturgicalPart.uppercase()} • ${hymn.hymnNumber}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = MaterialTheme.shapes.small
            ) {
              Text(
                text = "Key: ${hymn.musicalKey}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = hymn.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          Text(
            text = "${hymn.composerOrTune} • ${hymn.liturgicalSunday} (${hymn.serviceDate})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddVolunteerDutyDialog(
  defaultAssignee: String,
  onDismiss: () -> Unit,
  onAdd: (
    dutyType: MinistryDutyType,
    assigneeName: String,
    serviceDate: String,
    serviceTime: String,
    notes: String
  ) -> Unit
) {
  var selectedDuty by rememberSaveable { mutableStateOf(MinistryDutyType.USHERING) }
  var assigneeName by rememberSaveable { mutableStateOf(defaultAssignee) }
  var serviceDate by rememberSaveable { mutableStateOf("Sun 1 Nov 2026") }
  var serviceTime by rememberSaveable { mutableStateOf("10:00 AM") }
  var notes by rememberSaveable { mutableStateOf("Manual Roster Entry") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Volunteer Duty") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          MinistryDutyType.entries.forEach { duty ->
            AssistChip(
              onClick = { selectedDuty = duty },
              label = { Text(duty.displayName, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }
        Text(
          text = "Selected: ${selectedDuty.displayName}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
          value = assigneeName,
          onValueChange = { assigneeName = it },
          label = { Text("Volunteer Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = serviceDate,
          onValueChange = { serviceDate = it },
          label = { Text("Service Date") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = serviceTime,
          onValueChange = { serviceTime = it },
          label = { Text("Call Time") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Duty Notes") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (assigneeName.isNotBlank()) {
            onAdd(selectedDuty, assigneeName, serviceDate, serviceTime, notes)
          }
        }
      ) {
        Text("Save Commitment")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
