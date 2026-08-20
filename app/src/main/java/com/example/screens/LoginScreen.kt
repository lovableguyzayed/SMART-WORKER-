package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.DarkBlue
import com.example.ui.theme.Danger
import com.example.ui.theme.DividerColor
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.White
import com.example.ui.vm.AppViewModel

/**
 * Sign-in — mirrors the web app's `login.html`: a deep gradient canvas with
 * faint geometric shapes, the SmartWorker badge, and a white "Welcome Back"
 * card holding the credential form.
 */
@Composable
fun LoginScreen(appVm: AppViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val error by appVm.loginError.collectAsStateLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(DarkBlue, Color(0xFF0B3A86), PrimaryBlue)))
            .imePadding(),
    ) {
        // Decorative shapes from the reference canvas.
        Box(
            Modifier.offset(x = 24.dp, y = 96.dp).size(80.dp).rotate(12f)
                .border(2.dp, White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
        )
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = (-32).dp, y = 130.dp)
                .size(64.dp).clip(CircleShape).background(White.copy(alpha = 0.10f)),
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = 32.dp, y = (-120).dp)
                .size(56.dp).rotate(45f).clip(RoundedCornerShape(10.dp)).background(White.copy(alpha = 0.15f)),
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = (-48).dp, y = (-80).dp)
                .size(40.dp).clip(CircleShape).border(1.dp, White.copy(alpha = 0.25f), CircleShape),
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))

            // Badge with the online dot
            Box {
                Surface(shape = CircleShape, shadowElevation = 8.dp, color = White, modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, tint = PrimaryBlue, modifier = Modifier.size(30.dp))
                    }
                }
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp)
                        .size(16.dp).clip(CircleShape).background(Success)
                        .border(1.dp, White, CircleShape),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("SmartWorker", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "Professional Attendance Management",
                color = White.copy(alpha = 0.85f), fontSize = 13.sp, textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // Credential card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = White,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Welcome Back", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("Access your attendance dashboard", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(20.dp))

                    Text("Username or Email", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = Navy)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            if (error != null) appVm.clearLoginError()
                        },
                        singleLine = true,
                        placeholder = { Text("admin", fontSize = 14.sp, color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = TextSecondary, modifier = Modifier.size(19.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = DividerColor,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("Password", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = Navy)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (error != null) appVm.clearLoginError()
                        },
                        singleLine = true,
                        placeholder = { Text("••••••••", fontSize = 14.sp, color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = TextSecondary, modifier = Modifier.size(19.dp)) },
                        trailingIcon = {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                if (showPassword) "Hide password" else "Show password",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp).clickable { showPassword = !showPassword },
                            )
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = DividerColor,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Danger.copy(alpha = 0.1f)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(it, color = Danger, fontSize = 12.5.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { appVm.login(username.trim(), password) {} },
                        enabled = username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    ) {
                        Text("Sign In to Dashboard", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            // Demo credentials — this build ships with seeded accounts.
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(White.copy(alpha = 0.12f))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Demo accounts", color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("admin / admin123", color = White.copy(alpha = 0.9f), fontSize = 11.5.sp)
                        Text("Administrator", color = White.copy(alpha = 0.65f), fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("suresh / suresh123", color = White.copy(alpha = 0.9f), fontSize = 11.5.sp)
                        Text("Attendance user", color = White.copy(alpha = 0.65f), fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "© SmartWorker. All rights reserved.",
                color = White.copy(alpha = 0.6f), fontSize = 11.sp,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
