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
      address = "45 Cronyn St, N4S 3T4 Woodstock",
      rectorName = BOOTSTRAP_ERIC_NAME,
      primaryServiceTimes = "Sun 10:30 AM & Wed 7:30 PM",
      sundayServiceTime = "10:30 AM",
      wednesdayServiceTime = "7:30 PM"
    )

  private fun seedCongregation(
    id: String,
    districtId: String,
    displayName: String,
    address: String = ""
  ): CongregationEntity = CongregationEntity(
    id = id,
    districtId = districtId,
    displayName = displayName,
    address = address,
    primaryServiceTimes = "Sun 10:30 AM & Wed 7:30 PM",
    sundayServiceTime = "10:30 AM",
    wednesdayServiceTime = "7:30 PM"
  )

  /**
   * All 75 NAC Canada congregations across 9 districts (updated Nov 23, 2026).
   */
  fun defaultCanadianCongregations(): List<CongregationEntity> = listOf(
    // British Columbia (8)
    seedCongregation("cong_coquitlam", "dist_bc", "Coquitlam", "813 Sharpe St, V3C 3M4 Coquitlam"),
    seedCongregation("cong_langley", "dist_bc", "Langley", "19999 53rd Ave, V3A 6V2 Langley"),
    seedCongregation("cong_nanaimo", "dist_bc", "Nanaimo", "600 Wentworth St, V9R 3E5 Nanaimo"),
    seedCongregation("cong_fraser_valley", "dist_bc", "Fraser Valley", "6665 Beaufort Rd, V2R 2C5 Chilliwack"),
    seedCongregation("cong_victoria_bc", "dist_bc", "Victoria BC", "3382 Harriet Rd, V8Z 3S4 Victoria"),
    seedCongregation("cong_kamloops", "dist_bc", "Kamloops", "2085 Parkcrest Ave, V2B 4X2 Kamloops"),
    seedCongregation("cong_kelowna", "dist_bc", "Kelowna", "155 Nickel Rd, V1X 4E5 Kelowna"),
    seedCongregation("cong_100_mile_house", "dist_bc", "100 Mile House", "530 Horse Lake Rd Suite 6B, V0K 2E0 100 Mile House"),

    // Alberta (8)
    seedCongregation("cong_castlegar_bc", "dist_ab", "Castlegar, BC", "200 Lakeview St, V1N 1B8 Castlegar"),
    seedCongregation("cong_cranbrook_bc", "dist_ab", "Cranbrook, BC", "821 1st St S, V1C 6E9 Cranbrook"),
    seedCongregation("cong_calgary", "dist_ab", "Calgary", "7440 Old Banff Coach Rd SW, T3H 4C2 Calgary"),
    seedCongregation("cong_chestermere", "dist_ab", "Chestermere", "133 Rainbow Falls Dr, T1X 0A7 Chestermere"),
    seedCongregation("cong_red_deer", "dist_ab", "Red Deer", "1 Gilbert Cr, T4P 3L3 Red Deer"),
    seedCongregation("cong_edmonton", "dist_ab", "Edmonton", "8735 Connors Rd NW, T6C 4B4 Edmonton"),
    seedCongregation("cong_lethbridge", "dist_ab", "Lethbridge", "115 Mount Blakiston Rd W, T1K 6L3 Lethbridge"),
    seedCongregation("cong_medicine_hat", "dist_ab", "Medicine Hat", "701 1st St SW, T1A 3Z5 Medicine Hat"),

    // Saskatchewan (6)
    seedCongregation("cong_weyburn", "dist_sk", "Weyburn", "606 1st Ave NW, S4H 1P2 Weyburn"),
    seedCongregation("cong_regina", "dist_sk", "Regina", "3805 Green Falls Dr, S4V 3H5 Regina"),
    seedCongregation("cong_moose_jaw", "dist_sk", "Moose Jaw", "1130 Simcoe St, S6H 3J4 Moose Jaw"),
    seedCongregation("cong_saskatoon", "dist_sk", "Saskatoon", "325 Camponi Pl, S7M 1E9 Saskatoon"),
    seedCongregation("cong_prince_albert", "dist_sk", "Prince Albert", "125 12th St E, S6V 1B7 Prince Albert"),
    seedCongregation("cong_swift_current", "dist_sk", "Swift Current", ""),

    // Manitoba (3)
    seedCongregation("cong_winnipeg", "dist_mb", "Winnipeg", "1755 Taylor Ave, R3N 1Y2 Winnipeg"),
    seedCongregation("cong_brandon", "dist_mb", "Brandon", "256 10th Street, R7A 4E8 Brandon"),
    seedCongregation("cong_kenora_on", "dist_mb", "Kenora, ON", "51 Cambrian Dr, P9N 4A4 Kenora"),

    // Northern Ontario (7)
    seedCongregation("cong_thunder_bay", "dist_no", "Thunder Bay", "639 Van Norman St, P7A 4G4 Thunder Bay"),
    seedCongregation("cong_sault_ste_marie", "dist_no", "Sault Ste Marie", "135 Arden St, P6C 5K6 Sault Ste. Marie"),
    seedCongregation("cong_sudbury", "dist_no", "Sudbury", "522 Barrydowne Rd, P3A 3T3 Sudbury"),
    seedCongregation("cong_north_bay", "dist_no", "North Bay", "46 Joseph St, P1A 1L6 North Bay"),
    seedCongregation("cong_huntsville", "dist_no", "Huntsville", ""),
    seedCongregation("cong_marquette_usa", "dist_no", "Marquette USA", ""),
    seedCongregation("cong_duluth_usa", "dist_no", "Duluth USA", ""),

    // Kitchener (14)
    seedCongregation("cong_fergus", "dist_kitchener", "Fergus", "792 Wellington Rd 18, N1M 3A8 Fergus"),
    seedCongregation("cong_guelph", "dist_kitchener", "Guelph", "245 Delhi St, N1E 4K5 Guelph"),
    seedCongregation("cong_kitchener_east", "dist_kitchener", "Kitchener East", "180 Lorraine Ave, N2B 3R3 Kitchener"),
    seedCongregation("cong_kitchener_spanish", "dist_kitchener", "Kitchener Spanish", "180 Lorraine Ave, N2B 3R3 Kitchener"),
    seedCongregation("cong_margaret_ave", "dist_kitchener", "Margaret Ave", "160 Margaret Ave, N2H 4J2 Kitchener"),
    seedCongregation("cong_cambridge", "dist_kitchener", "Cambridge", "10 Birkinshaw Road, N1P 0A5 Cambridge"),
    seedCongregation("cong_new_hamburg", "dist_kitchener", "New Hamburg", "416 Waterloo St, N3A 1T5 New Hamburg"),
    woodstockCongregation(),
    seedCongregation("cong_hanover", "dist_kitchener", "Hanover", "517 12th St, N4N 1W1 Hanover"),
    seedCongregation("cong_owen_sound", "dist_kitchener", "Owen Sound", "1620 9th Ave E, N4K 6S2 Owen Sound"),
    seedCongregation("cong_london_ontario", "dist_kitchener", "London Ontario", "795 Baransway Drive, N5V 0A2 London"),
    seedCongregation("cong_windsor", "dist_kitchener", "Windsor", "5265 Wyandotte St E, N8S 1L5 Windsor"),
    seedCongregation("cong_sarnia", "dist_kitchener", "Sarnia", "116 Front St S, N7T 2M4 Sarnia"),
    seedCongregation("cong_stratford", "dist_kitchener", "Stratford", "350 St Vincent St S, N5A 1E7 Stratford"),

    // Toronto (7)
    seedCongregation("cong_toronto_central", "dist_toronto", "Toronto Central", "407 Dupont St, M5R 1W3 Toronto"),
    seedCongregation("cong_bellamy", "dist_toronto", "Bellamy", "930 Bellamy Road North, M1H 1G9 Scarborough"),
    seedCongregation("cong_brampton", "dist_toronto", "Brampton", "45 Eastbourne Dr, L6T 4E9 Brampton"),
    seedCongregation("cong_brampton_french", "dist_toronto", "Brampton French", "45 Eastbourne Dr, L6T 4E9 Brampton"),
    seedCongregation("cong_newmarket", "dist_toronto", "Newmarket", "1060 Jacarandah Dr, L3Y 3G8 Newmarket"),
    seedCongregation("cong_barrie", "dist_toronto", "Barrie", "110 Letitia St, L4N 1P5 Barrie"),
    seedCongregation("cong_orillia", "dist_toronto", "Orillia", "56 Wyandotte St, L3V 5M9 Orillia"),

    // Hamilton (5)
    seedCongregation("cong_halton", "dist_hamilton", "Halton", "4209 Palladium Way, L7M 0V9 Burlington"),
    seedCongregation("cong_hamilton_ontario", "dist_hamilton", "Hamilton Ontario", "922 Upper Sherman Ave, L8V 3N3 Hamilton"),
    seedCongregation("cong_niagara", "dist_hamilton", "Niagara", "3400 Schmon Pky, L2V 4Y6 Thorold"),
    seedCongregation("cong_st_catharines_spanish", "dist_hamilton", "St Catharines Spanish", "3400 Schmon Pky, L2V 4Y6 Thorold"),
    seedCongregation("cong_simcoe", "dist_hamilton", "Simcoe", ""),

    // Eastern Canada (17)
    seedCongregation("cong_oshawa", "dist_eastern", "Oshawa", "601 Laval St, L1J 6L8 Oshawa"),
    seedCongregation("cong_lindsay", "dist_eastern", "Lindsay", "45 Durham St W, K9V 2P7 Lindsay"),
    seedCongregation("cong_belleville", "dist_eastern", "Belleville", "495 Victoria Ave, K8N 2G4 Belleville"),
    seedCongregation("cong_kingston", "dist_eastern", "Kingston", "820 Rowantree Cr, K7P 1P5 Kingston"),
    seedCongregation("cong_ottawa_west", "dist_eastern", "Ottawa West", "2680 Priscilla Street, K2B 7E2 Ottawa"),
    seedCongregation("cong_ottawa_east", "dist_eastern", "Ottawa East", "250 Somerset Street East, K1N 6V6 Ottawa"),
    seedCongregation("cong_montreal_laval", "dist_eastern", "Montreal-Laval", "4375 Boul Samson, H7W 2G8 Laval"),
    seedCongregation("cong_st_leonard", "dist_eastern", "St. Léonard", "8970 Boul Lacordaire, H1R 2B3 Saint-Léonard"),
    seedCongregation("cong_quebec_city", "dist_eastern", "Quebec City", "5350 3e Av O, G1H 7B9 Québec"),
    seedCongregation("cong_sherbrooke", "dist_eastern", "Sherbrooke", "366 Rue Saint-Michel, J1E 2K7 Sherbrooke"),
    seedCongregation("cong_trois_rivieres", "dist_eastern", "Trois-Rivières", "2900 Mgr. St-Arnaud, G9A 5L2 Trois-Rivières"),
    seedCongregation("cong_halifax", "dist_eastern", "Halifax", "151 Joffre Street, B2Y 3C7 Dartmouth"),
    seedCongregation("cong_moncton", "dist_eastern", "Moncton", "99 Wynwood Drive, E1A 2M4 Moncton"),
    seedCongregation("cong_charlottetown", "dist_eastern", "Charlottetown", "17 St Pius X Ave, C1A 6A2 Charlottetown"),
    seedCongregation("cong_denbigh", "dist_eastern", "Denbigh", ""),
    seedCongregation("cong_st_hyacinthe", "dist_eastern", "St-Hyacinthe", ""),
    seedCongregation("cong_saint_john", "dist_eastern", "Saint John", "")
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
