package com.example.parishsync.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.parishsync.data.model.AppRole
import com.example.parishsync.data.model.Congregation
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.District
import com.example.parishsync.data.model.MinistryDutyType
import com.example.parishsync.data.model.PastoralRequestType
import com.example.parishsync.data.model.RequestStatus
import com.example.parishsync.data.model.RidePostType
import com.example.parishsync.data.model.UserRole
import com.example.parishsync.data.model.VolunteerSubTask
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Room Entity & Supabase `districts` table record.
 */
@Serializable
@Entity(tableName = "districts")
data class DistrictEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("name")
  val name: String,
  @SerialName("region_code")
  val regionCode: String = "",
  @SerialName("created_at")
  val createdAt: Long = System.currentTimeMillis()
)

fun DistrictEntity.toDomain(): District = District(
  id = id,
  name = name,
  regionCode = regionCode
)

fun District.toEntity(): DistrictEntity = DistrictEntity(
  id = id.ifBlank { "dist_${UUID.randomUUID().toString().take(8)}" },
  name = name,
  regionCode = regionCode
)

/**
 * Room Entity & Supabase `congregations` table record.
 * Created manually by Rectors or Administrators (no preloaded congregations).
 */
@Serializable
@Entity(tableName = "congregations")
data class CongregationEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("district_id")
  val districtId: String = "",
  @SerialName("display_name")
  val displayName: String,
  @SerialName("address")
  val address: String = "",
  @SerialName("rector_name")
  val rectorName: String = "",
  @SerialName("primary_service_times")
  val primaryServiceTimes: String = "",
  @SerialName("sunday_service_time")
  val sundayServiceTime: String = "10:30 AM",
  @SerialName("wednesday_service_time")
  val wednesdayServiceTime: String = "7:30 PM",
  @SerialName("created_at")
  val createdAt: Long = System.currentTimeMillis()
)

fun CongregationEntity.toDomain(): Congregation = Congregation(
  id = id,
  districtId = districtId,
  displayName = displayName,
  address = address,
  rectorName = rectorName,
  primaryServiceTimes = primaryServiceTimes,
  sundayServiceTime = sundayServiceTime,
  wednesdayServiceTime = wednesdayServiceTime
)

fun Congregation.toEntity(): CongregationEntity = CongregationEntity(
  id = id.ifBlank { "cong_${UUID.randomUUID().toString().take(8)}" },
  districtId = districtId,
  displayName = displayName,
  address = address,
  rectorName = rectorName,
  primaryServiceTimes = primaryServiceTimes.ifBlank { "Sun $sundayServiceTime & Wed $wednesdayServiceTime" },
  sundayServiceTime = sundayServiceTime,
  wednesdayServiceTime = wednesdayServiceTime
)

/**
 * Room Entity & Supabase `profiles` table record.
 * Accounts are created exclusively by Administrators and Rectors.
 * A user's `role`, `delegatedRole`, and `congregationId` are locked to their account.
 */
@Serializable
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("email")
  val email: String,
  @SerialName("full_name")
  val fullName: String,
  @SerialName("congregation_id")
  val congregationId: String,
  @SerialName("role")
  val role: UserRole,
  @SerialName("delegated_role")
  val delegatedRole: DelegatedRole = DelegatedRole.NONE,
  @SerialName("roles")
  val roles: Set<AppRole> = emptySet(),
  @SerialName("password_hash")
  val passwordHash: String = "",
  @SerialName("invite_code")
  val inviteCode: String = "",
  @SerialName("account_status")
  val accountStatus: String = "ACTIVE", // "INVITED" (pending password setup) or "ACTIVE"
  @SerialName("invited_by")
  val invitedBy: String = "System Bootstrap",
  @SerialName("phone")
  val phone: String = "",
  @SerialName("is_choir_member")
  val isChoirMember: Boolean = false,
  @SerialName("preferred_ministries")
  val preferredMinistries: String = "",
  @SerialName("is_active_session")
  val isActiveSession: Boolean = false,
  @SerialName("prefer_push_reminders")
  val preferPushReminders: Boolean = true,
  @SerialName("prefer_email_reminders")
  val preferEmailReminders: Boolean = true,
  @SerialName("created_at")
  val createdAt: Long = System.currentTimeMillis()
) {
  /**
   * Resolves the user's effective multi-role set, guaranteeing [AppRole.MEMBER] base membership
   * and backward-compatible mapping from legacy [role] / [delegatedRole] when [roles] is empty.
   */
  val effectiveRoles: Set<AppRole>
    get() = if (roles.isNotEmpty()) {
      roles + AppRole.MEMBER
    } else {
      AppRole.fromLegacy(role, delegatedRole)
    }
}

/**
 * Room Entity & Supabase `user_roles` pivot table for multi-role assignments.
 */
@Serializable
@Entity(
  tableName = "user_roles",
  primaryKeys = ["userId", "role"],
  foreignKeys = [
    ForeignKey(
      entity = UserProfileEntity::class,
      parentColumns = ["id"],
      childColumns = ["userId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index("userId")]
)
data class UserRoleAssignmentEntity(
  @SerialName("user_id")
  val userId: String,
  @SerialName("role")
  val role: AppRole,
  @SerialName("assigned_at")
  val assignedAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity & Supabase `announcements` table record for the Live Bulletin, Service Leaflets,
 * and Urgent Admin Push Notifications.
 */
@Serializable
@Entity(tableName = "announcements")
data class AnnouncementEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("congregation_id")
  val congregationId: String,
  @SerialName("title")
  val title: String,
  @SerialName("summary")
  val summary: String,
  @SerialName("body")
  val body: String,
  @SerialName("category")
  val category: String,
  @SerialName("is_urgent")
  val isUrgent: Boolean = false,
  @SerialName("service_date")
  val serviceDate: String = "",
  @SerialName("liturgical_order")
  val liturgicalOrder: String = "",
  @SerialName("author_name")
  val authorName: String,
  @SerialName("author_role")
  val authorRole: UserRole,
  @SerialName("created_at")
  val createdAt: Long = System.currentTimeMillis(),
  @SerialName("is_synced")
  val isSynced: Boolean = true
)

/**
 * Room Entity & Supabase `volunteer_assignments` table record for tracking liturgical & parish duties:
 * AV, Altar Setup/Takedown, Greeter, Usher, etc., including MAIN vs BACKUP tier from PDF schedules.
 */
@Serializable
@Entity(tableName = "volunteer_assignments")
data class VolunteerAssignmentEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("congregation_id")
  val congregationId: String,
  @SerialName("duty_type")
  val dutyType: MinistryDutyType,
  @SerialName("custom_task_title")
  val customTaskTitle: String = "",
  @SerialName("assignment_tier")
  val assignmentTier: String = "MAIN", // "MAIN" or "BACKUP"
  @SerialName("assignee_name")
  val assigneeName: String,
  @SerialName("backup_assignee_name")
  val backupAssigneeName: String = "",
  @SerialName("assignee_user_id")
  val assigneeUserId: String? = null,
  @SerialName("service_date")
  val serviceDate: String,
  @SerialName("service_time")
  val serviceTime: String,
  @SerialName("notes")
  val notes: String = "",
  @SerialName("sub_tasks")
  val subTasks: List<VolunteerSubTask> = emptyList(),
  @SerialName("is_confirmed")
  val isConfirmed: Boolean = false,
  @SerialName("reminder_scheduled")
  val reminderScheduled: Boolean = false,
  @SerialName("source_pdf_name")
  val sourcePdfName: String? = null,
  @SerialName("is_local_user_match")
  val isLocalUserMatch: Boolean = false,
  @SerialName("is_synced")
  val isSynced: Boolean = true
)

/**
 * Room Entity & Supabase `hymn_selections` table record for the Choir & Hymn Selection Hub.
 */
@Serializable
@Entity(tableName = "hymn_selections")
data class HymnSelectionEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("congregation_id")
  val congregationId: String,
  @SerialName("service_date")
  val serviceDate: String,
  @SerialName("liturgical_sunday")
  val liturgicalSunday: String,
  @SerialName("liturgical_part")
  val liturgicalPart: String,
  @SerialName("hymn_number")
  val hymnNumber: String,
  @SerialName("title")
  val title: String,
  @SerialName("musical_key")
  val musicalKey: String,
  @SerialName("composer_or_tune")
  val composerOrTune: String,
  @SerialName("choir_rehearsal_notes")
  val choirRehearsalNotes: String,
  @SerialName("four_part_satb_ready")
  val fourPartSatbReady: Boolean = true
)

/**
 * Room Entity & Supabase `prayer_requests` table record for Digital Prayer Requests & Pastoral Visits.
 */
@Serializable
@Entity(tableName = "prayer_requests")
data class PrayerRequestEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("congregation_id")
  val congregationId: String,
  @SerialName("requester_name")
  val requesterName: String,
  @SerialName("requester_contact")
  val requesterContact: String,
  @SerialName("request_type")
  val requestType: PastoralRequestType,
  @SerialName("content")
  val content: String,
  @SerialName("is_private_to_clergy")
  val isPrivateToClergy: Boolean = true,
  @SerialName("status")
  val status: RequestStatus = RequestStatus.NEW,
  @SerialName("ministerial_notes")
  val ministerialNotes: String = "",
  @SerialName("created_at")
  val createdAt: Long = System.currentTimeMillis(),
  @SerialName("is_synced")
  val isSynced: Boolean = true
)

/**
 * Room Entity & Supabase `ride_shares` table record for the Ride Sharing Board.
 */
@Serializable
@Entity(tableName = "ride_shares")
data class RideShareEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("congregation_id")
  val congregationId: String,
  @SerialName("post_type")
  val postType: RidePostType,
  @SerialName("member_name")
  val memberName: String,
  @SerialName("phone_contact")
  val phoneContact: String,
  @SerialName("neighborhood")
  val neighborhood: String,
  @SerialName("service_date")
  val serviceDate: String,
  @SerialName("service_time")
  val serviceTime: String,
  @SerialName("seats_count")
  val seatsCount: Int,
  @SerialName("matched_member_name")
  val matchedMemberName: String? = null,
  @SerialName("notes")
  val notes: String = "",
  @SerialName("is_fulfilled")
  val isFulfilled: Boolean = false,
  @SerialName("is_synced")
  val isSynced: Boolean = true
)

/**
 * Room Entity & Supabase `small_groups` table record for Ministerial Dashboard & DSGs.
 */
@Serializable
@Entity(tableName = "small_groups")
data class SmallGroupEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("congregation_id")
  val congregationId: String,
  @SerialName("name")
  val name: String,
  @SerialName("leader_name")
  val leaderName: String,
  @SerialName("meeting_schedule")
  val meetingSchedule: String,
  @SerialName("location")
  val location: String,
  @SerialName("current_topic")
  val currentTopic: String,
  @SerialName("member_count")
  val memberCount: Int,
  @SerialName("max_capacity")
  val maxCapacity: Int,
  @SerialName("pastoral_log_notes")
  val pastoralLogNotes: String,
  @SerialName("service_liturgy_notes")
  val serviceLiturgyNotes: String = "",
  @SerialName("is_user_enrolled")
  val isUserEnrolled: Boolean = false
)

/**
 * Room Entity tracking background WorkManager Smart PDF Schedule Indexing jobs.
 */
@Serializable
@Entity(tableName = "pdf_schedule_jobs")
data class PdfScheduleJobEntity(
  @PrimaryKey
  val id: String,
  val congregationId: String,
  val pdfFileName: String,
  val uploadedBy: String,
  val status: String,
  val extractedDatesCount: Int = 0,
  val matchedRosterCount: Int = 0,
  val highlightedLocalMatches: Int = 0,
  val detailedLog: String = "",
  val parsedScheduleTableText: String = "",
  val uploadedAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity & Supabase `serving_schedules` table record for Divine Services,
 * conducting ministers, themes, and liturgies matching the New Apostolic Church (NAC) Canada schedule style.
 */
@Serializable
@Entity(tableName = "serving_schedules")
data class ServingScheduleEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("congregation_id")
  val congregationId: String,
  @SerialName("service_title")
  val serviceTitle: String, // e.g. "Sunday Divine Service", "Midweek Divine Service", "Youth Service"
  @SerialName("service_date")
  val serviceDate: String, // e.g. "Sun Nov 22, 2026"
  @SerialName("service_time")
  val serviceTime: String = "10:30 AM",
  @SerialName("officiant_minister")
  val officiantMinister: String, // e.g. "Pr. J. Grau"
  @SerialName("officiant_rank")
  val officiantRank: String = "Priest", // "District Apostle", "Apostle", "Bishop", "Rector", "Priest", "Deacon"
  @SerialName("assisting_ministers")
  val assistingMinisters: String = "", // e.g. "Dc. Duncan, Dc. Baker"
  @SerialName("choir_conductor")
  val choirConductor: String = "",
  @SerialName("organist")
  val organist: String = "",
  @SerialName("service_theme")
  val serviceTheme: String = "",
  @SerialName("bible_word")
  val bibleWord: String = "",
  @SerialName("special_service_tag")
  val specialServiceTag: String = "", // e.g. "Holy Communion", "Youth Service", "First Advent"
  @SerialName("location")
  val location: String = "",
  @SerialName("notes")
  val notes: String = "",
  @SerialName("created_at")
  val createdAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity & Supabase `push_subscriptions` table record for mobile/device push notification
 * endpoints and encryption keys. Supports multiple devices per user.
 */
@Serializable
@Entity(
  tableName = "push_subscriptions",
  indices = [Index("userId")]
)
data class PushSubscriptionEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("user_id")
  val userId: String,
  @SerialName("congregation_id")
  val congregationId: String,
  @SerialName("device_label")
  val deviceLabel: String = "Android Phone",
  @SerialName("platform")
  val platform: String = "android",
  @SerialName("endpoint")
  val endpoint: String,
  @SerialName("p256dh")
  val p256dh: String = "",
  @SerialName("auth")
  val auth: String = "",
  @SerialName("is_active")
  val isActive: Boolean = true,
  @SerialName("created_at")
  val createdAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity tracking discovered & locally cached MiniHQ assets:
 * - Monthly District Serving Schedule PDFs (`resourceCategory = "DISTRICT_SCHEDULE"`)
 * - Divine Service Prep (DSG) resources (`resourceCategory = "DSG_ASSET"`):
 *   English DSG, French DSG, Audio, References, English Transcript, French Transcript.
 */
@Serializable
@Entity(
  tableName = "minihq_resources",
  indices = [Index("resourceCategory"), Index("monthYear"), Index("district")]
)
data class MiniHqResourceEntity(
  @PrimaryKey
  @SerialName("id")
  val id: String,
  @SerialName("resource_category")
  val resourceCategory: String, // "DISTRICT_SCHEDULE" or "DSG_ASSET"
  @SerialName("month_year")
  val monthYear: String, // e.g. "November 2026", "December 2026"
  @SerialName("is_next_month")
  val isNextMonth: Boolean = false,
  @SerialName("district")
  val district: String = "", // e.g. "Hamilton", "Toronto", "British Columbia", ...
  @SerialName("service_date")
  val serviceDate: String = "",
  @SerialName("service_title")
  val serviceTitle: String = "",
  @SerialName("asset_type")
  val assetType: String, // "Schedule PDF", "English DSG", "French DSG", "Audio", "References", "English Transcript", "French Transcript"
  @SerialName("file_name")
  val fileName: String,
  @SerialName("source_url")
  val sourceUrl: String,
  @SerialName("local_path")
  val localPath: String = "",
  @SerialName("file_size")
  val fileSize: Long = 0L,
  @SerialName("mime_type")
  val mimeType: String = "application/pdf",
  @ColumnInfo(name = "pdf_blob", typeAffinity = ColumnInfo.BLOB)
  val pdfBlob: ByteArray? = null,
  @SerialName("content_text")
  val contentText: String = "",
  @SerialName("checksum")
  val checksum: String = "",
  @SerialName("last_checked_at")
  val lastCheckedAt: Long = System.currentTimeMillis()
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MiniHqResourceEntity) return false
    return id == other.id &&
      resourceCategory == other.resourceCategory &&
      monthYear == other.monthYear &&
      isNextMonth == other.isNextMonth &&
      district == other.district &&
      serviceDate == other.serviceDate &&
      serviceTitle == other.serviceTitle &&
      assetType == other.assetType &&
      fileName == other.fileName &&
      sourceUrl == other.sourceUrl &&
      localPath == other.localPath &&
      fileSize == other.fileSize &&
      mimeType == other.mimeType &&
      contentText == other.contentText &&
      checksum == other.checksum &&
      lastCheckedAt == other.lastCheckedAt &&
      (pdfBlob contentEquals other.pdfBlob)
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + checksum.hashCode()
    result = 31 * result + fileSize.hashCode()
    result = 31 * result + (pdfBlob?.contentHashCode() ?: 0)
    return result
  }
}
