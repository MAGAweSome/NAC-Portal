package com.example.parishsync.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.parishsync.data.local.AnnouncementEntity
import com.example.parishsync.data.local.CongregationEntity
import com.example.parishsync.data.local.DistrictEntity
import com.example.parishsync.data.local.HymnSelectionEntity
import com.example.parishsync.data.local.MiniHqResourceEntity
import com.example.parishsync.data.local.ParishDao
import com.example.parishsync.data.local.ParishDatabase
import com.example.parishsync.data.local.ParishSeedData
import com.example.parishsync.data.local.PdfScheduleJobEntity
import com.example.parishsync.data.local.PrayerRequestEntity
import com.example.parishsync.data.local.PushSubscriptionEntity
import com.example.parishsync.data.local.RideShareEntity
import com.example.parishsync.data.local.ServingScheduleEntity
import com.example.parishsync.data.local.SmallGroupEntity
import com.example.parishsync.data.local.UserProfileEntity
import com.example.parishsync.data.local.UserRoleAssignmentEntity
import com.example.parishsync.data.local.VolunteerAssignmentEntity
import com.example.parishsync.data.local.toDomain
import com.example.parishsync.data.local.toEntity
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
import com.example.parishsync.data.model.canUploadAndEmailVolunteerSchedule
import com.example.parishsync.data.remote.MailtrapConfig
import com.example.parishsync.data.remote.MailtrapEmailService
import com.example.parishsync.data.remote.SupabaseClientService
import com.example.parishsync.data.remote.SupabaseConfig
import com.example.parishsync.workers.ParishNotificationHelper
import com.example.parishsync.workers.PdfScheduleParseWorker
import com.example.parishsync.workers.PdfScheduleParser
import com.example.parishsync.workers.ScheduleSyncWorker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

interface ParishRepository {
  val supabaseConfig: StateFlow<SupabaseConfig>
  val syncStatusMessage: StateFlow<String>
  val mailtrapConfig: StateFlow<MailtrapConfig>
  val mailtrapStatusMessage: StateFlow<String>

  fun observeActiveUser(): Flow<UserProfileEntity?>
  fun observeAllUsers(): Flow<List<UserProfileEntity>>
  fun observeDistricts(): Flow<List<District>>
  suspend fun createOrUpdateDistrict(id: String = "", name: String, regionCode: String = ""): Result<District>
  suspend fun deleteDistrict(districtId: String): Result<Unit>
  fun observeCongregations(): Flow<List<Congregation>>
  suspend fun getCongregationById(id: String): Congregation?
  suspend fun createOrUpdateCongregation(
    displayName: String,
    address: String,
    serviceTimes: String,
    rectorName: String,
    sundayServiceTime: String = "10:30 AM",
    wednesdayServiceTime: String = "7:30 PM",
    districtId: String = ""
  ): Result<Congregation>
  suspend fun moveCongregationToDistrict(congregationId: String, newDistrictId: String): Result<Unit>
  suspend fun deleteCongregation(congregationId: String): Result<Unit>
  suspend fun adminUpdateAndTransferUser(
    targetUser: UserProfileEntity,
    fullName: String,
    email: String,
    preferredMinistries: String,
    roles: Set<AppRole>,
    newCongregationId: String,
    actor: UserProfileEntity
  ): Result<Unit>
  suspend fun updateCongregationServiceTimes(
    congregationId: String,
    sundayServiceTime: String,
    wednesdayServiceTime: String
  ): Result<Congregation>
  fun observeCongregationRoster(congregationId: String): Flow<List<UserProfileEntity>>
  fun observeAnnouncements(congregationId: String): Flow<List<AnnouncementEntity>>
  fun observeVolunteerAssignments(congregationId: String): Flow<List<VolunteerAssignmentEntity>>
  fun observeAllVolunteerAssignments(): Flow<List<VolunteerAssignmentEntity>>
  fun observeHymnSelections(congregationId: String): Flow<List<HymnSelectionEntity>>
  fun observePrayerRequests(congregationId: String, activeRole: UserRole, activeUserName: String): Flow<List<PrayerRequestEntity>>
  fun observeRideShares(congregationId: String): Flow<List<RideShareEntity>>
  fun observeSmallGroups(congregationId: String): Flow<List<SmallGroupEntity>>
  fun observePdfScheduleJobs(congregationId: String): Flow<List<PdfScheduleJobEntity>>

  suspend fun ensureSeedDataPopulated()
  suspend fun resetAllDataToFreshBootstrap()

  suspend fun signInWithLockedAccount(
    email: String,
    password: String
  ): Result<UserProfileEntity>

  suspend fun acceptEmailInviteAndSetPassword(
    email: String,
    inviteCode: String,
    newPassword: String
  ): Result<UserProfileEntity>

  suspend fun createAndInviteAccountByEmail(
    email: String,
    fullName: String,
    congregation: Congregation,
    assignedRole: UserRole,
    delegatedRole: DelegatedRole,
    actor: UserProfileEntity,
    selectedRoles: Set<AppRole> = emptySet()
  ): Result<String>

  suspend fun signOut()

  suspend fun delegateRoleToCongregationMember(
    targetUser: UserProfileEntity,
    delegatedRole: DelegatedRole,
    actor: UserProfileEntity
  ): Result<Unit>

  suspend fun updateUserRoles(
    targetUser: UserProfileEntity,
    roles: Set<AppRole>,
    actor: UserProfileEntity
  ): Result<Unit>

  suspend fun publishAnnouncementOrBroadcast(
    congregationId: String,
    title: String,
    summary: String,
    body: String,
    category: String,
    isUrgent: Boolean,
    serviceDate: String,
    liturgicalOrder: String,
    authorName: String,
    actorRole: UserRole
  ): Result<Unit>

  suspend fun toggleVolunteerConfirmation(assignment: VolunteerAssignmentEntity)
  suspend fun toggleSubTaskCompletion(assignment: VolunteerAssignmentEntity, subTaskId: String)
  suspend fun triggerVolunteerReminder(assignment: VolunteerAssignmentEntity): Result<String>
  fun observeUserPushSubscriptions(userId: String): Flow<List<PushSubscriptionEntity>>
  fun observeMiniHqResources(): Flow<List<MiniHqResourceEntity>>
  suspend fun registerCurrentDevicePushSubscription(user: UserProfileEntity): Result<PushSubscriptionEntity>
  suspend fun disableCurrentDevicePushSubscription(user: UserProfileEntity): Result<Unit>
  suspend fun sendTestPhoneNotification(user: UserProfileEntity): Result<String>
  suspend fun updateOwnProfile(
    actor: UserProfileEntity,
    fullName: String,
    email: String,
    phone: String,
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String
  ): Result<UserProfileEntity>
  suspend fun updateNotificationPreferences(
    user: UserProfileEntity,
    preferPushReminders: Boolean,
    preferEmailReminders: Boolean
  ): Result<UserProfileEntity>

  suspend fun addManualVolunteerAssignment(
    congregationId: String,
    dutyType: MinistryDutyType,
    customTaskTitle: String = "",
    assignmentTier: String = "MAIN",
    assigneeName: String,
    backupAssigneeName: String = "",
    serviceDate: String,
    serviceTime: String,
    notes: String,
    subTasks: List<VolunteerSubTask> = emptyList()
  )

  suspend fun deleteVolunteerAssignment(assignmentId: String)

  fun observeServingSchedules(congregationId: String): Flow<List<ServingScheduleEntity>>
  fun observeAllServingSchedules(): Flow<List<ServingScheduleEntity>>

  suspend fun addServingSchedule(
    congregationId: String,
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
  )

  suspend fun deleteServingSchedule(scheduleId: String)

  suspend fun submitPrayerOrVisitRequest(
    congregationId: String,
    requesterName: String,
    requesterContact: String,
    requestType: PastoralRequestType,
    content: String,
    isPrivateToClergy: Boolean
  ): Result<Unit>

  suspend fun updatePrayerRequestStatus(
    requestId: String,
    newStatus: RequestStatus,
    ministerialNotes: String,
    actorRole: UserRole
  ): Result<Unit>

  suspend fun createRideSharePost(
    congregationId: String,
    postType: RidePostType,
    memberName: String,
    phoneContact: String,
    neighborhood: String,
    serviceDate: String,
    serviceTime: String,
    seatsCount: Int,
    notes: String
  ): Result<Unit>

  suspend fun matchRideSharePost(rideId: String, matchedPartnerName: String)

  suspend fun toggleSmallGroupEnrollment(group: SmallGroupEntity)
  suspend fun updateSmallGroupPastoralLog(
    group: SmallGroupEntity,
    updatedTopic: String,
    updatedPastoralLog: String,
    updatedServiceNotes: String,
    actorRole: UserRole
  ): Result<Unit>

  suspend fun enqueueSmartPdfScheduleIndexing(
    congregationId: String,
    pdfFileName: String,
    uploadedBy: String,
    pdfUri: Uri?,
    customScheduleText: String?,
    actorRole: UserRole,
    actorDelegatedRole: DelegatedRole
  ): Result<String>

  suspend fun commitParsedVolunteerAssignments(
    congregationId: String,
    pdfFileName: String,
    assignments: List<VolunteerAssignmentEntity>,
    uploadedBy: String,
    actorRole: UserRole,
    actorDelegatedRole: DelegatedRole
  ): Result<String>

  suspend fun emailSchedulePdfToCongregation(
    congregation: Congregation,
    additionalRecipientEmails: String,
    subject: String,
    customMessage: String,
    pdfFileName: String,
    pdfUri: Uri?,
    scheduleSummaryText: String,
    actorRole: UserRole,
    actorDelegatedRole: DelegatedRole
  ): Result<String>

  suspend fun sendMailtrapIntegrationTest(recipientEmail: String): Result<String>
  fun configureMailtrap(apiToken: String, senderEmail: String, senderName: String)

  fun configureLiveSupabase(supabaseUrl: String, supabaseAnonKey: String)
  suspend fun syncWithSupabaseNow(congregationId: String)
}

class OfflineFirstParishRepository(
  private val appContext: Context,
  private val dao: ParishDao,
  private val supabaseService: SupabaseClientService = SupabaseClientService(),
  private val mailtrapService: MailtrapEmailService = MailtrapEmailService()
) : ParishRepository {

  override val supabaseConfig: StateFlow<SupabaseConfig> = supabaseService.config
  override val syncStatusMessage: StateFlow<String> = supabaseService.lastSyncStatus
  override val mailtrapConfig: StateFlow<MailtrapConfig> = mailtrapService.config
  override val mailtrapStatusMessage: StateFlow<String> = mailtrapService.lastEmailLog

  override fun observeActiveUser(): Flow<UserProfileEntity?> = dao.observeActiveUser()

  override fun observeAllUsers(): Flow<List<UserProfileEntity>> = dao.observeAllUsers()

  override fun observeDistricts(): Flow<List<District>> =
    dao.observeDistricts().map { list -> list.map { it.toDomain() } }

  private suspend fun requireGlobalAdmin(): Result<UserProfileEntity> {
    val active = dao.getActiveUserOnce()
      ?: return Result.failure(SecurityException("Authentication required."))
    if (!ParishPolicy.isGlobalAdmin(active.effectiveRoles)) {
      return Result.failure(SecurityException("403 Forbidden: Administrator role required."))
    }
    return Result.success(active)
  }

  override suspend fun createOrUpdateDistrict(id: String, name: String, regionCode: String): Result<District> {
    requireGlobalAdmin().onFailure { return Result.failure(it) }
    val cleanName = name.trim()
    if (cleanName.isBlank()) {
      return Result.failure(IllegalArgumentException("Please enter a district name."))
    }
    val existing = if (id.isNotBlank()) dao.getDistrictById(id) else null
    val entity = existing?.copy(name = cleanName, regionCode = regionCode.trim())
      ?: DistrictEntity(
        id = id.ifBlank { "dist_${UUID.randomUUID().toString().take(8)}" },
        name = cleanName,
        regionCode = regionCode.trim()
      )
    dao.upsertDistrict(entity)
    return Result.success(entity.toDomain())
  }

  override suspend fun deleteDistrict(districtId: String): Result<Unit> {
    requireGlobalAdmin().onFailure { return Result.failure(it) }
    val congregationsInDistrict = dao.getCongregationsOnce().filter { it.districtId == districtId }
    if (congregationsInDistrict.isNotEmpty()) {
      return Result.failure(
        IllegalStateException("Cannot delete district: ${congregationsInDistrict.size} congregation(s) are still assigned to it. Move or delete them first.")
      )
    }
    dao.deleteDistrict(districtId)
    return Result.success(Unit)
  }

  override fun observeCongregations(): Flow<List<Congregation>> =
    dao.observeCongregations().map { list -> list.map { it.toDomain() } }

  override suspend fun getCongregationById(id: String): Congregation? =
    dao.getCongregationById(id)?.toDomain()

  override suspend fun createOrUpdateCongregation(
    displayName: String,
    address: String,
    serviceTimes: String,
    rectorName: String,
    sundayServiceTime: String,
    wednesdayServiceTime: String,
    districtId: String
  ): Result<Congregation> {
    val cleanName = displayName.trim()
    if (cleanName.isBlank()) {
      return Result.failure(IllegalArgumentException("Please enter a congregation name."))
    }
    val cleanSun = sundayServiceTime.trim().ifBlank { "10:30 AM" }
    val cleanWed = wednesdayServiceTime.trim().ifBlank { "7:30 PM" }
    val combinedTimes = serviceTimes.trim().ifBlank { "Sun $cleanSun & Wed $cleanWed" }
    val existing = dao.getCongregationsOnce().firstOrNull { it.displayName.equals(cleanName, ignoreCase = true) }
    val entity = existing?.copy(
      districtId = districtId.ifBlank { existing.districtId },
      address = address.trim(),
      rectorName = rectorName.trim(),
      primaryServiceTimes = combinedTimes,
      sundayServiceTime = cleanSun,
      wednesdayServiceTime = cleanWed
    ) ?: CongregationEntity(
      id = "cong_${UUID.randomUUID().toString().take(8)}",
      districtId = districtId,
      displayName = cleanName,
      address = address.trim(),
      rectorName = rectorName.trim(),
      primaryServiceTimes = combinedTimes,
      sundayServiceTime = cleanSun,
      wednesdayServiceTime = cleanWed
    )
    dao.upsertCongregation(entity)
    return Result.success(entity.toDomain())
  }

  override suspend fun moveCongregationToDistrict(congregationId: String, newDistrictId: String): Result<Unit> {
    requireGlobalAdmin().onFailure { return Result.failure(it) }
    if (congregationId.isBlank() || newDistrictId.isBlank()) {
      return Result.failure(IllegalArgumentException("Valid congregation and target district are required."))
    }
    dao.moveCongregationToDistrict(congregationId, newDistrictId)
    return Result.success(Unit)
  }

  override suspend fun deleteCongregation(congregationId: String): Result<Unit> {
    requireGlobalAdmin().onFailure { return Result.failure(it) }
    val members = dao.getCongregationRosterOnce(congregationId)
    if (members.isNotEmpty()) {
      return Result.failure(
        IllegalStateException("Cannot delete congregation: ${members.size} user(s) are still assigned to it. Transfer them first.")
      )
    }
    dao.deleteCongregation(congregationId)
    return Result.success(Unit)
  }

  override suspend fun adminUpdateAndTransferUser(
    targetUser: UserProfileEntity,
    fullName: String,
    email: String,
    preferredMinistries: String,
    roles: Set<AppRole>,
    newCongregationId: String,
    actor: UserProfileEntity
  ): Result<Unit> {
    if (!ParishPolicy.isGlobalAdmin(actor.effectiveRoles)) {
      return Result.failure(SecurityException("Only global Administrators can edit profiles or transfer users."))
    }
    val cleanName = fullName.trim()
    val cleanEmail = email.trim().lowercase()
    if (cleanName.isBlank() || !cleanEmail.contains("@")) {
      return Result.failure(IllegalArgumentException("Valid name and email are required."))
    }
    val normalized = roles + AppRole.MEMBER
    val legacyPrimary = when {
      AppRole.ADMIN in normalized -> UserRole.ADMINISTRATOR
      AppRole.RECTOR in normalized || AppRole.MINISTER in normalized -> UserRole.MINISTER
      else -> UserRole.MEMBER
    }
    val legacyDelegated = if (AppRole.VOLUNTEER_SCHEDULER in normalized) {
      DelegatedRole.VOLUNTEER_SCHEDULER
    } else {
      DelegatedRole.NONE
    }
    val updated = targetUser.copy(
      fullName = cleanName,
      email = cleanEmail,
      preferredMinistries = preferredMinistries.trim(),
      congregationId = newCongregationId,
      role = legacyPrimary,
      delegatedRole = legacyDelegated,
      roles = normalized
    )
    dao.upsertUser(updated)
    syncUserRolesPivot(updated.id, normalized)
    return Result.success(Unit)
  }

  override suspend fun updateCongregationServiceTimes(
    congregationId: String,
    sundayServiceTime: String,
    wednesdayServiceTime: String
  ): Result<Congregation> {
    val existing = dao.getCongregationById(congregationId)
      ?: return Result.failure(IllegalArgumentException("Congregation not found."))
    val cleanSun = sundayServiceTime.trim().ifBlank { "10:30 AM" }
    val cleanWed = wednesdayServiceTime.trim().ifBlank { "7:30 PM" }
    val updated = existing.copy(
      sundayServiceTime = cleanSun,
      wednesdayServiceTime = cleanWed,
      primaryServiceTimes = "Sun $cleanSun & Wed $cleanWed"
    )
    dao.upsertCongregation(updated)
    return Result.success(updated.toDomain())
  }

  override fun observeCongregationRoster(congregationId: String): Flow<List<UserProfileEntity>> =
    dao.observeCongregationRoster(congregationId)

  override fun observeAnnouncements(congregationId: String): Flow<List<AnnouncementEntity>> =
    dao.observeAnnouncements(congregationId)

  override fun observeVolunteerAssignments(congregationId: String): Flow<List<VolunteerAssignmentEntity>> =
    dao.observeVolunteerAssignments(congregationId)

  override fun observeAllVolunteerAssignments(): Flow<List<VolunteerAssignmentEntity>> =
    dao.observeAllVolunteerAssignments()

  override fun observeHymnSelections(congregationId: String): Flow<List<HymnSelectionEntity>> =
    dao.observeHymnSelections(congregationId)

  override fun observePrayerRequests(
    congregationId: String,
    activeRole: UserRole,
    activeUserName: String
  ): Flow<List<PrayerRequestEntity>> {
    return if (activeRole.canTriagePrayerQueue) {
      dao.observeAllPrayerRequestsForClergy(congregationId)
    } else {
      dao.observeVisiblePrayerRequestsForMember(congregationId, activeUserName)
    }
  }

  override fun observeRideShares(congregationId: String): Flow<List<RideShareEntity>> =
    dao.observeRideShares(congregationId)

  override fun observeSmallGroups(congregationId: String): Flow<List<SmallGroupEntity>> =
    dao.observeSmallGroups(congregationId)

  override fun observePdfScheduleJobs(congregationId: String): Flow<List<PdfScheduleJobEntity>> =
    dao.observePdfScheduleJobs(congregationId)

  override fun observeServingSchedules(congregationId: String): Flow<List<ServingScheduleEntity>> =
    dao.observeServingSchedules(congregationId)

  override fun observeAllServingSchedules(): Flow<List<ServingScheduleEntity>> =
    dao.observeAllServingSchedules()

  override suspend fun ensureSeedDataPopulated() {
    if (dao.getDistrictsOnce().isEmpty()) {
      dao.upsertDistricts(ParishSeedData.defaultCanadianDistricts())
    }
    val woodstock = ParishSeedData.woodstockCongregation()
    val existingWoodstock = dao.getCongregationById(woodstock.id)
    if (existingWoodstock == null) {
      dao.upsertCongregation(woodstock)
    } else if (existingWoodstock.districtId.isBlank()) {
      dao.upsertCongregation(existingWoodstock.copy(districtId = woodstock.districtId))
    }

    val seedAccounts = ParishSeedData.defaultRosterUsers()
    seedAccounts.forEach { seed ->
      val existing = dao.getUserByEmail(seed.email)
      if (existing == null) {
        dao.upsertUser(seed)
        syncUserRolesPivot(seed.id, seed.effectiveRoles)
      } else if (existing.id == "user_marcus_minister") {
        // Ensure Marcus stays Dc. Marcus Grau (Deacon, non-rector, non-admin) and ACTIVE in Woodstock
        val updated = existing.copy(
          fullName = seed.fullName,
          congregationId = seed.congregationId,
          role = seed.role,
          delegatedRole = seed.delegatedRole,
          roles = seed.effectiveRoles,
          passwordHash = existing.passwordHash.ifBlank { seed.passwordHash },
          accountStatus = "ACTIVE"
        )
        dao.upsertUser(updated)
        syncUserRolesPivot(updated.id, updated.effectiveRoles)
      }
    }
    schedulePeriodicBackgroundSync()
  }

  private suspend fun syncUserRolesPivot(userId: String, roles: Set<AppRole>) {
    val effective = roles + AppRole.MEMBER
    dao.clearUserRoles(userId)
    dao.upsertUserRoles(effective.map { UserRoleAssignmentEntity(userId = userId, role = it) })
  }

  override suspend fun resetAllDataToFreshBootstrap() {
    dao.clearAllCongregations()
    dao.clearAllAnnouncements()
    dao.clearAllVolunteerAssignments()
    dao.clearAllHymns()
    dao.clearAllPrayerRequests()
    dao.clearAllRideShares()
    dao.clearAllSmallGroups()
    dao.clearAllPdfJobs()
    dao.clearAllServingSchedules()
    dao.clearAllUsers()
    dao.upsertUser(ParishSeedData.bootstrapAdminAccount())
    dao.upsertUser(ParishSeedData.bootstrapMinisterAccount())
    supabaseService.signOut()
  }

  override suspend fun signInWithLockedAccount(
    email: String,
    password: String
  ): Result<UserProfileEntity> {
    val cleanEmail = email.trim().lowercase()
    if (cleanEmail.isBlank() || password.isBlank()) {
      return Result.failure(IllegalArgumentException("Please enter your email and password."))
    }

    val existingAccount = dao.getUserByEmail(cleanEmail)
      ?: return Result.failure(
        IllegalArgumentException(
          "No account found for '$cleanEmail'. Accounts can only be created by an Administrator or Rector."
        )
      )

    if (existingAccount.accountStatus == "INVITED") {
      return Result.failure(
        IllegalStateException(
          "Your account has a pending email invitation (Code: ${existingAccount.inviteCode}). Switch to the 'Accept Email Invite' tab to set your password."
        )
      )
    }

    if (existingAccount.passwordHash != password) {
      return Result.failure(IllegalArgumentException("Incorrect password for $cleanEmail."))
    }

    dao.clearActiveSessions()
    val activeProfile = existingAccount.copy(isActiveSession = true)
    dao.upsertUser(activeProfile)
    supabaseService.recordLocalSession(activeProfile)
    return Result.success(activeProfile)
  }

  override suspend fun acceptEmailInviteAndSetPassword(
    email: String,
    inviteCode: String,
    newPassword: String
  ): Result<UserProfileEntity> {
    val cleanEmail = email.trim().lowercase()
    val cleanCode = inviteCode.trim().uppercase()

    if (cleanEmail.isBlank() || newPassword.length < 4) {
      return Result.failure(IllegalArgumentException("Enter your invited email and choose a password (4+ characters)."))
    }

    val invitedAccount = dao.getUserByEmail(cleanEmail)
      ?: return Result.failure(
        IllegalArgumentException("No invitation found for '$cleanEmail'. Ask your Rector or Administrator to send an invite first.")
      )

    if (invitedAccount.inviteCode.isNotBlank() &&
      !invitedAccount.inviteCode.equals(cleanCode, ignoreCase = true)
    ) {
      return Result.failure(IllegalArgumentException("Invalid invite code. Check the invitation email sent via Mailtrap."))
    }

    dao.clearActiveSessions()
    val activatedProfile = invitedAccount.copy(
      passwordHash = newPassword,
      accountStatus = "ACTIVE",
      inviteCode = "",
      isActiveSession = true
    )
    dao.upsertUser(activatedProfile)
    supabaseService.recordLocalSession(activatedProfile)
    supabaseService.pushUserProfile(activatedProfile)
    return Result.success(activatedProfile)
  }

  override suspend fun createAndInviteAccountByEmail(
    email: String,
    fullName: String,
    congregation: Congregation,
    assignedRole: UserRole,
    delegatedRole: DelegatedRole,
    actor: UserProfileEntity,
    selectedRoles: Set<AppRole>
  ): Result<String> {
    if (!actor.role.canInviteMembers) {
      return Result.failure(SecurityException("Only Administrators and Rectors can create user accounts."))
    }
    val hasExplicitRoles = selectedRoles.isNotEmpty()
    val multiRoles = if (hasExplicitRoles) {
      (selectedRoles + AppRole.MEMBER).toSet()
    } else {
      val effectiveDelegated = if (assignedRole == UserRole.MEMBER) delegatedRole else DelegatedRole.NONE
      AppRole.fromLegacy(assignedRole, effectiveDelegated)
    }
    val derivedUserRole = if (hasExplicitRoles) {
      when {
        AppRole.ADMIN in multiRoles -> UserRole.ADMINISTRATOR
        AppRole.RECTOR in multiRoles || AppRole.MINISTER in multiRoles -> UserRole.MINISTER
        else -> UserRole.MEMBER
      }
    } else {
      assignedRole
    }
    val derivedDelegated = if (hasExplicitRoles) {
      if (AppRole.VOLUNTEER_SCHEDULER in multiRoles) DelegatedRole.VOLUNTEER_SCHEDULER else DelegatedRole.NONE
    } else {
      if (assignedRole == UserRole.MEMBER) delegatedRole else DelegatedRole.NONE
    }

    if (actor.role == UserRole.MINISTER && derivedUserRole != UserRole.MEMBER) {
      return Result.failure(SecurityException("Rectors can only create Member accounts for their congregation."))
    }

    val cleanEmail = email.trim().lowercase()
    val cleanName = fullName.trim()
    if (!cleanEmail.contains("@") || cleanName.isBlank()) {
      return Result.failure(IllegalArgumentException("Please enter a valid full name and email address."))
    }

    // Rectors are locked to inviting members into their own congregation
    val targetCongregation = if (actor.role == UserRole.MINISTER) {
      dao.getCongregationById(actor.congregationId)?.toDomain() ?: Congregation(
        id = actor.congregationId,
        displayName = "Rector's Congregation"
      )
    } else {
      congregation
    }

    if (targetCongregation.id.isNotBlank() && targetCongregation.displayName.isNotBlank()) {
      dao.upsertCongregation(targetCongregation.toEntity())
    }

    val inviteCode = "NAC-${Random.nextInt(1000, 9999)}"
    val existing = dao.getUserByEmail(cleanEmail)
    val preserveActive = existing != null && existing.accountStatus == "ACTIVE" && existing.passwordHash.isNotBlank()
    val newUser = UserProfileEntity(
      id = existing?.id ?: "usr_${UUID.randomUUID().toString().take(8)}",
      email = cleanEmail,
      fullName = cleanName,
      congregationId = targetCongregation.id,
      role = derivedUserRole,
      delegatedRole = derivedDelegated,
      roles = multiRoles,
      passwordHash = existing?.passwordHash.orEmpty(),
      inviteCode = inviteCode,
      accountStatus = if (preserveActive) "ACTIVE" else "INVITED",
      invitedBy = "${actor.fullName} (${actor.role.badgeLabel})",
      isActiveSession = existing?.isActiveSession ?: false
    )

    dao.upsertUser(newUser)
    syncUserRolesPivot(newUser.id, multiRoles)
    supabaseService.pushUserProfile(newUser)

    // Send real Mailtrap invitation email
    val emailResult = mailtrapService.sendAccountInvitationEmail(
      recipientEmail = cleanEmail,
      recipientName = cleanName,
      congregation = targetCongregation,
      assignedRole = derivedUserRole,
      delegatedRole = newUser.delegatedRole,
      inviteCode = inviteCode,
      invitedByName = actor.fullName
    )

    val statusNote = emailResult.fold(
      onSuccess = { "Mailtrap invite email sent to $cleanEmail (Invite Code: $inviteCode)." },
      onFailure = { err ->
        "Account created with Invite Code $inviteCode (Mailtrap warning: ${err.message})"
      }
    )
    return Result.success(statusNote)
  }

  override suspend fun signOut() {
    dao.clearActiveSessions()
    supabaseService.signOut()
  }

  override suspend fun delegateRoleToCongregationMember(
    targetUser: UserProfileEntity,
    delegatedRole: DelegatedRole,
    actor: UserProfileEntity
  ): Result<Unit> {
    if (!actor.role.canDelegateCongregationRoles && !ParishPolicy.canAccessAdminConsole(actor.effectiveRoles)) {
      return Result.failure(SecurityException("Only Rectors and Administrators can delegate congregation roles."))
    }
    if (!ParishPolicy.isGlobalAdmin(actor.effectiveRoles) && targetUser.congregationId != actor.congregationId) {
      return Result.failure(SecurityException("Rectors can only delegate roles within their own congregation."))
    }
    val updatedRoles = targetUser.effectiveRoles.toMutableSet().apply {
      if (delegatedRole == DelegatedRole.VOLUNTEER_SCHEDULER) add(AppRole.VOLUNTEER_SCHEDULER)
      else remove(AppRole.VOLUNTEER_SCHEDULER)
    }
    dao.updateDelegatedRole(targetUser.id, delegatedRole)
    dao.updateUserAppRoles(targetUser.id, updatedRoles)
    syncUserRolesPivot(targetUser.id, updatedRoles)
    return Result.success(Unit)
  }

  override suspend fun updateUserRoles(
    targetUser: UserProfileEntity,
    roles: Set<AppRole>,
    actor: UserProfileEntity
  ): Result<Unit> {
    val actorRoles = actor.effectiveRoles
    if (!ParishPolicy.canAccessAdminConsole(actorRoles)) {
      return Result.failure(SecurityException("Only Rectors and Administrators can update user roles."))
    }
    if (!ParishPolicy.isGlobalAdmin(actorRoles)) {
      if (targetUser.congregationId != actor.congregationId) {
        return Result.failure(SecurityException("Rectors can only manage roles within their own congregation."))
      }
      if (AppRole.ADMIN in roles) {
        return Result.failure(SecurityException("Only global Administrators can grant the Admin role."))
      }
    }
    val normalized = roles + AppRole.MEMBER
    val legacyPrimary = when {
      AppRole.ADMIN in normalized -> UserRole.ADMINISTRATOR
      AppRole.RECTOR in normalized || AppRole.MINISTER in normalized -> UserRole.MINISTER
      else -> UserRole.MEMBER
    }
    val legacyDelegated = if (AppRole.VOLUNTEER_SCHEDULER in normalized) {
      DelegatedRole.VOLUNTEER_SCHEDULER
    } else {
      DelegatedRole.NONE
    }
    val updatedUser = targetUser.copy(
      role = legacyPrimary,
      delegatedRole = legacyDelegated,
      roles = normalized
    )
    dao.upsertUser(updatedUser)
    syncUserRolesPivot(targetUser.id, normalized)
    return Result.success(Unit)
  }

  override suspend fun publishAnnouncementOrBroadcast(
    congregationId: String,
    title: String,
    summary: String,
    body: String,
    category: String,
    isUrgent: Boolean,
    serviceDate: String,
    liturgicalOrder: String,
    authorName: String,
    actorRole: UserRole
  ): Result<Unit> {
    if (!actorRole.canPublishAnnouncements) {
      return Result.failure(SecurityException("Only Rectors and Administrators can publish announcements."))
    }
    val entity = AnnouncementEntity(
      id = "ann_${UUID.randomUUID().toString().take(8)}",
      congregationId = congregationId,
      title = title.trim(),
      summary = summary.trim().ifBlank { body.take(90) },
      body = body.trim(),
      category = category,
      isUrgent = isUrgent,
      serviceDate = serviceDate.trim().ifBlank { "Upcoming Service" },
      liturgicalOrder = liturgicalOrder.trim(),
      authorName = authorName,
      authorRole = actorRole
    )
    dao.upsertAnnouncement(entity)
    if (isUrgent) {
      ParishNotificationHelper.sendUrgentBroadcastNotification(
        context = appContext,
        title = entity.title,
        summary = entity.summary,
        authorName = authorName
      )
    }
    return supabaseService.pushAnnouncement(entity, actorRole)
  }

  override suspend fun toggleVolunteerConfirmation(assignment: VolunteerAssignmentEntity) {
    val newConfirmed = !assignment.isConfirmed
    if (assignment.subTasks.isNotEmpty()) {
      val updatedSubTasks = assignment.subTasks.map { it.copy(isCompleted = newConfirmed) }
      dao.upsertVolunteerAssignment(assignment.copy(subTasks = updatedSubTasks, isConfirmed = newConfirmed))
    } else {
      dao.setAssignmentConfirmed(assignment.id, newConfirmed)
    }
  }

  override suspend fun toggleSubTaskCompletion(assignment: VolunteerAssignmentEntity, subTaskId: String) {
    if (assignment.subTasks.isEmpty()) {
      toggleVolunteerConfirmation(assignment)
      return
    }
    val updatedSubTasks = assignment.subTasks.map { sub ->
      if (sub.id == subTaskId) sub.copy(isCompleted = !sub.isCompleted) else sub
    }
    val allCompleted = updatedSubTasks.isNotEmpty() && updatedSubTasks.all { it.isCompleted }
    dao.upsertVolunteerAssignment(
      assignment.copy(
        subTasks = updatedSubTasks,
        isConfirmed = allCompleted
      )
    )
  }

  override fun observeUserPushSubscriptions(userId: String): Flow<List<PushSubscriptionEntity>> =
    dao.observeUserPushSubscriptions(userId)

  override fun observeMiniHqResources(): Flow<List<MiniHqResourceEntity>> =
    dao.observeMiniHqResources()

  override suspend fun registerCurrentDevicePushSubscription(user: UserProfileEntity): Result<PushSubscriptionEntity> {
    if (!ParishNotificationHelper.hasNotificationPermission(appContext)) {
      return Result.failure(IllegalStateException("Please grant Android Notification permission first."))
    }
    val deviceLabel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim().ifBlank { "Android Device" }
    val sub = PushSubscriptionEntity(
      id = "push_${user.id}_android",
      userId = user.id,
      congregationId = user.congregationId,
      deviceLabel = deviceLabel,
      platform = "android",
      endpoint = "android://local/${user.id}/${android.os.Build.MODEL}",
      isActive = true
    )
    dao.upsertPushSubscription(sub)
    return Result.success(sub)
  }

  override suspend fun disableCurrentDevicePushSubscription(user: UserProfileEntity): Result<Unit> {
    dao.deletePushSubscription("push_${user.id}_android")
    return Result.success(Unit)
  }

  override suspend fun updateOwnProfile(
    actor: UserProfileEntity,
    fullName: String,
    email: String,
    phone: String,
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String
  ): Result<UserProfileEntity> {
    val activeUser = dao.getActiveUserOnce()
      ?: return Result.failure(IllegalStateException("No active signed-in session."))
    if (activeUser.id != actor.id && !ParishPolicy.isGlobalAdmin(activeUser.effectiveRoles)) {
      return Result.failure(SecurityException("You can only edit your own profile."))
    }

    val cleanName = fullName.trim()
    val cleanEmail = email.trim().lowercase()
    val cleanPhone = phone.trim()
    if (cleanName.isBlank()) {
      return Result.failure(IllegalArgumentException("Full Name is required."))
    }
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      return Result.failure(IllegalArgumentException("Please enter a valid email address."))
    }

    val existingByEmail = dao.getUserByEmail(cleanEmail)
    if (existingByEmail != null && existingByEmail.id != actor.id) {
      return Result.failure(IllegalArgumentException("That email address is already registered to another account."))
    }

    val wantsPasswordChange = currentPassword.isNotBlank() || newPassword.isNotBlank() || confirmNewPassword.isNotBlank()
    val resolvedPasswordHash = if (wantsPasswordChange) {
      if (actor.passwordHash.isNotBlank() && currentPassword != actor.passwordHash) {
        return Result.failure(IllegalArgumentException("Current password does not match."))
      }
      if (newPassword.length < 6) {
        return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
      }
      if (newPassword != confirmNewPassword) {
        return Result.failure(IllegalArgumentException("New password and confirmation do not match."))
      }
      newPassword
    } else {
      actor.passwordHash
    }

    val updated = actor.copy(
      fullName = cleanName,
      email = cleanEmail,
      phone = cleanPhone,
      passwordHash = resolvedPasswordHash
    )
    dao.upsertUser(updated)
    return Result.success(updated)
  }

  override suspend fun updateNotificationPreferences(
    user: UserProfileEntity,
    preferPushReminders: Boolean,
    preferEmailReminders: Boolean
  ): Result<UserProfileEntity> {
    val updated = user.copy(
      preferPushReminders = preferPushReminders,
      preferEmailReminders = preferEmailReminders
    )
    dao.upsertUser(updated)
    return Result.success(updated)
  }

  override suspend fun sendTestPhoneNotification(user: UserProfileEntity): Result<String> {
    if (!ParishNotificationHelper.hasNotificationPermission(appContext)) {
      return Result.failure(IllegalStateException("OS notification permission is not granted. Please enable Notifications for NAC Portal in Android Settings."))
    }
    registerCurrentDevicePushSubscription(user)
    val posted = ParishNotificationHelper.sendTestPhoneNotification(appContext, user.fullName)
    return if (posted) {
      Result.success("Test phone notification sent to ${user.fullName}'s device.")
    } else {
      Result.failure(IllegalStateException("Failed to post notification on this device."))
    }
  }

  override suspend fun triggerVolunteerReminder(assignment: VolunteerAssignmentEntity): Result<String> {
    val activeUser = dao.getActiveUserOnce()
    val dutyTitle = assignment.customTaskTitle.ifBlank { assignment.dutyType.displayName }
    val subTaskLabels = assignment.subTasks.map { it.label }.filter { it.isNotBlank() }
    val congregationName = dao.getCongregationById(assignment.congregationId)?.displayName.orEmpty()
    val roster = dao.getCongregationRosterOnce(assignment.congregationId)

    val today = LocalDate.now()
    val parsedDate = listOf(
      "EEE d MMM yyyy",
      "EEE MMM d, yyyy",
      "EEE, MMM d, yyyy",
      "yyyy-MM-dd",
      "MMM d, yyyy"
    ).firstNotNullOfOrNull { pattern ->
      runCatching { LocalDate.parse(assignment.serviceDate.trim(), DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)) }.getOrNull()
    }
    val isToday = parsedDate == today

    // Resolve all Main and Backup volunteers (never skipping the currently logged-in user)
    val mainNames = assignment.assigneeName.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val backupNames = assignment.backupAssigneeName.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val targets = mainNames.map { it to "MAIN" } + backupNames.map { it to "BACKUP" }

    var pushDevicesCount = 0
    var emailFallbackCount = 0
    var unreachedCount = 0
    val dispatchedEndpoints = mutableSetOf<String>()
    var notifiedLocalDevice = false

    for ((name, tier) in targets) {
      val matchedUser = roster.firstOrNull { it.fullName.equals(name, ignoreCase = true) }
        ?: if (activeUser != null && activeUser.fullName.equals(name, ignoreCase = true)) activeUser else null

      val subs = if (matchedUser != null) {
        dao.getUserPushSubscriptionsOnce(matchedUser.id).distinctBy { it.endpoint }
      } else {
        emptyList()
      }
      val newSubs = subs.filter { it.endpoint !in dispatchedEndpoints }
      val isCurrentLoggedInDevice = matchedUser != null && activeUser != null && matchedUser.id == activeUser.id

      val formattedBody = ParishNotificationHelper.buildVolunteerReminderBody(
        roleTier = tier,
        items = subTaskLabels,
        fallbackTitle = dutyTitle,
        serviceDate = assignment.serviceDate,
        serviceTime = assignment.serviceTime,
        isToday = isToday
      )

      if (newSubs.isNotEmpty() || (isCurrentLoggedInDevice && !notifiedLocalDevice && ParishNotificationHelper.hasNotificationPermission(appContext))) {
        newSubs.forEach { dispatchedEndpoints += it.endpoint }
        val deviceCountForUser = newSubs.size.coerceAtLeast(1)
        if ((isCurrentLoggedInDevice || newSubs.any { it.platform == "android" }) && !notifiedLocalDevice) {
          val posted = ParishNotificationHelper.sendVolunteerReminderNotification(
            context = appContext,
            assignmentId = assignment.id,
            dutyTitle = dutyTitle,
            subTaskLabels = subTaskLabels,
            serviceDate = assignment.serviceDate,
            serviceTime = assignment.serviceTime,
            roleTier = tier,
            isToday = isToday
          )
          if (posted) {
            notifiedLocalDevice = true
            pushDevicesCount += deviceCountForUser
          } else if (matchedUser != null && matchedUser.email.contains("@") && !matchedUser.email.endsWith("@volunteer.local")) {
            val emailRes = mailtrapService.sendVolunteerReminderEmail(
              recipientEmail = matchedUser.email,
              recipientName = name,
              roleTier = tier,
              dutyTitle = dutyTitle,
              formattedBody = formattedBody,
              serviceDate = assignment.serviceDate,
              congregationName = congregationName
            )
            if (emailRes.isSuccess) emailFallbackCount++ else unreachedCount++
          } else {
            unreachedCount++
          }
        } else if (newSubs.isNotEmpty()) {
          pushDevicesCount += newSubs.size
        }
      } else if (matchedUser != null && matchedUser.email.contains("@") && !matchedUser.email.endsWith("@volunteer.local")) {
        val emailRes = mailtrapService.sendVolunteerReminderEmail(
          recipientEmail = matchedUser.email,
          recipientName = name,
          roleTier = tier,
          dutyTitle = dutyTitle,
          formattedBody = formattedBody,
          serviceDate = assignment.serviceDate,
          congregationName = congregationName
        )
        if (emailRes.isSuccess) emailFallbackCount++ else unreachedCount++
      } else {
        unreachedCount++
      }
    }

    dao.setAssignmentReminderScheduled(assignment.id, true)
    val syncWork = OneTimeWorkRequestBuilder<ScheduleSyncWorker>().build()
    runCatching {
      WorkManager.getInstance(appContext).enqueue(syncWork)
    }

    val summary = buildList {
      if (pushDevicesCount > 0) add("Push notification sent to $pushDevicesCount device(s)")
      if (emailFallbackCount > 0) add("email reminder sent to $emailFallbackCount member(s) without push enabled")
      if (unreachedCount > 0) add("$unreachedCount volunteer(s) have no push or email registered")
    }.joinToString("; ").ifBlank { "Reminder scheduled for $dutyTitle" }

    return Result.success(summary)
  }

  override suspend fun addManualVolunteerAssignment(
    congregationId: String,
    dutyType: MinistryDutyType,
    customTaskTitle: String,
    assignmentTier: String,
    assigneeName: String,
    backupAssigneeName: String,
    serviceDate: String,
    serviceTime: String,
    notes: String,
    subTasks: List<VolunteerSubTask>
  ) {
    val activeUser = dao.getActiveUserOnce() ?: return
    val actorRoles = activeUser.effectiveRoles
    if (!ParishPolicy.canManageVolunteerSchedule(actorRoles)) return
    if (!ParishPolicy.canAccessCongregation(actorRoles, activeUser.congregationId, congregationId)) return

    val cleanAssignee = assigneeName.trim()
    val cleanBackup = backupAssigneeName.trim()
    val cleanCustomTitle = customTaskTitle.trim()
    val cleanDate = serviceDate.trim()
    val tier = assignmentTier.ifBlank { "MAIN" }.uppercase()

    // Instant volunteer creation: if the typed volunteer is not on the roster yet, add a roster entry
    if (cleanAssignee.isNotBlank() && congregationId.isNotBlank()) {
      val roster = dao.getCongregationRosterOnce(congregationId)
      if (roster.none { it.fullName.equals(cleanAssignee, ignoreCase = true) }) {
        val slug = cleanAssignee.lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.')
        val rosterProfile = UserProfileEntity(
          id = "usr_vol_${UUID.randomUUID().toString().take(8)}",
          email = "${slug.ifBlank { "volunteer" }}@volunteer.local",
          fullName = cleanAssignee,
          congregationId = congregationId,
          role = UserRole.MEMBER,
          delegatedRole = DelegatedRole.NONE,
          roles = setOf(AppRole.MEMBER),
          accountStatus = "ROSTER_ONLY",
          invitedBy = activeUser.fullName
        )
        dao.upsertUser(rosterProfile)
      }
    }

    // If adding a BACKUP duty, pair it onto a matching MAIN card for the same date & duty if present
    if (tier == "BACKUP" && cleanAssignee.isNotBlank()) {
      val existingMain = dao.getVolunteerAssignmentsOnce(congregationId).firstOrNull {
        it.assignmentTier.equals("MAIN", ignoreCase = true) &&
          it.serviceDate.equals(cleanDate, ignoreCase = true) &&
          it.dutyType == dutyType &&
          it.customTaskTitle.equals(cleanCustomTitle, ignoreCase = true) &&
          it.backupAssigneeName.isBlank()
      }
      if (existingMain != null) {
        dao.upsertVolunteerAssignment(existingMain.copy(backupAssigneeName = cleanAssignee))
        return
      }
    }

    val effectiveSubTasks = if (subTasks.isNotEmpty()) {
      subTasks
    } else if (dutyType == MinistryDutyType.ALTAR_SETUP && cleanCustomTitle.isBlank()) {
      listOf(
        VolunteerSubTask(id = "sub_1_${UUID.randomUUID().toString().take(4)}", label = "Setup"),
        VolunteerSubTask(id = "sub_2_${UUID.randomUUID().toString().take(4)}", label = "Takedown")
      )
    } else {
      listOf(
        VolunteerSubTask(
          id = "sub_1_${UUID.randomUUID().toString().take(4)}",
          label = cleanCustomTitle.ifBlank { dutyType.displayName }
        )
      )
    }

    val isLocalMatch = activeUser.fullName.equals(cleanAssignee, ignoreCase = true)
    val assignment = VolunteerAssignmentEntity(
      id = "assign_${UUID.randomUUID().toString().take(8)}",
      congregationId = congregationId,
      dutyType = dutyType,
      customTaskTitle = cleanCustomTitle,
      assignmentTier = tier,
      assigneeName = cleanAssignee,
      backupAssigneeName = cleanBackup,
      assigneeUserId = if (isLocalMatch) activeUser.id else null,
      serviceDate = cleanDate,
      serviceTime = serviceTime.trim(),
      notes = notes.trim(),
      subTasks = effectiveSubTasks,
      isConfirmed = false,
      reminderScheduled = isLocalMatch,
      sourcePdfName = "Manual Entry",
      isLocalUserMatch = isLocalMatch
    )
    dao.upsertVolunteerAssignment(assignment)
  }

  override suspend fun deleteVolunteerAssignment(assignmentId: String) {
    val activeUser = dao.getActiveUserOnce() ?: return
    if (!ParishPolicy.canManageVolunteerSchedule(activeUser.effectiveRoles)) return
    dao.deleteVolunteerAssignment(assignmentId)
  }

  override suspend fun addServingSchedule(
    congregationId: String,
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
    val schedule = ServingScheduleEntity(
      id = "serv_${UUID.randomUUID().toString().take(8)}",
      congregationId = congregationId,
      serviceTitle = serviceTitle.trim().ifBlank { "Sunday Divine Service" },
      serviceDate = serviceDate.trim(),
      serviceTime = serviceTime.trim().ifBlank { "10:30 AM" },
      officiantMinister = officiantMinister.trim(),
      officiantRank = officiantRank.trim().ifBlank { "Priest" },
      assistingMinisters = assistingMinisters.trim(),
      choirConductor = choirConductor.trim(),
      organist = organist.trim(),
      serviceTheme = serviceTheme.trim(),
      bibleWord = bibleWord.trim(),
      specialServiceTag = specialServiceTag.trim(),
      location = location.trim(),
      notes = notes.trim()
    )
    dao.upsertServingSchedule(schedule)
  }

  override suspend fun deleteServingSchedule(scheduleId: String) {
    dao.deleteServingSchedule(scheduleId)
  }

  override suspend fun submitPrayerOrVisitRequest(
    congregationId: String,
    requesterName: String,
    requesterContact: String,
    requestType: PastoralRequestType,
    content: String,
    isPrivateToClergy: Boolean
  ): Result<Unit> {
    val entity = PrayerRequestEntity(
      id = "pray_${UUID.randomUUID().toString().take(8)}",
      congregationId = congregationId,
      requesterName = requesterName.trim().ifBlank { "Anonymous Member" },
      requesterContact = requesterContact.trim(),
      requestType = requestType,
      content = content.trim(),
      isPrivateToClergy = isPrivateToClergy,
      status = RequestStatus.NEW
    )
    dao.upsertPrayerRequest(entity)
    return supabaseService.pushPrayerRequest(entity)
  }

  override suspend fun updatePrayerRequestStatus(
    requestId: String,
    newStatus: RequestStatus,
    ministerialNotes: String,
    actorRole: UserRole
  ): Result<Unit> {
    if (!actorRole.canTriagePrayerQueue) {
      return Result.failure(SecurityException("Only Clergy/Administrators can triage pastoral requests."))
    }
    dao.updatePrayerRequestStatus(requestId, newStatus, ministerialNotes.trim())
    return Result.success(Unit)
  }

  override suspend fun createRideSharePost(
    congregationId: String,
    postType: RidePostType,
    memberName: String,
    phoneContact: String,
    neighborhood: String,
    serviceDate: String,
    serviceTime: String,
    seatsCount: Int,
    notes: String
  ): Result<Unit> {
    val entity = RideShareEntity(
      id = "ride_${UUID.randomUUID().toString().take(8)}",
      congregationId = congregationId,
      postType = postType,
      memberName = memberName.trim(),
      phoneContact = phoneContact.trim(),
      neighborhood = neighborhood.trim(),
      serviceDate = serviceDate.trim(),
      serviceTime = serviceTime.trim(),
      seatsCount = seatsCount.coerceAtLeast(1),
      notes = notes.trim(),
      isFulfilled = false
    )
    dao.upsertRideShare(entity)
    return supabaseService.pushRideShare(entity)
  }

  override suspend fun matchRideSharePost(rideId: String, matchedPartnerName: String) {
    dao.matchRideShare(rideId, matchedPartnerName.trim())
  }

  override suspend fun toggleSmallGroupEnrollment(group: SmallGroupEntity) {
    val nextEnrolled = !group.isUserEnrolled
    val delta = if (nextEnrolled) 1 else -1
    val updated = group.copy(
      isUserEnrolled = nextEnrolled,
      memberCount = (group.memberCount + delta).coerceIn(1, group.maxCapacity)
    )
    dao.updateSmallGroup(updated)
  }

  override suspend fun updateSmallGroupPastoralLog(
    group: SmallGroupEntity,
    updatedTopic: String,
    updatedPastoralLog: String,
    updatedServiceNotes: String,
    actorRole: UserRole
  ): Result<Unit> {
    if (!actorRole.canManageSmallGroups) {
      return Result.failure(SecurityException("Only Rectors and Administrators can edit DSG logs."))
    }
    val updated = group.copy(
      currentTopic = updatedTopic.trim(),
      pastoralLogNotes = updatedPastoralLog.trim(),
      serviceLiturgyNotes = updatedServiceNotes.trim()
    )
    dao.updateSmallGroup(updated)
    return supabaseService.pushSmallGroup(updated, actorRole)
  }

  override suspend fun commitParsedVolunteerAssignments(
    congregationId: String,
    pdfFileName: String,
    assignments: List<VolunteerAssignmentEntity>,
    uploadedBy: String,
    actorRole: UserRole,
    actorDelegatedRole: DelegatedRole
  ): Result<String> {
    val activeUser = dao.getActiveUserOnce()
      ?: return Result.failure(SecurityException("No active user session."))
    val actorRoles = activeUser.effectiveRoles
    if (!ParishPolicy.canManageVolunteerSchedule(actorRoles)) {
      return Result.failure(SecurityException("Only Volunteer Schedulers or Administrators can upload schedule PDFs."))
    }
    if (!ParishPolicy.canAccessCongregation(actorRoles, activeUser.congregationId, congregationId)) {
      return Result.failure(SecurityException("Cannot upload schedule to a different congregation."))
    }
    if (pdfFileName.isBlank() ||
      !pdfFileName.endsWith(".pdf", ignoreCase = true) ||
      pdfFileName.equals("Monthly_Volunteer_Schedule.pdf", ignoreCase = true) ||
      assignments.isEmpty()
    ) {
      return Result.failure(IllegalArgumentException("Please choose a valid .pdf schedule file before uploading."))
    }
    val scoped = assignments.map {
      val isLocal = it.isLocalUserMatch ||
        it.assigneeName.contains(activeUser.fullName, ignoreCase = true) ||
        it.backupAssigneeName.contains(activeUser.fullName, ignoreCase = true)
      it.copy(
        congregationId = congregationId,
        sourcePdfName = pdfFileName,
        isLocalUserMatch = isLocal
      )
    }
    if (scoped.isNotEmpty()) {
      dao.deleteAssignmentsBySourcePdf(congregationId, pdfFileName)
      dao.upsertVolunteerAssignments(scoped)
      supabaseService.pushVolunteerAssignments(scoped, actorRole, actorDelegatedRole)
    }
    return Result.success("Uploaded ${scoped.size} volunteer duties from '$pdfFileName'.")
  }

  override suspend fun enqueueSmartPdfScheduleIndexing(
    congregationId: String,
    pdfFileName: String,
    uploadedBy: String,
    pdfUri: Uri?,
    customScheduleText: String?,
    actorRole: UserRole,
    actorDelegatedRole: DelegatedRole
  ): Result<String> {
    if (!canUploadAndEmailVolunteerSchedule(actorRole, actorDelegatedRole)) {
      return Result.failure(
        SecurityException("RBAC Violation: Only the Rector, Administrator, or delegated Volunteer Scheduler can upload schedule PDFs.")
      )
    }
    val jobId = "pdf_job_${UUID.randomUUID().toString().take(8)}"

    val roster = dao.getCongregationRosterOnce(congregationId)
    val activeUser = dao.getActiveUserOnce()
    val rawText = if (!customScheduleText.isNullOrBlank()) {
      customScheduleText
    } else {
      PdfScheduleParser.extractTextFromPdfUri(appContext, pdfUri, roster)
    }

    val parsed = PdfScheduleParser.parseScheduleText(
      rawText = rawText,
      pdfFileName = pdfFileName,
      congregationId = congregationId,
      roster = roster,
      activeUser = activeUser
    )

    if (parsed.assignments.isNotEmpty()) {
      dao.deleteAssignmentsBySourcePdf(congregationId, pdfFileName)
      dao.upsertVolunteerAssignments(parsed.assignments)
      supabaseService.pushVolunteerAssignments(parsed.assignments, actorRole, actorDelegatedRole)

      // Also publish an automatic bulletin card notifying users their commitments are populated
      val bulletinCard = AnnouncementEntity(
        id = "ann_sched_${UUID.randomUUID().toString().take(8)}",
        congregationId = congregationId,
        title = "Volunteer Schedule Published: $pdfFileName",
        summary = "${parsed.assignments.size} MAIN & BACKUP commitments auto-populated across ${parsed.extractedDates.size} service dates.",
        body = "Uploaded by $uploadedBy. Open the Roster tab to view and confirm your upcoming AV, Altar Setup/Takedown, Greeter, and Usher duties.\n\n${parsed.formattedEmailTable}",
        category = "Weekly Bulletin",
        isUrgent = false,
        serviceDate = parsed.extractedDates.firstOrNull().orEmpty(),
        liturgicalOrder = "",
        authorName = uploadedBy,
        authorRole = actorRole
      )
      dao.upsertAnnouncement(bulletinCard)
    }

    dao.upsertPdfScheduleJob(
      PdfScheduleJobEntity(
        id = jobId,
        congregationId = congregationId,
        pdfFileName = pdfFileName,
        uploadedBy = uploadedBy,
        status = "COMPLETED",
        extractedDatesCount = parsed.extractedDates.size,
        matchedRosterCount = parsed.matchedRosterCount,
        highlightedLocalMatches = parsed.highlightedLocalMatches,
        detailedLog = parsed.summaryReport,
        parsedScheduleTableText = parsed.formattedEmailTable
      )
    )

    val workRequest = OneTimeWorkRequestBuilder<PdfScheduleParseWorker>()
      .setInputData(
        workDataOf(
          PdfScheduleParseWorker.KEY_JOB_ID to jobId,
          PdfScheduleParseWorker.KEY_CONGREGATION_ID to congregationId,
          PdfScheduleParseWorker.KEY_PDF_FILE_NAME to pdfFileName,
          PdfScheduleParseWorker.KEY_UPLOADED_BY to uploadedBy,
          PdfScheduleParseWorker.KEY_PDF_URI to (pdfUri?.toString() ?: ""),
          PdfScheduleParseWorker.KEY_CUSTOM_SCHEDULE_TEXT to rawText
        )
      )
      .build()

    runCatching {
      WorkManager.getInstance(appContext).enqueue(workRequest)
    }

    ParishNotificationHelper.sendPdfIndexingCompleteNotification(
      context = appContext,
      pdfFileName = pdfFileName,
      extractedDatesCount = parsed.extractedDates.size,
      matchedRosterCount = parsed.matchedRosterCount,
      localHighlights = parsed.highlightedLocalMatches
    )

    return Result.success(parsed.summaryReport)
  }

  override suspend fun emailSchedulePdfToCongregation(
    congregation: Congregation,
    additionalRecipientEmails: String,
    subject: String,
    customMessage: String,
    pdfFileName: String,
    pdfUri: Uri?,
    scheduleSummaryText: String,
    actorRole: UserRole,
    actorDelegatedRole: DelegatedRole
  ): Result<String> {
    if (!canUploadAndEmailVolunteerSchedule(actorRole, actorDelegatedRole)) {
      return Result.failure(
        SecurityException("Only the Rector, Administrator, or delegated Volunteer Scheduler can email schedules to the congregation.")
      )
    }
    val rosterEmails = dao.getCongregationRosterOnce(congregation.id).map { it.email }
    val extraEmails = additionalRecipientEmails
      .split(",", ";", " ")
      .map { it.trim() }
      .filter { it.contains("@") }

    val allRecipients = (extraEmails + rosterEmails)
      .filter { !it.equals(ParishSeedData.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) || extraEmails.isEmpty() }
      .distinct()

    val pdfBytes = PdfScheduleParser.readPdfBytesFromUri(appContext, pdfUri)
    val congregationAssignments = dao.getVolunteerAssignmentsOnce(congregation.id)
    return mailtrapService.sendSchedulePdfToCongregation(
      congregation = congregation,
      recipientEmails = allRecipients,
      subject = subject,
      customMessage = customMessage,
      scheduleSummaryText = scheduleSummaryText,
      pdfFileName = pdfFileName,
      pdfBytes = pdfBytes,
      assignments = congregationAssignments
    )
  }

  override suspend fun sendMailtrapIntegrationTest(recipientEmail: String): Result<String> {
    return mailtrapService.sendIntegrationTestEmail(recipientEmail)
  }

  override fun configureMailtrap(apiToken: String, senderEmail: String, senderName: String) {
    mailtrapService.updateConfig(apiToken, senderEmail, senderName)
  }

  override fun configureLiveSupabase(supabaseUrl: String, supabaseAnonKey: String) {
    supabaseService.updateConfiguration(supabaseUrl, supabaseAnonKey)
  }

  override suspend fun syncWithSupabaseNow(congregationId: String) {
    supabaseService.fetchRemoteAnnouncements(congregationId).onSuccess { remoteList ->
      if (remoteList.isNotEmpty()) {
        dao.upsertAnnouncements(remoteList)
      }
    }
  }

  private fun schedulePeriodicBackgroundSync() {
    runCatching {
      val periodicWork = PeriodicWorkRequestBuilder<ScheduleSyncWorker>(6, TimeUnit.HOURS).build()
      WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
        "nac_portal_periodic_worker",
        ExistingPeriodicWorkPolicy.KEEP,
        periodicWork
      )
    }
  }

  companion object {
    @Volatile
    private var INSTANCE: ParishRepository? = null

    fun getInstance(context: Context): ParishRepository {
      return INSTANCE ?: synchronized(this) {
        val db = ParishDatabase.getInstance(context)
        OfflineFirstParishRepository(context.applicationContext, db.parishDao()).also {
          INSTANCE = it
        }
      }
    }
  }
}
