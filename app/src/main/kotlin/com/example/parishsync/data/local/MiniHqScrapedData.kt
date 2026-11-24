package com.example.parishsync.data.local

import kotlinx.serialization.Serializable

@Serializable
data class MiniHqStagedService(
  val id: String,
  val monthYear: String, // e.g. "November 2026", "December 2026"
  val district: String, // e.g. "Hamilton District", "Kitchener District", "Toronto District"
  val congregation: String, // e.g. "Niagara", "Hamilton", "Simcoe", "Margaret Ave", "Cambridge"
  val serviceDate: String,
  val serviceTime: String,
  val serviceTitle: String,
  val theme: String,
  val bibleWord: String,
  val officiantMinister: String,
  val officiantRank: String,
  val assistingMinisters: String,
  val choirConductor: String = "",
  val organist: String = "",
  val specialServiceTag: String = "",
  val location: String = "Sanctuary",
  val notes: String = "",
  val dsgPdfLink: String = "",
  val audioLink: String = ""
)

@Serializable
data class MiniHqAnnouncement(
  val id: String,
  val title: String,
  val date: String,
  val category: String, // "Funeral Notice", "Serving Schedules", "Divine Service Prep"
  val summary: String,
  val content: String,
  val sourceUrl: String = ""
)

object MiniHqScrapedData {

  fun getMiniHqAnnouncements(): List<MiniHqAnnouncement> = listOf(
    MiniHqAnnouncement(
      id = "ann_fh_terry_clarke",
      title = "Funeral Notice - Priest (ir) Terry Clarke",
      date = "November 19, 2026",
      category = "Funeral Notice",
      summary = "Priest in retirement Terry Clarke has passed away.",
      content = "Priest (ir) Terry Clarke of the New Apostolic Church has entered into eternity. Funeral and memorial service arrangements have been communicated to district ministry. Please keep the family in prayer.",
      sourceUrl = "https://www.naccanada.org/imis_prod/MiniHQ/Announcements/Funeral-Notice-Priest-ir-Terry-Clarke.aspx"
    ),
    MiniHqAnnouncement(
      id = "ann_sched_dec_2026",
      title = "December 2026 Serving Schedules - Announcement #9",
      date = "November 19, 2026",
      category = "Serving Schedules",
      summary = "December 2026 Serving Schedules now posted for all Canadian districts.",
      content = "Serving schedules for December 2026 are now available:\n• Nov 19/26: Toronto now available\n• Nov 19/26: British Columbia now available\n• Nov 16/26: Northern Ontario & Alberta now available\n• Nov 13/26: Hamilton now available\n• Nov 12/26: Manitoba now available\n• Nov 5/26: Kitchener now available\n• Nov 3/26: Revised Saskatchewan posted",
      sourceUrl = "https://www.naccanada.org/imis_prod/MiniHQ/Announcements/December_2026_Serving_Schedules.aspx"
    ),
    MiniHqAnnouncement(
      id = "ann_sched_nov_2026",
      title = "November 2026 Serving Schedules - Announcement #17",
      date = "November 13, 2026",
      category = "Serving Schedules",
      summary = "November 2026 Serving Schedules updates and revisions log.",
      content = "Updates & Revisions:\n• Nov 13/26: Revised Hamilton posted\n• Nov 4 & Nov 2/26: Revised Kitchener posted\n• Oct 27/26: Revised Saskatchewan posted\n• Oct 26/26: Eastern Canada available\n• Oct 22/26: British Columbia available\n• Oct 20/26: Hamilton available\n• Oct 16/26: Alberta & Toronto available",
      sourceUrl = "https://www.naccanada.org/imis_prod/MiniHQ/Announcements/November_2026_Serving_Schedules.aspx"
    ),
    MiniHqAnnouncement(
      id = "ann_dsg_dec_2026",
      title = "December 2026 Divine Service Prep - Announcement #1",
      date = "November 2, 2026",
      category = "Divine Service Prep",
      summary = "December 2026 Divine Service Prep resources now available on MiniHQ.",
      content = "The December 2026 Divine Service Prep resources are now posted, including Advent, Christmas Day, and End of Year Divine Service guides (DSG), audio recordings, and full Bible reference guides.",
      sourceUrl = "https://www.naccanada.org/imis_prod/MiniHQ/Announcements/December_2026_Divine_Service_Prep.aspx"
    ),
    MiniHqAnnouncement(
      id = "ann_fh_gerald_smith",
      title = "Funeral Notice - Gerald Smith",
      date = "November 2, 2026",
      category = "Funeral Notice",
      summary = "Gerald Smith of the Niagara congregation has passed away. Officiant: Priest M. Grau.",
      content = "Gerald Smith of the Niagara congregation has passed away.\n\n• Visitation: Saturday, November 21, 2026 at 11:00 a.m. at New Apostolic Church, 4209 Palladium Way, Burlington, ON.\n• Funeral: Saturday, November 21, 2026 at 12:00 p.m. at New Apostolic Church, 4209 Palladium Way, Burlington, ON.\n• Officiant: Priest M. Grau\n• Choir Details: Choir seated by 11:40 a.m.\n• Reception: Follows service with finger foods and desserts welcome.",
      sourceUrl = "https://www.naccanada.org/imis_prod/MiniHQ/Announcements/Funeral_Notice_Gerald_Smith.aspx"
    ),
    MiniHqAnnouncement(
      id = "ann_dsg_nov_2026",
      title = "November 2026 Divine Service Prep - Announcement #4",
      date = "October 7, 2026",
      category = "Divine Service Prep",
      summary = "November 2026 Divine Service Prep guides, transcripts, and recordings.",
      content = "• Oct 7/26: Spanish DSG (digital version) now posted\n• Oct 1/26: English & French transcripts posted for Nov 1, Nov 4, and Nov 18\n• Oct 1/26: Divine Service Recording (Apostle J. Sturm — Saskatoon) now posted\n• Oct 1/26: November 2026 Divine Service Prep resources available",
      sourceUrl = "https://www.naccanada.org/imis_prod/MiniHQ/Announcements/November_2026_Divine_Service_Prep.aspx"
    )
  )

  fun getStagedDivineServices(): List<MiniHqStagedService> = getStagedServices()

  fun getStagedServices(): List<MiniHqStagedService> = listOf(
    // ==========================================
    // NOVEMBER 2026 (Current Month)
    // ==========================================
    MiniHqStagedService(
      id = "mhq_nov_01_niagara",
      monthYear = "November 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Sun Nov 1, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Divine Service for the Departed",
      theme = "God delivers from great distress",
      bibleWord = "Psalm 130: 7 — 'For with the Lord there is mercy, and with Him is abundant redemption.'",
      officiantMinister = "Pr. M. Grau",
      officiantRank = "Priest",
      assistingMinisters = "Dc. L. Hickey",
      specialServiceTag = "Departed • Holy Communion",
      location = "Niagara Sanctuary",
      notes = "Daylight Saving Time ends. Divine service for the departed.",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/November2026/English/2026-11-01-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_nov_01_hamilton",
      monthYear = "November 2026",
      district = "Hamilton District",
      congregation = "Hamilton",
      serviceDate = "Sun Nov 1, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Divine Service for the Departed",
      theme = "God delivers from great distress",
      bibleWord = "Psalm 130: 7 — 'For with the Lord there is mercy, and with Him is abundant redemption.'",
      officiantMinister = "Pr. N. Deppner",
      officiantRank = "Priest / Rector",
      assistingMinisters = "Pr. S. Tith",
      specialServiceTag = "Departed",
      location = "Hamilton Sanctuary",
      notes = "Daylight Saving Time ends."
    ),
    MiniHqStagedService(
      id = "mhq_nov_04_niagara",
      monthYear = "November 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Wed Nov 4, 2026",
      serviceTime = "7:30 PM",
      serviceTitle = "Midweek Divine Service",
      theme = "Focus on Christ",
      bibleWord = "Colossians 3: 12 — 'Therefore, as the elect of God, holy and beloved, put on tender mercies, kindness, humility, meekness, longsuffering.'",
      officiantMinister = "Pr. R. Connor",
      officiantRank = "Priest",
      assistingMinisters = "Dc. H. Sohail",
      location = "Niagara Sanctuary",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/November2026/English/2026-11-04-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_nov_08_niagara",
      monthYear = "November 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Sun Nov 8, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Sunday Divine Service",
      theme = "Our citizenship is in heaven",
      bibleWord = "Philippians 3: 20–21 — 'For our citizenship is in heaven, from which we also eagerly wait for the Savior, the Lord Jesus Christ.'",
      officiantMinister = "Pr. A. Wagner",
      officiantRank = "Priest",
      assistingMinisters = "Pr. S. Hickey",
      location = "Niagara Sanctuary",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/November2026/English/2026-11-08-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_nov_11_district",
      monthYear = "November 2026",
      district = "Hamilton District",
      congregation = "All Congregations",
      serviceDate = "Wed Nov 11, 2026",
      serviceTime = "7:30 PM",
      serviceTitle = "Midweek Divine Service (eChurch)",
      theme = "Doing good",
      bibleWord = "Galatians 6: 9 — 'And let us not grow weary while doing good, for in due season we shall reap if we do not lose heart.'",
      officiantMinister = "District eChurch Transmission",
      officiantRank = "Ministry",
      assistingMinisters = "District Broadcast",
      specialServiceTag = "eChurch",
      location = "Live Stream Broadcast"
    ),
    MiniHqStagedService(
      id = "mhq_nov_15_simcoe",
      monthYear = "November 2026",
      district = "Hamilton District",
      congregation = "Simcoe",
      serviceDate = "Sun Nov 15, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Sunday Divine Service",
      theme = "God is a righteous judge",
      bibleWord = "Ecclesiastes 12: 14 — 'For God will bring every work into judgement, including every secret thing, whether good or evil.'",
      officiantMinister = "Pr. M. Grau",
      officiantRank = "Priest",
      assistingMinisters = "Pr. B. Fiedler",
      location = "Simcoe Sanctuary",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/November2026/English/2026-11-15-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_nov_15_seniors",
      monthYear = "November 2026",
      district = "Kitchener District",
      congregation = "Margaret Ave",
      serviceDate = "Sun Nov 15, 2026",
      serviceTime = "11:00 AM",
      serviceTitle = "Tri-District Seniors' Divine Service",
      theme = "God is a righteous judge",
      bibleWord = "Ecclesiastes 12: 14",
      officiantMinister = "Pr. C. Lindner",
      officiantRank = "Priest / Rector",
      assistingMinisters = "Pr. M. Lindner",
      specialServiceTag = "Tri-District Seniors",
      location = "Margaret Ave Church (Kitchener, ON)",
      notes = "Tri-district seniors' divine service for Hamilton, Kitchener, and Toronto districts."
    ),
    MiniHqStagedService(
      id = "mhq_nov_21_funeral",
      monthYear = "November 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Sat Nov 21, 2026",
      serviceTime = "12:00 PM",
      serviceTitle = "Funeral Service — Gerald Smith",
      theme = "Eternal Peace in Christ",
      bibleWord = "John 14: 2–3 — 'In My Father's house are many mansions... I go to prepare a place for you.'",
      officiantMinister = "Priest M. Grau",
      officiantRank = "Priest / Officiant",
      assistingMinisters = "Niagara Ministry",
      choirConductor = "Choir seated by 11:40 AM",
      specialServiceTag = "Funeral Memorial",
      location = "4209 Palladium Way, Burlington, ON",
      notes = "Visitation 11:00 AM. Funeral 12:00 PM. Reception to follow."
    ),
    MiniHqStagedService(
      id = "mhq_nov_22_niagara",
      monthYear = "November 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Sun Nov 22, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Sunday Divine Service",
      theme = "Behold, I make all things new!",
      bibleWord = "Revelation 21: 5 — 'Then He who sat on the throne said, \"Behold, I make all things new.\" And He said to me, \"Write, for these words are true and faithful.\"'",
      officiantMinister = "Bi. J. Apfelbeck",
      officiantRank = "Bishop",
      assistingMinisters = "Pr. T. Kühnle",
      specialServiceTag = "Bishop Service",
      location = "Niagara Sanctuary",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/November2026/English/2026-11-22-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_nov_22_youth",
      monthYear = "November 2026",
      district = "Kitchener District",
      congregation = "Tri-District Youth",
      serviceDate = "Sun Nov 22, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Tri-District Youth Divine Service",
      theme = "We wait for the Lord",
      bibleWord = "Luke 12: 35–36 — 'Let your waist be girded and your lamps burning; and you yourselves be like men who wait for their master.'",
      officiantMinister = "District Youth Ministry",
      officiantRank = "Youth Leaders",
      assistingMinisters = "Tri-District Youth Staff",
      specialServiceTag = "Youth Service",
      location = "Bingemans (Kitchener, ON)",
      notes = "Tri-district youth gathering at Bingemans."
    ),
    MiniHqStagedService(
      id = "mhq_nov_29_niagara",
      monthYear = "November 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Sun Nov 29, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "First Sunday of Advent",
      theme = "Announcing the birth of Jesus",
      bibleWord = "Luke 1: 30–31 — 'Then the angel said to her, \"Do not be afraid, Mary, for you have found favor with God. And behold, you will conceive in your womb and bring forth a Son, and shall call His name Jesus.\"'",
      officiantMinister = "Pr. M. Jablonka",
      officiantRank = "Priest",
      assistingMinisters = "Pr. M. Grau",
      specialServiceTag = "First Advent",
      location = "Niagara Sanctuary",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/November2026/English/2026-11-29-dsg-en.pdf"
    ),

    // ==========================================
    // DECEMBER 2026 (Month Ahead)
    // ==========================================
    MiniHqStagedService(
      id = "mhq_dec_02_niagara",
      monthYear = "December 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Wed Dec 2, 2026",
      serviceTime = "7:30 PM",
      serviceTitle = "Midweek Divine Service",
      theme = "A joyful encounter",
      bibleWord = "Luke 1: 41 — 'And it happened, when Elizabeth heard the greeting of Mary, that the babe leaped in her womb; and Elizabeth was filled with the Holy Spirit.'",
      officiantMinister = "Pr. S. Hickey",
      officiantRank = "Priest",
      assistingMinisters = "Dc. H. Sohail",
      location = "Niagara Sanctuary",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/December2026/English/2026-12-02-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_dec_06_niagara",
      monthYear = "December 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Sun Dec 6, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Second Sunday of Advent",
      theme = "Jesus the Saviour",
      bibleWord = "Matthew 1: 21 — 'And she will bring forth a Son, and you shall call His name Jesus, for He will save His people from their sins.'",
      officiantMinister = "Pr. R. O'Connor",
      officiantRank = "Priest",
      assistingMinisters = "Pr. R. Connor",
      specialServiceTag = "Second Advent",
      location = "Niagara Sanctuary",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/December2026/English/2026-12-06-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_dec_09_echurch",
      monthYear = "December 2026",
      district = "Hamilton District",
      congregation = "All Congregations",
      serviceDate = "Wed Dec 9, 2026",
      serviceTime = "7:30 PM",
      serviceTitle = "Midweek Divine Service (eChurch)",
      theme = "My soul magnifies the Lord",
      bibleWord = "Luke 1: 46–48 — 'And Mary said: \"My soul magnifies the Lord, and my spirit has rejoiced in God my Savior. For He has regarded the lowly state of His maidservant; for behold, henceforth all generations will call me blessed.\"'",
      officiantMinister = "District eChurch Transmission",
      officiantRank = "Ministry",
      assistingMinisters = "District Broadcast",
      specialServiceTag = "eChurch",
      location = "Live Stream Broadcast",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/December2026/English/2026-12-09-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_dec_13_niagara",
      monthYear = "December 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Sun Dec 13, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Third Sunday of Advent & Children's Program",
      theme = "The light in the darkness",
      bibleWord = "John 1: 5 — 'And the light shines in the darkness, and the darkness did not comprehend it.'",
      officiantMinister = "Pr. M. Grau",
      officiantRank = "Priest",
      assistingMinisters = "Niagara Ministry",
      specialServiceTag = "Third Advent • Children's Christmas Program",
      location = "Niagara Sanctuary",
      notes = "Children's Christmas program during divine service.",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/December2026/English/2026-12-13-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_dec_15_seniors",
      monthYear = "December 2026",
      district = "Toronto District",
      congregation = "Brampton",
      serviceDate = "Tue Dec 15, 2026",
      serviceTime = "11:00 AM",
      serviceTitle = "Tri-District Seniors' Christmas Divine Service",
      theme = "Being ready for the returning Lord",
      bibleWord = "Luke 12: 40",
      officiantMinister = "DA. A. Martig",
      officiantRank = "District Apostle",
      assistingMinisters = "Pr. K. Peters, DEv. K. Berry",
      specialServiceTag = "District Apostle Service",
      location = "Brampton Church",
      notes = "Tri-district seniors' divine service conducted by District Apostle A. Martig."
    ),
    MiniHqStagedService(
      id = "mhq_dec_16_echurch",
      monthYear = "December 2026",
      district = "Hamilton District",
      congregation = "All Congregations",
      serviceDate = "Wed Dec 16, 2026",
      serviceTime = "7:30 PM",
      serviceTitle = "Midweek Divine Service (eChurch)",
      theme = "Being ready for the returning Lord",
      bibleWord = "Luke 12: 40 — 'Therefore you also be ready, for the Son of Man is coming at an hour you do not expect.'",
      officiantMinister = "District eChurch Transmission",
      officiantRank = "Ministry",
      assistingMinisters = "District Broadcast",
      specialServiceTag = "eChurch",
      location = "Live Stream Broadcast"
    ),
    MiniHqStagedService(
      id = "mhq_dec_20_niagara",
      monthYear = "December 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Sun Dec 20, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Fourth Sunday of Advent",
      theme = "Jesus Christ reveals what God is like",
      bibleWord = "1 John 5: 20 — 'And we know that the Son of God has come and has given us an understanding, that we may know Him who is true; and we are in Him who is true, in His Son Jesus Christ. This is the true God and eternal life.'",
      officiantMinister = "Pr. M. Dougherty",
      officiantRank = "Priest",
      assistingMinisters = "Pr. S. Hickey",
      specialServiceTag = "Fourth Advent",
      location = "Niagara Sanctuary",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/December2026/English/2026-12-20-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_dec_25_niagara",
      monthYear = "December 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Fri Dec 25, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Christmas Day Divine Service",
      theme = "Christmas",
      bibleWord = "Micah 5: 2 — 'But you, Bethlehem Ephrathah, though you are little among the thousands of Judah, yet out of you shall come forth to Me the One to be Ruler in Israel, whose goings forth are from of old, from everlasting.'",
      officiantMinister = "Pr. M. Grau",
      officiantRank = "Priest",
      assistingMinisters = "Dc. L. Hickey",
      specialServiceTag = "Christmas Day",
      location = "Niagara Sanctuary",
      notes = "Christmas Day celebration. Luke 2:1-14 reading.",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/December2026/English/2026-12-25-dsg-en.pdf"
    ),
    MiniHqStagedService(
      id = "mhq_dec_27_niagara",
      monthYear = "December 2026",
      district = "Hamilton District",
      congregation = "Niagara",
      serviceDate = "Sun Dec 27, 2026",
      serviceTime = "10:30 AM",
      serviceTitle = "Closing of the Year Divine Service",
      theme = "In God's hand",
      bibleWord = "Psalm 31: 15 — 'My times are in Your hand; deliver me from the hand of my enemies, and from those who persecute me.'",
      officiantMinister = "Pr. J. Bazal",
      officiantRank = "Priest",
      assistingMinisters = "Pr. R. Connor",
      specialServiceTag = "Year End Service",
      location = "Niagara Sanctuary",
      notes = "Year end Thanksgiving and blessing.",
      dsgPdfLink = "https://www.naccanada.org/imis_prod/NACDOCS/Minister%20HQ/Divine%20Service%20Prep/December2026/English/2026-12-27-dsg-en.pdf"
    )
  )
}
