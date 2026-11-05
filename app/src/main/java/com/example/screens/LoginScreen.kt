package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.Coral
import com.example.ui.theme.Flame
import com.example.ui.theme.Ink
import com.example.ui.theme.Lime
import com.example.ui.theme.OnDark
import com.example.ui.theme.OnDarkFaint
import com.example.ui.theme.OnDarkMuted
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface3
import com.example.ui.theme.SwType
import com.example.ui.theme.Volt
import com.example.ui.theme.White
import com.example.ui.vm.AppViewModel

/**
 * Sign-in — the dark canvas with two soft colour glows behind it, the wordmark,
 * and a single card holding the credentials.
 */
@Composable
fun LoginScreen(appVm: AppViewModel) {
    val error by appVm.loginError.collectAsStateLifecycle()
    LoginContent(
        error = error,
        onClearError = appVm::clearLoginError,
        onSubmit = { u, p -> appVm.login(u, p) {} },
    )
}

/** Presentation half — state in, callbacks out, so it renders from fixtures. */
@Composable
internal fun LoginContent(
    error: String?,
    onClearError: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val canSubmit = username.isNotBlank() && password.isNotBlank()
    fun submit() {
        if (canSubmit) onSubmit(username.trim(), password)
    }

    Box(Modifier.fillMaxSize().background(Ink).imePadding()) {
        // Two soft colour fields instead of a gradient sheet, so the canvas
        // stays near-black and the card and accent still read as the system's.
        //
        // These are radial gradients rather than Modifier.blur: blur clips to
        // its own rectangular bounds by default, and is a no-op below API 31
        // while this app ships to API 26 — either way it renders as a hard
        // -edged block. A gradient falls off smoothly on every version.
        Box(
            Modifier.offset(x = (-90).dp, y = (-70).dp).size(340.dp)
                .background(
                    Brush.radialGradient(listOf(Flame.copy(alpha = 0.34f), Color.Transparent)),
                ),
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 90.dp, y = 90.dp).size(320.dp)
                .background(
                    Brush.radialGradient(listOf(Volt.copy(alpha = 0.30f), Color.Transparent)),
                ),
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))

            Box(
                Modifier.size(62.dp).clip(RoundedCornerShape(20.dp)).background(Flame),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Bolt, null, tint = White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("SmartWorker", style = SwType.Headline.copy(fontSize = 32.sp), color = OnDark)
            Spacer(Modifier.height(4.dp))
            Text(
                "Attendance and payroll, on site",
                style = SwType.Body, color = OnDarkMuted, textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Surface1)
                    .padding(22.dp),
            ) {
                Text("Welcome back", style = SwType.Headline.copy(fontSize = 21.sp), color = OnDark)
                Spacer(Modifier.height(20.dp))

                FieldLabel("Username or email")
                LoginField(
                    value = username,
                    onValueChange = {
                        username = it
                        if (error != null) onClearError()
                    },
                    placeholder = "admin",
                    leading = Icons.Filled.Person,
                )

                Spacer(Modifier.height(14.dp))
                FieldLabel("Password")
                LoginField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (error != null) onClearError()
                    },
                    placeholder = "••••••••",
                    leading = Icons.Filled.Lock,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailing = {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            if (showPassword) "Hide password" else "Show password",
                            tint = OnDarkMuted,
                            modifier = Modifier.size(19.dp).clickable { showPassword = !showPassword },
                        )
                    },
                )

                error?.let {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(Coral.copy(alpha = 0.14f)).padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(it, color = Coral, style = SwType.Caption.copy(fontSize = 12.5.sp))
                    }
                }

                Spacer(Modifier.height(22.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(if (canSubmit) Flame else Surface3)
                        .clickable(enabled = canSubmit) { submit() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Sign in",
                        style = SwType.Title.copy(fontSize = 15.sp),
                        color = if (canSubmit) White else OnDarkFaint,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("DEMO ACCOUNTS", style = SwType.Label, color = OnDarkFaint)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DemoAccount("admin / admin123", "Administrator", Flame) {
                    username = "admin"; password = "admin123"
                }
                DemoAccount("suresh / suresh123", "Attendance", Lime) {
                    username = "suresh"; password = "suresh123"
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("© SmartWorker", style = SwType.Caption, color = OnDarkFaint)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = SwType.Label, color = OnDarkMuted)
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: androidx.compose.ui.graphics.vector.ImageVector,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Surface3)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(leading, null, tint = OnDarkMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(11.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = SwType.Body, color = OnDarkFaint)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = SwType.Body.copy(color = OnDark),
                cursorBrush = SolidColor(Flame),
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun DemoAccount(
    credentials: String,
    role: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(credentials, style = SwType.Caption.copy(fontSize = 11.5.sp), color = OnDark)
        Spacer(Modifier.height(3.dp))
        Text(role, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}
