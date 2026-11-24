package com.example.parishsync.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parishsync.data.local.AnnouncementEntity
import com.example.parishsync.data.local.HymnSelectionEntity
import com.example.parishsync.data.local.MiniHqResourceEntity
import com.example.parishsync.data.local.ParishSeedData
import com.example.parishsync.data.local.PdfScheduleJobEntity
import com.example.parishsync.data.local.PrayerRequestEntity
import com.example.parishsync.data.local.PushSubscriptionEntity
import com.example.parishsync.data.local.RideShareEntity
import com.example.parishsync.data.local.ServingScheduleEntity
import com.example.parishsync.data.local.SmallGroupEntity
import com.example.parishsync.data.local.UserProfileEntity
import com.example.parishsync.data.local.VolunteerAssignmentEntity
import com.example.parishsync.data.model.AppRole
import com.example.parishsync.data.model.Congregation
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.District
import com.example.parishsync.data.model.MinistryDutyType
import com.example.parishsync.data.model.ParishPolicy
import com.example.parishsync.data.model.PastoralRequestType
import com.example.parishsync.data.model.RequestStatus
import com.example.parishsync.data.model.RidePostType
import com.example.parishsync.data.model.UserRole
import com.example.parishsync.data.model.VolunteerSubTask
import com.example.parishsync.data.remote.MailtrapConfig
import com.example.parishsync.data.remote.SupabaseConfig
import com.example.parishsync.data.repository.ParishRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ParishUiState(
  val isInitialized: Boolean = false,
  val activeUser: UserProfileEntity? = null,
  val activeCongregation: Congregation = Congregation.NONE,
  val activeRole: UserRole = UserRole.MEMBER,
  val activeDelegatedRole: DelegatedRole = DelegatedRole.NONE,
  val activeRoles: Set<AppRole> = setOf(AppRole.MEMBER),
  val selectedAdminCongregationId: String = "",
  val districts: List<District> = emptyList(),
  val congregations: List<Congregation> = emptyList(),
  val allUsers: List<UserProfileEntity> = emptyList(),
  val rosterMembers: List<UserProfileEntity> = emptyList(),
  val announcements: List<AnnouncementEntity> = emptyList(),
  val volunteerAssignments: List<VolunteerAssignmentEntity> = emptyList(),
  val servingSchedules: List<ServingScheduleEntity> = emptyList(),
  val hymnSelections: List<HymnSelectionEntity> = emptyList(),
  val prayerRequests: List<PrayerRequestEntity> = emptyList(),
  val rideShares: List<RideShareEntity> = emptyList(),
  val smallGroups: List<SmallGroupEntity> = emptyList(),
  val pdfScheduleJobs: List<PdfScheduleJobEntity> = emptyList(),
  val pushSubscriptions: List<PushSubscriptionEntity> = emptyList(),
  val miniHqResources: List<MiniHqResourceEntity> = emptyList(),
  val supabaseConfig: SupabaseConfig = SupabaseConfig(),
  val syncStatusMessage: String = "",
  val mailtrapConfig: MailtrapConfig = MailtrapConfig(),
  val mailtrapStatusMessage: String = "",
  val bannerFeedback: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ParishViewModel(
  private val repository: ParishRepository
) : ViewModel() {

  private val _bannerFeedback = MutableStateFlow<String?>(null)
  val bannerFeedback: StateFlow<String?> = _bannerFeedback.asStateFlow()

  private val _selectedAdminCongregationId = MutableStateFlow("")
  val selectedAdminCongregationId: StateFlow<String> = _selectedAdminCongregationId.asStateFlow()

  fun selectAdminCongregationFilter(congregationId: String) {
    _selectedAdminCongregationId.value = congregationId
  }

  fun showBannerFeedback(message: String) {
    _bannerFeedback.value = message
  }

  init {
    viewModelScope.launch(Dispatchers.IO) {
      repository.ensureSeedDataPopulated()
    }
  }

  val uiState: StateFlow<ParishUiState> = combine(
    repository.observeActiveUser(),
    repository.observeCongregations(),
    _selectedAdminCongregationId
  ) { activeUser, congregations, adminCongFilter ->
    Triple(activeUser, congregations, adminCongFilter)
  }.flatMapLatest { (activeUser, congregations, adminCongFilter) ->
    if (activeUser == null) {
      // Fast unauthenticated state: Observe only account/config flows needed for Login
      val accountAndMailtrapFlow = combine(
        repository.observeAllUsers(),
        repository.mailtrapConfig,
        repository.mailtrapStatusMessage,
        _bannerFeedback
      ) { allUsers, mtConfig, mtStatus, feedback ->
        AccountMailtrapBundle(allUsers, mtConfig, mtStatus, feedback)
      }
      val syncFlow = combine(
        repository.supabaseConfig,
        repository.syncStatusMessage
      ) { sbConfig, syncMsg ->
        sbConfig to syncMsg
      }

      combine(accountAndMailtrapFlow, syncFlow) { accountMt, (sbConfig, syncMsg) ->
        ParishUiState(
          isInitialized = true,
          activeUser = null,
          activeCongregation = congregations.firstOrNull() ?: Congregation.NONE,
          activeRole = UserRole.MEMBER,
          activeDelegatedRole = DelegatedRole.NONE,
          congregations = congregations,
          allUsers = accountMt.allUsers,
          supabaseConfig = sbConfig,
          syncStatusMessage = syncMsg,
          mailtrapConfig = accountMt.mailtrapConfig,
          mailtrapStatusMessage = accountMt.mailtrapStatus,
          bannerFeedback = accountMt.feedback
        )
      }
    } else {
      val effectiveRoles = activeUser.effectiveRoles
      val isAdmin = ParishPolicy.isGlobalAdmin(effectiveRoles)
      val scopedCongId = if (isAdmin) adminCongFilter else activeUser.congregationId
      val congregation = when {
        isAdmin && scopedCongId.isBlank() -> Congregation(id = "", displayName = "All Congregations")
        scopedCongId.isNotBlank() -> congregations.firstOrNull { it.id == scopedCongId }
          ?: Congregation(id = scopedCongId, displayName = "Congregation $scopedCongId")
        else -> Congregation.NONE
      }
      val role = activeUser.role
      val delegatedRole = activeUser.delegatedRole
      val userName = activeUser.fullName

      val rosterFlow = if (isAdmin && scopedCongId.isBlank()) {
        repository.observeAllUsers()
      } else {
        repository.observeCongregationRoster(scopedCongId)
      }
      val assignmentsFlow = if (isAdmin && scopedCongId.isBlank()) {
        repository.observeAllVolunteerAssignments()
      } else {
        repository.observeVolunteerAssignments(scopedCongId)
      }
      val servingFlow = if (isAdmin && scopedCongId.isBlank()) {
        repository.observeAllServingSchedules()
      } else {
        repository.observeServingSchedules(scopedCongId)
      }

      val coreDataFlow = combine(
        rosterFlow,
        repository.observeAnnouncements(scopedCongId),
        assignmentsFlow,
        servingFlow,
        repository.observePrayerRequests(scopedCongId, role, userName)
      ) { roster, announcements, assignments, servingSchedules, prayers ->
        CoreDataBundle(roster, announcements, assignments, servingSchedules, prayers)
      }

      val communityAndMinistryFlow = combine(
        repository.observeRideShares(scopedCongId),
        repository.observeSmallGroups(scopedCongId),
        repository.observePdfScheduleJobs(scopedCongId),
        repository.supabaseConfig,
        repository.syncStatusMessage
      ) { rides, groups, pdfJobs, sbConfig, syncMsg ->
        MinistryDataBundle(rides, groups, pdfJobs, sbConfig, syncMsg)
      }

      val accountAndMailtrapFlow = combine(
        repository.observeAllUsers(),
        repository.mailtrapConfig,
        repository.mailtrapStatusMessage,
        _bannerFeedback
      ) { allUsers, mtConfig, mtStatus, feedback ->
        AccountMailtrapBundle(allUsers, mtConfig, mtStatus, feedback)
      }

      val districtAndMiniHqFlow = combine(
        repository.observeDistricts(),
        repository.observeMiniHqResources()
      ) { districts, miniHqResources ->
        districts to miniHqResources
      }

      combine(
        coreDataFlow,
        communityAndMinistryFlow,
        accountAndMailtrapFlow,
        repository.observeUserPushSubscriptions(activeUser.id),
        districtAndMiniHqFlow
      ) { core, ministry, accountMt, pushSubs, (districts, miniHqResources) ->
        ParishUiState(
          isInitialized = true,
          activeUser = activeUser,
          activeCongregation = congregation,
          activeRole = role,
          activeDelegatedRole = delegatedRole,
          activeRoles = effectiveRoles,
          selectedAdminCongregationId = if (isAdmin) adminCongFilter else "",
          districts = districts,
          congregations = congregations,
          allUsers = accountMt.allUsers,
          rosterMembers = core.roster,
          announcements = core.announcements,
          volunteerAssignments = core.assignments,
          servingSchedules = core.servingSchedules,
          hymnSelections = emptyList(),
          prayerRequests = core.prayers,
          rideShares = ministry.rides,
          smallGroups = ministry.groups,
          pdfScheduleJobs = ministry.pdfJobs,
          pushSubscriptions = pushSubs,
          miniHqResources = miniHqResources,
          supabaseConfig = ministry.sbConfig,
          syncStatusMessage = ministry.syncMsg,
          mailtrapConfig = accountMt.mailtrapConfig,
          mailtrapStatusMessage = accountMt.mailtrapStatus,
          bannerFeedback = accountMt.feedback
        )
      }
    }
  }
  .flowOn(Dispatchers.Default)
  .stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = ParishUiState()
  )

  fun createCongregation(
    displayName: String,
    address: String = "",
    serviceTimes: String = "",
    rectorName: String = "",
    sundayServiceTime: String = "10:30 AM",
    wednesdayServiceTime: String = "7:30 PM",
    districtId: String = ""
  ) {
    viewModelScope.launch {
      repository.createOrUpdateCongregation(
        displayName = displayName,
        address = address,
        serviceTimes = serviceTimes,
        rectorName = rectorName,
        sundayServiceTime = sundayServiceTime,
        wednesdayServiceTime = wednesdayServiceTime,
        districtId = districtId
      )
        .onSuccess { cong ->
          _bannerFeedback.value = "Saved congregation '${cong.displayName}'."
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message
        }
    }
  }

  fun createOrUpdateDistrict(id: String = "", name: String, regionCode: String = "") {
    viewModelScope.launch {
      repository.createOrUpdateDistrict(id, name, regionCode)
        .onSuccess { d -> _bannerFeedback.value = "Saved district '${d.name}'." }
        .onFailure { err -> _bannerFeedback.value = err.message }
    }
  }

  fun deleteDistrict(districtId: String) {
    viewModelScope.launch {
      repository.deleteDistrict(districtId)
        .onSuccess { _bannerFeedback.value = "District removed." }
        .onFailure { err -> _bannerFeedback.value = err.message }
    }
  }

  fun moveCongregationToDistrict(congregationId: String, newDistrictId: String) {
    viewModelScope.launch {
      repository.moveCongregationToDistrict(congregationId, newDistrictId)
        .onSuccess { _bannerFeedback.value = "Congregation moved to new district." }
        .onFailure { err -> _bannerFeedback.value = err.message }
    }
  }

  fun deleteCongregation(congregationId: String) {
    viewModelScope.launch {
      repository.deleteCongregation(congregationId)
        .onSuccess { _bannerFeedback.value = "Congregation removed." }
        .onFailure { err -> _bannerFeedback.value = err.message }
    }
  }

  fun adminUpdateAndTransferUser(
    targetUser: UserProfileEntity,
    fullName: String,
    email: String,
    preferredMinistries: String,
    roles: Set<AppRole>,
    newCongregationId: String
  ) {
    val actor = uiState.value.activeUser ?: return
    viewModelScope.launch {
      repository.adminUpdateAndTransferUser(
        targetUser = targetUser,
        fullName = fullName,
        email = email,
        preferredMinistries = preferredMinistries,
        roles = roles,
        newCongregationId = newCongregationId,
        actor = actor
      )
        .onSuccess { _bannerFeedback.value = "Updated ${fullName.trim()} and synced congregation/roles." }
        .onFailure { err -> _bannerFeedback.value = err.message }
    }
  }

  fun updateCongregationServiceTimes(
    congregationId: String,
    sundayServiceTime: String,
    wednesdayServiceTime: String
  ) {
    viewModelScope.launch {
      repository.updateCongregationServiceTimes(congregationId, sundayServiceTime, wednesdayServiceTime)
        .onSuccess { cong ->
          _bannerFeedback.value = "Updated service times for ${cong.displayName} (Sun ${cong.sundayServiceTime}, Wed ${cong.wednesdayServiceTime})."
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message
        }
    }
  }

  fun updateUserRoles(targetUser: UserProfileEntity, roles: Set<AppRole>) {
    val actor = uiState.value.activeUser ?: return
    viewModelScope.launch {
      repository.updateUserRoles(targetUser, roles, actor)
        .onSuccess {
          _bannerFeedback.value = "Updated roles for ${targetUser.fullName}: ${ParishPolicy.formatRoleBadges(roles)}"
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message
        }
    }
  }

  fun clearBannerFeedback() {
    _bannerFeedback.value = null
  }

  fun signInWithLockedAccount(email: String, password: String) {
    viewModelScope.launch {
      _selectedAdminCongregationId.value = ""
      repository.signInWithLockedAccount(email, password)
        .onSuccess { profile ->
          val roleSummary = ParishPolicy.formatRoleBadges(profile.effectiveRoles)
          _bannerFeedback.value = "Signed in as ${profile.fullName} ($roleSummary)"
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message ?: "Sign-in failed."
        }
    }
  }

  fun acceptEmailInviteAndSetPassword(email: String, inviteCode: String, newPassword: String) {
    viewModelScope.launch {
      _selectedAdminCongregationId.value = ""
      repository.acceptEmailInviteAndSetPassword(email, inviteCode, newPassword)
        .onSuccess { profile ->
          _bannerFeedback.value = "Account activated for ${profile.fullName}! Role locked to ${profile.role.badgeLabel}."
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message ?: "Invitation activation failed."
        }
    }
  }

  fun createAndInviteAccountByEmail(
    email: String,
    fullName: String,
    congregation: Congregation,
    assignedRole: UserRole,
    delegatedRole: DelegatedRole,
    selectedRoles: Set<AppRole> = emptySet()
  ) {
    val actor = uiState.value.activeUser ?: return
    viewModelScope.launch {
      repository.createAndInviteAccountByEmail(
        email = email,
        fullName = fullName,
        congregation = congregation,
        assignedRole = assignedRole,
        delegatedRole = delegatedRole,
        actor = actor,
        selectedRoles = selectedRoles
      ).onSuccess { msg ->
        _bannerFeedback.value = msg
      }.onFailure { err ->
        _bannerFeedback.value = err.message
      }
    }
  }

  fun delegateRoleToMember(targetUser: UserProfileEntity, delegatedRole: DelegatedRole) {
    val actor = uiState.value.activeUser ?: return
    viewModelScope.launch {
      repository.delegateRoleToCongregationMember(targetUser, delegatedRole, actor)
        .onSuccess {
          _bannerFeedback.value = "Updated ${targetUser.fullName}'s delegated role to '${delegatedRole.displayName}'."
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message
        }
    }
  }

  fun signOutToLoginScreen() {
    viewModelScope.launch {
      _selectedAdminCongregationId.value = ""
      repository.signOut()
      _bannerFeedback.value = "Signed out of NAC Portal session."
    }
  }

  fun resetAllDataToFreshBootstrap() {
    viewModelScope.launch {
      repository.resetAllDataToFreshBootstrap()
      _bannerFeedback.value = "Cleared all data! App reset to fresh state with only the Bootstrap Admin account."
    }
  }

  fun publishAnnouncementOrAlert(
    title: String,
    summary: String,
    body: String,
    category: String,
    isUrgent: Boolean,
    serviceDate: String,
    liturgicalOrder: String
  ) {
    val state = uiState.value
    viewModelScope.launch {
      repository.publishAnnouncementOrBroadcast(
        congregationId = state.activeCongregation.id,
        title = title,
        summary = summary,
        body = body,
        category = category,
        isUrgent = isUrgent,
        serviceDate = serviceDate,
        liturgicalOrder = liturgicalOrder,
        authorName = state.activeUser?.fullName ?: state.activeRole.title,
        actorRole = state.activeRole
      ).onSuccess {
        _bannerFeedback.value = if (isUrgent) {
          "Urgent Push Broadcast sent & published to Live Bulletin!"
        } else {
          "Published '$title' to ${state.activeCongregation.displayName} feed."
        }
      }.onFailure { err ->
        _bannerFeedback.value = err.message
      }
    }
  }

  fun toggleAssignmentConfirmed(assignment: VolunteerAssignmentEntity) {
    viewModelScope.launch {
      repository.toggleVolunteerConfirmation(assignment)
      val statusText = if (!assignment.isConfirmed) "Confirmed" else "Unconfirmed"
      _bannerFeedback.value = "$statusText ${assignment.dutyType.displayName} (${assignment.assignmentTier}) for ${assignment.serviceDate}"
    }
  }

  fun scheduleDutyNotificationReminder(assignment: VolunteerAssignmentEntity) {
    viewModelScope.launch {
      repository.triggerVolunteerReminder(assignment)
        .onSuccess { summary ->
          _bannerFeedback.value = summary
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message
        }
    }
  }

  fun enablePhoneNotifications() {
    val user = uiState.value.activeUser ?: return
    viewModelScope.launch {
      repository.registerCurrentDevicePushSubscription(user)
        .onSuccess { sub ->
          _bannerFeedback.value = "Phone notifications enabled on ${sub.deviceLabel}."
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message
        }
    }
  }

  fun disablePhoneNotifications() {
    val user = uiState.value.activeUser ?: return
    viewModelScope.launch {
      repository.disableCurrentDevicePushSubscription(user)
        .onSuccess {
          _bannerFeedback.value = "Phone notifications disabled for this device."
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message
        }
    }
  }

  fun updateOwnProfile(
    fullName: String,
    email: String,
    phone: String,
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    onResult: (Result<UserProfileEntity>) -> Unit = {}
  ) {
    val user = uiState.value.activeUser ?: return
    viewModelScope.launch {
      val result = repository.updateOwnProfile(
        actor = user,
        fullName = fullName,
        email = email,
        phone = phone,
        currentPassword = currentPassword,
        newPassword = newPassword,
        confirmNewPassword = confirmNewPassword
      )
      result
        .onSuccess { _bannerFeedback.value = "Profile updated." }
        .onFailure { err -> _bannerFeedback.value = err.message }
      onResult(result)
    }
  }

  fun updateNotificationPreferences(
    preferPushReminders: Boolean,
    preferEmailReminders: Boolean
  ) {
    val user = uiState.value.activeUser ?: return
    viewModelScope.launch {
      repository.updateNotificationPreferences(user, preferPushReminders, preferEmailReminders)
        .onSuccess {
          _bannerFeedback.value = "Notification preferences saved."
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message
        }
    }
  }

  fun sendTestPhoneNotification() {
    val user = uiState.value.activeUser ?: return
    viewModelScope.launch {
      repository.sendTestPhoneNotification(user)
        .onSuccess { msg ->
          _bannerFeedback.value = msg
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message
        }
    }
  }

  fun toggleSubTaskCompletion(assignment: VolunteerAssignmentEntity, subTaskId: String) {
    viewModelScope.launch {
      repository.toggleSubTaskCompletion(assignment, subTaskId)
    }
  }

  fun addVolunteerRosterDuty(
    dutyType: MinistryDutyType,
    customTaskTitle: String = "",
    assignmentTier: String,
    assigneeName: String,
    backupAssigneeName: String = "",
    serviceDate: String,
    serviceTime: String,
    notes: String,
    subTasks: List<VolunteerSubTask> = emptyList()
  ) {
    val state = uiState.value
    val targetCongId = if (state.activeCongregation.id.isNotBlank()) {
      state.activeCongregation.id
    } else {
      state.activeUser?.congregationId.orEmpty()
    }
    val taskLabel = customTaskTitle.ifBlank { dutyType.displayName }
    viewModelScope.launch {
      repository.addManualVolunteerAssignment(
        congregationId = targetCongId,
        dutyType = dutyType,
        customTaskTitle = customTaskTitle,
        assignmentTier = assignmentTier.ifBlank { "MAIN" },
        assigneeName = assigneeName,
        backupAssigneeName = backupAssigneeName,
        serviceDate = serviceDate,
        serviceTime = serviceTime,
        notes = notes,
        subTasks = subTasks
      )
      _bannerFeedback.value = "Added $assignmentTier $taskLabel for $assigneeName on $serviceDate"
    }
  }

  fun addVolunteerRosterDuty(
    dutyType: MinistryDutyType,
    assigneeName: String,
    serviceDate: String,
    serviceTime: String,
    notes: String
  ) {
    addVolunteerRosterDuty(
      dutyType = dutyType,
      assignmentTier = "MAIN",
      assigneeName = assigneeName,
      serviceDate = serviceDate,
      serviceTime = serviceTime,
      notes = notes
    )
  }

  fun deleteVolunteerAssignment(assignmentId: String) {
    viewModelScope.launch {
      repository.deleteVolunteerAssignment(assignmentId)
      _bannerFeedback.value = "Volunteer assignment removed."
    }
  }

  fun commitParsedScheduleCards(
    pdfFileName: String,
    cards: List<VolunteerAssignmentEntity>
  ) {
    val state = uiState.value
    val user = state.activeUser ?: return
    val targetCongId = if (state.activeCongregation.id.isNotBlank()) {
      state.activeCongregation.id
    } else {
      user.congregationId
    }
    viewModelScope.launch {
      repository.commitParsedVolunteerAssignments(
        congregationId = targetCongId,
        pdfFileName = pdfFileName,
        assignments = cards,
        uploadedBy = user.fullName,
        actorRole = state.activeRole,
        actorDelegatedRole = state.activeDelegatedRole
      ).onSuccess { msg ->
        _bannerFeedback.value = msg
      }.onFailure { err ->
        _bannerFeedback.value = err.message
      }
    }
  }

  fun addServingSchedule(
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
  ) {
    val state = uiState.value
    viewModelScope.launch {
      repository.addServingSchedule(
        congregationId = state.activeCongregation.id,
        serviceTitle = serviceTitle,
        serviceDate = serviceDate,
        serviceTime = serviceTime,
        officiantMinister = officiantMinister,
        officiantRank = officiantRank,
        assistingMinisters = assistingMinisters,
        choirConductor = choirConductor,
        organist = organist,
        serviceTheme = serviceTheme,
        bibleWord = bibleWord,
        specialServiceTag = specialServiceTag,
        location = location,
        notes = notes
      )
      _bannerFeedback.value = "Divine Service scheduled for $serviceDate"
    }
  }

  fun deleteServingSchedule(scheduleId: String) {
    viewModelScope.launch {
      repository.deleteServingSchedule(scheduleId)
      _bannerFeedback.value = "Divine Service removed."
    }
  }

  fun loadSampleNacServingSchedules() {
    val state = uiState.value
    val congId = state.activeCongregation.id
    viewModelScope.launch {
      val samples = ParishSeedData.sampleNacServingSchedules(congId)
      for (item in samples) {
        repository.addServingSchedule(
          congregationId = congId,
          serviceTitle = item.serviceTitle,
          serviceDate = item.serviceDate,
          serviceTime = item.serviceTime,
          officiantMinister = item.officiantMinister,
          officiantRank = item.officiantRank,
          assistingMinisters = item.assistingMinisters,
          choirConductor = item.choirConductor,
          organist = item.organist,
          serviceTheme = item.serviceTheme,
          bibleWord = item.bibleWord,
          specialServiceTag = item.specialServiceTag,
          location = item.location,
          notes = item.notes
        )
      }
      _bannerFeedback.value = "Loaded NAC Canada Divine Services schedule!"
    }
  }

  fun submitPrayerOrVisitRequest(
    requesterName: String,
    requesterContact: String,
    requestType: PastoralRequestType,
    content: String,
    isPrivateToClergy: Boolean
  ) {
    val state = uiState.value
    viewModelScope.launch {
      repository.submitPrayerOrVisitRequest(
        congregationId = state.activeCongregation.id,
        requesterName = requesterName,
        requesterContact = requesterContact,
        requestType = requestType,
        content = content,
        isPrivateToClergy = isPrivateToClergy
      ).onSuccess {
        _bannerFeedback.value = if (isPrivateToClergy) {
          "Confidential request sent directly to the Ministerial Queue."
        } else {
          "Prayer intention shared with the Congregation Prayer Chain."
        }
      }
    }
  }

  fun updatePrayerRequestTriage(
    requestId: String,
    newStatus: RequestStatus,
    ministerialNotes: String
  ) {
    val state = uiState.value
    viewModelScope.launch {
      repository.updatePrayerRequestStatus(
        requestId = requestId,
        newStatus = newStatus,
        ministerialNotes = ministerialNotes,
        actorRole = state.activeRole
      ).onSuccess {
        _bannerFeedback.value = "Updated pastoral request status to '${newStatus.displayName}'"
      }.onFailure { err ->
        _bannerFeedback.value = err.message
      }
    }
  }

  fun postRideShareListing(
    postType: RidePostType,
    memberName: String,
    phoneContact: String,
    neighborhood: String,
    serviceDate: String,
    serviceTime: String,
    seatsCount: Int,
    notes: String
  ) {
    val state = uiState.value
    viewModelScope.launch {
      repository.createRideSharePost(
        congregationId = state.activeCongregation.id,
        postType = postType,
        memberName = memberName,
        phoneContact = phoneContact,
        neighborhood = neighborhood,
        serviceDate = serviceDate,
        serviceTime = serviceTime,
        seatsCount = seatsCount,
        notes = notes
      ).onSuccess {
        _bannerFeedback.value = "Posted to ${state.activeCongregation.displayName} Ride Sharing Board!"
      }
    }
  }

  fun connectRideShareMatch(ride: RideShareEntity) {
    val partnerName = uiState.value.activeUser?.fullName ?: "Congregation Volunteer"
    viewModelScope.launch {
      repository.matchRideSharePost(ride.id, partnerName)
      _bannerFeedback.value = "Coordinated ride between ${ride.memberName} & $partnerName!"
    }
  }

  fun toggleDsgEnrollment(group: SmallGroupEntity) {
    viewModelScope.launch {
      repository.toggleSmallGroupEnrollment(group)
      _bannerFeedback.value = if (!group.isUserEnrolled) {
        "Joined '${group.name}' Small Group"
      } else {
        "Left '${group.name}' Small Group"
      }
    }
  }

  fun saveDsgPastoralNotes(
    group: SmallGroupEntity,
    updatedTopic: String,
    updatedPastoralLog: String,
    updatedServiceNotes: String
  ) {
    val state = uiState.value
    viewModelScope.launch {
      repository.updateSmallGroupPastoralLog(
        group = group,
        updatedTopic = updatedTopic,
        updatedPastoralLog = updatedPastoralLog,
        updatedServiceNotes = updatedServiceNotes,
        actorRole = state.activeRole
      ).onSuccess {
        _bannerFeedback.value = "Updated DSG pastoral log for '${group.name}'"
      }.onFailure { err ->
        _bannerFeedback.value = err.message
      }
    }
  }

  fun runSmartPdfScheduleIndexer(
    pdfFileName: String,
    pdfUri: Uri? = null,
    customScheduleText: String? = null
  ) {
    val state = uiState.value
    viewModelScope.launch {
      repository.enqueueSmartPdfScheduleIndexing(
        congregationId = state.activeCongregation.id,
        pdfFileName = pdfFileName,
        uploadedBy = state.activeUser?.fullName ?: state.activeRole.badgeLabel,
        pdfUri = pdfUri,
        customScheduleText = customScheduleText,
        actorRole = state.activeRole,
        actorDelegatedRole = state.activeDelegatedRole
      ).onSuccess { report ->
        _bannerFeedback.value = report
      }.onFailure { err ->
        _bannerFeedback.value = err.message
      }
    }
  }

  fun emailSchedulePdfToCongregation(
    additionalRecipientEmails: String,
    subject: String,
    customMessage: String,
    pdfFileName: String,
    pdfUri: Uri?,
    scheduleSummaryText: String
  ) {
    val state = uiState.value
    viewModelScope.launch {
      repository.emailSchedulePdfToCongregation(
        congregation = state.activeCongregation,
        additionalRecipientEmails = additionalRecipientEmails,
        subject = subject,
        customMessage = customMessage,
        pdfFileName = pdfFileName,
        pdfUri = pdfUri,
        scheduleSummaryText = scheduleSummaryText,
        actorRole = state.activeRole,
        actorDelegatedRole = state.activeDelegatedRole
      ).onSuccess { msg ->
        _bannerFeedback.value = msg
      }.onFailure { err ->
        _bannerFeedback.value = err.message
      }
    }
  }

  fun sendMailtrapTestEmail(recipientEmail: String) {
    viewModelScope.launch {
      repository.sendMailtrapIntegrationTest(recipientEmail)
        .onSuccess { msg ->
          _bannerFeedback.value = msg
        }
        .onFailure { err ->
          _bannerFeedback.value = err.message
        }
    }
  }

  fun updateMailtrapConfig(apiToken: String, senderEmail: String, senderName: String) {
    repository.configureMailtrap(apiToken, senderEmail, senderName)
    _bannerFeedback.value = "Updated Mailtrap configuration."
  }

  fun updateSupabaseCredentials(url: String, anonKey: String) {
    repository.configureLiveSupabase(url, anonKey)
    viewModelScope.launch {
      repository.syncWithSupabaseNow(uiState.value.activeCongregation.id)
      _bannerFeedback.value = if (url.isNotBlank()) {
        "Supabase Cloud credentials applied & synced!"
      } else {
        "Switched to Offline-First Local Room SQLite mode."
      }
    }
  }

  private data class CoreDataBundle(
    val roster: List<UserProfileEntity>,
    val announcements: List<AnnouncementEntity>,
    val assignments: List<VolunteerAssignmentEntity>,
    val servingSchedules: List<ServingScheduleEntity>,
    val prayers: List<PrayerRequestEntity>
  )

  private data class MinistryDataBundle(
    val rides: List<RideShareEntity>,
    val groups: List<SmallGroupEntity>,
    val pdfJobs: List<PdfScheduleJobEntity>,
    val sbConfig: SupabaseConfig,
    val syncMsg: String
  )

  private data class AccountMailtrapBundle(
    val allUsers: List<UserProfileEntity>,
    val mailtrapConfig: MailtrapConfig,
    val mailtrapStatus: String,
    val feedback: String?
  )

  companion object {
    fun provideFactory(repository: ParishRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return ParishViewModel(repository) as T
        }
      }
  }
}
