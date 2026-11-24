package com.example.parishsync

import androidx.navigation3.runtime.NavKey
import com.example.parishsync.data.model.AppRole
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.ParishPolicy
import com.example.parishsync.data.model.UserRole
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object OnboardingRoute : NavKey
@Serializable data object VolunteerScheduleRoute : NavKey
@Serializable data object ServingScheduleRoute : NavKey
@Serializable data object AdminConsoleRoute : NavKey
@Serializable data object SettingsRoute : NavKey

// Backward compatibility routes
@Serializable data object BulletinRoute : NavKey
@Serializable data object RosterRoute : NavKey
@Serializable data object ChoirHymnRoute : NavKey
@Serializable data object CommunityRoute : NavKey
@Serializable data object MinisterDashboardRoute : NavKey

/**
 * 3 Primary Navigation destinations for NAC Portal:
 * 1. Volunteer Schedule (manual entry & PDF indexing)
 * 2. Serving Schedule (NAC Canada divine service liturgy & minister assignments)
 * 3. Admin (Congregation setup, account invitations via Mailtrap, and role delegation)
 */
enum class ParishBottomDestination(
  val route: NavKey,
  val label: String
) {
  VOLUNTEER_SCHEDULE(VolunteerScheduleRoute, "Volunteer Schedule"),
  SERVING_SCHEDULE(ServingScheduleRoute, "Serving Schedule"),
  ADMIN(AdminConsoleRoute, "Admin");

  companion object {
    fun destinationsForRole(
      role: UserRole = UserRole.MEMBER,
      delegatedRole: DelegatedRole = DelegatedRole.NONE
    ): List<ParishBottomDestination> =
      destinationsForRoles(AppRole.fromLegacy(role, delegatedRole))

    fun destinationsForRoles(roles: Set<AppRole>): List<ParishBottomDestination> {
      val list = mutableListOf(VOLUNTEER_SCHEDULE)
      if (ParishPolicy.canAccessServingSchedule(roles)) {
        list += SERVING_SCHEDULE
      }
      if (ParishPolicy.isGlobalAdmin(roles)) {
        list += ADMIN
      }
      return list
    }

    fun isRouteAuthorized(route: NavKey, roles: Set<AppRole>): Boolean = when (route) {
      VolunteerScheduleRoute, Main -> true
      ServingScheduleRoute -> ParishPolicy.canAccessServingSchedule(roles)
      AdminConsoleRoute, MinisterDashboardRoute -> ParishPolicy.isGlobalAdmin(roles)
      BulletinRoute, RosterRoute, ChoirHymnRoute, CommunityRoute, SettingsRoute -> true
      OnboardingRoute -> true
      else -> false
    }
  }
}
