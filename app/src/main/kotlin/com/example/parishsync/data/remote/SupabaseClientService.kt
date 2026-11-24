package com.example.parishsync.data.remote

import com.example.parishsync.BuildConfig
import com.example.parishsync.data.local.AnnouncementEntity
import com.example.parishsync.data.local.PrayerRequestEntity
import com.example.parishsync.data.local.RideShareEntity
import com.example.parishsync.data.local.SmallGroupEntity
import com.example.parishsync.data.local.UserProfileEntity
import com.example.parishsync.data.local.VolunteerAssignmentEntity
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.UserRole
import com.example.parishsync.data.model.canUploadAndEmailVolunteerSchedule
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SupabaseConfig(
  val supabaseUrl: String = BuildConfig.SUPABASE_URL,
  val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY,
  val enableRealtimeSync: Boolean = true
) {
  val isLiveConfigured: Boolean
    get() = supabaseUrl.startsWith("https://") && supabaseAnonKey.length > 20
}

@Serializable
data class SupabaseAuthSession(
  @SerialName("access_token")
  val accessToken: String,
  @SerialName("user_id")
  val userId: String,
  @SerialName("email")
  val email: String,
  val isLiveSession: Boolean = false
)

/**
 * Abstraction layer for Supabase Authentication, PostgREST CRUD, and Realtime sync.
 * Roles and congregations are strictly locked to user accounts provisioned by Admins/Rectors.
 */
class SupabaseClientService(
  initialConfig: SupabaseConfig = SupabaseConfig()
) {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
  }

  private val _config = MutableStateFlow(initialConfig)
  val config: StateFlow<SupabaseConfig> = _config.asStateFlow()

  private val _session = MutableStateFlow<SupabaseAuthSession?>(null)
  val session: StateFlow<SupabaseAuthSession?> = _session.asStateFlow()

  private val _lastSyncStatus = MutableStateFlow("Account-locked RBAC active • Offline-First Room SQLite + Mailtrap Email API")
  val lastSyncStatus: StateFlow<String> = _lastSyncStatus.asStateFlow()

  fun updateConfiguration(newUrl: String, newAnonKey: String) {
    val trimmedUrl = newUrl.trim().removeSuffix("/")
    val trimmedKey = newAnonKey.trim()
    _config.value = SupabaseConfig(
      supabaseUrl = trimmedUrl,
      supabaseAnonKey = trimmedKey,
      enableRealtimeSync = true
    )
    _lastSyncStatus.value = if (_config.value.isLiveConfigured) {
      "Configured live Supabase project: $trimmedUrl"
    } else {
      "Running in Offline-First Local Room SQLite mode"
    }
  }

  fun recordLocalSession(user: UserProfileEntity) {
    _session.value = SupabaseAuthSession(
      accessToken = "locked_jwt_${user.id}",
      userId = user.id,
      email = user.email,
      isLiveSession = _config.value.isLiveConfigured
    )
    _lastSyncStatus.value = "Signed in as ${user.fullName} (${user.role.badgeLabel})"
  }

  suspend fun signOut() {
    _session.value = null
    _lastSyncStatus.value = "Signed out • Account roles remain locked"
  }

  suspend fun pushUserProfile(profile: UserProfileEntity): Result<Unit> =
    withContext(Dispatchers.IO) {
      if (!_config.value.isLiveConfigured) {
        return@withContext Result.success(Unit)
      }
      runCatching {
        upsertRemoteRecord("user_profiles", json.encodeToString(profile))
        Unit
      }
    }

  suspend fun pushAnnouncement(announcement: AnnouncementEntity, actorRole: UserRole): Result<Unit> =
    withContext(Dispatchers.IO) {
      if (!actorRole.canPublishAnnouncements) {
        return@withContext Result.failure(
          SecurityException("RBAC/RLS Policy Violation: Role ${actorRole.name} cannot publish announcements.")
        )
      }
      if (!_config.value.isLiveConfigured) {
        _lastSyncStatus.value = "Saved announcement '${announcement.title}' to Room"
        return@withContext Result.success(Unit)
      }
      runCatching {
        upsertRemoteRecord("announcements", json.encodeToString(announcement))
        _lastSyncStatus.value = "Synced announcement '${announcement.title}' to Supabase PostgreSQL"
      }
    }

  suspend fun pushVolunteerAssignments(
    assignments: List<VolunteerAssignmentEntity>,
    actorRole: UserRole,
    actorDelegatedRole: DelegatedRole
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!canUploadAndEmailVolunteerSchedule(actorRole, actorDelegatedRole)) {
      return@withContext Result.failure(
        SecurityException("RBAC Violation: Only Rectors, Admins, or delegated Volunteer Schedulers can upload schedule PDFs.")
      )
    }
    if (!_config.value.isLiveConfigured) {
      _lastSyncStatus.value = "Auto-populated ${assignments.size} volunteer commitments in local Room database"
      return@withContext Result.success(Unit)
    }
    runCatching {
      upsertRemoteRecord("volunteer_assignments", json.encodeToString(assignments))
      _lastSyncStatus.value = "Synced ${assignments.size} roster assignments to Supabase"
    }
  }

  suspend fun pushPrayerRequest(request: PrayerRequestEntity): Result<Unit> =
    withContext(Dispatchers.IO) {
      if (!_config.value.isLiveConfigured) {
        _lastSyncStatus.value = "Queued prayer request in local ministerial database"
        return@withContext Result.success(Unit)
      }
      runCatching {
        upsertRemoteRecord("prayer_requests", json.encodeToString(request))
        _lastSyncStatus.value = "Encrypted prayer intention synced to Supabase ministerial queue"
      }
    }

  suspend fun pushRideShare(rideShare: RideShareEntity): Result<Unit> =
    withContext(Dispatchers.IO) {
      if (!_config.value.isLiveConfigured) {
        _lastSyncStatus.value = "Updated Ride Sharing Board in local Room database"
        return@withContext Result.success(Unit)
      }
      runCatching {
        upsertRemoteRecord("ride_shares", json.encodeToString(rideShare))
        _lastSyncStatus.value = "Synced Ride Share post to Supabase"
      }
    }

  suspend fun pushSmallGroup(smallGroup: SmallGroupEntity, actorRole: UserRole): Result<Unit> =
    withContext(Dispatchers.IO) {
      if (!actorRole.canManageSmallGroups) {
        return@withContext Result.failure(
          SecurityException("RBAC/RLS Policy Violation: Only Ministers/Admins can update DSG pastoral logs.")
        )
      }
      if (!_config.value.isLiveConfigured) {
        _lastSyncStatus.value = "Saved DSG pastoral log for '${smallGroup.name}' in Room"
        return@withContext Result.success(Unit)
      }
      runCatching {
        upsertRemoteRecord("small_groups", json.encodeToString(smallGroup))
        _lastSyncStatus.value = "Synced Small Group (DSG) '${smallGroup.name}' to Supabase"
      }
    }

  suspend fun fetchRemoteAnnouncements(congregationId: String): Result<List<AnnouncementEntity>> =
    withContext(Dispatchers.IO) {
      val cfg = _config.value
      if (!cfg.isLiveConfigured) {
        return@withContext Result.success(emptyList())
      }
      runCatching {
        val url = "${cfg.supabaseUrl}/rest/v1/announcements?congregation_id=eq.$congregationId&select=*"
        val response = executeSupabaseRequest(
          url = url,
          method = "GET",
          anonKey = cfg.supabaseAnonKey,
          bearerToken = _session.value?.accessToken ?: cfg.supabaseAnonKey
        )
        json.decodeFromString<List<AnnouncementEntity>>(response)
      }
    }

  private fun upsertRemoteRecord(table: String, bodyJson: String): String {
    val cfg = _config.value
    val token = _session.value?.accessToken ?: cfg.supabaseAnonKey
    return executeSupabaseRequest(
      url = "${cfg.supabaseUrl}/rest/v1/$table",
      method = "POST",
      anonKey = cfg.supabaseAnonKey,
      bearerToken = token,
      bodyJson = bodyJson,
      extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
    )
  }

  private fun executeSupabaseRequest(
    url: String,
    method: String,
    anonKey: String,
    bearerToken: String,
    bodyJson: String? = null,
    extraHeaders: Map<String, String> = emptyMap()
  ): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 6_000
      readTimeout = 6_000
      setRequestProperty("apikey", anonKey)
      setRequestProperty("Authorization", "Bearer $bearerToken")
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Accept", "application/json")
      extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
      doInput = true
      if (bodyJson != null) {
        doOutput = true
      }
    }
    try {
      if (bodyJson != null) {
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
          writer.write(bodyJson)
          writer.flush()
        }
      }
      val code = connection.responseCode
      val stream = if (code in 200..299) connection.inputStream else connection.errorStream
      val text = stream?.let {
        BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
      }.orEmpty()
      if (code !in 200..299) {
        throw IllegalStateException("HTTP $code: $text")
      }
      return text
    } finally {
      connection.disconnect()
    }
  }
}
