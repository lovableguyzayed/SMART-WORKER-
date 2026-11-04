package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Danger
import com.example.ui.theme.Flame
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.OnBright
import com.example.ui.theme.OnDark
import com.example.ui.theme.OnDarkMuted
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface3
import com.example.ui.theme.SwType
import com.example.ui.theme.White

// ═════════════════════════════════════════════════════════════════════════════
//  Shared primitives for the dark colour-blocked design system.
// ═════════════════════════════════════════════════════════════════════════════

/** Rounded radius used by every card and tile in the system. */
val TileRadius = RoundedCornerShape(20.dp)

/**
 * Standard surface for anything card-shaped. Pass a bright [container] (Lime,
 * Sun, Coral, Cream…) to colour-block it — remember that bright containers need
 * [OnBright] content, which [StatTile] handles for you.
 */
@Composable
fun SwCard(
    modifier: Modifier = Modifier,
    container: Color = Surface1,
    bordered: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(TileRadius)
            .background(container, TileRadius)
            .then(
                if (bordered && container == Surface1) {
                    Modifier.border(1.dp, Hairline, TileRadius)
                } else {
                    Modifier
                },
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) { content() }
}

/**
 * The system's signature pairing: an oversized number over a small wide label.
 * Used for every headline figure — attendance counts, payroll totals, rates.
 */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    container: Color = Surface1,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val bright = container.luminanceIsBright()
    val fg = if (bright) OnBright else OnDark
    val sub = if (bright) OnBright.copy(alpha = 0.62f) else OnDarkMuted
    SwCard(modifier = modifier, container = container, onClick = onClick) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label.uppercase(), style = SwType.Label, color = sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(value, style = SwType.StatLarge, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (caption != null) {
                Text(caption, style = SwType.Caption, color = sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Rough perceptual check so a tile can pick ink-or-white content itself. */
internal fun Color.luminanceIsBright(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) > 0.6f

/** Selectable pill — date strips, filters, segmented controls. */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Flame,
) {
    val bg = if (selected) accent else Surface3
    val fg = if (selected) (if (accent.luminanceIsBright()) OnBright else White) else OnDarkMuted
    Box(
        modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
        )
    }
}

/**
 * Horizontal meter from the fitness reference — the label sits on the filled
 * portion, the percentage is pinned to its trailing edge.
 */
@Composable
fun MeterRow(
    label: String,
    percent: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val fraction = (percent.coerceIn(0, 100)) / 100f
    val fg = if (accent.luminanceIsBright()) OnBright else White
    Box(
        modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(TileRadius)
            .background(Surface3),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceAtLeast(0.34f))
                .fillMaxHeight()
                .clip(TileRadius)
                .background(accent),
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = SwType.Title, color = fg, maxLines = 1, modifier = Modifier.weight(1f))
            Text("$percent%", style = SwType.StatSmall, color = fg)
        }
    }
}

/** Standard top app bar with optional search + notification bell (with badge). */
@Composable
fun SwTopBar(
    title: String,
    unreadCount: Int = 0,
    onSearch: (() -> Unit)? = null,
    onNotifications: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(color = Ink) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = title,
                style = SwType.Headline.copy(fontSize = 22.sp),
                color = OnDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                trailing()
                Spacer(Modifier.width(8.dp))
            }
            if (onSearch != null) {
                CircleAction(Icons.Filled.Search, "Search", onSearch)
                Spacer(Modifier.width(8.dp))
            }
            if (onNotifications != null) {
                Box(
                    modifier = Modifier.semantics {
                        contentDescription =
                            if (unreadCount > 0) "Notifications, $unreadCount unread" else "Notifications"
                    },
                ) {
                    CircleAction(Icons.Filled.Notifications, null, onNotifications)
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(17.dp)
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

/** Icon in a dark circular well — the reference decks' header button. */
@Composable
fun CircleAction(icon: ImageVector, label: String?, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Surface3)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = OnDark, modifier = Modifier.size(19.dp))
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = SwType.Headline.copy(fontSize = 18.sp), color = OnDark)
        if (action != null) {
            Text(
                action,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Flame,
                modifier = Modifier.clickable { onAction?.invoke() },
            )
        }
    }
}

@Composable
fun StatusPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Surface3),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = OnDarkMuted, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = SwType.Title, color = OnDark)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = SwType.Caption, color = OnDarkMuted)
    }
}

val CardBorder = BorderStroke(1.dp, Hairline)
