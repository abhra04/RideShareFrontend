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
import kotlinx.coroutines.Dispatchers
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

    val currentUser = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current

    // Dynamic dropdown states
    var pickupLocation by remember { mutableStateOf("") }
    var dropOffLocation by remember { mutableStateOf("") }
    var pickupDropdownExpanded by remember { mutableStateOf(false) }
    var dropOffDropdownExpanded by remember { mutableStateOf(false) }

    val pickupOptions = listOf("KGP", "CCU")
    val dropOffOptions = when (pickupLocation) {
        "KGP" -> listOf("CCU")
        "CCU" -> listOf("KGP")
        else -> emptyList()
    }

    LaunchedEffect(currentUser) {
        contactNumber = currentUser?.phoneNumber ?: ""
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                onHomeClicked = { /* Do nothing, already on home */ },
                onMyTripsClicked = onClick
            )
        }
    ){
        padding -> Box(
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
                    position = CameraPosition.fromLatLngZoom(exactPickupLocation, 14f)
                }
            ) {
                Marker(
                    state = rememberMarkerState(position = exactPickupLocation),
                    title = "Exact Pickup Location"
                )
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


