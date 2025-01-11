package com.eternal.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.navOptions
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column {
            // Top App Bar
            TopAppBar(
                title = { Text("Hey! ${name}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = { isProfileWidgetVisible = true }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                Button(
                    onClick = { showRideForm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                ) {
                    Text("Book Another Ride", color = Color.White, fontSize = 16.sp)
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
                    .background(MaterialTheme.colorScheme.surface)
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
                        Text("Profile", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { isProfileWidgetVisible = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Profile"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onShowAllRides, // Navigate to AllRidesScreen
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Show All Rides", color = Color.White)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Logout", color = Color.White)
                    }
                }
            }
        }

        notificationMessage?.let { message ->
            Snackbar(
                action = {
                    TextButton(onClick = { notificationMessage = null }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(message)
            }
        }
    }
}
