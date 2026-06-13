package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VerifyOtpScreen(phoneNumber: String, onBack: () -> Unit, onVerify: (String) -> Unit) {
    var otpCode by remember { mutableStateOf("") }
    
    // Auto-verify when 6 digits are entered
    LaunchedEffect(otpCode) {
        if (otpCode.length == 6) {
            onVerify(otpCode)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
    ) {
        // Status Bar Space Placeholder
        Spacer(modifier = Modifier.height(44.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0D1321),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Verify OTP",
                color = Color(0xFF0D1321),
                fontSize = 28.sp,
                fontWeight = FontWeight(700),
                lineHeight = 34.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Enter 6-digit code sent to",
                color = Color(0xFF6B7280),
                fontSize = 16.sp,
                fontWeight = FontWeight(400),
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "+91 $phoneNumber",
                color = Color(0xFF0D1321),
                fontSize = 16.sp,
                fontWeight = FontWeight(500),
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                for (i in 0 until 6) {
                    val char = otpCode.getOrNull(i)?.toString() ?: ""
                    OtpBox(char = char, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF6B7280), fontWeight = FontWeight(400))) {
                        append("Resend OTP in ")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFF2563EB), fontWeight = FontWeight(500))) {
                        append("00:30")
                    }
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        androidx.compose.material3.Button(
            onClick = {
                if (otpCode.length == 6) {
                    onVerify(otpCode)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text(
                "Submit OTP",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF9FAFB))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 34.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "delete")
                )

                rows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.weight(1f).height(80.dp))
                            } else if (key == "delete") {
                                KeypadDeleteKey(onClick = {
                                    if (otpCode.isNotEmpty()) {
                                        otpCode = otpCode.dropLast(1)
                                    }
                                }, modifier = Modifier.weight(1f))
                            } else {
                                KeypadKey(text = key, onClick = {
                                    if (otpCode.length < 6) {
                                        otpCode += key
                                    }
                                }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    if (index < rows.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OtpBox(char: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(56.dp)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        if (char.isNotEmpty()) {
            Text(
                text = char,
                color = Color(0xFF0D1321),
                fontSize = 24.sp,
                fontWeight = FontWeight(600),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun KeypadKey(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(80.dp)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF0D1321),
            fontSize = 22.sp,
            fontWeight = FontWeight(500),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun KeypadDeleteKey(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(80.dp)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Delete",
            tint = Color(0xFF0D1321),
            modifier = Modifier.size(24.dp) 
        )
    }
}
