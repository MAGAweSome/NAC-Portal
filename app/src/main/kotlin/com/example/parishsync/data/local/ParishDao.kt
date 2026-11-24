package com.example.parishsync.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.parishsync.data.model.AppRole
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.RequestStatus
import com.example.parishsync.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface ParishDao {

  // --- User Profiles & Locked Account Sessions ---
  @Query("SELECT * FROM user_profiles WHERE isActiveSession = 1 LIMIT 1")
  fun observeActiveUser(): Flow<UserProfileEntity?>

  @Query("SELECT * FROM user_profiles WHERE isActiveSession = 1 LIMIT 1")
  suspend fun getActiveUserOnce(): UserProfileEntity?

  @Query("SELECT * FROM user_profiles WHERE LOWER(email) = LOWER(:email) LIMIT 1")
  suspend fun getUserByEmail(email: String): UserProfileEntity?

  @Query("SELECT * FROM user_profiles WHERE congregationId = :congregationId AND :congregationId != '' ORDER BY fullName ASC")
  fun observeCongregationRoster(congregationId: String): Flow<List<UserProfileEntity>>

  @Query("SELECT * FROM user_profiles WHERE congregationId = :congregationId AND :congregationId != '' ORDER BY fullName ASC")
  suspend fun getCongregationRosterOnce(congregationId: String): List<UserProfileEntity>

  @Query("SELECT * FROM user_profiles ORDER BY congregationId ASC, fullName ASC")
  fun observeAllUsers(): Flow<List<UserProfileEntity>>

  @Query("SELECT * FROM user_profiles ORDER BY congregationId ASC, fullName ASC")
  suspend fun getAllUsersOnce(): List<UserProfileEntity>

  @Query("SELECT COUNT(*) FROM user_profiles")
  suspend fun getUserCount(): Int

  @Query("UPDATE user_profiles SET isActiveSession = 0")
  suspend fun clearActiveSessions()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertUser(user: UserProfileEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertUsers(users: List<UserProfileEntity>)

  @Query("UPDATE user_profiles SET role = :newRole WHERE id = :userId")
  suspend fun updateUserRole(userId: String, newRole: UserRole)

  @Query("UPDATE user_profiles SET delegatedRole = :delegatedRole WHERE id = :userId")
  suspend fun updateDelegatedRole(userId: String, delegatedRole: DelegatedRole)

  @Query("UPDATE user_profiles SET roles = :roles WHERE id = :userId")
  suspend fun updateUserAppRoles(userId: String, roles: Set<AppRole>)

  // --- Multi-Role Pivot Table (`user_roles`) ---
  @Query("SELECT role FROM user_roles WHERE userId = :userId")
  fun observeUserRoles(userId: String): Flow<List<AppRole>>

  @Query("SELECT role FROM user_roles WHERE userId = :userId")
  suspend fun getUserRolesOnce(userId: String): List<AppRole>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertUserRoles(assignments: List<UserRoleAssignmentEntity>)

  @Query("DELETE FROM user_roles WHERE userId = :userId")
  suspend fun clearUserRoles(userId: String)

  // --- Announcements, Live Bulletin & Service Leaflets ---
  @Query("SELECT * FROM announcements WHERE congregationId = :congregationId ORDER BY isUrgent DESC, createdAt DESC")
  fun observeAnnouncements(congregationId: String): Flow<List<AnnouncementEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAnnouncement(announcement: AnnouncementEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAnnouncements(announcements: List<AnnouncementEntity>)

  @Query("DELETE FROM announcements WHERE id = :id")
  suspend fun deleteAnnouncement(id: String)

  // --- Volunteer Roster & Reminders ---
  @Query("SELECT * FROM volunteer_assignments WHERE congregationId = :congregationId AND :congregationId != '' ORDER BY serviceDate ASC, dutyType ASC, assignmentTier DESC")
  fun observeVolunteerAssignments(congregationId: String): Flow<List<VolunteerAssignmentEntity>>

  @Query("SELECT * FROM volunteer_assignments ORDER BY serviceDate ASC, dutyType ASC, assignmentTier DESC")
  fun observeAllVolunteerAssignments(): Flow<List<VolunteerAssignmentEntity>>

  @Query("SELECT * FROM volunteer_assignments WHERE congregationId = :congregationId AND :congregationId != '' ORDER BY serviceDate ASC")
  suspend fun getVolunteerAssignmentsOnce(congregationId: String): List<VolunteerAssignmentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertVolunteerAssignment(assignment: VolunteerAssignmentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertVolunteerAssignments(assignments: List<VolunteerAssignmentEntity>)

  @Query("DELETE FROM volunteer_assignments WHERE congregationId = :congregationId AND sourcePdfName = :pdfName")
  suspend fun deleteAssignmentsBySourcePdf(congregationId: String, pdfName: String)

  @Query("UPDATE volunteer_assignments SET isConfirmed = :confirmed WHERE id = :assignmentId")
  suspend fun setAssignmentConfirmed(assignmentId: String, confirmed: Boolean)

  @Query("UPDATE volunteer_assignments SET reminderScheduled = :scheduled WHERE id = :assignmentId")
  suspend fun setAssignmentReminderScheduled(assignmentId: String, scheduled: Boolean)

  @Query("DELETE FROM volunteer_assignments WHERE id = :assignmentId")
  suspend fun deleteVolunteerAssignment(assignmentId: String)

  // --- NAC Divine Services & Serving Schedules ---
  @Query("SELECT * FROM serving_schedules WHERE congregationId = :congregationId AND :congregationId != '' ORDER BY serviceDate ASC, id ASC")
  fun observeServingSchedules(congregationId: String): Flow<List<ServingScheduleEntity>>

  @Query("SELECT * FROM serving_schedules ORDER BY serviceDate ASC, id ASC")
  fun observeAllServingSchedules(): Flow<List<ServingScheduleEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertServingSchedule(schedule: ServingScheduleEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertServingSchedules(schedules: List<ServingScheduleEntity>)

  @Query("DELETE FROM serving_schedules WHERE id = :scheduleId")
  suspend fun deleteServingSchedule(scheduleId: String)

  @Query("DELETE FROM serving_schedules")
  suspend fun clearAllServingSchedules()

  // --- Choir & Hymn Selection Hub ---
  @Query("SELECT * FROM hymn_selections WHERE congregationId = :congregationId ORDER BY serviceDate ASC, id ASC")
  fun observeHymnSelections(congregationId: String): Flow<List<HymnSelectionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHymnSelection(hymn: HymnSelectionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHymnSelections(hymns: List<HymnSelectionEntity>)

  // --- Digital Prayer Requests & Pastoral Visits ---
  @Query("SELECT * FROM prayer_requests WHERE congregationId = :congregationId ORDER BY createdAt DESC")
  fun observeAllPrayerRequestsForClergy(congregationId: String): Flow<List<PrayerRequestEntity>>

  @Query("SELECT * FROM prayer_requests WHERE congregationId = :congregationId AND (isPrivateToClergy = 0 OR requesterName = :currentUserName) ORDER BY createdAt DESC")
  fun observeVisiblePrayerRequestsForMember(congregationId: String, currentUserName: String): Flow<List<PrayerRequestEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPrayerRequest(request: PrayerRequestEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPrayerRequests(requests: List<PrayerRequestEntity>)

  @Query("UPDATE prayer_requests SET status = :status, ministerialNotes = :notes WHERE id = :requestId")
  suspend fun updatePrayerRequestStatus(requestId: String, status: RequestStatus, notes: String)

  // --- Ride Sharing Board ---
  @Query("SELECT * FROM ride_shares WHERE congregationId = :congregationId ORDER BY isFulfilled ASC, serviceDate ASC")
  fun observeRideShares(congregationId: String): Flow<List<RideShareEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRideShare(rideShare: RideShareEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRideShares(rideShares: List<RideShareEntity>)

  @Query("UPDATE ride_shares SET isFulfilled = 1, matchedMemberName = :matchedName WHERE id = :rideId")
  suspend fun matchRideShare(rideId: String, matchedName: String)

  // --- Ministerial Dashboard & Small Groups (DSGs) ---
  @Query("SELECT * FROM small_groups WHERE congregationId = :congregationId ORDER BY name ASC")
  fun observeSmallGroups(congregationId: String): Flow<List<SmallGroupEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSmallGroup(group: SmallGroupEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSmallGroups(groups: List<SmallGroupEntity>)

  @Update
  suspend fun updateSmallGroup(group: SmallGroupEntity)

  // --- Smart PDF Schedule Indexing Jobs ---
  @Query("SELECT * FROM pdf_schedule_jobs WHERE congregationId = :congregationId ORDER BY uploadedAt DESC")
  fun observePdfScheduleJobs(congregationId: String): Flow<List<PdfScheduleJobEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPdfScheduleJob(job: PdfScheduleJobEntity)

  // --- Manually Managed Congregations & Districts ---
  @Query("SELECT * FROM districts ORDER BY name ASC")
  fun observeDistricts(): Flow<List<DistrictEntity>>

  @Query("SELECT * FROM districts ORDER BY name ASC")
  suspend fun getDistrictsOnce(): List<DistrictEntity>

  @Query("SELECT * FROM districts WHERE id = :id LIMIT 1")
  suspend fun getDistrictById(id: String): DistrictEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDistrict(district: DistrictEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDistricts(districts: List<DistrictEntity>)

  @Query("DELETE FROM districts WHERE id = :id")
  suspend fun deleteDistrict(id: String)

  @Query("SELECT * FROM congregations ORDER BY displayName ASC")
  fun observeCongregations(): Flow<List<CongregationEntity>>

  @Query("SELECT * FROM congregations ORDER BY displayName ASC")
  suspend fun getCongregationsOnce(): List<CongregationEntity>

  @Query("SELECT * FROM congregations WHERE id = :id LIMIT 1")
  suspend fun getCongregationById(id: String): CongregationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCongregation(congregation: CongregationEntity)

  @Query("DELETE FROM congregations WHERE id = :id")
  suspend fun deleteCongregation(id: String)

  @Query("UPDATE congregations SET districtId = :newDistrictId WHERE id = :congregationId")
  suspend fun moveCongregationToDistrict(congregationId: String, newDistrictId: String)

  // --- Fresh Reset Queries ---
  @Query("DELETE FROM congregations")
  suspend fun clearAllCongregations()

  @Query("DELETE FROM announcements")
  suspend fun clearAllAnnouncements()

  @Query("DELETE FROM volunteer_assignments")
  suspend fun clearAllVolunteerAssignments()

  @Query("DELETE FROM hymn_selections")
  suspend fun clearAllHymns()

  @Query("DELETE FROM prayer_requests")
  suspend fun clearAllPrayerRequests()

  @Query("DELETE FROM ride_shares")
  suspend fun clearAllRideShares()

  @Query("DELETE FROM small_groups")
  suspend fun clearAllSmallGroups()

  @Query("DELETE FROM pdf_schedule_jobs")
  suspend fun clearAllPdfJobs()

  @Query("DELETE FROM user_profiles")
  suspend fun clearAllUsers()

  // --- Device Push Subscriptions (`push_subscriptions`) ---
  @Query("SELECT * FROM push_subscriptions WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC")
  fun observeUserPushSubscriptions(userId: String): Flow<List<PushSubscriptionEntity>>

  @Query("SELECT * FROM push_subscriptions WHERE userId = :userId AND isActive = 1")
  suspend fun getUserPushSubscriptionsOnce(userId: String): List<PushSubscriptionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPushSubscription(subscription: PushSubscriptionEntity)

  @Query("DELETE FROM push_subscriptions WHERE id = :subscriptionId")
  suspend fun deletePushSubscription(subscriptionId: String)

  @Query("DELETE FROM push_subscriptions WHERE endpoint = :endpoint")
  suspend fun deletePushSubscriptionByEndpoint(endpoint: String)

  // --- Scraped MiniHQ Resources (`minihq_resources`) ---
  @Query("SELECT * FROM minihq_resources ORDER BY isNextMonth ASC, district ASC, serviceDate ASC, assetType ASC")
  fun observeMiniHqResources(): Flow<List<MiniHqResourceEntity>>

  @Query("SELECT * FROM minihq_resources")
  suspend fun getMiniHqResourcesOnce(): List<MiniHqResourceEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMiniHqResources(resources: List<MiniHqResourceEntity>)

  @Query("DELETE FROM minihq_resources WHERE id NOT IN (:activeIds)")
  suspend fun deleteStaleMiniHqResources(activeIds: List<String>)

  @Query("DELETE FROM minihq_resources WHERE resourceCategory = :category")
  suspend fun deleteMiniHqResourcesByCategory(category: String)

  @Query("DELETE FROM minihq_resources")
  suspend fun deleteAllMiniHqResources()
}
