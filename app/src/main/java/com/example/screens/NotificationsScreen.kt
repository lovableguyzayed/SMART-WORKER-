package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Notification
import com.example.ui.EmptyState
import com.example.ui.SwCard
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.Flame
import com.example.ui.theme.Ink
import com.example.ui.theme.Lime
import com.example.ui.theme.OnDark
import com.example.ui.theme.OnDarkFaint
import com.example.ui.theme.OnDarkMuted
import com.example.ui.theme.Surface3
import com.example.ui.theme.SwType
import com.example.ui.theme.Volt
import com.example.ui.theme.White
import com.example.ui.vm.NotificationsViewModel
import java.time.format.DateTimeFormatter

/** Notifications — attendance activity from every site, newest first. */
@Composable
fun NotificationsScreen(vm: NotificationsViewModel, onBack: () -> Unit) {
    val notifications by vm.notifications.collectAsStateLifecycle()
    val fmt = DateTimeFormatter.ofPattern("dd MMM, HH:mm")
    val unread = notifications.count { !it.isRead }

    Scaffold(
        containerColor = Ink,
        topBar = {
            SwTopBar(
                title = "Notifications",
                leading = {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Surface3)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = OnDark, modifier = Modifier.size(19.dp),
                        )
                    }
                },
                trailing = {
                    if (unread > 0) {
                        Row(
                            Modifier.clip(CircleShape).background(Flame)
                                .clickable { vm.markAllRead() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.DoneAll, null,
                                tint = White,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Read all",
                                style = SwType.Title.copy(fontSize = 12.sp),
                                color = White,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (notifications.isEmpty()) {
                EmptyState(
                    Icons.Filled.NotificationsNone,
                    "Nothing yet",
                    "Attendance marked by site users will appear here.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(notifications, key = { it.id }) { n -> NotificationRow(n, fmt) }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: Notification, fmt: DateTimeFormatter) {
    val (icon, accent) = categoryVisual(n.category)
    SwCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(13.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = if (n.isRead) 0.10f else 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon, null,
                    tint = if (n.isRead) accent.copy(alpha = 0.55f) else accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    n.title,
                    style = SwType.Title.copy(fontSize = 14.sp),
                    color = if (n.isRead) OnDarkMuted else OnDark,
                )
                if (n.body.isNotBlank()) {
                    Spacer(Modifier.size(2.dp))
                    Text(n.body, style = SwType.Caption, color = OnDarkMuted)
                }
                Spacer(Modifier.size(5.dp))
                Text(n.createdAt.format(fmt), style = SwType.Caption.copy(fontSize = 11.sp), color = OnDarkFaint)
            }
            if (!n.isRead) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(Flame))
            }
        }
    }
}

private fun categoryVisual(category: String): Pair<ImageVector, Color> = when (category) {
    "system" -> Icons.Filled.Settings to Volt
    "sync" -> Icons.Filled.CloudSync to Lime
    else -> Icons.Filled.HowToReg to Flame
}
