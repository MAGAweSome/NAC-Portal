package com.example.parishsync.data.local

import com.example.parishsync.data.model.AppRole
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.UserRole

/**
 * Fresh-start bootstrap seed data.
 * All congregations, announcements, volunteer rosters, hymns, prayers, rideshares, small groups,
 * and PDF jobs start completely empty so the app opens as a brand-new installation.
 * Rectors and Administrators will create their congregations manually.
 *
 * Only a single dummy Administrator account is seeded so the Admin can log in and control
 * creating Rector and Member accounts by email via Mailtrap.
 */
object ParishSeedData {
  const val WOODSTOCK_CONGREGATION_ID = "cong_woodstock"
  const val WOODSTOCK_CONGREGATION_NAME = "Woodstock"

  fun defaultCanadianDistricts(): List<DistrictEntity> = listOf(
    DistrictEntity(id = "dist_bc", name = "British Columbia", regionCode = "BC"),
    DistrictEntity(id = "dist_ab", name = "Alberta", regionCode = "AB"),
    DistrictEntity(id = "dist_sk", name = "Saskatchewan", regionCode = "SK"),
    DistrictEntity(id = "dist_mb", name = "Manitoba", regionCode = "MB"),
    DistrictEntity(id = "dist_no", name = "Northern Ontario", regionCode = "ON-N"),
    DistrictEntity(id = "dist_kitchener", name = "Kitchener", regionCode = "ON-K"),
    DistrictEntity(id = "dist_hamilton", name = "Hamilton", regionCode = "ON-H"),
    DistrictEntity(id = "dist_toronto", name = "Toronto", regionCode = "ON-T"),
    DistrictEntity(id = "dist_eastern", name = "Eastern Canada", regionCode = "EAST")
  )

  const val BOOTSTRAP_ADMIN_EMAIL = "admin@nacportal.org"
  const val BOOTSTRAP_ADMIN_PASSWORD = "admin1234"
  const val BOOTSTRAP_ADMIN_NAME = "NAC Portal Administrator"

  const val BOOTSTRAP_MINISTER_EMAIL = "marcus@matrek.com"
  const val BOOTSTRAP_MINISTER_PASSWORD = "MarcusChurch2003"
  const val BOOTSTRAP_MINISTER_NAME = "Dc. Marcus Grau"

  const val BOOTSTRAP_JAMES_EMAIL = "james@nacportal.org"
  const val BOOTSTRAP_JAMES_PASSWORD = "JamesChurch2026"
  const val BOOTSTRAP_JAMES_NAME = "Pr. James"

  const val BOOTSTRAP_ERIC_EMAIL = "eric@nacportal.org"
  const val BOOTSTRAP_ERIC_PASSWORD = "EricChurch2026"
  const val BOOTSTRAP_ERIC_NAME = "Pr. Eric"

  fun woodstockCongregation(): CongregationEntity =
    CongregationEntity(
      id = WOODSTOCK_CONGREGATION_ID,
      districtId = "dist_kitchener",
      displayName = WOODSTOCK_CONGREGATION_NAME,
      address = "Woodstock, ON",
      rectorName = BOOTSTRAP_ERIC_NAME,
      primaryServiceTimes = "Sun 10:30 AM & Wed 7:30 PM",
      sundayServiceTime = "10:30 AM",
      wednesdayServiceTime = "7:30 PM"
    )

  fun bootstrapAdminAccount(): UserProfileEntity =
    UserProfileEntity(
      id = "user_bootstrap_admin",
      email = BOOTSTRAP_ADMIN_EMAIL,
      fullName = BOOTSTRAP_ADMIN_NAME,
      congregationId = "",
      role = UserRole.ADMINISTRATOR,
      delegatedRole = DelegatedRole.NONE,
      roles = setOf(AppRole.MEMBER, AppRole.ADMIN),
      passwordHash = BOOTSTRAP_ADMIN_PASSWORD,
      inviteCode = "",
      accountStatus = "ACTIVE",
      invitedBy = "System Bootstrap",
      phone = "",
      isChoirMember = false,
      preferredMinistries = "System Administration",
      isActiveSession = false
    )

  fun bootstrapMinisterAccount(): UserProfileEntity =
    UserProfileEntity(
      id = "user_marcus_minister",
      email = BOOTSTRAP_MINISTER_EMAIL,
      fullName = BOOTSTRAP_MINISTER_NAME,
      congregationId = WOODSTOCK_CONGREGATION_ID,
      role = UserRole.MINISTER,
      delegatedRole = DelegatedRole.NONE,
      roles = setOf(AppRole.MEMBER, AppRole.MINISTER),
      passwordHash = BOOTSTRAP_MINISTER_PASSWORD,
      inviteCode = "DEACON2026",
      accountStatus = "ACTIVE",
      invitedBy = "System Bootstrap",
      phone = "",
      isChoirMember = false,
      preferredMinistries = "Deacon • Pastoral Care & Serving",
      isActiveSession = false
    )

  fun bootstrapJamesAccount(): UserProfileEntity =
    UserProfileEntity(
      id = "user_james_priest",
      email = BOOTSTRAP_JAMES_EMAIL,
      fullName = BOOTSTRAP_JAMES_NAME,
      congregationId = WOODSTOCK_CONGREGATION_ID,
      role = UserRole.MINISTER,
      delegatedRole = DelegatedRole.VOLUNTEER_SCHEDULER,
      roles = setOf(AppRole.MEMBER, AppRole.MINISTER, AppRole.VOLUNTEER_SCHEDULER),
      passwordHash = BOOTSTRAP_JAMES_PASSWORD,
      inviteCode = "JAMES2026",
      accountStatus = "ACTIVE",
      invitedBy = "System Bootstrap",
      phone = "",
      isChoirMember = false,
      preferredMinistries = "Priest • Volunteer Schedule Coordinator",
      isActiveSession = false
    )

  fun bootstrapEricAccount(): UserProfileEntity =
    UserProfileEntity(
      id = "user_eric_rector",
      email = BOOTSTRAP_ERIC_EMAIL,
      fullName = BOOTSTRAP_ERIC_NAME,
      congregationId = WOODSTOCK_CONGREGATION_ID,
      role = UserRole.MINISTER,
      delegatedRole = DelegatedRole.NONE,
      roles = setOf(AppRole.MEMBER, AppRole.MINISTER, AppRole.RECTOR),
      passwordHash = BOOTSTRAP_ERIC_PASSWORD,
      inviteCode = "ERIC2026",
      accountStatus = "ACTIVE",
      invitedBy = "System Bootstrap",
      phone = "",
      isChoirMember = false,
      preferredMinistries = "Priest • Rector of Woodstock Congregation",
      isActiveSession = false
    )

  fun defaultRosterUsers(): List<UserProfileEntity> = listOf(
    bootstrapAdminAccount(),
    bootstrapMinisterAccount(),
    bootstrapJamesAccount(),
    bootstrapEricAccount()
  )
  fun defaultAnnouncements(): List<AnnouncementEntity> = emptyList()
  fun defaultVolunteerAssignments(): List<VolunteerAssignmentEntity> = emptyList()
  fun defaultHymnSelections(): List<HymnSelectionEntity> = emptyList()
  fun defaultPrayerRequests(): List<PrayerRequestEntity> = emptyList()
  fun defaultRideShares(): List<RideShareEntity> = emptyList()
  fun defaultSmallGroups(): List<SmallGroupEntity> = emptyList()
  fun defaultPdfJobs(): List<PdfScheduleJobEntity> = emptyList()
  fun defaultServingSchedules(): List<ServingScheduleEntity> = emptyList()

  fun sampleNacServingSchedules(congregationId: String): List<ServingScheduleEntity> = listOf(
    ServingScheduleEntity(
      id = "serv_1",
      congregationId = congregationId,
      serviceTitle = "Sunday Divine Service",
      serviceDate = "Sun Nov 22, 2026",
      serviceTime = "10:30 AM",
      officiantMinister = "Pr. J. Grau",
      officiantRank = "Priest / Rector",
      assistingMinisters = "Dc. Duncan, Dc. Baker",
      choirConductor = "Marilyn P.",
      organist = "Sister Sarah",
      serviceTheme = "Victory in Faith",
      bibleWord = "1 John 5:4 — 'For whatever is born of God overcomes the world.'",
      specialServiceTag = "Holy Communion",
      location = "Sanctuary",
      notes = "Pre-service choir rehearsal at 9:45 AM."
    ),
    ServingScheduleEntity(
      id = "serv_2",
      congregationId = congregationId,
      serviceTitle = "Midweek Divine Service",
      serviceDate = "Wed Nov 25, 2026",
      serviceTime = "7:30 PM",
      officiantMinister = "Pr. E. Grau",
      officiantRank = "Priest",
      assistingMinisters = "Dc. Duncan",
      choirConductor = "Marilyn P.",
      organist = "Brother David",
      serviceTheme = "Watchfulness & Prayer",
      bibleWord = "Luke 12:35-37 — 'Let your waist be girded and your lamps burning.'",
      specialServiceTag = "Divine Service",
      location = "Sanctuary",
      notes = "Midweek fellowship following service."
    ),
    ServingScheduleEntity(
      id = "serv_3",
      congregationId = congregationId,
      serviceTitle = "Sunday Divine Service — First Advent",
      serviceDate = "Sun Nov 29, 2026",
      serviceTime = "10:30 AM",
      officiantMinister = "Ev. E. Grau",
      officiantRank = "Evangelist",
      assistingMinisters = "Pr. J. Grau, Dc. Baker",
      choirConductor = "Marilyn P.",
      organist = "Sister Sarah",
      serviceTheme = "The Lord Is Near",
      bibleWord = "Philippians 4:4-7 — 'Rejoice in the Lord always. Again I will say, rejoice!'",
      specialServiceTag = "First Advent • Holy Communion",
      location = "Sanctuary",
      notes = "Sunday School & Youth Choir participating."
    )
  )
}
