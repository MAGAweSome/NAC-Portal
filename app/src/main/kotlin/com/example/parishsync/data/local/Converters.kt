package com.example.parishsync.data.local

import androidx.room.TypeConverter
import com.example.parishsync.data.model.AppRole
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.MinistryDutyType
import com.example.parishsync.data.model.PastoralRequestType
import com.example.parishsync.data.model.RequestStatus
import com.example.parishsync.data.model.RidePostType
import com.example.parishsync.data.model.UserRole
import com.example.parishsync.data.model.VolunteerSubTask
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ParishTypeConverters {
  @TypeConverter
  fun fromAppRole(role: AppRole): String = role.key

  @TypeConverter
  fun toAppRole(value: String): AppRole =
    AppRole.fromKey(value) ?: AppRole.MEMBER

  @TypeConverter
  fun fromAppRoleSet(roles: Set<AppRole>): String =
    roles.map { it.key }.sorted().joinToString(",")

  @TypeConverter
  fun toAppRoleSet(value: String): Set<AppRole> {
    if (value.isBlank()) return setOf(AppRole.MEMBER)
    val parsed = value.split(",").mapNotNull { AppRole.fromKey(it) }.toSet()
    return parsed + AppRole.MEMBER
  }

  @TypeConverter
  fun fromUserRole(role: UserRole): String = role.name

  @TypeConverter
  fun toUserRole(value: String): UserRole =
    runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.MEMBER)

  @TypeConverter
  fun fromDelegatedRole(role: DelegatedRole): String = role.name

  @TypeConverter
  fun toDelegatedRole(value: String): DelegatedRole =
    runCatching { DelegatedRole.valueOf(value) }.getOrDefault(DelegatedRole.NONE)

  @TypeConverter
  fun fromMinistryDutyType(dutyType: MinistryDutyType): String = dutyType.name

  @TypeConverter
  fun toMinistryDutyType(value: String): MinistryDutyType =
    runCatching { MinistryDutyType.valueOf(value) }.getOrDefault(MinistryDutyType.USHERING)

  @TypeConverter
  fun fromPastoralRequestType(type: PastoralRequestType): String = type.name

  @TypeConverter
  fun toPastoralRequestType(value: String): PastoralRequestType =
    runCatching { PastoralRequestType.valueOf(value) }.getOrDefault(PastoralRequestType.PRAYER_INTENTION)

  @TypeConverter
  fun fromRequestStatus(status: RequestStatus): String = status.name

  @TypeConverter
  fun toRequestStatus(value: String): RequestStatus =
    runCatching { RequestStatus.valueOf(value) }.getOrDefault(RequestStatus.NEW)

  @TypeConverter
  fun fromRidePostType(type: RidePostType): String = type.name

  @TypeConverter
  fun toRidePostType(value: String): RidePostType =
    runCatching { RidePostType.valueOf(value) }.getOrDefault(RidePostType.OFFERING_RIDE)

  @TypeConverter
  fun fromSubTaskList(subTasks: List<VolunteerSubTask>): String =
    Json.encodeToString(subTasks)

  @TypeConverter
  fun toSubTaskList(value: String): List<VolunteerSubTask> =
    if (value.isBlank()) emptyList() else runCatching { Json.decodeFromString<List<VolunteerSubTask>>(value) }.getOrDefault(emptyList())
}
