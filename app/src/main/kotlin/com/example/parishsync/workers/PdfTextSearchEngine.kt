package com.example.parishsync.workers

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringWriter
import kotlin.math.abs

data class PdfNormalizedRect(
  val left: Float,
  val top: Float,
  val right: Float,
  val bottom: Float
)

data class PdfCharBox(
  val ch: Char,
  val leftNorm: Float,
  val topNorm: Float,
  val rightNorm: Float,
  val bottomNorm: Float
)

data class PdfPageTextIndex(
  val pageIndex: Int,
  val text: String,
  val charBoxes: List<PdfCharBox>
)

data class PdfTextMatch(
  val pageIndex: Int,
  val rects: List<PdfNormalizedRect>,
  val primaryRect: PdfNormalizedRect,
  val matchedText: String
)

/**
 * Decodes real PDF text layers (including FlateDecode streams, ToUnicode CMaps, and font encodings)
 * via PDFBox and computes normalized 0..1 page bounding boxes for direct on-canvas highlighting.
 */
object PdfTextSearchEngine {

  @Volatile
  private var isInitialized = false

  fun ensureInitialized(context: Context?) {
    if (isInitialized || context == null) return
    synchronized(this) {
      if (!isInitialized) {
        runCatching {
          PDFBoxResourceLoader.init(context.applicationContext)
        }
        isInitialized = true
      }
    }
  }

  fun extractPageTextIndexes(
    context: Context?,
    pdfFile: File,
    maxPages: Int = 24
  ): List<PdfPageTextIndex> {
    if (!pdfFile.exists() || pdfFile.length() == 0L) return emptyList()
    ensureInitialized(context)
    return runCatching {
      PDDocument.load(pdfFile).use { document ->
        extractFromDocument(document, maxPages)
      }
    }.getOrDefault(emptyList())
  }

  fun extractPlainTextLines(
    pdfBytes: ByteArray,
    context: Context? = null,
    maxPages: Int = 24
  ): List<String> {
    if (pdfBytes.isEmpty()) return emptyList()
    ensureInitialized(context)
    return runCatching {
      PDDocument.load(ByteArrayInputStream(pdfBytes)).use { document ->
        val stripper = PDFTextStripper().apply {
          sortByPosition = true
          startPage = 1
          endPage = document.numberOfPages.coerceAtMost(maxPages)
        }
        stripper.getText(document)
          .lines()
          .map { it.trim() }
          .filter { it.isNotBlank() }
      }
    }.getOrDefault(emptyList())
  }

  private fun extractFromDocument(
    document: PDDocument,
    maxPages: Int
  ): List<PdfPageTextIndex> {
    val totalPages = document.numberOfPages.coerceAtMost(maxPages)
    val result = ArrayList<PdfPageTextIndex>(totalPages)

    for (pageIdx in 0 until totalPages) {
      val sb = StringBuilder()
      val boxes = ArrayList<PdfCharBox>()

      val stripper = object : PDFTextStripper() {
        init {
          sortByPosition = true
          startPage = pageIdx + 1
          endPage = pageIdx + 1
        }

        override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
          if (textPositions.isNullOrEmpty()) return
          val page = currentPage
          val mediaBox = page?.mediaBox
          val cropBox = page?.cropBox ?: mediaBox
          val rotation = (((page?.rotation ?: 0) % 360) + 360) % 360

          val effectiveW = if (cropBox != null) {
            if (rotation == 90 || rotation == 270) cropBox.height else cropBox.width
          } else {
            val first = textPositions[0]
            if (rotation == 90 || rotation == 270) first.pageHeight else first.pageWidth
          }.takeIf { it > 0f } ?: 612f

          val effectiveH = if (cropBox != null) {
            if (rotation == 90 || rotation == 270) cropBox.width else cropBox.height
          } else {
            val first = textPositions[0]
            if (rotation == 90 || rotation == 270) first.pageWidth else first.pageHeight
          }.takeIf { it > 0f } ?: 792f

          val cropOffsetX = if (cropBox != null && mediaBox != null) {
            when (rotation) {
              90 -> cropBox.lowerLeftY - mediaBox.lowerLeftY
              180 -> mediaBox.upperRightX - cropBox.upperRightX
              270 -> mediaBox.upperRightY - cropBox.upperRightY
              else -> cropBox.lowerLeftX - mediaBox.lowerLeftX
            }
          } else 0f

          val cropOffsetY = if (cropBox != null && mediaBox != null) {
            when (rotation) {
              90 -> cropBox.lowerLeftX - mediaBox.lowerLeftX
              180 -> cropBox.lowerLeftY - mediaBox.lowerLeftY
              270 -> mediaBox.upperRightX - cropBox.upperRightX
              else -> mediaBox.upperRightY - cropBox.upperRightY
            }
          } else 0f

          for (tp in textPositions) {
            val unicode = tp.unicode ?: continue
            if (unicode.isEmpty()) continue

            val adjX = tp.xDirAdj - cropOffsetX
            val adjY = tp.yDirAdj - cropOffsetY

            val left = (adjX / effectiveW).coerceIn(0f, 1f)
            val right = ((adjX + tp.widthDirAdj) / effectiveW).coerceIn(left, 1f)
            val glyphHeight = tp.heightDir.coerceAtLeast(4f)
            val top = ((adjY - glyphHeight * 1.05f) / effectiveH).coerceIn(0f, 1f)
            val bottom = ((adjY + glyphHeight * 0.25f) / effectiveH).coerceIn(top, 1f)

            for (ch in unicode) {
              sb.append(ch)
              if (ch.isWhitespace()) {
                boxes.add(PdfCharBox(ch, -1f, -1f, -1f, -1f))
              } else {
                boxes.add(PdfCharBox(ch, left, top, right, bottom))
              }
            }
          }
        }

        override fun writeWordSeparator() {
          if (sb.isNotEmpty() && !sb.last().isWhitespace()) {
            sb.append(' ')
            boxes.add(PdfCharBox(' ', -1f, -1f, -1f, -1f))
          }
        }

        override fun writeLineSeparator() {
          if (sb.isNotEmpty() && sb.last() != '\n') {
            sb.append('\n')
            boxes.add(PdfCharBox('\n', -1f, -1f, -1f, -1f))
          }
        }
      }

      stripper.writeText(document, StringWriter())
      result.add(
        PdfPageTextIndex(
          pageIndex = pageIdx,
          text = sb.toString(),
          charBoxes = boxes
        )
      )
    }

    return result
  }

  fun searchMatches(
    pages: List<PdfPageTextIndex>,
    query: String
  ): List<PdfTextMatch> {
    val q = query.trim()
    if (q.isBlank() || pages.isEmpty()) return emptyList()

    val matches = mutableListOf<PdfTextMatch>()
    for (page in pages) {
      val text = page.text
      val boxes = page.charBoxes
      if (text.isEmpty() || boxes.size != text.length) continue

      var searchFrom = 0
      while (searchFrom <= text.length - q.length) {
        val idx = text.indexOf(q, startIndex = searchFrom, ignoreCase = true)
        if (idx < 0) break

        val slice = boxes.subList(idx, idx + q.length).filter { it.leftNorm >= 0f }
        if (slice.isNotEmpty()) {
          val lineRects = groupIntoLineRects(slice)
          if (lineRects.isNotEmpty()) {
            matches.add(
              PdfTextMatch(
                pageIndex = page.pageIndex,
                rects = lineRects,
                primaryRect = lineRects.first(),
                matchedText = text.substring(idx, idx + q.length)
              )
            )
          }
        }
        searchFrom = idx + q.length.coerceAtLeast(1)
      }
    }
    return matches
  }

  private fun groupIntoLineRects(boxes: List<PdfCharBox>): List<PdfNormalizedRect> {
    if (boxes.isEmpty()) return emptyList()
    val rects = mutableListOf<PdfNormalizedRect>()
    var curLeft = boxes[0].leftNorm
    var curTop = boxes[0].topNorm
    var curRight = boxes[0].rightNorm
    var curBottom = boxes[0].bottomNorm

    for (i in 1 until boxes.size) {
      val b = boxes[i]
      if (abs(b.topNorm - curTop) <= 0.02f) {
        curLeft = minOf(curLeft, b.leftNorm)
        curTop = minOf(curTop, b.topNorm)
        curRight = maxOf(curRight, b.rightNorm)
        curBottom = maxOf(curBottom, b.bottomNorm)
      } else {
        rects.add(PdfNormalizedRect(curLeft, curTop, curRight, curBottom))
        curLeft = b.leftNorm
        curTop = b.topNorm
        curRight = b.rightNorm
        curBottom = b.bottomNorm
      }
    }
    rects.add(PdfNormalizedRect(curLeft, curTop, curRight, curBottom))
    return rects
  }
}
