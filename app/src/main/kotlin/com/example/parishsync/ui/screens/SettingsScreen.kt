package com.example.parishsync.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.parishsync.data.local.UserProfileEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  activeUser: UserProfileEntity,
  congregationName: String,
  hasOsNotificationPermission: Boolean,
  hasPushSubscription: Boolean,
  onSaveProfile: (
    fullName: String,
    email: String,
    phone: String,
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    onResult: (Result<UserProfileEntity>) -> Unit
  ) -> Unit,
  onEnablePhoneNotifications: () -> Unit,
  onDisablePhoneNotifications: () -> Unit,
  onTestPhoneNotification: () -> Unit,
  onUpdateNotificationPreferences: (preferPush: Boolean, preferEmail: Boolean) -> Unit,
  onSignOut: () -> Unit
) {
  var fullName by remember(activeUser.id) { mutableStateOf(activeUser.fullName) }
  var email by remember(activeUser.id) { mutableStateOf(activeUser.email) }
  var phone by remember(activeUser.id) { mutableStateOf(activeUser.phone) }
  var currentPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmNewPassword by remember { mutableStateOf("") }
  var profileFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

  var preferPush by remember(activeUser.id, activeUser.preferPushReminders) {
    mutableStateOf(activeUser.preferPushReminders)
  }
  var preferEmail by remember(activeUser.id, activeUser.preferEmailReminders) {
    mutableStateOf(activeUser.preferEmailReminders)
  }

  LaunchedEffect(activeUser.fullName, activeUser.email, activeUser.phone) {
    fullName = activeUser.fullName
    email = activeUser.email
    phone = activeUser.phone
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = "Profile & Settings",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Manage your personal details, notification preferences, and account session.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Section 1: Personal Profile Editor
    ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Personal Profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
        }

        // Read-only Congregation & Roles
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Congregation: ${congregationName.ifBlank { activeUser.congregationId }}",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              activeUser.effectiveRoles.forEach { appRole ->
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(16.dp)
                ) {
                  Text(
                    text = appRole.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("Full Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "Change Password (Optional)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        OutlinedTextField(
          value = currentPassword,
          onValueChange = { currentPassword = it },
          label = { Text("Current Password") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = newPassword,
          onValueChange = { newPassword = it },
          label = { Text("New Password (min 6 characters)") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = confirmNewPassword,
          onValueChange = { confirmNewPassword = it },
          label = { Text("Confirm New Password") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth()
        )

        profileFeedback?.let { (isSuccess, message) ->
          Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
          )
        }

        Button(
          onClick = {
            onSaveProfile(
              fullName,
              email,
              phone,
              currentPassword,
              newPassword,
              confirmNewPassword
            ) { result ->
              result
                .onSuccess {
                  currentPassword = ""
                  newPassword = ""
                  confirmNewPassword = ""
                  profileFeedback = true to "Profile updated successfully."
                }
                .onFailure { err ->
                  profileFeedback = false to (err.message ?: "Unable to update profile.")
                }
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Save Profile")
        }
      }
    }

    // Section 2: Push Notification & Alert Preferences
    ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Push Notifications & Alerts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
        }

        val statusLabel = when {
          !hasOsNotificationPermission -> "Blocked by OS"
          hasPushSubscription -> "Enabled on this device"
          else -> "Not enabled"
        }
        val statusIcon = when {
          !hasOsNotificationPermission -> Icons.Default.Warning
          hasPushSubscription -> Icons.Default.CheckCircle
          else -> Icons.Default.NotificationsOff
        }
        val statusColor = when {
          !hasOsNotificationPermission -> MaterialTheme.colorScheme.error
          hasPushSubscription -> MaterialTheme.colorScheme.primary
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Device Push Status",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
              ) {
                Icon(
                  imageVector = statusIcon,
                  contentDescription = null,
                  tint = statusColor,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = statusLabel,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = statusColor
                )
              }
            }

            Switch(
              checked = hasOsNotificationPermission && hasPushSubscription,
              onCheckedChange = { enabled ->
                if (enabled) {
                  onEnablePhoneNotifications()
                } else {
                  onDisablePhoneNotifications()
                }
              }
            )
          }
        }

        OutlinedButton(
          onClick = {
            if (!hasOsNotificationPermission || !hasPushSubscription) {
              onEnablePhoneNotifications()
            } else {
              onTestPhoneNotification()
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            if (hasOsNotificationPermission && hasPushSubscription) {
              "Test Phone Notification"
            } else {
              "Enable Phone Notifications"
            }
          )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Push Reminders",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Receive mobile push alerts when reminded of upcoming duties.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = preferPush,
            onCheckedChange = { checked ->
              preferPush = checked
              onUpdateNotificationPreferences(checked, preferEmail)
            }
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Email Reminders",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
              )
            }
            Text(
              text = "Receive schedule summaries and duty reminders via email.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = preferEmail,
            onCheckedChange = { checked ->
              preferEmail = checked
              onUpdateNotificationPreferences(preferPush, checked)
            }
          )
        }
      }
    }

    // Section 3: Account & Session Actions
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f))
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Account & Session",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "Signing out clears your active session and returns to the sign-in screen.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
          onClick = onSignOut,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          ),
          border = BorderStroke(0.dp, MaterialTheme.colorScheme.error),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text("Sign Out")
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
  }
}
