package com.example.parishsync

import com.example.parishsync.data.local.MiniHqScrapedData
import com.example.parishsync.data.local.ParishSeedData
import com.example.parishsync.data.local.ParishTypeConverters
import com.example.parishsync.data.local.UserProfileEntity
import com.example.parishsync.data.model.AppRole
import com.example.parishsync.data.model.Congregation
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.MinistryDutyType
import com.example.parishsync.data.model.ParishPolicy
import com.example.parishsync.data.model.UserRole
import com.example.parishsync.data.model.VolunteerSubTask
import com.example.parishsync.data.model.canUploadAndEmailVolunteerSchedule
import com.example.parishsync.workers.MiniHqScraperService
import com.example.parishsync.workers.MiniHqScrapeWorker
import com.example.parishsync.workers.ParishNotificationHelper
import com.example.parishsync.workers.PdfScheduleParser
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ParishSyncUnitTest {

  @Test
  fun freshBootstrapState_hasBootstrapAdminAndClergyAccountsAndZeroData() {
    val users = ParishSeedData.defaultRosterUsers()
    assertEquals(4, users.size)
    val admin = users.first { it.email == ParishSeedData.BOOTSTRAP_ADMIN_EMAIL }
    assertEquals(UserRole.ADMINISTRATOR, admin.role)
    assertTrue(AppRole.ADMIN in admin.effectiveRoles)

    val marcus = users.first { it.email == ParishSeedData.BOOTSTRAP_MINISTER_EMAIL }
    assertEquals("Dc. Marcus Grau", marcus.fullName)
    assertEquals("marcus@matrek.com", marcus.email)
    assertEquals(ParishSeedData.WOODSTOCK_CONGREGATION_ID, marcus.congregationId)
    assertFalse(AppRole.RECTOR in marcus.effectiveRoles)
    assertFalse(AppRole.ADMIN in marcus.effectiveRoles)
    assertFalse(ParishPolicy.canManageVolunteerSchedule(marcus.effectiveRoles))

    val james = users.first { it.email == ParishSeedData.BOOTSTRAP_JAMES_EMAIL }
    assertEquals("Pr. James", james.fullName)
    assertEquals(ParishSeedData.WOODSTOCK_CONGREGATION_ID, james.congregationId)
    assertTrue(AppRole.MINISTER in james.effectiveRoles)
    assertTrue(AppRole.VOLUNTEER_SCHEDULER in james.effectiveRoles)
    assertTrue(ParishPolicy.canManageVolunteerSchedule(james.effectiveRoles))

    val eric = users.first { it.email == ParishSeedData.BOOTSTRAP_ERIC_EMAIL }
    assertEquals("Pr. Eric", eric.fullName)
    assertEquals(ParishSeedData.WOODSTOCK_CONGREGATION_ID, eric.congregationId)
    assertTrue(AppRole.RECTOR in eric.effectiveRoles)
    assertTrue(AppRole.MINISTER in eric.effectiveRoles)
    assertFalse(ParishPolicy.canManageVolunteerSchedule(eric.effectiveRoles))

    assertTrue(ParishSeedData.defaultAnnouncements().isEmpty())
    assertTrue(ParishSeedData.defaultVolunteerAssignments().isEmpty())
    assertTrue(ParishSeedData.defaultHymnSelections().isEmpty())
    assertTrue(ParishSeedData.defaultPrayerRequests().isEmpty())
    assertTrue(ParishSeedData.defaultRideShares().isEmpty())
    assertTrue(ParishSeedData.defaultSmallGroups().isEmpty())
  }

  @Test
  fun rbacAndDelegatedRolePermissions_enforceScheduleUploadAndInviteRules() {
    // 1. Standard Member cannot upload schedules or invite users
    assertFalse(UserRole.MEMBER.canInviteMembers)
    assertFalse(canUploadAndEmailVolunteerSchedule(UserRole.MEMBER, DelegatedRole.NONE))
    assertFalse(ParishPolicy.canManageVolunteerSchedule(setOf(AppRole.MEMBER)))

    // 2. Member with VOLUNTEER_SCHEDULER role CAN manage & email schedules
    assertTrue(canUploadAndEmailVolunteerSchedule(UserRole.MEMBER, DelegatedRole.VOLUNTEER_SCHEDULER))
    assertTrue(ParishPolicy.canManageVolunteerSchedule(setOf(AppRole.MEMBER, AppRole.VOLUNTEER_SCHEDULER)))

    // 3. Minister / Rector WITHOUT VOLUNTEER_SCHEDULER has read-only Volunteer Schedule access
    assertTrue(UserRole.MINISTER.canInviteMembers)
    assertTrue(UserRole.MINISTER.canDelegateCongregationRoles)
    assertFalse(canUploadAndEmailVolunteerSchedule(UserRole.MINISTER, DelegatedRole.NONE))
    assertFalse(ParishPolicy.canManageVolunteerSchedule(setOf(AppRole.MEMBER, AppRole.MINISTER, AppRole.RECTOR)))
    assertTrue(ParishPolicy.canAccessServingSchedule(setOf(AppRole.MEMBER, AppRole.MINISTER, AppRole.RECTOR)))
    assertFalse(UserRole.MINISTER.canCreateRectorsOrAdmins)

    // 4. Stackable Minister + Volunteer Scheduler CAN manage schedules
    assertTrue(ParishPolicy.canManageVolunteerSchedule(setOf(AppRole.MEMBER, AppRole.MINISTER, AppRole.VOLUNTEER_SCHEDULER)))

    // 5. Global Admin has full system account creation & schedule privileges
    assertTrue(UserRole.ADMINISTRATOR.canCreateRectorsOrAdmins)
    assertTrue(UserRole.ADMINISTRATOR.canInviteMembers)
    assertTrue(canUploadAndEmailVolunteerSchedule(UserRole.ADMINISTRATOR, DelegatedRole.NONE))
    assertTrue(ParishPolicy.canManageVolunteerSchedule(setOf(AppRole.MEMBER, AppRole.ADMIN)))

    // 6. Remind button visibility: strictly volunteer_scheduler, rector, or admin (hidden for member & non-rector minister)
    assertFalse(ParishPolicy.canRemindVolunteer(setOf(AppRole.MEMBER)))
    assertFalse(ParishPolicy.canRemindVolunteer(setOf(AppRole.MEMBER, AppRole.MINISTER)))
    assertTrue(ParishPolicy.canRemindVolunteer(setOf(AppRole.MEMBER, AppRole.VOLUNTEER_SCHEDULER)))
    assertTrue(ParishPolicy.canRemindVolunteer(setOf(AppRole.MEMBER, AppRole.MINISTER, AppRole.RECTOR)))
    assertTrue(ParishPolicy.canRemindVolunteer(setOf(AppRole.MEMBER, AppRole.ADMIN)))
  }

  @Test
  fun congregationMultiTenancy_isolatesStandardRolesAndAllowsAdminCrossTenantAccess() {
    val woodstock = "cong_woodstock"
    val margaretAve = "cong_margaret_ave"

    // Standard Member, Minister, Rector, and Volunteer Scheduler are strictly scoped to their congregation
    val schedulerRoles = setOf(AppRole.MEMBER, AppRole.VOLUNTEER_SCHEDULER)
    assertTrue(ParishPolicy.canAccessCongregation(schedulerRoles, woodstock, woodstock))
    assertFalse(ParishPolicy.canAccessCongregation(schedulerRoles, woodstock, margaretAve))
    assertFalse(ParishPolicy.canAccessCongregation(setOf(AppRole.MEMBER), "", woodstock))

    val ministerRoles = setOf(AppRole.MEMBER, AppRole.MINISTER, AppRole.RECTOR)
    assertTrue(ParishPolicy.canAccessCongregation(ministerRoles, margaretAve, margaretAve))
    assertFalse(ParishPolicy.canAccessCongregation(ministerRoles, margaretAve, woodstock))

    // Global Admin bypasses single-congregation restriction
    val adminRoles = setOf(AppRole.MEMBER, AppRole.ADMIN)
    assertTrue(ParishPolicy.canAccessCongregation(adminRoles, "", woodstock))
    assertTrue(ParishPolicy.canAccessCongregation(adminRoles, "", margaretAve))
  }

  @Test
  fun bottomNavigation_onlyMinistersAndAdminsCanSeeServingScheduleTab() {
    val memberDestinations = ParishBottomDestination.destinationsForRole(UserRole.MEMBER)
    assertEquals(1, memberDestinations.size)
    assertEquals(ParishBottomDestination.VOLUNTEER_SCHEDULE, memberDestinations[0])
    assertFalse(memberDestinations.contains(ParishBottomDestination.SERVING_SCHEDULE))
    assertFalse(memberDestinations.contains(ParishBottomDestination.ADMIN))

    val ministerDestinations = ParishBottomDestination.destinationsForRole(UserRole.MINISTER)
    assertEquals(2, ministerDestinations.size)
    assertTrue(ministerDestinations.contains(ParishBottomDestination.VOLUNTEER_SCHEDULE))
    assertTrue(ministerDestinations.contains(ParishBottomDestination.SERVING_SCHEDULE))
    assertFalse(ministerDestinations.contains(ParishBottomDestination.ADMIN))

    val adminDestinations = ParishBottomDestination.destinationsForRole(UserRole.ADMINISTRATOR)
    assertEquals(3, adminDestinations.size)
    assertTrue(adminDestinations.contains(ParishBottomDestination.SERVING_SCHEDULE))
    assertTrue(adminDestinations.contains(ParishBottomDestination.ADMIN))
    assertEquals("Serving Schedule", ParishBottomDestination.SERVING_SCHEDULE.label)

    // Route-level authorization guards
    val memberRoles = setOf(AppRole.MEMBER)
    assertTrue(ParishBottomDestination.isRouteAuthorized(VolunteerScheduleRoute, memberRoles))
    assertFalse(ParishBottomDestination.isRouteAuthorized(ServingScheduleRoute, memberRoles))
    assertFalse(ParishBottomDestination.isRouteAuthorized(AdminConsoleRoute, memberRoles))
    assertFalse(ParishBottomDestination.isRouteAuthorized(MinisterDashboardRoute, memberRoles))

    val ministerRoles = setOf(AppRole.MEMBER, AppRole.MINISTER, AppRole.RECTOR)
    assertTrue(ParishBottomDestination.isRouteAuthorized(VolunteerScheduleRoute, ministerRoles))
    assertTrue(ParishBottomDestination.isRouteAuthorized(ServingScheduleRoute, ministerRoles))
    assertFalse(ParishBottomDestination.isRouteAuthorized(AdminConsoleRoute, ministerRoles))
    assertFalse(ParishBottomDestination.isRouteAuthorized(MinisterDashboardRoute, ministerRoles))

    val adminRoles = setOf(AppRole.MEMBER, AppRole.ADMIN)
    assertTrue(ParishBottomDestination.isRouteAuthorized(VolunteerScheduleRoute, adminRoles))
    assertTrue(ParishBottomDestination.isRouteAuthorized(ServingScheduleRoute, adminRoles))
    assertTrue(ParishBottomDestination.isRouteAuthorized(AdminConsoleRoute, adminRoles))
    assertTrue(ParishBottomDestination.isRouteAuthorized(MinisterDashboardRoute, adminRoles))
  }

  @Test
  fun november2026VolunteerSchedulePdfParser_extractsMainAndBackupCommitments() {
    val testCongregation = Congregation(
      id = "cong_kitchener",
      displayName = "NAC Kitchener Central",
      address = "160 Margaret Ave",
      rectorName = "Pr. J. Grau",
      primaryServiceTimes = "Sun 10:30 AM & Wed 7:30 PM"
    )
    val adamUser = UserProfileEntity(
      id = "usr_adam",
      email = "adam@stjudes.org",
      fullName = "Adam B.",
      congregationId = testCongregation.id,
      role = UserRole.MEMBER,
      delegatedRole = DelegatedRole.VOLUNTEER_SCHEDULER
    )
    val prGrauUser = UserProfileEntity(
      id = "usr_grau",
      email = "grau.marcus.1@gmail.com",
      fullName = "Pr. J. Grau",
      congregationId = testCongregation.id,
      role = UserRole.MINISTER
    )
    val roster = listOf(adamUser, prGrauUser)

    val rawSchedule = PdfScheduleParser.november2026VolunteerScheduleText()
    val result = PdfScheduleParser.parseScheduleText(
      rawText = rawSchedule,
      pdfFileName = "November_2026_Volunteer_Schedule.pdf",
      congregationId = testCongregation.id,
      roster = roster,
      activeUser = adamUser
    )

    assertEquals(
      listOf(
        "Sun 1 Nov 2026",
        "Wed 4 Nov 2026",
        "Sun 8 Nov 2026",
        "Sun 15 Nov 2026",
        "Sun 22 Nov 2026",
        "Sun 29 Nov 2026"
      ),
      result.extractedDates
    )

    // Verify MAIN and BACKUP tiers and all 4 columns (AV, Altar Setup/Takedown, Greeter, Usher)
    val duties = result.assignments.map { it.dutyType }.toSet()
    assertTrue(
      duties.containsAll(
        listOf(
          MinistryDutyType.AV_SOUND_BOOTH,
          MinistryDutyType.ALTAR_SETUP,
          MinistryDutyType.GREETER,
          MinistryDutyType.USHERING
        )
      )
    )
    assertTrue(result.assignments.any { it.assignmentTier == "MAIN" && it.assigneeName == "Adam B." })
    assertTrue(result.assignments.any { it.assignmentTier == "BACKUP" && it.assigneeName == "Pr. J. Grau" })
    assertTrue(result.highlightedLocalMatches >= 6)
    assertTrue(result.formattedEmailTable.contains("MAIN: Adam B."))
  }

  @Test
  fun roomTypeConverters_roundTripAllEnumsCleanly() {
    val converters = ParishTypeConverters()
    UserRole.entries.forEach { role ->
      assertEquals(role, converters.toUserRole(converters.fromUserRole(role)))
    }
    DelegatedRole.entries.forEach { del ->
      assertEquals(del, converters.toDelegatedRole(converters.fromDelegatedRole(del)))
    }
    MinistryDutyType.entries.forEach { duty ->
      assertEquals(duty, converters.toMinistryDutyType(converters.fromMinistryDutyType(duty)))
    }
    val subTasks = listOf(
      VolunteerSubTask(id = "sub_1", label = "Setup", isCompleted = true),
      VolunteerSubTask(id = "sub_2", label = "Takedown", isCompleted = false)
    )
    assertEquals(subTasks, converters.toSubTaskList(converters.fromSubTaskList(subTasks)))
  }

  @Test
  fun notificationFormatting_formatsNaturalListsAndDatesCleanly() {
    assertEquals("AV", ParishNotificationHelper.formatNaturalItemList(listOf("AV"), "AV"))
    assertEquals("Setup and Takedown", ParishNotificationHelper.formatNaturalItemList(listOf("Setup", "Takedown"), "Altar"))
    assertEquals("Item A, Item B, and Item C", ParishNotificationHelper.formatNaturalItemList(listOf("Item A", "Item B", "Item C"), "Custom"))

    assertEquals(
      "You are scheduled as MAIN to take care of Setup and Takedown for service today at 10:30 AM.",
      ParishNotificationHelper.buildVolunteerReminderBody(
        roleTier = "MAIN",
        items = listOf("Setup", "Takedown"),
        fallbackTitle = "Altar Setup/Takedown",
        serviceDate = "Sun 22 Nov 2026",
        serviceTime = "10:30 AM",
        isToday = true
      )
    )
    assertEquals(
      "You are scheduled as BACKUP to take care of AV for service on Sun 29 Nov 2026 at 10:30 AM.",
      ParishNotificationHelper.buildVolunteerReminderBody(
        roleTier = "BACKUP",
        items = listOf("AV"),
        fallbackTitle = "AV",
        serviceDate = "Sun 29 Nov 2026",
        serviceTime = "10:30 AM",
        isToday = false
      )
    )
  }

  @Test
  fun minihqScraper_skipsWhenScheduleAndDsgAreIdentical() {
    val services = MiniHqScrapedData.getStagedDivineServices()
    val scheduleHash = MiniHqScraperService.computeScheduleHash(services)
    val dsgHash = MiniHqScraperService.computeDsgHash(services)

    assertTrue(scheduleHash.isNotBlank())
    assertTrue(dsgHash.isNotBlank())

    // When last hash equals current hash, scraper skips update
    val shouldSkip = MiniHqScraperService.shouldSkipUpdate(
      currentScheduleHash = scheduleHash,
      lastScheduleHash = scheduleHash,
      currentDsgHash = dsgHash,
      lastDsgHash = dsgHash
    )
    assertTrue(shouldSkip)

    // When schedule hash changes, scraper does NOT skip
    val differentScheduleSkip = MiniHqScraperService.shouldSkipUpdate(
      currentScheduleHash = scheduleHash,
      lastScheduleHash = "different_hash_123",
      currentDsgHash = dsgHash,
      lastDsgHash = dsgHash
    )
    assertFalse(differentScheduleSkip)

    // When DSG hash changes, scraper does NOT skip
    val differentDsgSkip = MiniHqScraperService.shouldSkipUpdate(
      currentScheduleHash = scheduleHash,
      lastScheduleHash = scheduleHash,
      currentDsgHash = dsgHash,
      lastDsgHash = "different_dsg_hash_456"
    )
    assertFalse(differentDsgSkip)

    // On very first run (last hash blank), scraper does NOT skip
    val firstRunSkip = MiniHqScraperService.shouldSkipUpdate(
      currentScheduleHash = scheduleHash,
      lastScheduleHash = "",
      currentDsgHash = dsgHash,
      lastDsgHash = ""
    )
    assertFalse(firstRunSkip)
  }

  @Test
  fun minihqScrapeWorker_intervalIsConfiguredTo2Days() {
    assertEquals(2L, MiniHqScrapeWorker.INTERVAL_DAYS)
    assertEquals("com.example.parishsync.MiniHqPeriodicScrape", MiniHqScrapeWorker.UNIQUE_PERIODIC_WORK_NAME)
    assertEquals("MiniHqSync", MiniHqScrapeWorker.WORK_TAG)
  }

  @Test
  fun settingsRoute_isAuthorizedForAllAuthenticatedRoles() {
    assertTrue(ParishBottomDestination.isRouteAuthorized(SettingsRoute, setOf(AppRole.MEMBER)))
    assertTrue(ParishBottomDestination.isRouteAuthorized(SettingsRoute, setOf(AppRole.MINISTER)))
    assertTrue(ParishBottomDestination.isRouteAuthorized(SettingsRoute, setOf(AppRole.VOLUNTEER_SCHEDULER)))
    assertTrue(ParishBottomDestination.isRouteAuthorized(SettingsRoute, setOf(AppRole.ADMIN)))
    assertFalse(ParishBottomDestination.isRouteAuthorized(AdminConsoleRoute, setOf(AppRole.MEMBER)))
  }

  @Test
  fun userProfileEntity_notificationPreferencesDefaultToTrue() {
    val profile = UserProfileEntity(
      id = "u_1",
      email = "member@nac.ca",
      fullName = "Test Member",
      congregationId = "cong_1",
      role = UserRole.MEMBER
    )
    assertTrue(profile.preferPushReminders)
    assertTrue(profile.preferEmailReminders)
  }

  @Test
  fun minihqScraper_discoversAllCanadianDistrictSchedulesAndDsgAssetsForCurrentAndNextMonth() {
    val resources = MiniHqScraperService.discoverCatalogResources()
    val districtSchedules = resources.filter {
      it.resourceCategory == MiniHqScraperService.CATEGORY_DISTRICT_SCHEDULE
    }
    // 9 Canadian districts * 2 target months = 18 district schedule PDFs
    assertEquals(18, districtSchedules.size)
    assertTrue(
      MiniHqScraperService.CANADIAN_DISTRICTS.all { district ->
        districtSchedules.any { it.district == district && it.monthYear == "November 2026" && !it.isNextMonth } &&
          districtSchedules.any { it.district == district && it.monthYear == "December 2026" && it.isNextMonth }
      }
    )

    val dsgAssets = resources.filter {
      it.resourceCategory == MiniHqScraperService.CATEGORY_DSG_ASSET
    }
    val expectedTypes = setOf(
      MiniHqScraperService.ASSET_EN_DSG,
      MiniHqScraperService.ASSET_FR_DSG,
      MiniHqScraperService.ASSET_AUDIO,
      MiniHqScraperService.ASSET_REFERENCES,
      MiniHqScraperService.ASSET_EN_TRANSCRIPT,
      MiniHqScraperService.ASSET_FR_TRANSCRIPT
    )
    assertTrue(dsgAssets.map { it.assetType }.toSet().containsAll(expectedTypes))
    assertTrue(dsgAssets.any { !it.isNextMonth })
    assertTrue(dsgAssets.any { it.isNextMonth })
  }

  @Test
  fun minihqScraper_rejectsCorruptAndMockFilesAndResolvesFullUntruncatedUrls() {
    val htmlLoginStub = "<html><body>Sign in to MiniHQ</body></html>".repeat(8).toByteArray()
    assertTrue(htmlLoginStub.size in 250..400)
    assertFalse(MiniHqScraperService.isValidBinaryAsset("Hamilton_November_2026_Serving_Schedule.pdf", "text/html", htmlLoginStub))
    assertFalse(MiniHqScraperService.isValidBinaryAsset("Hamilton_November_2026_Serving_Schedule.pdf", "application/pdf", htmlLoginStub))
    assertFalse(MiniHqScraperService.isValidBinaryAsset("Sun_Nov_1_2026_Audio.mp3", "audio/mpeg", htmlLoginStub))

    val mockStubPdf = ("%PDF-1.4\nNEW APOSTOLIC CHURCH CANADA - MINI HQ\n" + "0".repeat(6000)).toByteArray()
    assertTrue(MiniHqScraperService.isMockPlaceholderPdf(mockStubPdf))
    assertFalse(MiniHqScraperService.isValidBinaryAsset("Hamilton_November_2026_Serving_Schedule.pdf", "application/pdf", mockStubPdf))

    val genuinePdf = ("%PDF-1.7\n" + "A".repeat(MiniHqScraperService.MIN_PDF_BYTES)).toByteArray()
    assertTrue(MiniHqScraperService.isValidBinaryAsset("Hamilton_November_2026_Serving_Schedule.pdf", "application/pdf", genuinePdf))

    val genuineMp3 = (byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte()) + ByteArray(MiniHqScraperService.MIN_AUDIO_BYTES))
    assertTrue(MiniHqScraperService.isValidBinaryAsset("Sun_Nov_1_2026_Audio.mp3", "audio/mpeg", genuineMp3))

    val html = """
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Hamilton_November_2026_Serving_Schedule.pdf?v=2">Hamilton</a>
    """.trimIndent()
    val links = MiniHqScraperService.extractFullHrefLinks("https://www.naccanada.org/imis_prod/MiniHQ", html)
    assertEquals(
      listOf("https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Hamilton_November_2026_Serving_Schedule.pdf?v=2"),
      links
    )
  }

  @Test
  fun minihqScraper_extractsOnlySchedulesMonthsAndNineDistrictPdfLinks() {
    val landingHtml = """
      <h2>Divine Service Prep</h2>
      <a href="/imis_prod/MiniHQ/DSG/Nov2026.aspx">November 2026</a>
      <h2>Schedules</h2>
      <a href="/imis_prod/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx">November 2026</a>
      <a href="/imis_prod/MiniHQ/Schedules/Serving_Schedules_December_2026.aspx">December 2026</a>
      <h2>Announcements</h2>
      <a href="/imis_prod/MiniHQ/Announcements/Funeral.aspx">Funeral Notice</a>
    """.trimIndent()
    val monthLinks = MiniHqScraperService.extractSchedulesMonthUrls("https://www.naccanada.org/imis_prod/MiniHQ", landingHtml)
    assertEquals(2, monthLinks.size)
    assertEquals("November 2026" to "https://www.naccanada.org/imis_prod/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx", monthLinks[0])
    assertEquals("December 2026" to "https://www.naccanada.org/imis_prod/MiniHQ/Schedules/Serving_Schedules_December_2026.aspx", monthLinks[1])

    val monthHtml = """
      <h3>District Serving Schedules</h3>
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/British%20Columbia.pdf">British Columbia</a>
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Alberta.pdf">Alberta</a>
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Saskatchewan.pdf">Saskatchewan</a>
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Manitoba.pdf">Manitoba</a>
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Northern%20Ontario.pdf">Northern Ontario</a>
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Kitchener.pdf">Kitchener</a>
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Hamilton.pdf">Hamilton</a>
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Toronto.pdf">Toronto</a>
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Eastern%20Canada.pdf">Eastern Canada</a>
      <h3>NACC Calendars 2026</h3>
      <a href="/imis_prod/NACDOCS/Calendars/National.pdf">National</a>
      <h3>Youth Schedules</h3>
      <a href="/imis_prod/NACDOCS/Youth/Kitchener_Youth.pdf">Kitchener District</a>
      <h3>Seniors Schedules</h3>
      <a href="/imis_prod/NACDOCS/Seniors/Hamilton_Seniors.pdf">Hamilton District</a>
    """.trimIndent()
    val districtLinks = MiniHqScraperService.extractDistrictSchedulePdfLinks(
      "https://www.naccanada.org/imis_prod/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx",
      monthHtml
    )
    assertEquals(9, districtLinks.size)
    assertEquals(
      "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/Hamilton.pdf",
      districtLinks["Hamilton"]
    )
    assertFalse(districtLinks.values.any { it.contains("Youth") || it.contains("Seniors") || it.contains("Calendars") })

    val dsgMonthLinks = MiniHqScraperService.extractDsgMonthUrls("https://www.naccanada.org/imis_prod/MiniHQ", landingHtml)
    assertEquals(1, dsgMonthLinks.size)
    assertEquals("November 2026" to "https://www.naccanada.org/imis_prod/MiniHQ/DSG/Nov2026.aspx", dsgMonthLinks[0])

    val dsgCatalog = MiniHqScraperService.discoverCatalogResources().filter {
      it.resourceCategory == MiniHqScraperService.CATEGORY_DSG_ASSET
    }
    assertTrue(dsgCatalog.isNotEmpty())
    assertTrue(dsgCatalog.all { it.contentText.isNotBlank() })

    assertEquals(
      "2026-11-hamilton-serving-schedule-rev1.pdf",
      MiniHqScraperService.extractFileNameFromUrl(
        "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/2026-11-hamilton-serving-schedule-rev1.pdf?v=1",
        "fallback.pdf"
      )
    )
  }

  @Test
  fun minihqScraper_detectsUnauthenticatedImisPageEvenWithSchedulesInNavAndHandlesNavDuplicates() {
    val unauthHtmlWithNavSchedules = """
      <html>
        <body>
          <nav><a href="/login">Sign In</a><a href="/resources">Schedules</a></nav>
          <form><input type="password" name="pw" /></form>
        </body>
      </html>
    """.trimIndent()
    assertTrue(MiniHqScraperService.isLandingPageAuthRequired(unauthHtmlWithNavSchedules))

    val authHtmlWithNavSchedules = """
      <html>
        <body>
          <nav><a href="/signout">Sign Out</a><span>Schedules</span></nav>
          <h2>Divine Service Prep</h2>
          <a href="/imis_prod/MiniHQ/DSG/Nov2026.aspx">November 2026</a>
          <h2>Schedules</h2>
          <a href="/imis_prod/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx">November&nbsp;2026</a>
          <a href="/imis_prod/MiniHQ/Schedules/Serving_Schedules_December_2026.aspx">December
            2026</a>
          <h2>Announcements</h2>
        </body>
      </html>
    """.trimIndent()
    val months = MiniHqScraperService.extractSchedulesMonthUrls(
      "https://www.naccanada.org/imis_prod/MiniHQ",
      authHtmlWithNavSchedules
    )
    assertEquals(2, months.size)
    assertEquals("November 2026", months[0].first)
    assertEquals("December 2026", months[1].first)
    assertFalse(MiniHqScraperService.isLandingPageAuthRequired(authHtmlWithNavSchedules, months))

    val monthHtmlWithWhitespaceAndRedirect = """
      <h3>District Serving Schedules</h3>
      <a href="/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/2026-11-kitchener-serving-schedule.pdf">Kitchener</a>
      <a href="/imis_prod/Download.aspx?doc=bc">British&nbsp;Columbia</a>
      <h3>Youth Schedules</h3>
      <a href="/imis_prod/NACDOCS/Youth/Kitchener_Youth.pdf">Kitchener District</a>
    """.trimIndent()
    val districtLinks = MiniHqScraperService.extractDistrictSchedulePdfLinks(
      "https://www.naccanada.org/imis_prod/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx",
      monthHtmlWithWhitespaceAndRedirect
    )
    assertEquals(2, districtLinks.size)
    assertEquals(
      "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Serving%20Schedules/November%202026/2026-11-kitchener-serving-schedule.pdf",
      districtLinks["Kitchener"]
    )
    assertEquals(
      "https://www.naccanada.org/imis_prod/Download.aspx?doc=bc",
      districtLinks["British Columbia"]
    )
  }

  @Test
  fun minihqDsgScraper_extractsDatedAccordionsAndSpecialUndatedItemsWithCanonicalFilenames() {
    val dsgPageUrl = "https://www.naccanada.org/imis_prod/MiniHQ/Divine_Service_Prep_November_2026.aspx"
    val dsgHtml = """
      <h1>DIVINE SERVICE PREP</h1>
      <div>Confirmation DSG <a href="/imis_prod/NACDOCS/Conf-EN.pdf">EN</a> | <a href="/imis_prod/NACDOCS/Conf-FR.pdf">FR</a></div>
      <div><h3>Foreword</h3><div><a href="/imis_prod/NACDOCS/Foreword-EN.pdf">English</a></div></div>
      <div><h3>Sunday, November 1, 2026 — Divine service for the departed</h3>
        <div>
          <a href="/imis_prod/NACDOCS/Nov2026/2026-11-01-dsg-en.pdf">EN</a>
          <a href="/imis_prod/NACDOCS/Nov2026/2026-11-01-dsg-fr.pdf">FR</a>
          <a href="/imis_prod/NACDOCS/Nov2026/2026-11-01-audio.mp3">AUDIO</a>
          <a href="/imis_prod/NACDOCS/Nov2026/2026-11-01-ref.pdf">REFERENCES</a>
          <a href="/imis_prod/NACDOCS/Nov2026/2026-11-01-transcript-en.pdf">English Transcript</a>
          <div>Bible Reading: <a href="/imis_prod/NACDOCS/Nov2026/2026-11-01-bible-reading-en.pdf">EN</a> | <a href="/imis_prod/NACDOCS/Nov2026/2026-11-01-bible-reading-fr.pdf">FR</a></div>
        </div>
      </div>
      <div><h3>Wednesday, November 25, 2026</h3>
        <div>
          <a href="/imis_prod/NACDOCS/Nov2026/2026-11-25-dsg-en.pdf">EN</a>
          <a href="/imis_prod/NACDOCS/Nov2026/2026-11-25-dsg-fr.pdf">FR</a>
          <a href="/imis_prod/NACDOCS/Nov2026/2026-11-25-ref.pdf">REFERENCES</a>
        </div>
      </div>
      <div><h3><span class="ui-icon"></span>&#9656; Youth Service</h3>
        <div>
          <a href="/imis_prod/NACDOCS/Nov2026/youth-dsg-en.pdf">EN</a>
          <a href="/imis_prod/NACDOCS/Nov2026/youth-dsg-fr.pdf">FR</a>
          <a href="/imis_prod/NACDOCS/Nov2026/youth-audio.mp3">AUDIO</a>
          <a href="/imis_prod/NACDOCS/Nov2026/youth-ref.pdf">REFERENCES</a>
          <div>Bible Reading: <a href="/imis_prod/NACDOCS/Nov2026/youth-bible-en.pdf">EN</a> | <a href="/imis_prod/NACDOCS/Nov2026/youth-bible-fr.pdf">FR</a></div>
        </div>
      </div>
      <div><h3>Full DSG</h3>
        <div>
          <a href="/imis_prod/NACDOCS/Nov2026/full-de.pdf">November 2026 - German</a>
          <a href="/imis_prod/NACDOCS/Nov2026/full-en.pdf">November 2026 - English</a>
          <a href="/imis_prod/NACDOCS/Nov2026/full-es.pdf">November 2026 - Spanish</a>
          <a href="/imis_prod/NACDOCS/Nov2026/full-fr.pdf">November 2026 - French</a>
          <a href="/imis_prod/NACDOCS/Nov2026/full-bible-ref.pdf">November 2026 - Bible References</a>
        </div>
      </div>
      <div><h3>Special Edition DSG</h3>
        <div>
          <a href="/imis_prod/NACDOCS/Special/feb-2026-en.pdf">February 2026 - English</a>
          <a href="/imis_prod/NACDOCS/Special/feb-2026-fr.pdf">February 2026 - French</a>
          <a href="/imis_prod/NACDOCS/Special/aug-2026-es.pdf">August 2026 - Spanish</a>
        </div>
      </div>
    """.trimIndent()

    val extracted = MiniHqScraperService.extractDsgResourcesFromHtml(dsgPageUrl, "November 2026", dsgHtml)
    val fileNames = extracted.map { it.fileName }.toSet()

    assertTrue(fileNames.contains("DSG_EN_Sunday_Nov_01_2026.pdf"))
    assertTrue(fileNames.contains("DSG_FR_Sunday_Nov_01_2026.pdf"))
    assertTrue(fileNames.contains("Audio_Sunday_Nov_01_2026.mp3"))
    assertTrue(fileNames.contains("References_Sunday_Nov_01_2026.pdf"))
    assertTrue(fileNames.contains("Transcript_Sunday_Nov_01_2026_EN.pdf"))
    assertTrue(fileNames.contains("BibleReading_Sunday_Nov_01_2026_EN.pdf"))
    assertTrue(fileNames.contains("BibleReading_Sunday_Nov_01_2026_FR.pdf"))
    assertTrue(fileNames.contains("DSG_EN_Wednesday_Nov_25_2026.pdf"))
    assertTrue(fileNames.contains("SpecialEdition_DSG_Feb_2026_EN.pdf"))
    assertTrue(fileNames.contains("SpecialEdition_DSG_Feb_2026_FR.pdf"))
    assertTrue(fileNames.contains("SpecialEdition_DSG_Aug_2026_ES.pdf"))

    val wedNov25Assets = extracted.filter { it.serviceDate == "Wed Nov 25, 2026" }.map { it.assetType }.toSet()
    assertEquals(
      setOf(
        MiniHqScraperService.ASSET_EN_DSG,
        MiniHqScraperService.ASSET_FR_DSG,
        MiniHqScraperService.ASSET_REFERENCES
      ),
      wedNov25Assets
    )

    val youthAssets = extracted.filter { it.serviceTitle == "Youth Service" }.map { it.assetType }.toSet()
    assertTrue(youthAssets.contains(MiniHqScraperService.ASSET_EN_DSG))
    assertTrue(youthAssets.contains(MiniHqScraperService.ASSET_FR_DSG))
    assertTrue(youthAssets.contains(MiniHqScraperService.ASSET_AUDIO))
    assertTrue(youthAssets.contains(MiniHqScraperService.ASSET_REFERENCES))
    assertTrue(youthAssets.contains(MiniHqScraperService.ASSET_EN_BIBLE_READING))
    assertTrue(youthAssets.contains(MiniHqScraperService.ASSET_FR_BIBLE_READING))

    val fullDsgChips = extracted.filter { it.serviceTitle == "Full DSG" }.map { it.assetType }.toSet()
    assertEquals(setOf("German", "English", "Spanish", "French", "Bible References"), fullDsgChips)

    val specialFebEn = extracted.first { it.fileName == "SpecialEdition_DSG_Feb_2026_EN.pdf" }
    assertEquals("", specialFebEn.serviceDate)
    assertEquals("Special Edition DSG", specialFebEn.serviceTitle)
    assertEquals("English (Feb)", specialFebEn.assetType)

    assertEquals(
      java.time.LocalDate.of(2026, 11, 25),
      MiniHqScraperService.parseServiceDateToLocalDate("Wed Nov 25, 2026")
    )
    assertEquals(
      java.time.LocalDate.of(2026, 11, 1),
      MiniHqScraperService.parseServiceDateToLocalDate("Sun Nov 1, 2026")
    )
  }

  @Test
  fun minihqScraper_normalizesHttpAndDeduplicatesRepeatedMiniHqPathAndInfersMonthLabels() {
    val resolved =
      MiniHqScraperService.resolveFullHrefUrl(
        "http://www.naccanada.org/IMIS_PROD/MiniHQ/Resources/Schedules/",
        "/IMIS_PROD/MiniHQ/Resources/Schedules/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx?hkey=abc"
      )
    assertEquals(
      "https://www.naccanada.org/imis_prod/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx?hkey=abc",
      resolved
    )

    val landingHtml =
      """
      <div>
        <h2>Schedules</h2>
        <a href="/IMIS_PROD/MiniHQ/Resources/Schedules/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx?hkey=abc">Schedules</a>
        <a href="Serving_Schedules_December_2026.aspx?hkey=def">December 2026</a>
      </div>
      """.trimIndent()
    val months =
      MiniHqScraperService.extractSchedulesMonthUrls(
        "http://www.naccanada.org/IMIS_PROD/MiniHQ/Resources/Schedules/",
        landingHtml
      )
    assertEquals(2, months.size)
    assertEquals("November 2026", months[0].first)
    assertEquals(
      "https://www.naccanada.org/imis_prod/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx?hkey=abc",
      months[0].second
    )
    assertEquals("December 2026", months[1].first)
  }

  @Test
  fun minihqScraper_requiresAuthWhenSignInOrPasswordPresentDespiteNavScheduleLinks_andRespectsStaleSyncLock() {
    val unauthWithNavLinks =
      """
      <html>
        <body>
          <nav>
            <a href="/imis_prod/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx?hkey=abc">Schedules</a>
          </nav>
          <a href="/imis_prod/Sign_In.aspx">Sign In</a>
        </body>
      </html>
      """.trimIndent()
    val navMonths = MiniHqScraperService.extractSchedulesMonthUrls(MiniHqScraperService.MINIHQ_URL, unauthWithNavLinks)
    assertTrue(navMonths.isNotEmpty())
    assertTrue(MiniHqScraperService.isLandingPageAuthRequired(unauthWithNavLinks, navMonths))

    val authWithSignOut =
      """
      <html>
        <body>
          <a href="/SignOut">Sign Out</a>
          <h2>Schedules</h2>
          <a href="/imis_prod/MiniHQ/Schedules/Serving_Schedules_November_2026.aspx?hkey=abc">November 2026</a>
        </body>
      </html>
      """.trimIndent()
    val authMonths = MiniHqScraperService.extractSchedulesMonthUrls(MiniHqScraperService.MINIHQ_URL, authWithSignOut)
    assertFalse(MiniHqScraperService.isLandingPageAuthRequired(authWithSignOut, authMonths))

    val start = 1_000_000L
    assertTrue(MiniHqScraperService.isSyncLockActive(isSyncing = true, startedAtMs = start, nowMs = start + 30_000L, forceRefresh = false))
    assertFalse(MiniHqScraperService.isSyncLockActive(isSyncing = true, startedAtMs = start, nowMs = start + 125_000L, forceRefresh = false))
    assertFalse(MiniHqScraperService.isSyncLockActive(isSyncing = true, startedAtMs = start, nowMs = start + 10_000L, forceRefresh = true))
  }

  @Test
  fun defaultCanadianCongregations_containsAll75CongregationsAcross9Districts() {
    val districts = com.example.parishsync.data.local.ParishSeedData.defaultCanadianDistricts()
    val congregations = com.example.parishsync.data.local.ParishSeedData.defaultCanadianCongregations()

    assertEquals(9, districts.size)
    assertEquals(75, congregations.size)
    assertEquals(75, congregations.map { it.id }.toSet().size)

    val districtIds = districts.map { it.id }.toSet()
    assertTrue(congregations.all { it.districtId in districtIds })

    val countsByDistrict = congregations.groupingBy { it.districtId }.eachCount()
    assertEquals(8, countsByDistrict["dist_bc"])
    assertEquals(8, countsByDistrict["dist_ab"])
    assertEquals(6, countsByDistrict["dist_sk"])
    assertEquals(3, countsByDistrict["dist_mb"])
    assertEquals(7, countsByDistrict["dist_no"])
    assertEquals(14, countsByDistrict["dist_kitchener"])
    assertEquals(7, countsByDistrict["dist_toronto"])
    assertEquals(5, countsByDistrict["dist_hamilton"])
    assertEquals(17, countsByDistrict["dist_eastern"])

    val woodstock = congregations.first { it.id == com.example.parishsync.data.local.ParishSeedData.WOODSTOCK_CONGREGATION_ID }
    assertEquals("Woodstock", woodstock.displayName)
    assertEquals("45 Cronyn St, N4S 3T4 Woodstock", woodstock.address)
    assertEquals("dist_kitchener", woodstock.districtId)
  }
}
