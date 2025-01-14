package com.eternal.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eternal.R
import com.eternal.submitRideRequest
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(name: String, onLogout: () -> Unit, onShowAllRides: () -> Unit) {
    var isProfileWidgetVisible by remember { mutableStateOf(false) }
    var showRideForm by remember { mutableStateOf(true) }
    var notificationMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFB2F7EF), // Light Cyan
                        Color(0xFFFFD1DC), // Soft Pink
                        Color(0xFFB2F7EF)  // Light Cream
                    )
                )
            )
    ) {
        Column {
            // Top App Bar
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxHeight(), // Take up full height of the TopAppBar
                        contentAlignment = Alignment.Center // Center content vertically
                    ) {
                        Text(
                            "Hey, $name!",
                            color = Color(0xFF232323),
                            fontSize = 18.sp, // Adjusted font size
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isProfileWidgetVisible = true }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = Color(0xFF232323)
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFFB2F7EF),
                    titleContentColor = Color(0xFF232323)
                ),
                modifier = Modifier
                    .height(56.dp)
                    .border(1.dp, Color.Black, shape = RectangleShape)
            // Adjusted height
            )

//            Spacer(modifier = Modifier.height(16.dp))

            if (showRideForm) {
                // Book Ride Form
                BookRideScreen(
                    onRideRequestSubmit = { rideRequest ->
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        submitRideRequest(
                            rideRequest = rideRequest,
                            onSuccess = {
                                notificationMessage = "Ride request submitted successfully!"
                                showRideForm = false
                            },
                            onError = {
                                notificationMessage = "Ride request submitted successfully!"
                                showRideForm = false
                            }
                        )
                    },
                    onDismiss = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    onClick = onShowAllRides
                )
            } else {



                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cool animation for the message

                    Spacer(modifier = Modifier.height(36.dp))

                    // Original button to book another ride
                    Button(
                        onClick = { showRideForm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5AD2F4), // Bright Cyan
                            contentColor = Color.White
                        )
                    ) {
                        Text("Book Another Ride", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Display the app icon below the button
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_round), // Replace with your resource name
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(400.dp) // Set the size of the icon
                            .clip(RoundedCornerShape(12.dp)) // Optional rounded corners
                    )
                }
            }
        }

        // Profile Widget
        AnimatedVisibility(
            visible = isProfileWidgetVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(250.dp)
                    .background(Color(0xFFFAF4E1)) // Cream Color
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Profile",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF232323)
                        )
                        IconButton(onClick = { isProfileWidgetVisible = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Profile",
                                tint = Color(0xFF232323)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {  }, // Navigate to AllRidesScreen
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5AD2F4), // Bright Cyan
                            contentColor = Color.White
                        )
                    ) {
                        Text("Contact Us")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFA8072), // Soft Red-Orange
                            contentColor = Color.White
                        )
                    ) {
                        Text("Logout")
                    }
                }
            }
        }

        notificationMessage?.let { message ->
            Snackbar(
                action = {
                    TextButton(onClick = { notificationMessage = null }) {
                        Text("Ok", color = Color(0xFF232323))
                    }
                },
                modifier = Modifier.padding(16.dp),
                containerColor = Color(0xFFFBE7C6),
                contentColor = Color(0xFF232323)
            ) {
                Text(message)
            }
        }
    }
}
