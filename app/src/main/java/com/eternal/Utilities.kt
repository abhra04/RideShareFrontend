package com.eternal

import android.util.Log
import android.widget.Toast
import com.eternal.api.ApiClient
import com.eternal.models.Ride
import com.eternal.models.RideRequest
import com.eternal.models.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit


fun validateInputs(
    pickupLocation: String,
    dropoffLocation: String,
    contactNumber: String,
    pickupDate: String,
    pickupTime: String,
    numberOfPassengers: String
): Boolean {
    return pickupLocation.isNotBlank() &&
            dropoffLocation.isNotBlank() &&
            contactNumber.isNotBlank() &&
            pickupDate.matches("\\d{4}-\\d{2}-\\d{2}".toRegex()) &&
            pickupTime.matches("\\d{2}:\\d{2}".toRegex()) &&
            (numberOfPassengers.toIntOrNull() ?: 0) in 1..8
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

fun fetchRides(userUid: String, filter: String, onResult: (List<Ride>) -> Unit) {
    val currentStatus = if (filter != "All") filter else null

    ApiClient.apiService.getRides(userUid, currentStatus).enqueue(object : Callback<List<String>> {
        override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
            if (response.isSuccessful) {
                val jsonStrings = response.body() ?: emptyList()
                val gson = Gson()
                val rideList = jsonStrings.mapNotNull { jsonString ->
                    try {
                        gson.fromJson(jsonString, Ride::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                onResult(rideList)
            } else {
                onResult(emptyList())
            }
        }

        override fun onFailure(call: Call<List<String>>, t: Throwable) {
            onResult(emptyList())
        }
    })
}


fun submitRideRequest(
    rideRequest: RideRequest,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    ApiClient.apiService.submitRideRequest(rideRequest).enqueue(object :
        Callback<Map<String, String>> {
        override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
            Log.d("API","Response ${response}")
            if (response.isSuccessful) {
                Log.d("API", "Ride Request added successfully")
                onSuccess()
            } else {
                onError()
            }
        }

        override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
            Log.e("API", "Error adding Ride Request: ${t.message}")
            onError()
        }
    })
}
