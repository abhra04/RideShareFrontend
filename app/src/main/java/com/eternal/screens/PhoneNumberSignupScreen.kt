package com.eternal.screens

import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.eternal.MainActivity
import com.eternal.R
import com.eternal.sendOtp
import com.eternal.verifyOtp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberSignUpScreen(
    activity: MainActivity,
    onNavigation: (String, String, String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(Country("+91", "India", R.drawable.india_flag)) }
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val countries = listOf(
        Country("+1", "US", R.drawable.us_flag),
        Country("+44", "UK", R.drawable.uk_flag),
        Country("+91", "India", R.drawable.india_flag),
        Country("+81", "Japan", R.drawable.japan_flag)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)) // Subtle light gray background
    ) {
        // Background Video
        AndroidView(
            factory = {
                val videoView = VideoView(it).apply {
                    setVideoPath("android.resource://${context.packageName}/${R.raw.background_video_2}")
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true
                        mediaPlayer.setVolume(0.5f, 0.5f)
                        start()
                    }
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                videoView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent overlay for contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo
            Text(
                text = "RideShare",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isOtpSent) {
                // Phone number input section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Country code dropdown
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Image(
                                    painter = painterResource(id = selectedCountry.flagRes),
                                    contentDescription = selectedCountry.name,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedCountry.code,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Black,
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            countries.forEach { country ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedCountry = country
                                        expanded = false
                                    },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Image(
                                                painter = painterResource(id = country.flagRes),
                                                contentDescription = country.name,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = country.name)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Phone number field
                    Box(
                        modifier = Modifier
                            .padding(bottom = 8.dp) // Optional padding for space around the text field
                    ) {
                        Column {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    errorBorderColor = Color.Transparent,
                                    focusedTextColor = Color.Black,
                                    cursorColor = Color.Black
                                ),
                                shape = RoundedCornerShape(0.dp), // No rounded corners
                            )

                            // Custom underline
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(if (phoneNumber.isNotEmpty()) Color.Black else Color.Gray)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Send OTP button
                Button(
                    onClick = {
                        val fullPhoneNumber = "${selectedCountry.code}$phoneNumber"
                        sendOtp(activity, fullPhoneNumber) { id ->
                            verificationId = id
                            isOtpSent = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Login")
                }
            } else {
                // OTP input section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val focusRequesters = remember { List(6) { FocusRequester() } }
                    val focusManager = LocalFocusManager.current

                    for (i in 0 until 6) {
                        OTPDigitBox(
                            digit = otp.getOrNull(i)?.toString() ?: "",
                            onValueChange = { newDigit ->
                                if (newDigit.isNotEmpty()) {
                                    otp = otp.take(i) + newDigit + otp.drop(i + 1)
                                    if (i < 5) focusRequesters[i + 1].requestFocus()
                                }
                            },
                            focusRequester = focusRequesters[i],
                            isLast = i == 5,
                            onBackspace = {
                                if (i > 0) {
                                    otp = otp.take(i) + otp.drop(i + 1)
                                    focusRequesters[i - 1].requestFocus()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    verifyOtp(activity, verificationId, otp) { isNewUser, name ->
                        if (isNewUser) {
                            onNavigation("EnterName", phoneNumber, "")
                        } else {
                            onNavigation("Dashboard", phoneNumber, name)
                        }
                    }
                }) {
                    Text("Verify OTP")
                }
            }
        }
    }
}

// Country data model
data class Country(val code: String, val name: String, val flagRes: Int)
