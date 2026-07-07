package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkBlue
import com.example.ui.theme.DividerColor
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecurityBannerBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.White
import com.example.ui.vm.AppViewModel

@Composable
fun LoginScreen(appVm: AppViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val error by appVm.loginError.collectAsStateLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, shadowElevation = 8.dp, color = White, modifier = Modifier.size(96.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.VerifiedUser, null, tint = PrimaryBlue, modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row {
                Text("SMART ", color = PrimaryBlue, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text("WORKER", color = DarkBlue, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text("Workforce Management Simplified", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.62f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = CardBackground,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Welcome Back!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text("Login to continue to your account", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; appVm.clearLoginError() },
                    label = { Text("Username") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; appVm.clearLoginError() },
                    label = { Text("Password") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Lock, null, tint = TextSecondary) },
                    trailingIcon = {
                        val icon: ImageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        Icon(
                            icon,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { showPassword = !showPassword },
                        )
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                )

                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error!!, color = com.example.ui.theme.Danger, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { if (username.isNotBlank() && password.isNotBlank()) appVm.login(username.trim(), password) {} },
                    enabled = username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                ) {
                    Icon(Icons.Filled.Lock, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Login", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(24.dp))
                Surface(color = SecurityBannerBackground, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.VerifiedUser, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Your data is 100% secure and protected", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Demo: admin / admin123", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = DividerColor,
    focusedLabelColor = PrimaryBlue,
)
