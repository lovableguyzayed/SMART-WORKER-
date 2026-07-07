package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Notification
import com.example.ui.CardBorder
import com.example.ui.EmptyState
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SelectedCardBackground
import com.example.ui.theme.TextSecondary
import com.example.ui.vm.NotificationsViewModel
import java.time.format.DateTimeFormatter

@Composable
fun NotificationsScreen(vm: NotificationsViewModel, onBack: () -> Unit) {
    val notifications by vm.notifications.collectAsStateLifecycle()
    val fmt = DateTimeFormatter.ofPattern("dd MMM, HH:mm")

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Notifications",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
                onSearch = null,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (notifications.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().clickable { vm.markAllRead() }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.DoneAll, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Mark all as read", color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (notifications.isEmpty()) {
                EmptyState(Icons.Filled.NotificationsNone, "No notifications", "Attendance and system alerts will appear here.")
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    items(notifications, key = { it.id }) { n -> NotificationRow(n, fmt) }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: Notification, fmt: DateTimeFormatter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (n.isRead) CardBackground else SelectedCardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            Box(Modifier.size(8.dp).background(if (n.isRead) TextSecondary.copy(alpha = 0.3f) else PrimaryBlue, CircleShape))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(n.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                if (n.body.isNotBlank()) {
                    Spacer(Modifier.size(2.dp))
                    Text(n.body, fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(Modifier.size(4.dp))
                Text(n.createdAt.format(fmt), fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
