package com.example.parishsync

import com.example.parishsync.data.local.MiniHqAnnouncement
import com.example.parishsync.workers.MiniHqScraperService
import com.example.parishsync.workers.PdfCharBox
import com.example.parishsync.workers.PdfPageTextIndex
import com.example.parishsync.workers.PdfScheduleParser
import com.example.parishsync.workers.PdfTextSearchEngine
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfAndAnnouncementSearchTest {

  private fun buildMinimalFlateCompressedPdf(): ByteArray {
    val contentStream = """
      BT
      /F1 11 Tf
      50 700 Td
      (Pr. J. P. Mulumba) Tj
      0 -20 Td
      (Pr. D. Riesen) Tj
      0 -20 Td
      (Pr. A. Proctor) Tj
      0 -20 Td
      (Pr. R. James) Tj
      ET
    """.trimIndent().toByteArray(Charsets.ISO_8859_1)

    val deflater = Deflater()
    deflater.setInput(contentStream)
    deflater.finish()
    val compressedOut = ByteArrayOutputStream()
    val buf = ByteArray(1024)
    while (!deflater.finished()) {
      val n = deflater.deflate(buf)
      if (n <= 0) break
      compressedOut.write(buf, 0, n)
    }
    deflater.end()
    val compressedBytes = compressedOut.toByteArray()

    val out = ByteArrayOutputStream()
    val offsets = mutableListOf<Int>()
    fun writeStr(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))

    writeStr("%PDF-1.4\n")
    offsets.add(out.size())
    writeStr("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
    offsets.add(out.size())
    writeStr("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
    offsets.add(out.size())
    writeStr("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n")
    offsets.add(out.size())
    writeStr("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")
    offsets.add(out.size())
    writeStr("5 0 obj\n<< /Length ${compressedBytes.size} /Filter /FlateDecode >>\nstream\n")
    out.write(compressedBytes)
    writeStr("\nendstream\nendobj\n")

    val xrefOffset = out.size()
    writeStr("xref\n0 ${offsets.size + 1}\n")
    writeStr("0000000000 65535 f \n")
    for (off in offsets) {
      writeStr(String.format("%010d 00000 n \n", off))
    }
    writeStr("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
    return out.toByteArray()
  }

  private fun buildPageIndexFromLines(pageIndex: Int, lines: List<String>): PdfPageTextIndex {
    val sb = StringBuilder()
    val boxes = mutableListOf<PdfCharBox>()
    lines.forEachIndexed { lineIdx, line ->
      val top = 0.1f + lineIdx * 0.05f
      val bottom = top + 0.03f
      line.forEachIndexed { chIdx, ch ->
        sb.append(ch)
        if (ch.isWhitespace()) {
          boxes.add(PdfCharBox(ch, -1f, -1f, -1f, -1f))
        } else {
          val left = 0.1f + chIdx * 0.015f
          boxes.add(PdfCharBox(ch, left, top, left + 0.014f, bottom))
        }
      }
      if (lineIdx < lines.lastIndex) {
        sb.append('\n')
        boxes.add(PdfCharBox('\n', -1f, -1f, -1f, -1f))
      }
    }
    return PdfPageTextIndex(pageIndex = pageIndex, text = sb.toString(), charBoxes = boxes)
  }

  @Test
  fun pdfTextSearchEngine_findsRealWordsAndBoundingBoxesInCompressedPdf() {
    val pdfBytes = buildMinimalFlateCompressedPdf()
    val indexes = listOf(
      buildPageIndexFromLines(
        pageIndex = 0,
        lines = listOf(
          "Pr. J. P. Mulumba",
          "Pr. D. Riesen",
          "Pr. A. Proctor",
          "Pr. R. James"
        )
      )
    )

    // Note: "Pr. A. Proctor" contains "Pr" twice ("Pr." and "Proctor"), so 5 total matches of "pr"
    val matches = PdfTextSearchEngine.searchMatches(indexes, "pr")
    assertEquals(5, matches.size)
    for (m in matches) {
      assertTrue(m.primaryRect.left in 0f..1f)
      assertTrue(m.primaryRect.top in 0f..1f)
      assertTrue(m.primaryRect.right > m.primaryRect.left)
      assertTrue(m.primaryRect.bottom > m.primaryRect.top)
    }

    val lines = PdfScheduleParser.extractSearchableLinesFromPdfBytes(pdfBytes)
    assertTrue(lines.any { it.contains("Mulumba") })
    assertFalse(lines.any { it.contains("FlateDecode") })
  }

  @Test
  fun miniHqScraper_parsesAnnouncementDetailHtmlAndSummary() {
    val sampleHtml = """
      <html>
        <body>
          <header><nav><a href="/signout">Sign Out</a></nav></header>
          <div id="MainContent">
            <h2>Funeral Notice</h2>
            <p>It is with sadness that we share the news that Priest (ir) Terry Clarke of the Woodstock congregation has passed away.</p>
            <p>Arrangements are as follows:</p>
            <h3><u>Visitation</u></h3>
            <p><strong>Date:</strong> Friday, November 20, 2026<br/>
            <strong>Time:</strong> 6:00-8:00 p.m.<br/>
            <strong>Location:</strong> Brock &amp; Visser Funeral Home, 845 Devonshire Ave, Woodstock, ON</p>
            <h3><u>Funeral</u></h3>
            <p><strong>Date:</strong> Sunday, November 22, 2026<br/>
            <strong>Time:</strong> 4:00 p.m.<br/>
            <strong>Officiant:</strong> Priest J. Cudney</p>
            <p><a href="/docs/Terry_Clarke_Notice.pdf">Download Notice PDF</a></p>
          </div>
          <footer>Copyright NAC Canada</footer>
        </body>
      </html>
    """.trimIndent()

    val base = MiniHqAnnouncement(
      id = "ann_1",
      title = "Funeral Notice - Priest (ir) Terry Clarke",
      date = "November 19, 2026",
      category = "Funeral Notice",
      summary = "",
      content = "",
      sourceUrl = "https://www.naccanada.org/imis_prod/MiniHQ/Announcements/Funeral-Notice-Priest-ir-Terry-Clarke.aspx"
    )

    val parsed = MiniHqScraperService.parseAnnouncementDetailHtml(base, sampleHtml)
    assertTrue(parsed.bodyHtml.contains("Visitation"))
    assertTrue(parsed.bodyHtml.contains("Priest J. Cudney"))
    assertFalse(parsed.bodyHtml.contains("Sign Out"))
    assertFalse(parsed.bodyHtml.contains("Copyright NAC Canada"))
    assertTrue(parsed.content.contains("Brock & Visser Funeral Home"))
    assertTrue(parsed.summary.length in 30..185)
    assertFalse(parsed.summary.endsWith(" "))
    assertEquals("https://www.naccanada.org/docs/Terry_Clarke_Notice.pdf", parsed.attachedPdfUrl)
    assertEquals("Download Notice PDF", parsed.attachedPdfName)
  }
}
