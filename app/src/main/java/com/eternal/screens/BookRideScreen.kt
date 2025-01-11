package com.eternal.screens


import android.Manifest
import com.google.android.libraries.places.api.Places
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.content.ContextCompat
import com.eternal.models.RideRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.*



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookRideScreen(
    onRideRequestSubmit: (RideRequest) -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    var mapHeightFraction by remember { mutableStateOf(0.33f) } // Start with 1/3rd for the map
    var boxHeight by remember { mutableStateOf(0) } // Total box height
    var exactPickupLocation by remember { mutableStateOf(LatLng(37.7749, -122.4194)) } // Default: San Francisco
    var contactNumber by remember { mutableStateOf("") }
    var pickupDate by remember { mutableStateOf("") }
    var pickupTime by remember { mutableStateOf("") }
    var numberOfPassengers by remember { mutableStateOf("1") }
    var openToSharing by remember { mutableStateOf(false) }
    var okToSplitGroup by remember { mutableStateOf(false) }
    var exactDropoffLocation by  remember { mutableStateOf("") }
    var exactPickupLocationBox by  remember { mutableStateOf("") }
    var pickupSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var dropOffSuggestions by remember { mutableStateOf(emptyList<String>()) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current

    // Dynamic dropdown states
    var pickupLocation by remember { mutableStateOf("") }
    var dropOffLocation by remember { mutableStateOf("") }
    var pickupDropdownExpanded by remember { mutableStateOf(false) }
    var dropOffDropdownExpanded by remember { mutableStateOf(false) }
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var userLocation by remember { mutableStateOf(LatLng(0.0, 0.0)) }

    val pickupOptions = listOf("KGP", "CCU")
    val dropOffOptions = when (pickupLocation) {
        "KGP" -> listOf("CCU")
        "CCU" -> listOf("KGP")
        else -> emptyList()
    }


    // Function to fetch location suggestions (must be called in a Composable context)
//    val fetchLocationSuggestions = @androidx.compose.runtime.Composable { query: String ->
//        // This should be a suspend function
//        LaunchedEffect(query) {
//            if (query.isBlank()) {
//                pickupSuggestions = emptyList()
//                dropOffSuggestions = emptyList()
//            } else {
//                // Make sure to launch a coroutine to handle the fetch operation
//                val suggestions = fetchSuggestions(query)
//                pickupSuggestions = suggestions
//                dropOffSuggestions = suggestions
//            }
//        }
//    }



    LaunchedEffect(currentUser) {
        contactNumber = currentUser?.phoneNumber ?: ""
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Request current location
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    // Update the map to the current location
                    exactPickupLocation = LatLng(location.latitude, location.longitude)
                } else {
                    Toast.makeText(context, "Unable to fetch current location.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Location permission not granted.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                onHomeClicked = { /* Do nothing, already on home */ },
                onMyTripsClicked = onClick
            )
        }
    ){
        innerPadding -> Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                boxHeight = layoutCoordinates.size.height
            }
    ) {
        // Map Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(mapHeightFraction)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(userLocation.takeIf { it.latitude != 0.0 } ?: exactPickupLocation, 14f)
                }
            ) {
                Marker(
                    state = rememberMarkerState(position = exactPickupLocation),
                    title = "Exact Pickup Location"
                )
                if (userLocation.latitude != 0.0) {
                    Marker(
                        state = rememberMarkerState(position = userLocation),
                        title = "Your Location",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }
        }

        // Draggable Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .offset { IntOffset(0, (boxHeight * mapHeightFraction).toInt()) }
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        mapHeightFraction = (mapHeightFraction + dragAmount.y / boxHeight).coerceIn(0.2f, 0.8f)
                    }
                }
        )

        // Booking Form Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(1f - mapHeightFraction)
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Book a Ride", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Contact Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pickup Location Dropdown
            ExposedDropdownMenuBox(
                expanded = pickupDropdownExpanded,
                onExpandedChange = { pickupDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = pickupLocation,
                    onValueChange = { },
                    label = { Text("Pickup Location") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pickupDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = pickupDropdownExpanded,
                    onDismissRequest = { pickupDropdownExpanded = false }
                ) {
                    pickupOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                pickupLocation = option
                                dropOffLocation = "" // Reset dropOffLocation
                                pickupDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Exact Pickup Location Autocomplete
            Spacer(modifier = Modifier.height(16.dp))

            LaunchedEffect(exactDropoffLocation) {
                if (exactDropoffLocation.isNotBlank()) {
                    dropOffSuggestions = fetchSuggestions(exactDropoffLocation) // Call the suspend function
                } else {
                    dropOffSuggestions = emptyList()
                }
            }

            LaunchedEffect(exactPickupLocationBox) {
                if (exactPickupLocationBox.isNotBlank()) {
                    pickupSuggestions = fetchSuggestions(exactPickupLocationBox) // Call the suspend function
                } else {
                    pickupSuggestions = emptyList()
                }
            }


            PickupLocationField(
                exactPickupLocationBox = exactPickupLocationBox,
                onPickupLocationChange = { query ->
                    exactPickupLocationBox = query
                    // Update suggestions dynamically based on the query
                    pickupSuggestions = if (query.isBlank()) emptyList() else emptyList()
                },
                pickupSuggestions = pickupSuggestions,
                onSuggestionClick = { suggestion ->
                    exactPickupLocationBox = suggestion
                }
            )



            Spacer(modifier = Modifier.height(16.dp))

            // Drop-Off Location Dropdown
            ExposedDropdownMenuBox(
                expanded = dropOffDropdownExpanded,
                onExpandedChange = { dropOffDropdownExpanded = it && pickupLocation.isNotEmpty() }
            ) {
                OutlinedTextField(
                    value = dropOffLocation,
                    onValueChange = { },
                    label = { Text("Drop-Off Location") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropOffDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = pickupLocation.isNotEmpty()
                )
                ExposedDropdownMenu(
                    expanded = dropOffDropdownExpanded,
                    onDismissRequest = { dropOffDropdownExpanded = false }
                ) {
                    dropOffOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                dropOffLocation = option
                                dropOffDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Exact Drop-Off Location Autocomplete
            Spacer(modifier = Modifier.height(16.dp))

            DropOffLocationField(
                exactDropoffLocation = exactDropoffLocation,
                onDropOffLocationChange = { query ->
                    exactDropoffLocation = query
                    // Update suggestions dynamically based on the query
                    dropOffSuggestions = if (query.isBlank()) emptyList() else emptyList()
                },
                dropOffSuggestions = dropOffSuggestions,
                onSuggestionClick = { suggestion ->
                    exactDropoffLocation = suggestion
                }
            )


            Spacer(modifier = Modifier.height(16.dp))

            PickupDatePicker(pickupDate) { selectedDate ->
                pickupDate = selectedDate
            }

            Spacer(modifier = Modifier.height(16.dp))

            PickupTimePicker(pickupTime) { selectedTime ->
                pickupTime = selectedTime
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Number of Passengers")
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = numberOfPassengers,
                    onValueChange = {
                        if (it.isEmpty() || (it.toIntOrNull() ?: 0) in 1..8) {
                            numberOfPassengers = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(60.dp)
                )
            }

            CheckboxRow(label = "Open to Sharing", value = openToSharing) {
                openToSharing = it
            }

            CheckboxRow(label = "Okay to Split Group", value = okToSplitGroup) {
                okToSplitGroup = it
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .padding(innerPadding) // Respect bottom navigation bar padding
                    .padding(16.dp) // Additional padding for spacing
            ){
                Button(
                    onClick = {
                        onRideRequestSubmit(
                            RideRequest(
                                customerName = currentUser?.uid ?: "Unknown",
                                pickupLocation = pickupLocation,
                                dropoffLocation = dropOffLocation,
                                contactNumber = contactNumber,
                                pickupDate = pickupDate,
                                pickupTime = pickupTime,
                                numberOfPassengers = numberOfPassengers,
                                openToSharing = openToSharing.toString(),
                                okToSplitGroup = okToSplitGroup.toString(),
                                exactPickupLocation = exactPickupLocation.toString()
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pickupLocation.isNotEmpty() && dropOffLocation.isNotEmpty()
                ) {
                    Text("Submit Ride Request")
                }
            }
        }
    }
    }
}

@Composable
fun DraggableMarker(
    initialPosition: LatLng,
    title: String,
    onPositionChange: (LatLng) -> Unit
) {
    val markerState = rememberMarkerState(position = initialPosition)

    Marker(
        state = markerState,
        title = title,
        draggable = true
    )

    LaunchedEffect(markerState.position) {
        onPositionChange(markerState.position)
    }
}

@Composable
fun PickupDatePicker(
    currentDate: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var isDialogOpen by remember { mutableStateOf(false) }

    // DatePicker Dialog
    if (isDialogOpen) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = "$year-${month + 1}-$dayOfMonth"
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        LaunchedEffect(isDialogOpen) {
            datePickerDialog.show()
            isDialogOpen = false
        }
    }

    OutlinedTextField(
        value = currentDate,
        onValueChange = {},
        label = { Text("Pickup Date") },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { isDialogOpen = true }) {
                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Select Date")
            }
        }
    )
}

@Composable
fun PickupTimePicker(
    currentTime: String,
    onTimeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var isDialogOpen by remember { mutableStateOf(false) }

    // TimePicker Dialog
    if (isDialogOpen) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                onTimeSelected(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )

        LaunchedEffect(isDialogOpen) {
            timePickerDialog.show()
            isDialogOpen = false
        }
    }

    OutlinedTextField(
        value = currentTime,
        onValueChange = {},
        label = { Text("Pickup Time") },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { isDialogOpen = true }) {
                Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Select Time")
            }
        }
    )
}

@Composable
fun CheckboxRow(label: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = value, onCheckedChange = onValueChange)
        Text(label)
    }
}


@Composable
fun BottomNavigationBar(
    onHomeClicked: () -> Unit,
    onMyTripsClicked: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = true,
            onClick = onHomeClicked
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = "My Trips") },
            label = { Text("My Trips") },
            selected = false,
            onClick = onMyTripsClicked
        )
    }
}


fun parsePredictions(json: String): List<String> {
    val descriptions = mutableListOf<String>()

    // Regular expression pattern to match the description field
    val regex = """"description" : "(.*?)"""".toRegex()

    // Find all matches for the description field in the response
    val matches = regex.findAll(json)

    // Extract descriptions from the matches and add them to the list
    for (match in matches) {
        descriptions.add(match.groupValues[1])
    }

    return descriptions
}

suspend fun fetchSuggestions(query: String): List<String> {
    if (query.isBlank()) return emptyList()

    val apiKey = "AIzaSyBcGEnvmXH8Aztnq5Kjx2vfz3XmWwPgfsA"
    val baseUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/json"
    val url = "$baseUrl?input=${query}&key=${apiKey}"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    return try {
        // Perform the network request in the background
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (response.isSuccessful) {
            val responseBody = response.body()?.string()!!
            val predictions = parsePredictions(responseBody) // Implement your parsing logic
            predictions
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupLocationField(
    exactPickupLocationBox: String,
    onPickupLocationChange: (String) -> Unit,
    pickupSuggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Use ExposedDropdownMenuBox for built-in alignment and dropdown behavior
    ExposedDropdownMenuBox(
        expanded = isDropdownExpanded,
        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
    ) {
        // Input Text Field
        OutlinedTextField(
            value = exactPickupLocationBox,
            onValueChange = { query ->
                onPickupLocationChange(query)
                isDropdownExpanded = query.isNotEmpty()
            },
            label = { Text("Exact Pick Up Location") },
            modifier = Modifier
                .menuAnchor() // Required for proper alignment with ExposedDropdownMenuBox
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
            }
        )

        // Dropdown Menu
        ExposedDropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false }
        ) {
            pickupSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onSuggestionClick(suggestion)
                        isDropdownExpanded = false // Close the dropdown
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropOffLocationField(
    exactDropoffLocation: String,
    onDropOffLocationChange: (String) -> Unit,
    dropOffSuggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Use ExposedDropdownMenuBox for built-in alignment and dropdown behavior
    ExposedDropdownMenuBox(
        expanded = isDropdownExpanded,
        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
    ) {
        // Input Text Field
        OutlinedTextField(
            value = exactDropoffLocation,
            onValueChange = { query ->
                onDropOffLocationChange(query)
                isDropdownExpanded = query.isNotEmpty()
            },
            label = { Text("Exact Drop Off Location") },
            modifier = Modifier
                .menuAnchor() // Required for proper alignment with ExposedDropdownMenuBox
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
            }
        )

        // Dropdown Menu
        ExposedDropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false }
        ) {
            dropOffSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onSuggestionClick(suggestion)
                        isDropdownExpanded = false // Close the dropdown
                    }
                )
            }
        }
    }
}


//fun main(){
//    println(fetchLocationSuggestions("kol"))
//}