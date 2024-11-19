package com.eternal

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eternal.api.ApiClient
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView


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
    var screenState by remember { mutableStateOf("PhoneNumberSignUp") }
    var userName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    when (screenState) {
        "PhoneNumberSignUp" -> PhoneNumberSignUpScreen(activity) { nextScreen, phone, name ->
            screenState = nextScreen
            phoneNumber = phone
            userName = name
        }
        "EnterName" -> EnterNameScreen(phoneNumber) { name ->
            addUserToBackend(phoneNumber, name)
            userName = name
            screenState = "Dashboard"
        }
        "Dashboard" -> DashboardScreen(userName) {
            logout(activity)
            screenState = "PhoneNumberSignUp"
        }
    }
}

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

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AndroidView(
            factory = {
                val videoView = VideoView(it)
                videoView.apply {
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "RideShare",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isOtpSent) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White,
                        focusedBorderColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    sendOtp(activity, phoneNumber) { id ->
                        verificationId = id
                        isOtpSent = true
                    }
                }) {
                    Text("Send OTP")
                }
            } else {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(name: String, onLogout: () -> Unit) {
    var isProfileWidgetVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column {
            TopAppBar(
                title = {
                    Text("RideShare", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp)
                },
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

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { /* Handle Book a Ride */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                ) {
                    Text("Book a Ride", color = Color.White, fontSize = 16.sp)
                }

                Button(
                    onClick = { /* Handle Show Pending Bookings */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                ) {
                    Text("Show Pending Bookings", color = Color.White, fontSize = 16.sp)
                }

                Button(
                    onClick = { /* Handle Show Complete Bookings */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                ) {
                    Text("Show Complete Bookings", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // Profile Widget
        AnimatedVisibility(
            visible = isProfileWidgetVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }), // Slide in from the left
            exit = slideOutHorizontally(targetOffsetX = { -it }) // Slide out to the left
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(250.dp) // Fixed width for the widget
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
                    .align(Alignment.TopStart) // Ensures it starts at the left
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

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { onLogout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Logout", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterNameScreen(phone: String, onNameEntered: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Enter Your Name")

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = Color.White,
                focusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { onNameEntered(name) }) {
            Text("Submit")
        }
    }
}


@Composable
fun OTPDigitBox(
    digit: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    isLast: Boolean,
    onBackspace: () -> Unit
) {
    val context = LocalContext.current

    BasicTextField(
        value = digit,
        onValueChange = { value ->
            if (value.length <= 1) {
                onValueChange(value)
            }
        },
        modifier = Modifier
            .size(50.dp)
            .focusRequester(focusRequester)
            .border(1.dp, Color.White)
            .background(Color.Transparent)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                    onBackspace()
                    true
                } else false
            },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            color = Color.White,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(Color.White),
        decorationBox = { innerTextField ->
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    )
}


@Composable
fun WelcomeScreen(name: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome, $name!")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { onLogout() }) {
            Text("Logout")
        }
    }
}

fun sendOtp(activity: MainActivity, phoneNumber: String, onVerificationIdReceived: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val options = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(phoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(activity, credential) { _, _ -> }
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                Toast.makeText(activity, "Verification failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                onVerificationIdReceived(verificationId)
                Toast.makeText(activity, "OTP Sent!", Toast.LENGTH_SHORT).show()
            }
        }).build()

    PhoneAuthProvider.verifyPhoneNumber(options)
}

fun verifyOtp(activity: MainActivity, verificationId: String, otp: String, onVerificationComplete: (Boolean, String) -> Unit) {
    val credential = PhoneAuthProvider.getCredential(verificationId, otp)
    signInWithPhoneAuthCredential(activity, credential, onVerificationComplete)
}

fun signInWithPhoneAuthCredential(activity: MainActivity, credential: PhoneAuthCredential, onVerificationComplete: (Boolean, String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    auth.signInWithCredential(credential)
        .addOnCompleteListener(activity) { task ->
            if (task.isSuccessful) {
                val user = task.result?.user
                val phone = user?.phoneNumber ?: ""
                fetchUserFromBackend(phone) { backendUser ->
                    if (backendUser == null || backendUser.name.isNullOrEmpty()) {
                        onVerificationComplete(true, "")
                    } else {
                        onVerificationComplete(false, backendUser.name)
                    }
                }
            } else {
                Toast.makeText(activity, "Sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
}

fun addUserToBackend(phone: String, name: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val firebaseUid = currentUser?.uid ?: return
    val user = User(phone = phone, name = name, uid = firebaseUid  )
    println("going to hit backend")
    ApiClient.apiService.addUser(user).enqueue(object : Callback<Map<String, String>> {
        override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
            if (response.isSuccessful) {
                Log.d("API", "User added successfully")
            }
        }
        override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
            Log.e("API", "Error adding user: ${t.message}")
        }
    })
}

fun fetchUserFromBackend(phone: String, onResult: (User?) -> Unit) {
    getFirebaseIdToken { token ->
        if (token != null) {
            val authHeader = "Bearer $token"
            ApiClient.apiService.getUser(phone, authHeader).enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<User>, t: Throwable) {
                    onResult(null)
                }
            })
        } else {
            onResult(null)
        }
    }
}

fun logout(activity: MainActivity) {
    FirebaseAuth.getInstance().signOut()
    Toast.makeText(activity, "Logged out successfully", Toast.LENGTH_SHORT).show()
}
fun getFirebaseIdToken(onTokenReceived: (String?) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    user?.getIdToken(true)?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            onTokenReceived(task.result?.token)
        } else {
            onTokenReceived(null)
        }
    }
}
