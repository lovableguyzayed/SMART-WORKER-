package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.DividerColor
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.White

/** Standard top app bar with optional search + notification bell (with badge). */
@Composable
fun SwTopBar(
    title: String,
    unreadCount: Int = 0,
    onSearch: (() -> Unit)? = null,
    onNotifications: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(color = CardBackground, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Navy,
                modifier = Modifier.weight(1f),
            )
            if (onSearch != null) {
                Icon(
                    Icons.Filled.Search, "Search",
                    tint = Navy,
                    modifier = Modifier.size(24.dp).clickable(onClick = onSearch),
                )
                Spacer(Modifier.width(16.dp))
            }
            if (onNotifications != null) {
                Box(modifier = Modifier.clickable(onClick = onNotifications).semantics {
                    contentDescription = if (unreadCount > 0) "Notifications, $unreadCount unread" else "Notifications"
                }) {
                    Icon(Icons.Filled.Notifications, null, tint = Navy, modifier = Modifier.size(24.dp))
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Danger, CircleShape)
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (unreadCount > 9) "9+" else "$unreadCount",
                                fontSize = 9.sp, color = White, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
        if (action != null) {
            Text(
                action, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryBlue,
                modifier = Modifier.clickable { onAction?.invoke() },
            )
        }
    }
}

@Composable
fun StatusPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 13.sp, color = TextSecondary)
    }
}

val CardBorder = BorderStroke(1.dp, DividerColor)
