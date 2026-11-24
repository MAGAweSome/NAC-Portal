package com.example.parishsync.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TaskAlt
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parishsync.R
import com.example.parishsync.data.local.PdfScheduleJobEntity
import com.example.parishsync.data.local.PrayerRequestEntity
import com.example.parishsync.data.local.SmallGroupEntity
import com.example.parishsync.data.local.UserProfileEntity
import com.example.parishsync.data.model.AppRole
import com.example.parishsync.data.model.Congregation
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.District
import com.example.parishsync.data.model.ParishPolicy
import com.example.parishsync.data.model.RequestStatus
import com.example.parishsync.data.model.UserRole
import com.example.parishsync.data.remote.MailtrapConfig
import com.example.parishsync.data.remote.MailtrapEmailService
import com.example.parishsync.data.remote.SupabaseConfig
import com.example.parishsync.workers.PdfScheduleParser

@Composable
fun MinisterAndAdminScreen(
  activeUser: UserProfileEntity,
  activeCongregation: Congregation,
  activeRole: UserRole,
  activeDelegatedRole: DelegatedRole,
  allUsers: List<UserProfileEntity>,
  rosterMembers: List<UserProfileEntity>,
  smallGroups: List<SmallGroupEntity>,
  prayerRequests: List<PrayerRequestEntity>,
  pdfJobs: List<PdfScheduleJobEntity>,
  supabaseConfig: SupabaseConfig,
  syncStatusMessage: String,
  mailtrapConfig: MailtrapConfig,
  mailtrapStatusMessage: String,
  congregations: List<Congregation> = emptyList(),
  districts: List<District> = emptyList(),
  onCreateCongregation: (name: String, address: String, serviceTimes: String, rectorName: String, sundayServiceTime: String, wednesdayServiceTime: String, districtId: String) -> Unit = { _, _, _, _, _, _, _ -> },
  onUpdateCongregationServiceTimes: (congregationId: String, sundayServiceTime: String, wednesdayServiceTime: String) -> Unit = { _, _, _ -> },
  onCreateOrUpdateDistrict: (id: String, name: String, regionCode: String) -> Unit = { _, _, _ -> },
  onDeleteDistrict: (districtId: String) -> Unit = {},
  onMoveCongregationToDistrict: (congregationId: String, newDistrictId: String) -> Unit = { _, _ -> },
  onDeleteCongregation: (congregationId: String) -> Unit = {},
  onAdminUpdateAndTransferUser: (
    targetUser: UserProfileEntity,
    fullName: String,
    email: String,
    preferredMinistries: String,
    roles: Set<AppRole>,
    newCongregationId: String
  ) -> Unit = { _, _, _, _, _, _ -> },
  onRunPdfScheduleIndexer: (pdfFileName: String, pdfUri: Uri?, customScheduleText: String?) -> Unit,
  onEmailSchedulePdfToCongregation: (
    additionalRecipientEmails: String,
    subject: String,
    customMessage: String,
    pdfFileName: String,
    pdfUri: Uri?,
    scheduleSummaryText: String
  ) -> Unit,
  onCreateAndInviteAccount: (
    email: String,
    fullName: String,
    congregation: Congregation,
    assignedRole: UserRole,
    delegatedRole: DelegatedRole,
    selectedRoles: Set<AppRole>
  ) -> Unit,
  onDelegateRoleToMember: (targetUser: UserProfileEntity, delegatedRole: DelegatedRole) -> Unit,
  onToggleDsgEnrollment: (SmallGroupEntity) -> Unit,
  onUpdateDsgPastoralLog: (
    group: SmallGroupEntity,
    updatedTopic: String,
    updatedPastoralLog: String,
    updatedServiceNotes: String
  ) -> Unit,
  onTriagePrayerRequest: (requestId: String, newStatus: RequestStatus, notes: String) -> Unit,
  onBroadcastInstantAlert: (title: String, summary: String, body: String) -> Unit,
  onSendMailtrapTestEmail: (recipientEmail: String) -> Unit,
  onUpdateMailtrapConfig: (apiToken: String, senderEmail: String, senderName: String) -> Unit,
  onUpdateSupabaseCredentials: (url: String, anonKey: String) -> Unit,
  onResetAllDataToFresh: () -> Unit,
  initialSubTab: Int = 0,
  modifier: Modifier = Modifier
) {
  // If user is a Member with DelegatedRole.VOLUNTEER_SCHEDULER, lock them strictly to the Schedule PDF & Emailer hub!
  val isDelegatedSchedulerOnly = activeRole == UserRole.MEMBER &&
    activeDelegatedRole == DelegatedRole.VOLUNTEER_SCHEDULER

  if (isDelegatedSchedulerOnly) {
    Column(modifier = modifier.fillMaxSize()) {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Delegated Volunteer Scheduler Hub • Authorized by Rector to upload PDF schedules & email the congregation",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(12.dp)
        )
      }
      SmartPdfScheduleAndEmailSection(
        activeCongregation = activeCongregation,
        rosterMembers = rosterMembers,
        pdfJobs = pdfJobs,
        defaultRecipientEmail = mailtrapConfig.defaultTestRecipient,
        mailtrapStatusMessage = mailtrapStatusMessage,
        onRunPdfScheduleIndexer = onRunPdfScheduleIndexer,
        onEmailSchedulePdfToCongregation = onEmailSchedulePdfToCongregation
      )
    }
    return
  }

  var selectedTab by rememberSaveable { mutableIntStateOf(initialSubTab.coerceIn(0, 1)) }

  Column(modifier = modifier.fillMaxSize()) {
    TabRow(selectedTabIndex = selectedTab) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Accounts") },
        icon = { Icon(Icons.Default.ManageAccounts, contentDescription = null) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Management") },
        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) }
      )
    }

    when (selectedTab) {
      0 -> AccountsSection(
        activeUser = activeUser,
        activeCongregation = activeCongregation,
        activeRole = activeRole,
        congregations = congregations,
        onCreateAndInviteAccount = onCreateAndInviteAccount,
        onBroadcastInstantAlert = onBroadcastInstantAlert
      )
      1 -> ManagementSection(
        districts = districts,
        congregations = congregations,
        allUsers = allUsers,
        onCreateOrUpdateDistrict = onCreateOrUpdateDistrict,
        onDeleteDistrict = onDeleteDistrict,
        onCreateCongregation = onCreateCongregation,
        onUpdateCongregationServiceTimes = onUpdateCongregationServiceTimes,
        onMoveCongregationToDistrict = onMoveCongregationToDistrict,
        onDeleteCongregation = onDeleteCongregation,
        onAdminUpdateAndTransferUser = onAdminUpdateAndTransferUser
      )
    }
  }
}

@Composable
private fun SmartPdfScheduleAndEmailSection(
  activeCongregation: Congregation,
  rosterMembers: List<UserProfileEntity>,
  pdfJobs: List<PdfScheduleJobEntity>,
  defaultRecipientEmail: String,
  mailtrapStatusMessage: String,
  onRunPdfScheduleIndexer: (pdfFileName: String, pdfUri: Uri?, customScheduleText: String?) -> Unit,
  onEmailSchedulePdfToCongregation: (
    additionalRecipientEmails: String,
    subject: String,
    customMessage: String,
    pdfFileName: String,
    pdfUri: Uri?,
    scheduleSummaryText: String
  ) -> Unit
) {
  var customPdfName by rememberSaveable { mutableStateOf("November_2026_Volunteer_Schedule.pdf") }
  var selectedPdfUriString by rememberSaveable { mutableStateOf("") }
  var scheduleTextBuffer by rememberSaveable {
    mutableStateOf(PdfScheduleParser.november2026VolunteerScheduleText())
  }
  var showTextInspector by rememberSaveable { mutableStateOf(false) }

  // Email Composer fields for sending the uploaded PDF to the congregation mailing list
  var emailSubject by rememberSaveable {
    mutableStateOf("${activeCongregation.displayName}: November 2026 Volunteer Schedule (Sun, Nov 01 - Mon, Nov 30)")
  }
  var additionalRecipients by rememberSaveable { mutableStateOf(defaultRecipientEmail) }
  var emailCustomBody by rememberSaveable {
    mutableStateOf(
      "Dear ${activeCongregation.displayName} Family,\n\nAttached is the November 2026 Volunteer Schedule (AV, Altar Setup/Takedown, Greeter, and Usher MAIN & BACKUP assignments). Your individual commitments have also been auto-populated in the NAC Portal app under the Roster tab."
    )
  }

  val pdfPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      selectedPdfUriString = uri.toString()
      val name = uri.lastPathSegment?.substringAfterLast("/") ?: "November_2026_Volunteer_Schedule.pdf"
      customPdfName = name
      onRunPdfScheduleIndexer(name, uri, null)
    }
  }

  val latestFormattedSchedule = pdfJobs.firstOrNull()?.parsedScheduleTableText?.takeIf { it.isNotBlank() }
    ?: scheduleTextBuffer

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Upload & Auto-Populate PDF Schedule Card
    item {
      ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PictureAsPdf,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp)
            )
            Column {
              Text(
                text = "1. Upload Volunteer Schedule PDF & Auto-Populate App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Authorized for Rectors and delegated Volunteer Schedulers. Parses DATE, AV, ALTAR SETUP/TAKEDOWN, GREETER, and USHER (MAIN & BACKUP) and populates each member's upcoming commitments.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { pdfPickerLauncher.launch("application/pdf") },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Upload PDF File")
            }

            OutlinedButton(
              onClick = {
                onRunPdfScheduleIndexer(customPdfName, null, scheduleTextBuffer)
              },
              modifier = Modifier
                .weight(1f)
                .testTag("index_sample_pdf_button")
            ) {
              Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Auto-Populate Nov 2026 PDF")
            }
          }

          AssistChip(
            onClick = { showTextInspector = !showTextInspector },
            label = {
              Text(if (showTextInspector) "Hide November 2026 Schedule Table" else "Inspect / Edit November 2026 Volunteer Schedule Table")
            },
            leadingIcon = {
              Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
            }
          )

          if (showTextInspector) {
            OutlinedTextField(
              value = customPdfName,
              onValueChange = { customPdfName = it },
              label = { Text("Schedule PDF Filename") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = scheduleTextBuffer,
              onValueChange = { scheduleTextBuffer = it },
              label = { Text("November 2026 Volunteer Schedule Stream (MAIN & BACKUP)") },
              minLines = 7,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }
    }

    // 2. Email PDF Schedule to Congregation Mailing List via Mailtrap
    item {
      ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Email,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(26.dp)
            )
            Column {
              Text(
                text = "2. Email PDF Schedule to Congregation Mailing List",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Attaches '$customPdfName' and emails the schedule to all ${rosterMembers.size} congregation member email(s) plus any additional mailing list addresses below.",
                style = MaterialTheme.typography.bodySmall
              )
            }
          }

          OutlinedTextField(
            value = additionalRecipients,
            onValueChange = { additionalRecipients = it },
            label = { Text("Congregation Mailing List / Recipient Emails (comma-separated)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = emailSubject,
            onValueChange = { emailSubject = it },
            label = { Text("Email Subject") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = emailCustomBody,
            onValueChange = { emailCustomBody = it },
            label = { Text("Message to Congregation (PDF & Table Attached Automatically)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
          )

          Button(
            onClick = {
              val uri = selectedPdfUriString.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
              onEmailSchedulePdfToCongregation(
                additionalRecipients,
                emailSubject,
                emailCustomBody,
                customPdfName,
                uri,
                latestFormattedSchedule
              )
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send PDF Schedule Email")
          }
        }
      }
    }

    // 3. Indexing History
    if (pdfJobs.isNotEmpty()) {
      item {
        Text(
          text = "Uploaded Schedule PDF History (${pdfJobs.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
      }
      items(pdfJobs, key = { it.id }) { job ->
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "${job.pdfFileName} • by ${job.uploadedBy}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = job.status,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = job.detailedLog,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountsInviteAndDelegationSection(
  activeUser: UserProfileEntity,
  activeCongregation: Congregation,
  activeRole: UserRole,
  allUsers: List<UserProfileEntity>,
  rosterMembers: List<UserProfileEntity>,
  congregations: List<Congregation>,
  onCreateCongregation: (name: String, address: String, serviceTimes: String, rectorName: String, sundayServiceTime: String, wednesdayServiceTime: String) -> Unit,
  onUpdateCongregationServiceTimes: (congregationId: String, sundayServiceTime: String, wednesdayServiceTime: String) -> Unit,
  onCreateAndInviteAccount: (
    email: String,
    fullName: String,
    congregation: Congregation,
    assignedRole: UserRole,
    delegatedRole: DelegatedRole,
    selectedRoles: Set<AppRole>
  ) -> Unit,
  onDelegateRoleToMember: (targetUser: UserProfileEntity, delegatedRole: DelegatedRole) -> Unit
) {
  var inviteFullName by rememberSaveable { mutableStateOf("") }
  var inviteEmail by rememberSaveable { mutableStateOf("") }
  var inviteCongregation by remember(congregations) {
    mutableStateOf(congregations.firstOrNull { it.id == activeCongregation.id } ?: congregations.firstOrNull() ?: activeCongregation)
  }
  var inviteSelectedRoles by remember {
    mutableStateOf(
      if (activeRole == UserRole.ADMINISTRATOR) setOf(AppRole.MEMBER, AppRole.MINISTER)
      else setOf(AppRole.MEMBER)
    )
  }

  // Congregation creation state (manual creation by Rectors & Admins)
  var newCongName by rememberSaveable { mutableStateOf("") }
  var newCongAddress by rememberSaveable { mutableStateOf("") }
  var newCongServiceTimes by rememberSaveable { mutableStateOf("") }
  var newCongSundayTime by rememberSaveable { mutableStateOf("10:30 AM") }
  var newCongWednesdayTime by rememberSaveable { mutableStateOf("7:30 PM") }
  var newCongRectorName by rememberSaveable { mutableStateOf("") }

  var editSunTime by remember(inviteCongregation.id, inviteCongregation.sundayServiceTime) {
    mutableStateOf(inviteCongregation.sundayServiceTime.ifBlank { "10:30 AM" })
  }
  var editWedTime by remember(inviteCongregation.id, inviteCongregation.wednesdayServiceTime) {
    mutableStateOf(inviteCongregation.wednesdayServiceTime.ifBlank { "7:30 PM" })
  }

  val visibleDirectory = if (activeRole == UserRole.ADMINISTRATOR) allUsers else rosterMembers

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Create & Manage Congregations (Rectors and Administrators)
    item {
      ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_nac_emblem),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Column {
              Text(
                text = "Create & Manage Congregations (${congregations.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Congregations are created manually by Rectors and Administrators. No congregations are preloaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          if (congregations.isNotEmpty()) {
            Text(
              text = "Registered Congregations:",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              congregations.forEach { cong ->
                AssistChip(
                  onClick = { inviteCongregation = cong },
                  label = {
                    Text(
                      text = cong.displayName + if (cong.rectorName.isNotBlank()) " (${cong.rectorName})" else "",
                      style = MaterialTheme.typography.labelSmall
                    )
                  }
                )
              }
            }

            if (inviteCongregation.id.isNotBlank()) {
              HorizontalDivider()
              Text(
                text = "Default Service Times — ${inviteCongregation.displayName}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = editSunTime,
                  onValueChange = { editSunTime = it },
                  label = { Text("Sunday AM") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                  value = editWedTime,
                  onValueChange = { editWedTime = it },
                  label = { Text("Wednesday PM") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
              }
              OutlinedButton(
                onClick = {
                  onUpdateCongregationServiceTimes(inviteCongregation.id, editSunTime, editWedTime)
                },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Save Default Service Times")
              }
            }
          }

          Text(
            text = "Quick Presets:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf(
              listOf("NAC Kitchener Central", "160 Margaret Ave, Kitchener, ON", "Sun 10:30 AM & Wed 7:30 PM", "Pr. J. Grau"),
              listOf("NAC Toronto Central", "840 Coxwell Ave, Toronto, ON", "Sun 10:30 AM & Wed 8:00 PM", "Pr. E. Grau"),
              listOf("NAC Cambridge", "29 Queen St E, Cambridge, ON", "Sun 10:00 AM & Wed 7:30 PM", "Pr. Cudney")
            ).forEach { preset ->
              AssistChip(
                onClick = {
                  newCongName = preset[0]
                  newCongAddress = preset[1]
                  newCongServiceTimes = preset[2]
                  newCongRectorName = preset[3]
                },
                label = { Text(preset[0], style = MaterialTheme.typography.labelSmall) }
              )
            }
          }

          OutlinedTextField(
            value = newCongName,
            onValueChange = { newCongName = it },
            label = { Text("Congregation Name (e.g., NAC Kitchener Central)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = newCongAddress,
            onValueChange = { newCongAddress = it },
            label = { Text("Church Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = newCongSundayTime,
              onValueChange = { newCongSundayTime = it },
              label = { Text("Sunday AM Time") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = newCongWednesdayTime,
              onValueChange = { newCongWednesdayTime = it },
              label = { Text("Wednesday PM Time") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = newCongServiceTimes,
              onValueChange = { newCongServiceTimes = it },
              label = { Text("Service Times Summary") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = newCongRectorName,
              onValueChange = { newCongRectorName = it },
              label = { Text("Rector Name") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          Button(
            onClick = {
              if (newCongName.isNotBlank()) {
                onCreateCongregation(
                  newCongName.trim(),
                  newCongAddress.trim(),
                  newCongServiceTimes.trim(),
                  newCongRectorName.trim(),
                  newCongSundayTime.trim(),
                  newCongWednesdayTime.trim()
                )
                newCongName = ""
                newCongAddress = ""
                newCongServiceTimes = ""
                newCongSundayTime = "10:30 AM"
                newCongWednesdayTime = "7:30 PM"
                newCongRectorName = ""
              }
            },
            enabled = newCongName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(painter = painterResource(id = R.drawable.ic_nac_emblem), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Register New Congregation")
          }
        }
      }
    }

    // 2. Provision Account & Send Email Invite via Mailtrap
    item {
      ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PersonAdd,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Column {
              Text(
                text = "Create Account by Email",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (activeRole == UserRole.ADMINISTRATOR) {
                  "Administrator Mode: Create Rector, Member, or Admin accounts for any congregation."
                } else {
                  val activeName = activeCongregation.displayName.ifBlank { "Congregation" }
                  "Rector Mode: Create Member accounts for $activeName and assign delegated roles."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Quick-fill names from the November 2026 Volunteer Schedule for fast setup
          Text(
            text = "Quick-Fill Schedule Names:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf(
              Triple("Pr. J. Grau", UserRole.MINISTER, DelegatedRole.NONE),
              Triple("Adam B.", UserRole.MEMBER, DelegatedRole.VOLUNTEER_SCHEDULER),
              Triple("Marilyn", UserRole.MEMBER, DelegatedRole.NONE),
              Triple("Dc. Duncan", UserRole.MEMBER, DelegatedRole.NONE)
            ).forEach { (sampleName, sampleRole, sampleDel) ->
              AssistChip(
                onClick = {
                  inviteFullName = sampleName
                  inviteSelectedRoles = if (activeRole == UserRole.ADMINISTRATOR) {
                    when (sampleRole) {
                      UserRole.MINISTER -> setOf(AppRole.MEMBER, AppRole.MINISTER)
                      UserRole.ADMINISTRATOR -> setOf(AppRole.MEMBER, AppRole.ADMIN)
                      UserRole.MEMBER -> if (sampleDel == DelegatedRole.VOLUNTEER_SCHEDULER) {
                        setOf(AppRole.MEMBER, AppRole.VOLUNTEER_SCHEDULER)
                      } else {
                        setOf(AppRole.MEMBER)
                      }
                    }
                  } else {
                    if (sampleDel == DelegatedRole.VOLUNTEER_SCHEDULER) {
                      setOf(AppRole.MEMBER, AppRole.VOLUNTEER_SCHEDULER)
                    } else {
                      setOf(AppRole.MEMBER)
                    }
                  }
                },
                label = { Text(sampleName, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }

          OutlinedTextField(
            value = inviteFullName,
            onValueChange = { inviteFullName = it },
            label = { Text("Full Name (e.g., Pr. J. Grau, Adam B., Marilyn)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = inviteEmail,
            onValueChange = { inviteEmail = it },
            label = { Text("Recipient Email Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          if (activeRole == UserRole.ADMINISTRATOR) {
            Text(
              text = "Locked Home Congregation:",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
            if (congregations.isEmpty()) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Please register a congregation using the card above before assigning members.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.padding(10.dp)
                )
              }
            } else {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                congregations.forEach { cong ->
                  FilterChip(
                    selected = inviteCongregation.id == cong.id,
                    onClick = { inviteCongregation = cong },
                    label = { Text(cong.displayName, style = MaterialTheme.typography.labelSmall) }
                  )
                }
              }
            }

            Text(
              text = "Account Roles (Stackable):",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
            val roleOptions = listOf(
              AppRole.MEMBER to "Standard Member",
              AppRole.VOLUNTEER_SCHEDULER to "Volunteer Scheduler",
              AppRole.MINISTER to "Minister",
              AppRole.RECTOR to "Rector",
              AppRole.ADMIN to "Admin"
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              roleOptions.forEach { (roleOption, label) ->
                val isSelected = roleOption in inviteSelectedRoles
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    inviteSelectedRoles = if (roleOption == AppRole.MEMBER) {
                      inviteSelectedRoles + AppRole.MEMBER
                    } else if (isSelected) {
                      inviteSelectedRoles - roleOption
                    } else {
                      inviteSelectedRoles + roleOption
                    }
                  },
                  label = { Text(label) }
                )
              }
            }
          } else {
            Text(
              text = "Delegated Congregation Role (Rector Delegation):",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              val isScheduler = AppRole.VOLUNTEER_SCHEDULER in inviteSelectedRoles
              FilterChip(
                selected = !isScheduler,
                onClick = { inviteSelectedRoles = setOf(AppRole.MEMBER) },
                label = { Text("Standard Member") }
              )
              FilterChip(
                selected = isScheduler,
                onClick = {
                  inviteSelectedRoles = if (isScheduler) {
                    setOf(AppRole.MEMBER)
                  } else {
                    setOf(AppRole.MEMBER, AppRole.VOLUNTEER_SCHEDULER)
                  }
                },
                label = { Text("Volunteer Scheduler") }
              )
            }
          }

          Button(
            onClick = {
              val finalRoles = if (activeRole == UserRole.ADMINISTRATOR) {
                inviteSelectedRoles + AppRole.MEMBER
              } else {
                (inviteSelectedRoles intersect setOf(AppRole.MEMBER, AppRole.VOLUNTEER_SCHEDULER)) + AppRole.MEMBER
              }
              val derivedUserRole = when {
                AppRole.ADMIN in finalRoles -> UserRole.ADMINISTRATOR
                AppRole.RECTOR in finalRoles || AppRole.MINISTER in finalRoles -> UserRole.MINISTER
                else -> UserRole.MEMBER
              }
              val derivedDelegated = if (AppRole.VOLUNTEER_SCHEDULER in finalRoles) {
                DelegatedRole.VOLUNTEER_SCHEDULER
              } else {
                DelegatedRole.NONE
              }
              onCreateAndInviteAccount(
                inviteEmail,
                inviteFullName,
                if (activeRole == UserRole.ADMINISTRATOR) inviteCongregation else activeCongregation,
                derivedUserRole,
                derivedDelegated,
                finalRoles
              )
              inviteFullName = ""
            },
            enabled = inviteEmail.isNotBlank() && inviteFullName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.MarkEmailRead, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Account by Email")
          }
        }
      }
    }

    // 3. Rector Delegation Hub: Delegate "Volunteer Scheduler" Role to Congregation Members
    item {
      ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Groups,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Column {
              Text(
                text = "Rector Role Delegation & Congregation Directory (${visibleDirectory.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Delegate the 'Volunteer Scheduler' role to a member in your congregation so they can upload the monthly Schedule PDF and email it to the congregation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          visibleDirectory.forEach { member ->
            HorizontalDivider()
            val memberCong = congregations.firstOrNull { it.id == member.congregationId }
            val memberCongName = memberCong?.displayName ?: if (member.congregationId.isBlank()) "Admin Console" else member.congregationId
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${member.fullName} (${member.email})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "$memberCongName • Locked Role: ${member.role.badgeLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
                Surface(
                  color = if (member.accountStatus == "ACTIVE") {
                    MaterialTheme.colorScheme.secondaryContainer
                  } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                  },
                  shape = MaterialTheme.shapes.small
                ) {
                  Text(
                    text = if (member.accountStatus == "ACTIVE") {
                      "ACTIVE"
                    } else {
                      "INVITED (${member.inviteCode})"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }

              if (member.role == UserRole.MEMBER) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text(
                    text = "Delegated Role:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                  )
                  DelegatedRole.entries.forEach { delOption ->
                    FilterChip(
                      selected = member.delegatedRole == delOption,
                      onClick = { onDelegateRoleToMember(member, delOption) },
                      label = { Text(delOption.displayName, style = MaterialTheme.typography.labelSmall) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountsSection(
  activeUser: UserProfileEntity,
  activeCongregation: Congregation,
  activeRole: UserRole,
  congregations: List<Congregation>,
  onCreateAndInviteAccount: (
    email: String,
    fullName: String,
    congregation: Congregation,
    assignedRole: UserRole,
    delegatedRole: DelegatedRole,
    selectedRoles: Set<AppRole>
  ) -> Unit,
  onBroadcastInstantAlert: (title: String, summary: String, body: String) -> Unit
) {
  var inviteFullName by rememberSaveable { mutableStateOf("") }
  var inviteEmail by rememberSaveable { mutableStateOf("") }
  var inviteCongregation by remember(congregations) {
    mutableStateOf(congregations.firstOrNull { it.id == activeCongregation.id } ?: congregations.firstOrNull() ?: activeCongregation)
  }
  var inviteSelectedRoles by remember {
    mutableStateOf(
      if (activeRole == UserRole.ADMINISTRATOR) setOf(AppRole.MEMBER, AppRole.MINISTER)
      else setOf(AppRole.MEMBER)
    )
  }

  var broadcastTitle by rememberSaveable { mutableStateOf("") }
  var broadcastSummary by rememberSaveable { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Create Account by Email
    item {
      ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
              Text(
                text = "Create Account by Email",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Register a user, assign their initial congregation and roles, and send an invite code.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          OutlinedTextField(
            value = inviteFullName,
            onValueChange = { inviteFullName = it },
            label = { Text("Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = inviteEmail,
            onValueChange = { inviteEmail = it },
            label = { Text("Email Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          if (congregations.isNotEmpty()) {
            Text(
              text = "Initial Congregation:",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              congregations.forEach { cong ->
                FilterChip(
                  selected = inviteCongregation.id == cong.id,
                  onClick = { inviteCongregation = cong },
                  label = { Text(cong.displayName, style = MaterialTheme.typography.labelSmall) }
                )
              }
            }
          }

          Text(
            text = "Account Roles (Stackable):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          val roleOptions = listOf(
            AppRole.MEMBER to "Standard Member",
            AppRole.VOLUNTEER_SCHEDULER to "Volunteer Scheduler",
            AppRole.MINISTER to "Minister",
            AppRole.RECTOR to "Rector",
            AppRole.ADMIN to "Admin"
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            roleOptions.forEach { (roleOption, label) ->
              val isSelected = roleOption in inviteSelectedRoles
              FilterChip(
                selected = isSelected,
                onClick = {
                  inviteSelectedRoles = if (roleOption == AppRole.MEMBER) {
                    inviteSelectedRoles + AppRole.MEMBER
                  } else if (isSelected) {
                    inviteSelectedRoles - roleOption
                  } else {
                    inviteSelectedRoles + roleOption
                  }
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }

          Button(
            onClick = {
              val finalRoles = inviteSelectedRoles + AppRole.MEMBER
              val derivedUserRole = when {
                AppRole.ADMIN in finalRoles -> UserRole.ADMINISTRATOR
                AppRole.RECTOR in finalRoles || AppRole.MINISTER in finalRoles -> UserRole.MINISTER
                else -> UserRole.MEMBER
              }
              val derivedDelegated = if (AppRole.VOLUNTEER_SCHEDULER in finalRoles) {
                DelegatedRole.VOLUNTEER_SCHEDULER
              } else {
                DelegatedRole.NONE
              }
              onCreateAndInviteAccount(
                inviteEmail,
                inviteFullName,
                inviteCongregation,
                derivedUserRole,
                derivedDelegated,
                finalRoles
              )
              inviteFullName = ""
              inviteEmail = ""
            },
            enabled = inviteEmail.isNotBlank() && inviteFullName.isNotBlank() && inviteCongregation.id.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.MarkEmailRead, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Account by Email")
          }
        }
      }
    }

    // 2. Instant Push Broadcast (Moved onto Accounts page)
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error
            )
            Text(
              text = "Instant Push Broadcast",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
          OutlinedTextField(
            value = broadcastTitle,
            onValueChange = { broadcastTitle = it },
            label = { Text("Urgent Alert Headline") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = broadcastSummary,
            onValueChange = { broadcastSummary = it },
            label = { Text("Alert Message") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
          Button(
            onClick = {
              if (broadcastTitle.isNotBlank()) {
                onBroadcastInstantAlert(
                  broadcastTitle.trim(),
                  broadcastSummary.trim().ifBlank { broadcastTitle.trim() },
                  broadcastSummary.trim().ifBlank { broadcastTitle.trim() }
                )
                broadcastTitle = ""
                broadcastSummary = ""
              }
            },
            enabled = broadcastTitle.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Campaign, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Instant Push Broadcast")
          }
        }
      }
    }
  }
}

private enum class ManagementLevel {
  DISTRICTS,
  CONGREGATIONS,
  ROSTER
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManagementSection(
  districts: List<District>,
  congregations: List<Congregation>,
  allUsers: List<UserProfileEntity>,
  onCreateOrUpdateDistrict: (id: String, name: String, regionCode: String) -> Unit,
  onDeleteDistrict: (districtId: String) -> Unit,
  onCreateCongregation: (name: String, address: String, serviceTimes: String, rectorName: String, sundayServiceTime: String, wednesdayServiceTime: String, districtId: String) -> Unit,
  onUpdateCongregationServiceTimes: (congregationId: String, sundayServiceTime: String, wednesdayServiceTime: String) -> Unit,
  onMoveCongregationToDistrict: (congregationId: String, newDistrictId: String) -> Unit,
  onDeleteCongregation: (congregationId: String) -> Unit,
  onAdminUpdateAndTransferUser: (
    targetUser: UserProfileEntity,
    fullName: String,
    email: String,
    preferredMinistries: String,
    roles: Set<AppRole>,
    newCongregationId: String
  ) -> Unit
) {
  var level by rememberSaveable { mutableStateOf(ManagementLevel.DISTRICTS) }
  var selectedDistrictId by rememberSaveable { mutableStateOf("") }
  var selectedCongregationId by rememberSaveable { mutableStateOf("") }

  // Dialog states
  var editingDistrict by remember { mutableStateOf<District?>(null) }
  var showAddDistrictDialog by remember { mutableStateOf(false) }
  var deletingDistrict by remember { mutableStateOf<District?>(null) }

  var showAddCongDialog by remember { mutableStateOf(false) }
  var editingCong by remember { mutableStateOf<Congregation?>(null) }
  var movingCong by remember { mutableStateOf<Congregation?>(null) }
  var deletingCong by remember { mutableStateOf<Congregation?>(null) }

  var editingPerson by remember { mutableStateOf<UserProfileEntity?>(null) }

  val selectedDistrict = districts.firstOrNull { it.id == selectedDistrictId }
  val selectedCongregation = congregations.firstOrNull { it.id == selectedCongregationId }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    when (level) {
      ManagementLevel.DISTRICTS -> {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "Canadian Districts (${districts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Tap a district to manage its congregations and clergy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Button(onClick = { showAddDistrictDialog = true }) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Add")
            }
          }
        }

        items(districts, key = { it.id }) { district ->
          val congCount = congregations.count { it.districtId == district.id }
          ElevatedCard(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                selectedDistrictId = district.id
                level = ManagementLevel.CONGREGATIONS
              }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_nac_emblem),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
                Column {
                  Text(
                    text = district.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "$congCount congregation(s)" + if (district.regionCode.isNotBlank()) " • ${district.regionCode}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Row {
                androidx.compose.material3.IconButton(onClick = { editingDistrict = district }) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit district", modifier = Modifier.size(18.dp))
                }
                androidx.compose.material3.IconButton(onClick = { deletingDistrict = district }) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete district",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }

      ManagementLevel.CONGREGATIONS -> {
        val districtCongs = congregations.filter { it.districtId == selectedDistrictId }
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              TextButton(onClick = { level = ManagementLevel.DISTRICTS }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("All Districts")
              }
              Button(onClick = { showAddCongDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Congregation")
              }
            }
            Text(
              text = "${selectedDistrict?.name ?: "District"} — Congregations (${districtCongs.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        }

        if (districtCongs.isEmpty()) {
          item {
            Card(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "No congregations in this district yet. Tap 'Add Congregation' above to add one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
              )
            }
          }
        } else {
          items(districtCongs, key = { it.id }) { cong ->
            val memberCount = allUsers.count { it.congregationId == cong.id }
            ElevatedCard(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedCongregationId = cong.id
                  level = ManagementLevel.ROSTER
                }
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = cong.displayName,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "Sun ${cong.sundayServiceTime} • Wed ${cong.wednesdayServiceTime} • $memberCount member(s)",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (cong.address.isNotBlank()) {
                      Text(
                        text = cong.address,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                  Row {
                    androidx.compose.material3.IconButton(onClick = { editingCong = cong }) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit congregation", modifier = Modifier.size(18.dp))
                    }
                    androidx.compose.material3.IconButton(onClick = { movingCong = cong }) {
                      Icon(Icons.Default.SwapHoriz, contentDescription = "Move to district", modifier = Modifier.size(18.dp))
                    }
                    androidx.compose.material3.IconButton(onClick = { deletingCong = cong }) {
                      Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete congregation",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      ManagementLevel.ROSTER -> {
        val congUsers = allUsers.filter { it.congregationId == selectedCongregationId }
        val rectors = congUsers.filter { AppRole.RECTOR in it.effectiveRoles }
        val ministers = congUsers.filter { AppRole.MINISTER in it.effectiveRoles && AppRole.RECTOR !in it.effectiveRoles }
        val members = congUsers.filter { AppRole.RECTOR !in it.effectiveRoles && AppRole.MINISTER !in it.effectiveRoles }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { level = ManagementLevel.CONGREGATIONS }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Back to ${selectedDistrict?.name ?: "District"}")
            }
            Text(
              text = "${selectedCongregation?.displayName ?: "Congregation"} Directory (${congUsers.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Tap any person to edit details, stackable roles, or transfer to another congregation.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // 1. Rector Section
        item {
          Text(
            text = "Rector (${rectors.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
        if (rectors.isEmpty()) {
          item {
            Text(
              text = "No Rector assigned to this congregation.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          items(rectors, key = { "rector_${it.id}" }) { person ->
            DirectoryPersonCard(person = person, isHighlighted = true, onClick = { editingPerson = person })
          }
        }

        // 2. Ministers Section
        item {
          Text(
            text = "Ministers (${ministers.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
        if (ministers.isEmpty()) {
          item {
            Text(
              text = "No additional Ministers assigned.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          items(ministers, key = { "min_${it.id}" }) { person ->
            DirectoryPersonCard(person = person, isHighlighted = false, onClick = { editingPerson = person })
          }
        }

        // 3. Members Section
        item {
          Text(
            text = "Members (${members.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
        if (members.isEmpty()) {
          item {
            Text(
              text = "No standard Members assigned yet.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          items(members, key = { "mem_${it.id}" }) { person ->
            DirectoryPersonCard(person = person, isHighlighted = false, onClick = { editingPerson = person })
          }
        }
      }
    }
  }

  // --- DIALOGS FOR DISTRICTS, CONGREGATIONS, AND MEMBER EDIT/TRANSFER ---
  if (showAddDistrictDialog || editingDistrict != null) {
    val target = editingDistrict
    var nameDraft by remember(target) { mutableStateOf(target?.name.orEmpty()) }
    var codeDraft by remember(target) { mutableStateOf(target?.regionCode.orEmpty()) }
    AlertDialog(
      onDismissRequest = {
        showAddDistrictDialog = false
        editingDistrict = null
      },
      title = { Text(if (target == null) "Add District" else "Edit District") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = nameDraft,
            onValueChange = { nameDraft = it },
            label = { Text("District Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = codeDraft,
            onValueChange = { codeDraft = it },
            label = { Text("Region Code (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onCreateOrUpdateDistrict(target?.id.orEmpty(), nameDraft, codeDraft)
            showAddDistrictDialog = false
            editingDistrict = null
          },
          enabled = nameDraft.isNotBlank()
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = {
          showAddDistrictDialog = false
          editingDistrict = null
        }) { Text("Cancel") }
      }
    )
  }

  if (deletingDistrict != null) {
    val d = deletingDistrict!!
    AlertDialog(
      onDismissRequest = { deletingDistrict = null },
      title = { Text("Delete ${d.name}?") },
      text = { Text("This will remove the district if no congregations remain assigned to it.") },
      confirmButton = {
        Button(onClick = {
          onDeleteDistrict(d.id)
          deletingDistrict = null
        }) { Text("Delete") }
      },
      dismissButton = {
        TextButton(onClick = { deletingDistrict = null }) { Text("Cancel") }
      }
    )
  }

  if (showAddCongDialog || editingCong != null) {
    val target = editingCong
    var nameDraft by remember(target) { mutableStateOf(target?.displayName.orEmpty()) }
    var addrDraft by remember(target) { mutableStateOf(target?.address.orEmpty()) }
    var rectorDraft by remember(target) { mutableStateOf(target?.rectorName.orEmpty()) }
    var sunDraft by remember(target) { mutableStateOf(target?.sundayServiceTime ?: "10:30 AM") }
    var wedDraft by remember(target) { mutableStateOf(target?.wednesdayServiceTime ?: "7:30 PM") }
    AlertDialog(
      onDismissRequest = {
        showAddCongDialog = false
        editingCong = null
      },
      title = { Text(if (target == null) "Add Congregation" else "Edit ${target.displayName}") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = nameDraft,
            onValueChange = { nameDraft = it },
            label = { Text("Congregation Name *") },
            singleLine = true,
            enabled = target == null,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = addrDraft,
            onValueChange = { addrDraft = it },
            label = { Text("Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = rectorDraft,
            onValueChange = { rectorDraft = it },
            label = { Text("Rector Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = sunDraft,
              onValueChange = { sunDraft = it },
              label = { Text("Sunday AM") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = wedDraft,
              onValueChange = { wedDraft = it },
              label = { Text("Wednesday PM") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (target == null) {
              onCreateCongregation(nameDraft, addrDraft, "", rectorDraft, sunDraft, wedDraft, selectedDistrictId)
            } else {
              onCreateCongregation(target.displayName, addrDraft, "", rectorDraft, sunDraft, wedDraft, target.districtId)
              onUpdateCongregationServiceTimes(target.id, sunDraft, wedDraft)
            }
            showAddCongDialog = false
            editingCong = null
          },
          enabled = nameDraft.isNotBlank()
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = {
          showAddCongDialog = false
          editingCong = null
        }) { Text("Cancel") }
      }
    )
  }

  if (movingCong != null) {
    val cong = movingCong!!
    var targetDistId by remember(cong) { mutableStateOf(cong.districtId) }
    AlertDialog(
      onDismissRequest = { movingCong = null },
      title = { Text("Move ${cong.displayName} to District") },
      text = {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          districts.forEach { d ->
            FilterChip(
              selected = targetDistId == d.id,
              onClick = { targetDistId = d.id },
              label = { Text(d.name, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onMoveCongregationToDistrict(cong.id, targetDistId)
            movingCong = null
          },
          enabled = targetDistId.isNotBlank() && targetDistId != cong.districtId
        ) { Text("Move") }
      },
      dismissButton = {
        TextButton(onClick = { movingCong = null }) { Text("Cancel") }
      }
    )
  }

  if (deletingCong != null) {
    val c = deletingCong!!
    AlertDialog(
      onDismissRequest = { deletingCong = null },
      title = { Text("Delete ${c.displayName}?") },
      text = { Text("This removes the congregation if no members remain assigned to it.") },
      confirmButton = {
        Button(onClick = {
          onDeleteCongregation(c.id)
          deletingCong = null
        }) { Text("Delete") }
      },
      dismissButton = {
        TextButton(onClick = { deletingCong = null }) { Text("Cancel") }
      }
    )
  }

  if (editingPerson != null) {
    val person = editingPerson!!
    var nameDraft by remember(person) { mutableStateOf(person.fullName) }
    var emailDraft by remember(person) { mutableStateOf(person.email) }
    var notesDraft by remember(person) { mutableStateOf(person.preferredMinistries) }
    var rolesDraft by remember(person) { mutableStateOf(person.effectiveRoles) }
    var targetCongId by remember(person) { mutableStateOf(person.congregationId) }

    AlertDialog(
      onDismissRequest = { editingPerson = null },
      title = { Text("Edit & Transfer: ${person.fullName}") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = nameDraft,
            onValueChange = { nameDraft = it },
            label = { Text("Full Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = emailDraft,
            onValueChange = { emailDraft = it },
            label = { Text("Email *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = notesDraft,
            onValueChange = { notesDraft = it },
            label = { Text("Ministry / Notes") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Text(
            text = "Stackable Roles:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            AppRole.entries.forEach { roleOpt ->
              val isSelected = roleOpt in rolesDraft
              FilterChip(
                selected = isSelected,
                onClick = {
                  if (roleOpt == AppRole.MEMBER) return@FilterChip
                  rolesDraft = if (isSelected) rolesDraft - roleOpt else rolesDraft + roleOpt
                },
                label = { Text(roleOpt.displayName, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }

          Text(
            text = "Transfer / Move to Congregation:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            congregations.forEach { cong ->
              val distName = districts.firstOrNull { it.id == cong.districtId }?.name
              FilterChip(
                selected = targetCongId == cong.id,
                onClick = { targetCongId = cong.id },
                label = {
                  Text(
                    text = if (distName != null) "${cong.displayName} ($distName)" else cong.displayName,
                    style = MaterialTheme.typography.labelSmall
                  )
                }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onAdminUpdateAndTransferUser(person, nameDraft, emailDraft, notesDraft, rolesDraft, targetCongId)
            editingPerson = null
          },
          enabled = nameDraft.isNotBlank() && emailDraft.contains("@")
        ) { Text("Save & Transfer") }
      },
      dismissButton = {
        TextButton(onClick = { editingPerson = null }) { Text("Cancel") }
      }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DirectoryPersonCard(
  person: UserProfileEntity,
  isHighlighted: Boolean,
  onClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
      }
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = person.fullName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = ParishPolicy.formatRoleBadges(person.effectiveRoles),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )
      }
      Text(
        text = person.email,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (person.preferredMinistries.isNotBlank()) {
        Text(
          text = person.preferredMinistries,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
