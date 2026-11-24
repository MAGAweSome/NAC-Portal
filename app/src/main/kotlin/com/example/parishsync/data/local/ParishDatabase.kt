package com.example.parishsync.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  entities = [
    UserProfileEntity::class,
    UserRoleAssignmentEntity::class,
    DistrictEntity::class,
    CongregationEntity::class,
    AnnouncementEntity::class,
    VolunteerAssignmentEntity::class,
    HymnSelectionEntity::class,
    PrayerRequestEntity::class,
    RideShareEntity::class,
    SmallGroupEntity::class,
    PdfScheduleJobEntity::class,
    ServingScheduleEntity::class,
    PushSubscriptionEntity::class,
    MiniHqResourceEntity::class
  ],
  version = 13,
  exportSchema = false
)
@TypeConverters(ParishTypeConverters::class)
abstract class ParishDatabase : RoomDatabase() {

  abstract fun parishDao(): ParishDao

  companion object {
    private const val TAG = "ParishDatabase"
    private const val DB_NAME = "nac_portal_v3.db"

    @Volatile
    private var INSTANCE: ParishDatabase? = null

    fun getInstance(context: Context): ParishDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: run {
          recoverFromOversizedSqliteBlobs(context.applicationContext)
          Room.databaseBuilder(
            context.applicationContext,
            ParishDatabase::class.java,
            DB_NAME
          )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
            .also { INSTANCE = it }
        }
      }
    }

    /**
     * Prevents SQLiteBlobTooBigException (Android's 2 MB CursorWindow limit) on startup
     * by clearing any legacy binary `pdf_blob` payloads stored in `minihq_resources`
     * before Room queries the table.
     */
    private fun recoverFromOversizedSqliteBlobs(context: Context) {
      val prefs = context.getSharedPreferences("parish_db_recovery_prefs", Context.MODE_PRIVATE)
      if (prefs.getBoolean("blob_recovery_v13_done", false)) return
      val dbFile = context.getDatabasePath(DB_NAME)
      if (!dbFile.exists()) {
        prefs.edit().putBoolean("blob_recovery_v13_done", true).apply()
        return
      }
      runCatching {
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
          val hasTable = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='minihq_resources'",
            null
          ).use { cursor -> cursor.moveToFirst() }
          if (hasTable) {
            db.execSQL("UPDATE minihq_resources SET pdf_blob = NULL WHERE pdf_blob IS NOT NULL")
          }
        }
        prefs.edit().putBoolean("blob_recovery_v13_done", true).apply()
      }.onFailure { err ->
        Log.w(TAG, "Recovering SQLite cache after blob check failure: ${err.message}")
        runCatching {
          SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("DROP TABLE IF EXISTS minihq_resources")
          }
          prefs.edit().putBoolean("blob_recovery_v13_done", true).apply()
        }
      }
    }
  }
}
