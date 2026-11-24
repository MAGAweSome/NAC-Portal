package com.example.parishsync.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.parishsync.R
import androidx.compose.ui.unit.dp
import com.example.parishsync.data.local.ParishSeedData
import com.example.parishsync.data.local.UserProfileEntity

/**
 * Clean, direct Login Screen for NAC Portal.
 * Public self-registration and preloaded congregations are completely removed.
 * Member and Rector accounts are provisioned exclusively by Administrators and Rectors.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
  allProvisionedUsers: List<UserProfileEntity>,
  syncStatusMessage: String,
  mailtrapStatusMessage: String,
  onSignInWithLockedAccount: (email: String, password: String) -> Unit,
  onAcceptEmailInvite: (email: String, inviteCode: String, newPassword: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var signInEmail by rememberSaveable { mutableStateOf(ParishSeedData.BOOTSTRAP_ADMIN_EMAIL) }
  var signInPassword by rememberSaveable { mutableStateOf(ParishSeedData.BOOTSTRAP_ADMIN_PASSWORD) }

  var showInviteActivation by rememberSaveable { mutableStateOf(false) }
  var invitedEmail by rememberSaveable { mutableStateOf("") }
  var inviteCode by rememberSaveable { mutableStateOf("") }
  var newPassword by rememberSaveable { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 28.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // NAC Portal Header
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    ) {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.size(72.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_nac_emblem),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(44.dp)
          )
        }
      }

      Text(
        text = "NAC Portal",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = "Church Administration & Member Portal",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Main Sign-In Card
    ElevatedCard(
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Sign In",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            AssistChip(
              onClick = {
                signInEmail = ParishSeedData.BOOTSTRAP_ADMIN_EMAIL
                signInPassword = ParishSeedData.BOOTSTRAP_ADMIN_PASSWORD
              },
              label = { Text("Admin") },
              leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
              }
            )
            AssistChip(
              onClick = {
                signInEmail = ParishSeedData.BOOTSTRAP_MINISTER_EMAIL
                signInPassword = ParishSeedData.BOOTSTRAP_MINISTER_PASSWORD
              },
              label = { Text("Deacon (Marcus)") },
              leadingIcon = {
                Icon(painter = painterResource(id = R.drawable.ic_nac_emblem), contentDescription = null, modifier = Modifier.size(14.dp))
              }
            )
            AssistChip(
              onClick = {
                signInEmail = ParishSeedData.BOOTSTRAP_JAMES_EMAIL
                signInPassword = ParishSeedData.BOOTSTRAP_JAMES_PASSWORD
              },
              label = { Text("Priest (James)") },
              leadingIcon = {
                Icon(painter = painterResource(id = R.drawable.ic_nac_emblem), contentDescription = null, modifier = Modifier.size(14.dp))
              }
            )
            AssistChip(
              onClick = {
                signInEmail = ParishSeedData.BOOTSTRAP_ERIC_EMAIL
                signInPassword = ParishSeedData.BOOTSTRAP_ERIC_PASSWORD
              },
              label = { Text("Rector (Eric)") },
              leadingIcon = {
                Icon(painter = painterResource(id = R.drawable.ic_nac_emblem), contentDescription = null, modifier = Modifier.size(14.dp))
              }
            )
          }
        }

        OutlinedTextField(
          value = signInEmail,
          onValueChange = { signInEmail = it },
          label = { Text("Email Address") },
          leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = signInPassword,
          onValueChange = { signInPassword = it },
          label = { Text("Password") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Button(
          onClick = { onSignInWithLockedAccount(signInEmail, signInPassword) },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("sign_in_button")
        ) {
          Icon(Icons.Default.Lock, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Sign In",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        TextButton(
          onClick = { showInviteActivation = !showInviteActivation },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = if (showInviteActivation) {
              "Hide Invitation Setup"
            } else {
              "Received an Email Invitation? Set Your Password"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )
        }

        AnimatedVisibility(visible = showInviteActivation) {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Enter your email and the Invite Code sent by your Rector or Administrator via Mailtrap to choose your password.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
              value = invitedEmail,
              onValueChange = { invitedEmail = it },
              label = { Text("Invited Email") },
              leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
              value = inviteCode,
              onValueChange = { inviteCode = it },
              label = { Text("Invite Code (e.g. NAC-1234)") },
              leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
              value = newPassword,
              onValueChange = { newPassword = it },
              label = { Text("Create Your Password") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            Button(
              onClick = { onAcceptEmailInvite(invitedEmail, inviteCode, newPassword) },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
            ) {
              Icon(Icons.Default.MarkEmailRead, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Activate Account & Set Password")
            }
          }
        }
      }
    }

    Text(
      text = "Accounts are created exclusively by Rectors and Administrators.",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
