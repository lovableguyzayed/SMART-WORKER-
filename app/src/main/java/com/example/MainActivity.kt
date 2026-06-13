package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SmartWorkerTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation() {
    var currentScreen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("login") }
    var phoneNumber by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    androidx.compose.animation.Crossfade(targetState = currentScreen) { screen ->
        when (screen) {
            "login" -> LoginScreen(onSendOtp = {
                phoneNumber = it
                currentScreen = "verify_otp"
            })
            "verify_otp" -> com.example.screens.VerifyOtpScreen(
                phoneNumber = phoneNumber,
                onBack = { currentScreen = "login" },
                onVerify = {
                    currentScreen = "home"
                }
            )
            "home" -> HomeScreen(onLogout = {
                currentScreen = "login"
            })
        }
    }
}

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Welcome to Smart Worker!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1321))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun LoginScreen(onSendOtp: (String) -> Unit = {}) {
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var mobileNumber by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Background Image Header area
        Image(
            painter = painterResource(id = R.drawable.bg_workers),
            contentDescription = "Construction Workers Background",
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = (-120).dp)
        )

        // Top Content Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                shadowElevation = 8.dp,
                color = White,
                modifier = Modifier.size(100.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Smart Worker Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row {
                        Text("SMART ", color = PrimaryBlue, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        Text("WORKER", color = DarkBlue, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Workforce Management Simplified",
                        color = DarkBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom Login Card area
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.60f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = CardBackground,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Welcome Back!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Login to continue to your account",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Mobile Number Field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("+91", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp),
                        color = DividerColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    BasicTextField(
                        value = mobileNumber,
                        onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) mobileNumber = it },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = TextPrimary),
                        decorationBox = { innerTextField ->
                            if (mobileNumber.isEmpty()) {
                                Text("Enter mobile number", color = TextSecondary, fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    )

                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Send OTP Button
                Button(
                    onClick = {
                        if (mobileNumber.length == 10) {
                            onSendOtp(mobileNumber)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Send OTP",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // OR Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
                    Text(
                        "OR",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Login as",
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Roles Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoleCard(
                        title = "Contractor",
                        subtitle = "Manage Projects\n& Workers",
                        iconId = R.drawable.ic_contractor,
                        iconSize = 76,
                        selected = selectedRole == "Contractor",
                        onClick = { selectedRole = "Contractor" },
                        modifier = Modifier.weight(1f)
                    )
                    RoleCard(
                        title = "Supervisor",
                        subtitle = "Supervise & Track\nWorkforce",
                        iconId = R.drawable.ic_supervisor,
                        iconSize = 60,
                        selected = selectedRole == "Supervisor",
                        onClick = { selectedRole = "Supervisor" },
                        modifier = Modifier.weight(1f)
                    )
                    RoleCard(
                        title = "Accountant",
                        subtitle = "Manage Payroll\n& Payments",
                        iconId = R.drawable.ic_accountant,
                        iconSize = 76,
                        selected = selectedRole == "Accountant",
                        onClick = { selectedRole = "Accountant" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Security Shield Banner
                Surface(
                    color = SecurityBannerBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Your data is 100% secure and protected",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Footer
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Don't have an account? ", color = TextSecondary, fontSize = 14.sp)
                    Text("Contact Admin", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    subtitle: String,
    iconId: Int,
    iconSize: Int = 64,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) PrimaryBlue else DividerColor
    val bgColor = if (selected) SelectedCardBackground else White

    Box(modifier = modifier.clickable { onClick() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, end = 6.dp)
                .height(176.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, borderColor),
            color = bgColor
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(iconSize.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    maxLines = 2
                )
            }
        }

        if (selected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(White, CircleShape)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    SmartWorkerTheme {
        LoginScreen()
    }
}
