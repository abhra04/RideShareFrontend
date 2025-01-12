package com.eternal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eternal.screens.AllRidesScreen
import com.eternal.screens.DashboardScreen
import com.eternal.screens.EnterNameScreen
import com.eternal.screens.PhoneNumberSignUpScreen
import com.google.firebase.auth.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(this)
        }
    }
}

@Composable
fun MainScreen(activity: MainActivity) {
    var screenState by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var userUid by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val sharedPreferences = activity.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        isLoading = true
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            userUid = firebaseUser.uid

            // Retrieve the username from SharedPreferences
            userName = sharedPreferences.getString("userName", "") ?: ""

            if (userName.isEmpty()) {
                // If the username is not found, navigate to EnterName screen
                screenState = "EnterName"
            } else {
                screenState = "Dashboard"
            }
        } else {
            screenState = "PhoneNumberSignUp"
        }
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        when (screenState) {
            "PhoneNumberSignUp" -> PhoneNumberSignUpScreen(activity) { nextScreen, phone, name ->
                userUid = FirebaseAuth.getInstance().currentUser?.uid
                screenState = nextScreen
                phoneNumber = phone
                userName = name

                // Save the username to SharedPreferences
                sharedPreferences.edit().putString("userName", userName).apply()
            }
            "EnterName" -> EnterNameScreen(userUid ?: "") { name ->
                addUserToBackend(phoneNumber, name)
                userName = name

                // Save the username to SharedPreferences
                sharedPreferences.edit().putString("userName", userName).apply()

                screenState = "Dashboard"
            }
            "Dashboard" -> {
                DashboardScreen(
                    name = userName,
                    onLogout = {
                        logout(activity)

                        // Clear SharedPreferences on logout
                        sharedPreferences.edit().clear().apply()

                        screenState = "PhoneNumberSignUp"
                    },
                    onShowAllRides = {
                        screenState = "AllRides"
                    }
                )
            }
            "AllRides" -> {
                AllRidesScreen(userUid = userUid!!) {
                    screenState = "Dashboard"
                }
            }
        }
    }
}




