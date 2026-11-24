package com.example.parishsync.data.model

import kotlinx.serialization.Serializable

/**
 * Primary Role-Based Access Control (RBAC) tiers for NAC Portal.
 * Strictly locked per account upon creation by an Administrator or Rector.
 * Users cannot self-switch roles.
 */
@Serializable
enum class UserRole(
  val title: String,
  val subtitle: String,
  val badgeLabel: String
) {
  MEMBER(
    title = "Congregation Member",
    subtitle = "Access live bulletins, personal & parish volunteer commitments, choir hymns, prayer requests, and ridesharing.",
    badgeLabel = "Member"
  ),
  MINISTER(
    title = "Rector / Minister",
    subtitle = "Invite congregation members, delegate Volunteer Scheduler roles, upload & email PDF schedules, and manage DSGs & pastoral care.",
    badgeLabel = "Rector / Minister"
  ),
  ADMINISTRATOR(
    title = "Parish Administrator",
    subtitle = "Full system control: provision Rectors & Members by email via Mailtrap, manage all congregations, upload schedules, and broadcast alerts.",
    badgeLabel = "Administrator"
  );

  val canAccessMinisterDashboard: Boolean
    get() = this == MINISTER || this == ADMINISTRATOR

  val canInviteMembers: Boolean
    get() = this == MINISTER || this == ADMINISTRATOR

  val canCreateRectorsOrAdmins: Boolean
    get() = this == ADMINISTRATOR

  val canDelegateCongregationRoles: Boolean
    get() = this == MINISTER || this == ADMINISTRATOR

  val canManageSmallGroups: Boolean
    get() = this == MINISTER || this == ADMINISTRATOR

  val canTriagePrayerQueue: Boolean
    get() = this == MINISTER || this == ADMINISTRATOR

  val canPublishAnnouncements: Boolean
    get() = this == MINISTER || this == ADMINISTRATOR

  val canBroadcastInstantPush: Boolean
    get() = this == ADMINISTRATOR || this == MINISTER
}

/**
 * Delegated congregation roles that a Rector (or Administrator) can assign to a Member
 * within their congregation without making them full clergy.
 */
@Serializable
enum class DelegatedRole(
  val displayName: String,
  val description: String
) {
  NONE(
    displayName = "Standard Member",
    description = "Standard congregation member permissions."
  ),
  VOLUNTEER_SCHEDULER(
    displayName = "Volunteer Scheduler",
    description = "Authorized by Rector to upload Volunteer Schedule PDFs, auto-populate member commitments, and email the PDF schedule to the congregation mailing list."
  )
}

/**
 * Multi-role authorization capabilities (stackable, non-exclusive).
 * Every registered user holds [MEMBER] by default.
 */
@Serializable
enum class AppRole(
  val key: String,
  val displayName: String,
  val description: String
) {
  MEMBER(
    key = "member",
    displayName = "Member",
    description = "Base congregation membership: view own congregation schedule and confirm personal duties."
  ),
  MINISTER(
    key = "minister",
    displayName = "Minister",
    description = "Ordained minister: access Divine Service serving schedules and pastoral care."
  ),
  RECTOR(
    key = "rector",
    displayName = "Rector",
    description = "Congregation Rector: manage congregation members, delegate Volunteer Scheduler roles, and oversee congregation."
  ),
  VOLUNTEER_SCHEDULER(
    key = "volunteer_scheduler",
    displayName = "Volunteer Scheduler",
    description = "Create/edit/delete volunteer duties, upload/parse schedule PDFs, and send schedule reminder emails."
  ),
  ADMIN(
    key = "admin",
    displayName = "Admin",
    description = "Global system administrator: cross-congregation visibility and full Admin Console access."
  );

  companion object {
    fun fromKey(key: String): AppRole? =
      entries.firstOrNull { it.key.equals(key.trim(), ignoreCase = true) || it.name.equals(key.trim(), ignoreCase = true) }

    fun fromLegacy(role: UserRole, delegatedRole: DelegatedRole = DelegatedRole.NONE): Set<AppRole> {
      val set = mutableSetOf(MEMBER)
      when (role) {
        UserRole.MEMBER -> Unit
        UserRole.MINISTER -> {
          set += MINISTER
          set += RECTOR
        }
        UserRole.ADMINISTRATOR -> {
          set += ADMIN
        }
      }
      if (delegatedRole == DelegatedRole.VOLUNTEER_SCHEDULER) {
        set += VOLUNTEER_SCHEDULER
      }
      return set
    }
  }
}

/**
 * Authorization policy helpers for stackable [AppRole] sets.
 */
object ParishPolicy {
  fun canManageVolunteerSchedule(roles: Set<AppRole>): Boolean =
    AppRole.VOLUNTEER_SCHEDULER in roles || AppRole.ADMIN in roles

  fun canRemindVolunteer(roles: Set<AppRole>): Boolean =
    AppRole.VOLUNTEER_SCHEDULER in roles || AppRole.RECTOR in roles || AppRole.ADMIN in roles

  fun canAccessServingSchedule(roles: Set<AppRole>): Boolean =
    AppRole.MINISTER in roles || AppRole.RECTOR in roles || AppRole.ADMIN in roles

  fun canAccessAdminConsole(roles: Set<AppRole>): Boolean =
    AppRole.MINISTER in roles || AppRole.RECTOR in roles || AppRole.ADMIN in roles

  fun isGlobalAdmin(roles: Set<AppRole>): Boolean =
    AppRole.ADMIN in roles

  fun canAccessCongregation(roles: Set<AppRole>, userCongregationId: String, targetCongregationId: String): Boolean {
    if (isGlobalAdmin(roles)) return true
    if (userCongregationId.isBlank() || targetCongregationId.isBlank()) return false
    return userCongregationId == targetCongregationId
  }

  fun formatRoleBadges(roles: Set<AppRole>): String {
    val ordered = listOf(AppRole.ADMIN, AppRole.RECTOR, AppRole.MINISTER, AppRole.VOLUNTEER_SCHEDULER)
      .filter { it in roles }
    return if (ordered.isEmpty()) {
      AppRole.MEMBER.displayName
    } else {
      ordered.joinToString(" • ") { it.displayName }
    }
  }
}

/**
 * Helper to check if a user can create/edit/delete duties, upload/parse PDFs, and email schedules.
 * Strictly gated to [AppRole.VOLUNTEER_SCHEDULER] or [AppRole.ADMIN].
 * Ministers/Rectors without [AppRole.VOLUNTEER_SCHEDULER] or [AppRole.ADMIN] have read-only schedule access.
 */
fun canUploadAndEmailVolunteerSchedule(role: UserRole, delegatedRole: DelegatedRole): Boolean {
  return role == UserRole.ADMINISTRATOR ||
    delegatedRole == DelegatedRole.VOLUNTEER_SCHEDULER
}

fun canUploadAndEmailVolunteerSchedule(roles: Set<AppRole>): Boolean {
  return ParishPolicy.canManageVolunteerSchedule(roles)
}

/**
 * Dynamic Congregation model.
 * Created and managed manually by Rectors and Administrators (no hardcoded preloaded congregations).
 */
@Serializable
data class VolunteerSubTask(
  val id: String,
  val label: String,
  val isCompleted: Boolean = false
)

@Serializable
data class District(
  val id: String = "",
  val name: String = "",
  val regionCode: String = ""
)

@Serializable
data class Congregation(
  val id: String = "",
  val districtId: String = "",
  val displayName: String = "",
  val address: String = "",
  val rectorName: String = "",
  val primaryServiceTimes: String = "",
  val sundayServiceTime: String = "10:30 AM",
  val wednesdayServiceTime: String = "7:30 PM"
) {
  companion object {
    val NONE = Congregation(
      id = "",
      districtId = "",
      displayName = "No Congregation Assigned",
      address = "",
      rectorName = "",
      primaryServiceTimes = "",
      sundayServiceTime = "10:30 AM",
      wednesdayServiceTime = "7:30 PM"
    )
  }
}

/**
 * Volunteer Roster duty categories matching the parish PDF schedule columns:
 * AV | ALTAR SETUP/TAKEDOWN | GREETER | USHER (plus Sunday School, Coffee Hour, Cleaning).
 */
@Serializable
enum class MinistryDutyType(
  val displayName: String,
  val description: String,
  val defaultLeadTimeMinutes: Int
) {
  AV_SOUND_BOOTH(
    displayName = "AV",
    description = "Operate sanctuary sound board, microphones, projection, and livestream.",
    defaultLeadTimeMinutes = 40
  ),
  ALTAR_SETUP(
    displayName = "Altar Setup/Takedown",
    description = "Prepare and reset sacred vessels, altar linens, paraments, and candles.",
    defaultLeadTimeMinutes = 45
  ),
  GREETER(
    displayName = "Greeter",
    description = "Welcome parishioners and visitors at the narthex doors before service.",
    defaultLeadTimeMinutes = 30
  ),
  USHERING(
    displayName = "Usher",
    description = "Assist seating, distribute bulletins, collect offering, and guide communion.",
    defaultLeadTimeMinutes = 30
  ),
  SUNDAY_SCHOOL(
    displayName = "Sunday School",
    description = "Lead children's catechesis and Sunday school classes.",
    defaultLeadTimeMinutes = 25
  ),
  COFFEE_HOUR(
    displayName = "Coffee Hour",
    description = "Prepare parish hall refreshments and fellowship hospitality.",
    defaultLeadTimeMinutes = 30
  ),
  CLEANING(
    displayName = "Sanctuary Cleaning",
    description = "Post-service nave reset and tidying.",
    defaultLeadTimeMinutes = 15
  )
}

/**
 * Categories for Digital Prayer Requests & Pastoral Visits.
 */
@Serializable
enum class PastoralRequestType(val displayName: String) {
  PRAYER_INTENTION("Prayer Intention"),
  HOSPITAL_VISIT("Urgent Hospital Visit"),
  HOMEBOUND_COMMUNION("Homebound Communion"),
  PASTORAL_COUNSELING("Pastoral Counseling"),
  SACRAMENTAL_PREP("Baptism / Confirmation / Marriage")
}

@Serializable
enum class RequestStatus(val displayName: String) {
  NEW("New in Queue"),
  IN_PRAYER("Held in Prayer"),
  SCHEDULED_VISIT("Visit Scheduled"),
  COMPLETED("Completed / Archived")
}

/**
 * Ride Sharing Board post types.
 */
@Serializable
enum class RidePostType(val displayName: String) {
  OFFERING_RIDE("Volunteer Driver (Offering Seats)"),
  NEEDING_RIDE("Member Requesting Ride")
}
