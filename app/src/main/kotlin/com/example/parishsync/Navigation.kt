package com.example.parishsync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.ParishPolicy
import com.example.parishsync.data.repository.OfflineFirstParishRepository
import com.example.parishsync.ui.ParishUiState
import com.example.parishsync.ui.ParishViewModel
import com.example.parishsync.ui.screens.CommunityHubScreen
import com.example.parishsync.ui.screens.MemberBulletinScreen
import com.example.parishsync.ui.screens.MinisterAndAdminScreen
import com.example.parishsync.ui.screens.OnboardingScreen
import com.example.parishsync.ui.screens.RosterAndChoirScreen
import com.example.parishsync.ui.screens.ServingScheduleScreen
import com.example.parishsync.ui.screens.VolunteerScheduleScreen
import com.example.parishsync.workers.ParishNotificationHelper

@Composable
fun MainNavigation(
  viewModel: ParishViewModel = viewModel(
    factory = ParishViewModel.provideFactory(
      OfflineFirstParishRepository.getInstance(LocalContext.current)
    )
  )
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  NACPortalAppShell(
    uiState = uiState,
    viewModel = viewModel
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NACPortalAppShell(
  uiState: ParishUiState,
  viewModel: ParishViewModel
) {
  val backStack = rememberNavBackStack(VolunteerScheduleRoute)
  val snackbarHostState = remember { SnackbarHostState() }
  var volunteerScheduleNavTrigger by remember { mutableIntStateOf(0) }
  var lastObservedUserId by remember { mutableStateOf<String?>(null) }

  // Reset navigation only when switching users or signing in/out (never clear on initial frame)
  LaunchedEffect(uiState.activeUser?.id) {
    val currentId = uiState.activeUser?.id
    if (lastObservedUserId != null && lastObservedUserId != currentId) {
      if (backStack.size != 1 || backStack.firstOrNull() != VolunteerScheduleRoute) {
        backStack.clear()
        backStack.add(VolunteerScheduleRoute)
      }
      volunteerScheduleNavTrigger++
    }
    lastObservedUserId = currentId
  }

  val currentTopKeyForGuard: NavKey = backStack.lastOrNull() ?: VolunteerScheduleRoute
  LaunchedEffect(currentTopKeyForGuard, uiState.activeRoles, uiState.activeUser?.id) {
    if (uiState.activeUser != null && !ParishBottomDestination.isRouteAuthorized(currentTopKeyForGuard, uiState.activeRoles)) {
      backStack.clear()
      backStack.add(VolunteerScheduleRoute)
      snackbarHostState.showSnackbar("Redirected: You do not have permission to access that section.")
    }
  }

  LaunchedEffect(uiState.bannerFeedback) {
    val msg = uiState.bannerFeedback
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBannerFeedback()
    }
  }

  if (!uiState.isInitialized) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      androidx.compose.material3.CircularProgressIndicator()
    }
    return
  }

  // If no active user session exists yet, present the Account-Locked Sign-In & Invite Activation screen
  if (uiState.activeUser == null) {
    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
      OnboardingScreen(
        allProvisionedUsers = uiState.allUsers,
        syncStatusMessage = uiState.syncStatusMessage,
        mailtrapStatusMessage = uiState.mailtrapStatusMessage,
        onSignInWithLockedAccount = { email, password ->
          viewModel.signInWithLockedAccount(email, password)
        },
        onAcceptEmailInvite = { email, inviteCode, newPassword ->
          viewModel.acceptEmailInviteAndSetPassword(email, inviteCode, newPassword)
        },
        modifier = Modifier.padding(innerPadding)
      )
    }
    return
  }

  val roleDestinations = remember(uiState.activeRoles) {
    ParishBottomDestination.destinationsForRoles(uiState.activeRoles)
  }
  val currentTopKey: NavKey = backStack.lastOrNull() ?: VolunteerScheduleRoute

  val lockedRoleText = remember(uiState.activeRoles) {
    ParishPolicy.formatRoleBadges(uiState.activeRoles)
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_nac_emblem),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp)
              )
              Column {
                val congName = if (uiState.activeCongregation.id.isBlank() || uiState.activeCongregation == com.example.parishsync.data.model.Congregation.NONE) {
                  "Admin Console"
                } else {
                  uiState.activeCongregation.displayName
                }
                Text(
                  text = "NAC Portal • $congName",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${uiState.activeUser.fullName} (${uiState.activeUser.email})",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  if (backStack.lastOrNull() != SettingsRoute) {
                    backStack.add(SettingsRoute)
                  }
                }
              ) {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = "Profile & Settings",
                  tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
              IconButton(
                onClick = { viewModel.signOutToLoginScreen() }
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Logout,
                  contentDescription = "Sign Out",
                  tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }
          }

          // Account-Locked Role Indicator placed cleanly underneath the portal title & user name
          Surface(
            color = Color.White.copy(alpha = 0.16f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(start = 36.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Role Locked by Account",
                modifier = Modifier.size(12.dp),
                tint = Color.White
              )
              Text(
                text = lockedRoleText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    },
    bottomBar = {
      NavigationBar {
        roleDestinations.forEach { destination ->
          val isSelected = currentTopKey == destination.route
          NavigationBarItem(
            selected = isSelected,
            onClick = {
              if (destination == ParishBottomDestination.VOLUNTEER_SCHEDULE) {
                volunteerScheduleNavTrigger++
              }
              if (currentTopKey != destination.route) {
                backStack.clear()
                backStack.add(destination.route)
              }
            },
            icon = {
              if (destination == ParishBottomDestination.SERVING_SCHEDULE) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_nac_emblem),
                  contentDescription = destination.label,
                  modifier = Modifier.size(24.dp)
                )
              } else {
                val icon = when (destination) {
                  ParishBottomDestination.VOLUNTEER_SCHEDULE -> Icons.Default.EventAvailable
                  ParishBottomDestination.ADMIN -> Icons.Default.AdminPanelSettings
                  ParishBottomDestination.SERVING_SCHEDULE -> error("Handled above")
                }
                Icon(imageVector = icon, contentDescription = destination.label)
              }
            },
            label = { Text(destination.label) },
            modifier = Modifier.testTag("nav_${destination.name}")
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      NavDisplay(
        backStack = backStack,
        onBack = {
          if (backStack.size > 1) {
            backStack.removeLastOrNull()
          }
        },
        entryProvider = entryProvider {
          entry<VolunteerScheduleRoute> {
            VolunteerScheduleScreen(
              activeUser = uiState.activeUser,
              activeCongregation = uiState.activeCongregation,
              activeRole = uiState.activeRole,
              activeDelegatedRole = uiState.activeDelegatedRole,
              activeRoles = uiState.activeRoles,
              congregations = uiState.congregations,
              selectedAdminCongregationId = uiState.selectedAdminCongregationId,
              onSelectAdminCongregation = { viewModel.selectAdminCongregationFilter(it) },
              navReclickTrigger = volunteerScheduleNavTrigger,
              volunteerAssignments = uiState.volunteerAssignments,
              rosterMembers = uiState.rosterMembers,
              onToggleConfirmation = { viewModel.toggleAssignmentConfirmed(it) },
              onToggleSubTaskCompletion = { assignment, subTaskId ->
                viewModel.toggleSubTaskCompletion(assignment, subTaskId)
              },
              onTriggerReminder = { viewModel.scheduleDutyNotificationReminder(it) },
              onAddManualAssignment = { duty, customTitle, tier, name, backupName, date, time, notes, subTasks ->
                viewModel.addVolunteerRosterDuty(
                  dutyType = duty,
                  customTaskTitle = customTitle,
                  assignmentTier = tier,
                  assigneeName = name,
                  backupAssigneeName = backupName,
                  serviceDate = date,
                  serviceTime = time,
                  notes = notes,
                  subTasks = subTasks
                )
              },
              onDeleteAssignment = { viewModel.deleteVolunteerAssignment(it) },
              onRunPdfScheduleIndexer = { fileName, uri, text ->
                viewModel.runSmartPdfScheduleIndexer(fileName, uri, text)
              },
              onCommitParsedCards = { fileName, cards ->
                viewModel.commitParsedScheduleCards(fileName, cards)
              },
              onEmailScheduleToCongregation = { extraEmails, subject, message, fileName, uri, summaryText ->
                viewModel.emailSchedulePdfToCongregation(
                  additionalRecipientEmails = extraEmails,
                  subject = subject,
                  customMessage = message,
                  pdfFileName = fileName,
                  pdfUri = uri,
                  scheduleSummaryText = summaryText
                )
              }
            )
          }

          entry<ServingScheduleRoute> {
            if (!ParishPolicy.canAccessServingSchedule(uiState.activeRoles)) {
              LaunchedEffect(Unit) {
                backStack.clear()
                backStack.add(VolunteerScheduleRoute)
              }
              return@entry
            }
            val assignedDistrictName = uiState.districts
              .firstOrNull { it.id == uiState.activeCongregation.districtId }
              ?.name
              .orEmpty()
            ServingScheduleScreen(
              activeUser = uiState.activeUser,
              activeCongregation = uiState.activeCongregation,
              activeRole = uiState.activeRole,
              assignedDistrictName = assignedDistrictName,
              miniHqResources = uiState.miniHqResources,
              onFeedback = { viewModel.showBannerFeedback(it) },
              servingSchedules = uiState.servingSchedules,
              onAddServingSchedule = { title, date, time, off, offRank, assist, choir, organ, theme, bible, tag, loc, notes ->
                viewModel.addServingSchedule(
                  serviceTitle = title,
                  serviceDate = date,
                  serviceTime = time,
                  officiantMinister = off,
                  officiantRank = offRank,
                  assistingMinisters = assist,
                  choirConductor = choir,
                  organist = organ,
                  serviceTheme = theme,
                  bibleWord = bible,
                  specialServiceTag = tag,
                  location = loc,
                  notes = notes
                )
              },
              onDeleteServingSchedule = { viewModel.deleteServingSchedule(it) },
              onLoadSampleSchedule = { viewModel.loadSampleNacServingSchedules() }
            )
          }

          entry<BulletinRoute> {
            MemberBulletinScreen(
              congregation = uiState.activeCongregation,
              activeRole = uiState.activeRole,
              announcements = uiState.announcements,
              onPublishAnnouncement = { title, summary, body, category, isUrgent, serviceDate, liturgicalOrder ->
                viewModel.publishAnnouncementOrAlert(
                  title = title,
                  summary = summary,
                  body = body,
                  category = category,
                  isUrgent = isUrgent,
                  serviceDate = serviceDate,
                  liturgicalOrder = liturgicalOrder
                )
              }
            )
          }

          entry<RosterRoute> {
            RosterAndChoirScreen(
              activeUserName = uiState.activeUser.fullName,
              volunteerAssignments = uiState.volunteerAssignments,
              hymnSelections = uiState.hymnSelections,
              onToggleConfirmation = { viewModel.toggleAssignmentConfirmed(it) },
              onTriggerReminder = { viewModel.scheduleDutyNotificationReminder(it) },
              onAddManualAssignment = { duty, assignee, date, time, notes ->
                viewModel.addVolunteerRosterDuty(duty, assignee, date, time, notes)
              },
              initialSubTab = 0
            )
          }

          entry<ChoirHymnRoute> {
            RosterAndChoirScreen(
              activeUserName = uiState.activeUser.fullName,
              volunteerAssignments = uiState.volunteerAssignments,
              hymnSelections = uiState.hymnSelections,
              onToggleConfirmation = { viewModel.toggleAssignmentConfirmed(it) },
              onTriggerReminder = { viewModel.scheduleDutyNotificationReminder(it) },
              onAddManualAssignment = { duty, assignee, date, time, notes ->
                viewModel.addVolunteerRosterDuty(duty, assignee, date, time, notes)
              },
              initialSubTab = 1
            )
          }

          entry<CommunityRoute> {
            CommunityHubScreen(
              activeUserName = uiState.activeUser.fullName,
              activeRole = uiState.activeRole,
              prayerRequests = uiState.prayerRequests,
              rideShares = uiState.rideShares,
              onSubmitPrayerRequest = { name, contact, type, content, isPrivate ->
                viewModel.submitPrayerOrVisitRequest(name, contact, type, content, isPrivate)
              },
              onCreateRideShare = { postType, name, phone, area, date, time, seats, notes ->
                viewModel.postRideShareListing(postType, name, phone, area, date, time, seats, notes)
              },
              onMatchRideShare = { viewModel.connectRideShareMatch(it) }
            )
          }

          entry<MinisterDashboardRoute> {
            if (!ParishPolicy.isGlobalAdmin(uiState.activeRoles)) {
              LaunchedEffect(Unit) {
                backStack.clear()
                backStack.add(VolunteerScheduleRoute)
              }
              return@entry
            }
            MinisterAndAdminScreen(
              activeUser = uiState.activeUser,
              activeCongregation = uiState.activeCongregation,
              activeRole = uiState.activeRole,
              activeDelegatedRole = uiState.activeDelegatedRole,
              allUsers = uiState.allUsers,
              rosterMembers = uiState.rosterMembers,
              smallGroups = uiState.smallGroups,
              prayerRequests = uiState.prayerRequests,
              pdfJobs = uiState.pdfScheduleJobs,
              supabaseConfig = uiState.supabaseConfig,
              syncStatusMessage = uiState.syncStatusMessage,
              mailtrapConfig = uiState.mailtrapConfig,
              mailtrapStatusMessage = uiState.mailtrapStatusMessage,
              congregations = uiState.congregations,
              districts = uiState.districts,
              onCreateCongregation = { name, addr, times, rector, sunTime, wedTime, distId ->
                viewModel.createCongregation(name, addr, times, rector, sunTime, wedTime, distId)
              },
              onUpdateCongregationServiceTimes = { congId, sunTime, wedTime ->
                viewModel.updateCongregationServiceTimes(congId, sunTime, wedTime)
              },
              onCreateOrUpdateDistrict = { id, name, regionCode ->
                viewModel.createOrUpdateDistrict(id, name, regionCode)
              },
              onDeleteDistrict = { id -> viewModel.deleteDistrict(id) },
              onMoveCongregationToDistrict = { congId, newDistId ->
                viewModel.moveCongregationToDistrict(congId, newDistId)
              },
              onDeleteCongregation = { congId -> viewModel.deleteCongregation(congId) },
              onAdminUpdateAndTransferUser = { targetUser, fullName, email, notes, roles, newCongId ->
                viewModel.adminUpdateAndTransferUser(targetUser, fullName, email, notes, roles, newCongId)
              },
              onRunPdfScheduleIndexer = { fileName, uri, text ->
                viewModel.runSmartPdfScheduleIndexer(fileName, uri, text)
              },
              onEmailSchedulePdfToCongregation = { extraEmails, subject, message, fileName, uri, summaryText ->
                viewModel.emailSchedulePdfToCongregation(
                  additionalRecipientEmails = extraEmails,
                  subject = subject,
                  customMessage = message,
                  pdfFileName = fileName,
                  pdfUri = uri,
                  scheduleSummaryText = summaryText
                )
              },
              onCreateAndInviteAccount = { email, fullName, congregation, assignedRole, delegatedRole, selectedRoles ->
                viewModel.createAndInviteAccountByEmail(email, fullName, congregation, assignedRole, delegatedRole, selectedRoles)
              },
              onDelegateRoleToMember = { member, delegatedRole ->
                viewModel.delegateRoleToMember(member, delegatedRole)
              },
              onToggleDsgEnrollment = { viewModel.toggleDsgEnrollment(it) },
              onUpdateDsgPastoralLog = { group, topic, log, serviceNotes ->
                viewModel.saveDsgPastoralNotes(group, topic, log, serviceNotes)
              },
              onTriagePrayerRequest = { id, status, notes ->
                viewModel.updatePrayerRequestTriage(id, status, notes)
              },
              onBroadcastInstantAlert = { title, summary, body ->
                viewModel.publishAnnouncementOrAlert(
                  title = title,
                  summary = summary,
                  body = body,
                  category = "Urgent Broadcast",
                  isUrgent = true,
                  serviceDate = "Immediate Alert",
                  liturgicalOrder = ""
                )
              },
              onSendMailtrapTestEmail = { recipient ->
                viewModel.sendMailtrapTestEmail(recipient)
              },
              onUpdateMailtrapConfig = { token, senderEmail, senderName ->
                viewModel.updateMailtrapConfig(token, senderEmail, senderName)
              },
              onUpdateSupabaseCredentials = { url, key ->
                viewModel.updateSupabaseCredentials(url, key)
              },
              onResetAllDataToFresh = {
                viewModel.resetAllDataToFreshBootstrap()
              },
              initialSubTab = 0
            )
          }

          entry<AdminConsoleRoute> {
            if (!ParishPolicy.isGlobalAdmin(uiState.activeRoles)) {
              LaunchedEffect(Unit) {
                backStack.clear()
                backStack.add(VolunteerScheduleRoute)
              }
              return@entry
            }
            MinisterAndAdminScreen(
              activeUser = uiState.activeUser,
              activeCongregation = uiState.activeCongregation,
              activeRole = uiState.activeRole,
              activeDelegatedRole = uiState.activeDelegatedRole,
              allUsers = uiState.allUsers,
              rosterMembers = uiState.rosterMembers,
              smallGroups = uiState.smallGroups,
              prayerRequests = uiState.prayerRequests,
              pdfJobs = uiState.pdfScheduleJobs,
              supabaseConfig = uiState.supabaseConfig,
              syncStatusMessage = uiState.syncStatusMessage,
              mailtrapConfig = uiState.mailtrapConfig,
              mailtrapStatusMessage = uiState.mailtrapStatusMessage,
              congregations = uiState.congregations,
              districts = uiState.districts,
              onCreateCongregation = { name, addr, times, rector, sunTime, wedTime, distId ->
                viewModel.createCongregation(name, addr, times, rector, sunTime, wedTime, distId)
              },
              onUpdateCongregationServiceTimes = { congId, sunTime, wedTime ->
                viewModel.updateCongregationServiceTimes(congId, sunTime, wedTime)
              },
              onCreateOrUpdateDistrict = { id, name, regionCode ->
                viewModel.createOrUpdateDistrict(id, name, regionCode)
              },
              onDeleteDistrict = { id -> viewModel.deleteDistrict(id) },
              onMoveCongregationToDistrict = { congId, newDistId ->
                viewModel.moveCongregationToDistrict(congId, newDistId)
              },
              onDeleteCongregation = { congId -> viewModel.deleteCongregation(congId) },
              onAdminUpdateAndTransferUser = { targetUser, fullName, email, notes, roles, newCongId ->
                viewModel.adminUpdateAndTransferUser(targetUser, fullName, email, notes, roles, newCongId)
              },
              onRunPdfScheduleIndexer = { fileName, uri, text ->
                viewModel.runSmartPdfScheduleIndexer(fileName, uri, text)
              },
              onEmailSchedulePdfToCongregation = { extraEmails, subject, message, fileName, uri, summaryText ->
                viewModel.emailSchedulePdfToCongregation(
                  additionalRecipientEmails = extraEmails,
                  subject = subject,
                  customMessage = message,
                  pdfFileName = fileName,
                  pdfUri = uri,
                  scheduleSummaryText = summaryText
                )
              },
              onCreateAndInviteAccount = { email, fullName, congregation, assignedRole, delegatedRole, selectedRoles ->
                viewModel.createAndInviteAccountByEmail(email, fullName, congregation, assignedRole, delegatedRole, selectedRoles)
              },
              onDelegateRoleToMember = { member, delegatedRole ->
                viewModel.delegateRoleToMember(member, delegatedRole)
              },
              onToggleDsgEnrollment = { viewModel.toggleDsgEnrollment(it) },
              onUpdateDsgPastoralLog = { group, topic, log, serviceNotes ->
                viewModel.saveDsgPastoralNotes(group, topic, log, serviceNotes)
              },
              onTriagePrayerRequest = { id, status, notes ->
                viewModel.updatePrayerRequestTriage(id, status, notes)
              },
              onBroadcastInstantAlert = { title, summary, body ->
                viewModel.publishAnnouncementOrAlert(
                  title = title,
                  summary = summary,
                  body = body,
                  category = "Urgent Broadcast",
                  isUrgent = true,
                  serviceDate = "Immediate Alert",
                  liturgicalOrder = ""
                )
              },
              onSendMailtrapTestEmail = { recipient ->
                viewModel.sendMailtrapTestEmail(recipient)
              },
              onUpdateMailtrapConfig = { token, senderEmail, senderName ->
                viewModel.updateMailtrapConfig(token, senderEmail, senderName)
              },
              onUpdateSupabaseCredentials = { url, key ->
                viewModel.updateSupabaseCredentials(url, key)
              },
              onResetAllDataToFresh = {
                viewModel.resetAllDataToFreshBootstrap()
              },
              initialSubTab = 1
            )
          }

          entry<SettingsRoute> {
            val context = LocalContext.current
            val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
              contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { granted ->
              if (granted) {
                viewModel.enablePhoneNotifications()
              }
            }
            val hasOsPermission = ParishNotificationHelper.hasNotificationPermission(context)
            com.example.parishsync.ui.screens.SettingsScreen(
              activeUser = uiState.activeUser,
              congregationName = uiState.activeCongregation.displayName,
              hasOsNotificationPermission = hasOsPermission,
              hasPushSubscription = uiState.pushSubscriptions.isNotEmpty(),
              onSaveProfile = { fullName, email, phone, currentPassword, newPassword, confirmNewPassword, onResult ->
                viewModel.updateOwnProfile(
                  fullName = fullName,
                  email = email,
                  phone = phone,
                  currentPassword = currentPassword,
                  newPassword = newPassword,
                  confirmNewPassword = confirmNewPassword,
                  onResult = onResult
                )
              },
              onEnablePhoneNotifications = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                  !ParishNotificationHelper.hasNotificationPermission(context)
                ) {
                  notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                  viewModel.enablePhoneNotifications()
                }
              },
              onDisablePhoneNotifications = { viewModel.disablePhoneNotifications() },
              onTestPhoneNotification = { viewModel.sendTestPhoneNotification() },
              onUpdateNotificationPreferences = { preferPush, preferEmail ->
                viewModel.updateNotificationPreferences(preferPush, preferEmail)
              },
              onSignOut = { viewModel.signOutToLoginScreen() }
            )
          }
        }
      )
    }
  }
}
