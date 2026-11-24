package com.example.parishsync.data.remote

import android.util.Base64
import com.example.parishsync.BuildConfig
import com.example.parishsync.data.local.VolunteerAssignmentEntity
import com.example.parishsync.data.model.Congregation
import com.example.parishsync.data.model.DelegatedRole
import com.example.parishsync.data.model.UserRole
import com.example.parishsync.workers.SchedulePdfGenerator
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
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
data class MailtrapConfig(
  val apiToken: String = BuildConfig.MAILTRAP_API_TOKEN.ifBlank {
    System.getenv("MAILTRAP_API_TOKEN").orEmpty()
  },
  val senderEmail: String = BuildConfig.MAILTRAP_SENDER_EMAIL.ifBlank { "info@NACPortal.com" },
  val senderName: String = BuildConfig.MAILTRAP_SENDER_NAME.ifBlank { "NAC Portal" },
  val defaultTestRecipient: String = BuildConfig.MAILTRAP_DEFAULT_RECIPIENT,
  val smtpHost: String = BuildConfig.MAILTRAP_SMTP_HOST.ifBlank { "sandbox.smtp.mailtrap.io" },
  val smtpPort: Int = BuildConfig.MAILTRAP_SMTP_PORT.toIntOrNull() ?: 2525,
  val smtpUsername: String = BuildConfig.MAILTRAP_SMTP_USERNAME.ifBlank {
    System.getenv("MAILTRAP_SMTP_USERNAME").orEmpty()
  },
  val smtpPassword: String = BuildConfig.MAILTRAP_SMTP_PASSWORD.ifBlank {
    System.getenv("MAILTRAP_SMTP_PASSWORD").orEmpty()
  }
) {
  val isSmtpConfigured: Boolean
    get() = smtpHost.isNotBlank() && smtpUsername.isNotBlank() && smtpPassword.isNotBlank()

  val isConfigured: Boolean
    get() = (isSmtpConfigured || apiToken.isNotBlank()) && senderEmail.contains("@")
}

@Serializable
data class MailtrapAddress(
  @SerialName("email")
  val email: String,
  @SerialName("name")
  val name: String? = null
)

@Serializable
data class MailtrapAttachment(
  @SerialName("content")
  val contentBase64: String,
  @SerialName("filename")
  val filename: String,
  @SerialName("type")
  val mimeType: String = "application/pdf",
  @SerialName("disposition")
  val disposition: String = "attachment"
)

@Serializable
data class MailtrapSendRequest(
  @SerialName("from")
  val from: MailtrapAddress,
  @SerialName("to")
  val to: List<MailtrapAddress>,
  @SerialName("subject")
  val subject: String,
  @SerialName("text")
  val text: String,
  @SerialName("category")
  val category: String,
  @SerialName("attachments")
  val attachments: List<MailtrapAttachment>? = null
)

@Serializable
data class MailtrapSendResponse(
  @SerialName("success")
  val success: Boolean = false,
  @SerialName("message_ids")
  val messageIds: List<String> = emptyList(),
  @SerialName("errors")
  val errors: List<String> = emptyList()
)

/**
 * Mailtrap Email Sending API client (`https://send.api.mailtrap.io/api/send`).
 * Used for:
 * 1. Sending account invitation links/codes when an Admin or Rector provisions a user by email.
 * 2. Sending the uploaded Volunteer Schedule PDF & commitment summary to the congregation mailing list.
 * 3. Sending integration test emails (inspectable at https://mailtrap.io/sending/email_logs).
 */
class MailtrapEmailService(
  initialConfig: MailtrapConfig = MailtrapConfig()
) {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
  }

  private val _config = MutableStateFlow(initialConfig)
  val config: StateFlow<MailtrapConfig> = _config.asStateFlow()

  private val _lastEmailLog = MutableStateFlow(
    if (initialConfig.isConfigured) {
      "Mailtrap Email API ready (${initialConfig.senderEmail}) • Check logs at https://mailtrap.io/sending/email_logs"
    } else {
      "Mailtrap token not set in environment/local.properties • Set MAILTRAP_API_TOKEN or configure in Admin Console"
    }
  )
  val lastEmailLog: StateFlow<String> = _lastEmailLog.asStateFlow()

  fun updateConfig(apiToken: String, senderEmail: String, senderName: String) {
    _config.value = _config.value.copy(
      apiToken = apiToken.trim(),
      senderEmail = senderEmail.trim().ifBlank { "hello@nacportal.org" },
      senderName = senderName.trim().ifBlank { "NAC Portal" }
    )
    _lastEmailLog.value = if (_config.value.isConfigured) {
      "Mailtrap configured for sender ${_config.value.senderEmail}. View logs: https://mailtrap.io/sending/email_logs"
    } else {
      "Mailtrap API token cleared."
    }
  }

  /**
   * Sends the sample Mailtrap integration test email.
   */
  suspend fun sendIntegrationTestEmail(recipientEmail: String): Result<String> {
    val cleanRecipient = recipientEmail.trim().ifBlank { _config.value.defaultTestRecipient }
    return sendEmail(
      recipients = listOf(MailtrapAddress(email = cleanRecipient)),
      subject = "You are awesome!",
      text = "Congrats for sending test email with Mailtrap!\n\nSent from NAC Portal Android Administration Platform.",
      category = "Integration Test"
    )
  }

  /**
   * Sends an Account Setup & Password Invitation email when an Admin or Rector creates an account.
   */
  suspend fun sendAccountInvitationEmail(
    recipientEmail: String,
    recipientName: String,
    congregation: Congregation,
    assignedRole: UserRole,
    delegatedRole: DelegatedRole,
    inviteCode: String,
    invitedByName: String
  ): Result<String> {
    val deepLink = "nacportal://invite?email=${recipientEmail.trim()}&code=$inviteCode"
    val delegatedLine = if (delegatedRole != DelegatedRole.NONE) {
      "\n• Delegated Congregation Role: ${delegatedRole.displayName}"
    } else {
      ""
    }
    val body = """
      Hello $recipientName,

      $invitedByName has created your NAC Portal account for ${congregation.displayName}.
      Your account role and congregation have been pre-configured and locked to your profile:

      • Congregation: ${congregation.displayName}
      • Account Role: ${assignedRole.title}$delegatedLine
      • Login Email: ${recipientEmail.trim()}
      • One-Time Invite Code: $inviteCode

      HOW TO COMPLETE YOUR ACCOUNT & SET YOUR PASSWORD:
      1. Open the NAC Portal app (or tap your invite link: $deepLink)
      2. Select the "Accept Email Invite" tab on the sign-in screen.
      3. Enter your email (${recipientEmail.trim()}), Invite Code ($inviteCode), and choose your password.

      Grace and Peace,
      ${congregation.displayName} Administration
    """.trimIndent()

    return sendEmail(
      recipients = listOf(MailtrapAddress(email = recipientEmail.trim(), name = recipientName)),
      subject = "You're invited to join ${congregation.displayName} on NAC Portal",
      text = body,
      category = "Account Invitation"
    )
  }

  /**
   * Sends a Volunteer Reminder email fallback when a volunteer has not enabled phone push notifications.
   */
  suspend fun sendVolunteerReminderEmail(
    recipientEmail: String,
    recipientName: String,
    roleTier: String,
    dutyTitle: String,
    formattedBody: String,
    serviceDate: String,
    congregationName: String
  ): Result<String> {
    val body = """
      Hello $recipientName,

      $formattedBody

      Congregation: ${congregationName.ifBlank { "NAC Portal" }}

      Please open the NAC Portal app to view and confirm your duty.
    """.trimIndent()

    return sendEmail(
      recipients = listOf(MailtrapAddress(email = recipientEmail.trim(), name = recipientName)),
      subject = "$dutyTitle ($serviceDate)",
      text = body,
      category = "Volunteer Reminder"
    )
  }

  /**
   * Sends the uploaded Volunteer Schedule PDF and schedule summary to the congregation mailing list.
   */
  suspend fun sendSchedulePdfToCongregation(
    congregation: Congregation,
    recipientEmails: List<String>,
    subject: String,
    customMessage: String,
    scheduleSummaryText: String,
    pdfFileName: String,
    pdfBytes: ByteArray?,
    assignments: List<VolunteerAssignmentEntity> = emptyList()
  ): Result<String> {
    val validRecipients = recipientEmails
      .map { it.trim() }
      .filter { it.contains("@") }
      .distinct()
      .map { MailtrapAddress(email = it) }

    if (validRecipients.isEmpty()) {
      return Result.failure(IllegalArgumentException("No valid recipient emails found in the congregation mailing list."))
    }

    val effectivePdfBytes = pdfBytes ?: if (assignments.isNotEmpty()) {
      SchedulePdfGenerator.generateTabularSchedulePdf(
        congregationName = congregation.displayName.ifBlank { "NAC Portal" },
        scheduleTitle = subject.ifBlank { "Volunteer Schedule" },
        assignments = assignments
      )
    } else {
      buildSimplePdfDocumentBytes(
        title = subject,
        bodyText = "$customMessage\n\n$scheduleSummaryText"
      )
    }
    val base64Content = Base64.encodeToString(effectivePdfBytes, Base64.NO_WRAP)

    val fullBody = """
      $customMessage

      Please see the attached PDF schedule for ${congregation.displayName.ifBlank { "our congregation" }}.
    """.trimIndent()

    return sendEmail(
      recipients = validRecipients,
      subject = subject,
      text = fullBody,
      category = "Volunteer Schedule PDF",
      attachments = listOf(
        MailtrapAttachment(
          contentBase64 = base64Content,
          filename = pdfFileName.ifBlank { "Volunteer_Schedule.pdf" },
          mimeType = "application/pdf",
          disposition = "attachment"
        )
      )
    )
  }

  suspend fun sendEmail(
    recipients: List<MailtrapAddress>,
    subject: String,
    text: String,
    category: String,
    attachments: List<MailtrapAttachment>? = null
  ): Result<String> = withContext(Dispatchers.IO) {
    val cfg = _config.value
    if (cfg.isSmtpConfigured) {
      return@withContext runCatching {
        sendViaSmtpStartTls(cfg, recipients, subject, text, attachments)
      }
    }
    if (cfg.apiToken.isBlank()) {
      val msg = "Mailtrap credentials are not configured."
      _lastEmailLog.value = msg
      return@withContext Result.failure(IllegalStateException(msg))
    }

    runCatching {
      val requestPayload = MailtrapSendRequest(
        from = MailtrapAddress(email = cfg.senderEmail, name = cfg.senderName),
        to = recipients,
        subject = subject,
        text = text,
        category = category,
        attachments = attachments
      )
      val bodyJson = json.encodeToString(requestPayload)

      val connection = (URL(MAILTRAP_SEND_ENDPOINT).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 8_000
        readTimeout = 8_000
        setRequestProperty("Authorization", "Bearer ${cfg.apiToken}")
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
        doInput = true
        doOutput = true
      }

      try {
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
          writer.write(bodyJson)
          writer.flush()
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.let {
          BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { it.readText() }
        }.orEmpty()

        if (code !in 200..299) {
          val errorMsg = "Mailtrap HTTP $code: $responseBody (Verify sender domain '${cfg.senderEmail}' & API token at https://mailtrap.io/sending/email_logs)"
          _lastEmailLog.value = errorMsg
          throw IllegalStateException(errorMsg)
        }

        val parsed = runCatching { json.decodeFromString<MailtrapSendResponse>(responseBody) }.getOrNull()
        val msgId = parsed?.messageIds?.firstOrNull() ?: "ok"
        val successMsg = "Sent '$subject' to ${recipients.joinToString { it.email }} via Mailtrap (id: $msgId). View at https://mailtrap.io/sending/email_logs"
        _lastEmailLog.value = successMsg
        successMsg
      } finally {
        connection.disconnect()
      }
    }
  }

  private fun sendViaSmtpStartTls(
    cfg: MailtrapConfig,
    recipients: List<MailtrapAddress>,
    subject: String,
    text: String,
    attachments: List<MailtrapAttachment>?
  ): String {
    val plainSocket = Socket()
    plainSocket.connect(InetSocketAddress(cfg.smtpHost, cfg.smtpPort), 10_000)
    plainSocket.soTimeout = 10_000

    var activeSocket: Socket = plainSocket
    try {
      var reader = BufferedReader(InputStreamReader(activeSocket.getInputStream(), Charsets.UTF_8))
      var writer = OutputStreamWriter(activeSocket.getOutputStream(), Charsets.UTF_8)

      fun readSmtpResponse(): Pair<Int, String> {
        val sb = StringBuilder()
        var code = -1
        while (true) {
          val line = reader.readLine() ?: break
          sb.appendLine(line)
          if (line.length >= 3 && line.substring(0, 3).all { it.isDigit() }) {
            code = line.substring(0, 3).toInt()
            if (line.length == 3 || line[3] == ' ') break
          } else {
            break
          }
        }
        return code to sb.toString().trim()
      }

      fun sendCmd(cmd: String, expectedPrefix: Int): String {
        writer.write("$cmd\r\n")
        writer.flush()
        val (code, resp) = readSmtpResponse()
        if (code / 100 != expectedPrefix) {
          throw IllegalStateException("SMTP error ($code) on '$cmd': $resp")
        }
        return resp
      }

      val (greetCode, greetMsg) = readSmtpResponse()
      if (greetCode / 100 != 2) throw IllegalStateException("SMTP greeting failed ($greetCode): $greetMsg")

      sendCmd("EHLO nacportal.local", 2)
      sendCmd("STARTTLS", 2)

      val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
      val sslSocket = sslFactory.createSocket(plainSocket, cfg.smtpHost, cfg.smtpPort, true) as SSLSocket
      sslSocket.startHandshake()
      activeSocket = sslSocket
      reader = BufferedReader(InputStreamReader(sslSocket.getInputStream(), Charsets.UTF_8))
      writer = OutputStreamWriter(sslSocket.getOutputStream(), Charsets.UTF_8)

      sendCmd("EHLO nacportal.local", 2)
      sendCmd("AUTH LOGIN", 3)
      sendCmd(Base64.encodeToString(cfg.smtpUsername.toByteArray(Charsets.UTF_8), Base64.NO_WRAP), 3)
      sendCmd(Base64.encodeToString(cfg.smtpPassword.toByteArray(Charsets.UTF_8), Base64.NO_WRAP), 2)

      sendCmd("MAIL FROM:<${cfg.senderEmail}>", 2)
      recipients.forEach { rcpt ->
        sendCmd("RCPT TO:<${rcpt.email}>", 2)
      }
      sendCmd("DATA", 3)

      val boundary = "----NACPortalBoundary${System.currentTimeMillis()}"
      val toHeader = recipients.joinToString(", ") { r ->
        if (!r.name.isNullOrBlank()) "\"${r.name}\" <${r.email}>" else r.email
      }
      val mimePayload = buildString {
        append("From: \"${cfg.senderName}\" <${cfg.senderEmail}>\r\n")
        append("To: $toHeader\r\n")
        append("Subject: $subject\r\n")
        append("MIME-Version: 1.0\r\n")
        if (attachments.isNullOrEmpty()) {
          append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
          append(text.replace("\r\n", "\n").replace("\n", "\r\n"))
        } else {
          append("Content-Type: multipart/mixed; boundary=\"$boundary\"\r\n\r\n")
          append("--$boundary\r\n")
          append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
          append(text.replace("\r\n", "\n").replace("\n", "\r\n"))
          append("\r\n")
          attachments.forEach { att ->
            append("--$boundary\r\n")
            append("Content-Type: ${att.mimeType}; name=\"${att.filename}\"\r\n")
            append("Content-Disposition: ${att.disposition}; filename=\"${att.filename}\"\r\n")
            append("Content-Transfer-Encoding: base64\r\n\r\n")
            att.contentBase64.chunked(76).forEach { chunk ->
              append("$chunk\r\n")
            }
          }
          append("--$boundary--\r\n")
        }
        append("\r\n.\r\n")
      }
      writer.write(mimePayload)
      writer.flush()
      val (dataCode, dataResp) = readSmtpResponse()
      if (dataCode / 100 != 2) throw IllegalStateException("SMTP DATA rejected ($dataCode): $dataResp")
      runCatching { sendCmd("QUIT", 2) }

      val successMsg = "Sent '$subject' to ${recipients.joinToString { it.email }} via Mailtrap Sandbox"
      _lastEmailLog.value = successMsg
      return successMsg
    } finally {
      runCatching { activeSocket.close() }
    }
  }

  /**
   * Generates a valid minimal PDF-1.4 byte stream when sending a schedule PDF attachment
   * if the source schedule was indexed from text or built-in template.
   */
  private fun buildSimplePdfDocumentBytes(title: String, bodyText: String): ByteArray {
    val sanitizedLines = (listOf(title, "") + bodyText.lines())
      .take(45)
      .map { line ->
        line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
      }
    val textOps = buildString {
      appendLine("BT")
      appendLine("/F1 10 Tf")
      appendLine("36 756 Td")
      appendLine("14 TL")
      sanitizedLines.forEach { line ->
        appendLine("($line) Tj T*")
      }
      appendLine("ET")
    }
    val pdfContent = """
      %PDF-1.4
      1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
      2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
      3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj
      4 0 obj << /Length ${textOps.length} >>
      stream
      $textOps
      endstream
      endobj
      5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj
      xref
      0 6
      0000000000 65535 f 
      trailer << /Size 6 /Root 1 0 R >>
      startxref
      0
      %%EOF
    """.trimIndent()
    return pdfContent.toByteArray(Charsets.ISO_8859_1)
  }

  companion object {
    const val MAILTRAP_SEND_ENDPOINT = "https://send.api.mailtrap.io/api/send"
    const val MAILTRAP_LOGS_URL = "https://mailtrap.io/sending/email_logs"
  }
}
