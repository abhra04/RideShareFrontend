package com.eternal.screens

import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
            .background(brush = Brush.verticalGradient(listOf(Color(0xFF3EDBF0), Color(0xFFFEEA87))))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color(0xFFFFA8A8), radius = 150f, center = center.copy(x = 100f, y = 200f))
            drawCircle(color = Color(0xFF7BEE96), radius = 100f, center = center.copy(x = size.width - 100f, y = size.height - 400f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo

            Image(
                painter = painterResource(id = R.drawable.ic_launcher_round),
                contentDescription = "RideShare Logo",
                modifier = Modifier
                    .size(100.dp) // Adjust the size as needed
                    .padding(bottom = 8.dp) // Optional spacing below the logo
            )

            Text(
                text = "RideShare",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp), // Optional vertical padding
                contentAlignment = Alignment.Center // Centers the text horizontally
            ) {
                Text(
                    text = "Welcome to RideShare!",
                    fontSize = 28.sp, // Fixed font size
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A73E8),
                    letterSpacing = 1.5.sp, // Fixed letter spacing
                    modifier = Modifier.align(Alignment.Center) // Ensures alignment inside the box
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

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
                                    unfocusedBorderColor = Color(0xFF1A73E8),
                                    focusedBorderColor = Color(0xFF1A73E8),
                                    disabledBorderColor = Color(0xFF1A73E8),
                                    errorBorderColor = Color.Transparent,
                                    focusedTextColor = Color.Black,
                                    cursorColor = Color.Black
                                ),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number // Sets the keyboard to number pad
                                ),
                                shape = RoundedCornerShape(0.dp), // No rounded corners
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
                    shape = RoundedCornerShape(50.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                ) {
                    Text(
                        text = "Send OTP",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
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

                Button(
                    onClick = {
                        verifyOtp(activity, verificationId, otp) { isNewUser, name ->
                            if (isNewUser) {
                                onNavigation("EnterName", phoneNumber, "")
                            } else {
                                onNavigation("Dashboard", phoneNumber, name)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA8A8))
                ) {
                    Text(
                        text = "Verify OTP",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                }
            }
        }
    }
}

// Country data model
data class Country(val code: String, val name: String, val flagRes: Int)
