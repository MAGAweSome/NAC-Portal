package com.example.parishsync.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parishsync.data.local.PrayerRequestEntity
import com.example.parishsync.data.local.RideShareEntity
import com.example.parishsync.data.model.PastoralRequestType
import com.example.parishsync.data.model.RidePostType
import com.example.parishsync.data.model.UserRole

@Composable
fun CommunityHubScreen(
  activeUserName: String,
  activeRole: UserRole,
  prayerRequests: List<PrayerRequestEntity>,
  rideShares: List<RideShareEntity>,
  onSubmitPrayerRequest: (
    requesterName: String,
    requesterContact: String,
    requestType: PastoralRequestType,
    content: String,
    isPrivateToClergy: Boolean
  ) -> Unit,
  onCreateRideShare: (
    postType: RidePostType,
    memberName: String,
    phoneContact: String,
    neighborhood: String,
    serviceDate: String,
    serviceTime: String,
    seatsCount: Int,
    notes: String
  ) -> Unit,
  onMatchRideShare: (RideShareEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }

  Column(modifier = modifier.fillMaxSize()) {
    TabRow(selectedTabIndex = selectedTab) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Prayer & Pastoral Visits") },
        icon = { Icon(Icons.Default.VolunteerActivism, contentDescription = null) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Ride Sharing Board (${rideShares.size})") },
        icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) }
      )
    }

    when (selectedTab) {
      0 -> PrayerRequestsSection(
        activeUserName = activeUserName,
        activeRole = activeRole,
        prayerRequests = prayerRequests,
        onSubmitPrayerRequest = onSubmitPrayerRequest
      )
      1 -> RideSharingSection(
        activeUserName = activeUserName,
        rideShares = rideShares,
        onCreateRideShare = onCreateRideShare,
        onMatchRideShare = onMatchRideShare
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrayerRequestsSection(
  activeUserName: String,
  activeRole: UserRole,
  prayerRequests: List<PrayerRequestEntity>,
  onSubmitPrayerRequest: (
    requesterName: String,
    requesterContact: String,
    requestType: PastoralRequestType,
    content: String,
    isPrivateToClergy: Boolean
  ) -> Unit
) {
  var requesterName by rememberSaveable { mutableStateOf(activeUserName) }
  var requesterContact by rememberSaveable { mutableStateOf("(555) 234-8910") }
  var selectedType by rememberSaveable { mutableStateOf(PastoralRequestType.PRAYER_INTENTION) }
  var content by rememberSaveable { mutableStateOf("") }
  var isPrivateToClergy by rememberSaveable { mutableStateOf(true) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Submission Form Card
    item {
      ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.LocalHospital,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Column {
              Text(
                text = "Submit Digital Prayer Intention or Pastoral Visit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Dispatched directly to the Rector & Ministerial Care Queue with RLS privacy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            PastoralRequestType.entries.forEach { reqType ->
              FilterChip(
                selected = selectedType == reqType,
                onClick = { selectedType = reqType },
                label = { Text(reqType.displayName, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = requesterName,
              onValueChange = { requesterName = it },
              label = { Text("Your Name") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = requesterContact,
              onValueChange = { requesterContact = it },
              label = { Text("Phone / Room #") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Prayer Intention or Hospital / Homebound Visit Details") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = if (isPrivateToClergy) Icons.Default.Lock else Icons.Default.Public,
                contentDescription = null,
                tint = if (isPrivateToClergy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
              )
              Column {
                Text(
                  text = if (isPrivateToClergy) {
                    "Confidential — Clergy Queue Only"
                  } else {
                    "Public — Share on Congregation Prayer Chain"
                  },
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = if (isPrivateToClergy) {
                    "Protected by Supabase RLS; visible only to Ministers/Rector."
                  } else {
                    "Visible to congregation members for intercessory prayer."
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Switch(
              checked = isPrivateToClergy,
              onCheckedChange = { isPrivateToClergy = it }
            )
          }

          Button(
            onClick = {
              if (content.isNotBlank()) {
                onSubmitPrayerRequest(
                  requesterName,
                  requesterContact,
                  selectedType,
                  content,
                  isPrivateToClergy
                )
                content = ""
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submit to Ministerial Queue")
          }
        }
      }
    }

    item {
      Text(
        text = if (activeRole.canTriagePrayerQueue) {
          "All Congregation Requests (Clergy View • Includes Confidential)"
        } else {
          "Congregation Prayer Chain & Your Submitted Intentions"
        },
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
    }

    items(prayerRequests, key = { it.id }) { req ->
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = if (req.isPrivateToClergy) Icons.Default.Lock else Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Text(
                text = req.requestType.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = MaterialTheme.shapes.small
            ) {
              Text(
                text = req.status.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          Text(
            text = req.content,
            style = MaterialTheme.typography.bodyMedium
          )

          Text(
            text = "Requested by ${req.requesterName} (${req.requesterContact})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (req.ministerialNotes.isNotBlank()) {
            Text(
              text = "Pastoral Care Note: ${req.ministerialNotes}",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.tertiary
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RideSharingSection(
  activeUserName: String,
  rideShares: List<RideShareEntity>,
  onCreateRideShare: (
    postType: RidePostType,
    memberName: String,
    phoneContact: String,
    neighborhood: String,
    serviceDate: String,
    serviceTime: String,
    seatsCount: Int,
    notes: String
  ) -> Unit,
  onMatchRideShare: (RideShareEntity) -> Unit
) {
  var showPostDialog by rememberSaveable { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Congregation Ride Sharing Board",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
              text = "Connect members needing Sunday transportation with volunteer drivers in their neighborhood.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(onClick = { showPostDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Post")
          }
        }
      }
    }

    items(rideShares, key = { it.id }) { ride ->
      val isOffering = ride.postType == RidePostType.OFFERING_RIDE
      ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = if (isOffering) {
                MaterialTheme.colorScheme.secondaryContainer
              } else {
                MaterialTheme.colorScheme.tertiaryContainer
              },
              shape = MaterialTheme.shapes.small
            ) {
              Text(
                text = if (isOffering) {
                  "DRIVER OFFERING ${ride.seatsCount} SEAT(S)"
                } else {
                  "MEMBER REQUESTING ${ride.seatsCount} SEAT(S)"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
            Text(
              text = "${ride.serviceDate} • ${ride.serviceTime}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = ride.neighborhood,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          }

          Text(
            text = "${ride.memberName} • Contact: ${ride.phoneContact}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
          )

          if (ride.notes.isNotBlank()) {
            Text(
              text = ride.notes,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          HorizontalDivider()

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (ride.isFulfilled) {
              Text(
                text = "✓ Matched with ${ride.matchedMemberName ?: "Congregation Volunteer"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
            } else {
              Text(
                text = if (isOffering) "Seats open for Sunday pickup" else "Awaiting volunteer driver",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Button(onClick = { onMatchRideShare(ride) }) {
                Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isOffering) "Claim Seat" else "Offer Ride")
              }
            }
          }
        }
      }
    }
  }

  if (showPostDialog) {
    CreateRideShareDialog(
      defaultName = activeUserName,
      onDismiss = { showPostDialog = false },
      onCreate = { type, name, phone, area, date, time, seats, notes ->
        onCreateRideShare(type, name, phone, area, date, time, seats, notes)
        showPostDialog = false
      }
    )
  }
}

@Composable
private fun CreateRideShareDialog(
  defaultName: String,
  onDismiss: () -> Unit,
  onCreate: (
    postType: RidePostType,
    memberName: String,
    phoneContact: String,
    neighborhood: String,
    serviceDate: String,
    serviceTime: String,
    seatsCount: Int,
    notes: String
  ) -> Unit
) {
  var postType by rememberSaveable { mutableStateOf(RidePostType.OFFERING_RIDE) }
  var memberName by rememberSaveable { mutableStateOf(defaultName) }
  var phoneContact by rememberSaveable { mutableStateOf("(555) 234-8910") }
  var neighborhood by rememberSaveable { mutableStateOf("Oakridge & Maplewood") }
  var serviceDate by rememberSaveable { mutableStateOf("Sunday, Nov 15") }
  var serviceTime by rememberSaveable { mutableStateOf("10:30 AM Service") }
  var seatsText by rememberSaveable { mutableStateOf("2") }
  var notes by rememberSaveable { mutableStateOf("Can pick up 25 mins before service.") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Post to Ride Sharing Board") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          RidePostType.entries.forEach { type ->
            AssistChip(
              onClick = { postType = type },
              label = {
                Text(
                  text = if (type == RidePostType.OFFERING_RIDE) "Offer Ride" else "Need a Ride",
                  style = MaterialTheme.typography.labelSmall
                )
              }
            )
          }
        }
        OutlinedTextField(
          value = memberName,
          onValueChange = { memberName = it },
          label = { Text("Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = phoneContact,
          onValueChange = { phoneContact = it },
          label = { Text("Phone Number") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = neighborhood,
          onValueChange = { neighborhood = it },
          label = { Text("Neighborhood / Cross Streets") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = serviceDate,
            onValueChange = { serviceDate = it },
            label = { Text("Date") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = seatsText,
            onValueChange = { seatsText = it },
            label = { Text("Seats") },
            singleLine = true,
            modifier = Modifier.weight(0.6f)
          )
        }
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Accessibility / Pickup Notes") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onCreate(
            postType,
            memberName,
            phoneContact,
            neighborhood,
            serviceDate,
            serviceTime,
            seatsText.toIntOrNull() ?: 1,
            notes
          )
        }
      ) {
        Text("Publish Ride Post")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
