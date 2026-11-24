package com.example.parishsync.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PostAdd
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parishsync.data.local.AnnouncementEntity
import com.example.parishsync.data.model.Congregation
import com.example.parishsync.data.model.UserRole

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberBulletinScreen(
  congregation: Congregation,
  activeRole: UserRole,
  announcements: List<AnnouncementEntity>,
  onPublishAnnouncement: (
    title: String,
    summary: String,
    body: String,
    category: String,
    isUrgent: Boolean,
    serviceDate: String,
    liturgicalOrder: String
  ) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedCategory by rememberSaveable { mutableStateOf("All") }
  var showComposeDialog by rememberSaveable { mutableStateOf(false) }

  val categories = listOf("All", "Urgent Broadcast", "Service Leaflet", "Weekly Bulletin")
  val filteredAnnouncements = remember(announcements, selectedCategory) {
    if (selectedCategory == "All") {
      announcements
    } else {
      announcements.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Congregation Welcome Banner
    item {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = congregation.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
              Text(
                text = "${congregation.address} • ${congregation.rectorName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
              )
            }
            if (activeRole.canPublishAnnouncements) {
              Button(
                onClick = { showComposeDialog = true }
              ) {
                Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Publish")
              }
            }
          }
          Text(
            text = "Worship Schedule: ${congregation.primaryServiceTimes}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    // Category Filter Chips
    item {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        categories.forEach { category ->
          FilterChip(
            selected = selectedCategory == category,
            onClick = { selectedCategory = category },
            label = { Text(category) }
          )
        }
      }
    }

    // Feed Items
    if (filteredAnnouncements.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Fresh Bulletin Feed — No Announcements Yet",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "This congregation feed is completely fresh. Rectors and Administrators can tap 'Publish' above or upload the monthly Volunteer Schedule PDF in the Schedule/Ministry tab to auto-populate updates.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    items(filteredAnnouncements, key = { it.id }) { item ->
      BulletinFeedCard(item = item)
    }
  }

  if (showComposeDialog) {
    PublishBulletinDialog(
      onDismiss = { showComposeDialog = false },
      onPublish = { title, summary, body, category, isUrgent, serviceDate, liturgicalOrder ->
        onPublishAnnouncement(title, summary, body, category, isUrgent, serviceDate, liturgicalOrder)
        showComposeDialog = false
      }
    )
  }
}

@Composable
private fun BulletinFeedCard(
  item: AnnouncementEntity,
  modifier: Modifier = Modifier
) {
  var isExpanded by rememberSaveable(item.id) { mutableStateOf(item.category == "Service Leaflet" || item.isUrgent) }

  val cardColors = if (item.isUrgent) {
    CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer
    )
  } else {
    CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  }

  ElevatedCard(
    colors = cardColors,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = when {
              item.isUrgent -> Icons.Default.NotificationsActive
              item.category == "Service Leaflet" -> Icons.Default.MenuBook
              else -> Icons.Default.Campaign
            },
            contentDescription = null,
            tint = if (item.isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
          )
          Surface(
            color = if (item.isUrgent) {
              MaterialTheme.colorScheme.error
            } else {
              MaterialTheme.colorScheme.primaryContainer
            },
            shape = MaterialTheme.shapes.small
          ) {
            Text(
              text = if (item.isUrgent) "URGENT PUSH ALERT" else item.category.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (item.isUrgent) {
                MaterialTheme.colorScheme.onError
              } else {
                MaterialTheme.colorScheme.onPrimaryContainer
              },
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
        if (item.serviceDate.isNotBlank()) {
          Text(
            text = item.serviceDate,
            style = MaterialTheme.typography.labelMedium,
            color = if (item.isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (item.isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = item.summary,
        style = MaterialTheme.typography.bodyMedium,
        color = if (item.isUrgent) {
          MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        }
      )

      AnimatedVisibility(visible = isExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          Text(
            text = item.body,
            style = MaterialTheme.typography.bodyMedium
          )
          if (item.liturgicalOrder.isNotBlank()) {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = "Liturgical Order of Worship",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
                Text(
                  text = item.liturgicalOrder,
                  style = MaterialTheme.typography.bodySmall
                )
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Posted by ${item.authorName} (${item.authorRole.badgeLabel})",
          style = MaterialTheme.typography.labelSmall,
          color = if (item.isUrgent) {
            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          }
        )
        TextButton(onClick = { isExpanded = !isExpanded }) {
          Text(if (isExpanded) "Collapse" else "Read Full Leaflet")
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null
          )
        }
      }
    }
  }
}

@Composable
private fun PublishBulletinDialog(
  onDismiss: () -> Unit,
  onPublish: (
    title: String,
    summary: String,
    body: String,
    category: String,
    isUrgent: Boolean,
    serviceDate: String,
    liturgicalOrder: String
  ) -> Unit
) {
  var title by rememberSaveable { mutableStateOf("") }
  var summary by rememberSaveable { mutableStateOf("") }
  var body by rememberSaveable { mutableStateOf("") }
  var category by rememberSaveable { mutableStateOf("Weekly Bulletin") }
  var isUrgent by rememberSaveable { mutableStateOf(false) }
  var serviceDate by rememberSaveable { mutableStateOf("Nov 15, 2026") }
  var liturgicalOrder by rememberSaveable { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Publish Bulletin or Service Leaflet") },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("Weekly Bulletin", "Service Leaflet", "Urgent Broadcast").forEach { cat ->
            AssistChip(
              onClick = {
                category = cat
                if (cat == "Urgent Broadcast") isUrgent = true
              },
              label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Headline / Sunday Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = summary,
          onValueChange = { summary = it },
          label = { Text("Brief Summary") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = serviceDate,
          onValueChange = { serviceDate = it },
          label = { Text("Service Date") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = body,
          onValueChange = { body = it },
          label = { Text("Full Announcement / Lectionary Details") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth()
        )
        if (category == "Service Leaflet") {
          OutlinedTextField(
            value = liturgicalOrder,
            onValueChange = { liturgicalOrder = it },
            label = { Text("Liturgical Order of Service") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.AddAlert, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text("Send Instant Push Notification", style = MaterialTheme.typography.bodySmall)
          }
          Switch(checked = isUrgent, onCheckedChange = { isUrgent = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onPublish(title, summary, body, category, isUrgent, serviceDate, liturgicalOrder)
          }
        }
      ) {
        Text("Publish")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
