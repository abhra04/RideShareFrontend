package com.eternal.screens


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.eternal.mapStyle
import com.eternal.models.RideRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDateTime


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookRideScreen(
    onRideRequestSubmit: (RideRequest) -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var mapHeightFraction by remember { mutableStateOf(0.33f) } // Start with 1/3rd for the map
    var boxHeight by remember { mutableStateOf(0) } // Total box height
    var exactPickupLocation by remember { mutableStateOf(LatLng(22.314871, 87.286537)) }
    var pinDropOffLocation by remember { mutableStateOf(LatLng(22.315971, 87.287637)) }
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
    var currentDateTime = LocalDateTime.now()
    var exactPickupMarkerState = rememberMarkerState(position = exactPickupLocation)
    var dropOffMarkerState = rememberMarkerState(position = pinDropOffLocation)
    var lastKnownPosition by remember { mutableStateOf(exactPickupLocation) }
    var lastKnownDropOffPosition by remember { mutableStateOf(pinDropOffLocation) }


    fun LatLng.toBounds(other: LatLng): LatLngBounds {
        val builder = LatLngBounds.Builder()
        builder.include(this)
        builder.include(other)
        return builder.build()
    }

    fun LatLngBounds.getZoomLevel(): Float {
        val width = northeast.longitude - southwest.longitude + 0.0422
        val height = northeast.latitude - southwest.latitude + 0.0422
        val maxSpan = maxOf(width, height)

        // Adjust these values to control min/max zoom
        return when {
            maxSpan > 0.5 -> 10f  // Far apart markers
            maxSpan > 0.1 -> 12f  // Medium distance
            maxSpan > 0.05 -> 14f // Closer
            else -> 16f           // Very close
        }
    }

// In your composable, add this state
    val cameraPositionState = rememberCameraPositionState()

// Add this effect to update camera when markers change
    LaunchedEffect(exactPickupLocation, pinDropOffLocation) {
        val bounds = exactPickupLocation.toBounds(pinDropOffLocation)
        val zoom = bounds.getZoomLevel()

        // Center point between two markers
        val center = LatLng(
            (exactPickupLocation.latitude + pinDropOffLocation.latitude) / 2,
            (exactPickupLocation.longitude + pinDropOffLocation.longitude) / 2
        )

        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(center, zoom),
            durationMs = 1000
        )
    }


    RequestLocationPermission(
        currentUser = currentUser,
        fusedLocationClient = fusedLocationClient,
        context = context,
        onLocationUpdate = { newLocation ->
            userLocation = newLocation
            exactPickupLocation = newLocation
        }
    )


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
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isTrafficEnabled = true, mapStyleOptions = MapStyleOptions(mapStyle.trimIndent()))
            ) {
                Marker(
                    state = exactPickupMarkerState,
                    title = "Exact Pickup Location",
                    draggable = true
                )

                LaunchedEffect(exactPickupMarkerState.position) {

                    if (exactPickupMarkerState.position != lastKnownPosition) {
                        exactPickupLocation = exactPickupMarkerState.position
                        lastKnownPosition = exactPickupMarkerState.position
                        val apiKey = "AIzaSyBcGEnvmXH8Aztnq5Kjx2vfz3XmWwPgfsA"
                        val address = reverseGeocode(exactPickupMarkerState.position, apiKey)
                        exactPickupLocationBox = address
                    }
                }

                LaunchedEffect(exactPickupLocation) {
                    if (exactPickupLocation != exactPickupMarkerState.position) {
                        lastKnownPosition = exactPickupLocation
                        exactPickupMarkerState.position = exactPickupLocation
                    }
                }

                LaunchedEffect(exactPickupLocationBox) {
                    if (exactPickupLocationBox.isNotBlank()) {
                        pickupSuggestions = fetchSuggestions(exactPickupLocationBox)
                        val apiKey = "AIzaSyBcGEnvmXH8Aztnq5Kjx2vfz3XmWwPgfsA"
                        val coordinates = geocode(exactPickupLocationBox, apiKey)
                        exactPickupLocation = LatLng(coordinates.first, coordinates.second)
                    } else {
                        pickupSuggestions = emptyList()
                    }
                }

                // Drop Off Marker
                Marker(
                    state = dropOffMarkerState,
                    title = "Drop Off Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                    draggable = true
                )

                LaunchedEffect(dropOffMarkerState.position) {
                    if (dropOffMarkerState.position != lastKnownDropOffPosition) {
                        pinDropOffLocation = dropOffMarkerState.position
                        lastKnownDropOffPosition = dropOffMarkerState.position
                        val apiKey = "AIzaSyBcGEnvmXH8Aztnq5Kjx2vfz3XmWwPgfsA"
                        val address = reverseGeocode(dropOffMarkerState.position, apiKey)
                        exactDropoffLocation = address
                    }
                }
                LaunchedEffect(pinDropOffLocation) {
                    if (pinDropOffLocation != dropOffMarkerState.position) {
                        lastKnownDropOffPosition = pinDropOffLocation
                        dropOffMarkerState.position = pinDropOffLocation
                    }
                }
            }
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(userLocation, 15f),
                            durationMs = 1000
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location"
                )
            }
        }

        // Draggable Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .offset { IntOffset(0, (boxHeight * mapHeightFraction).toInt()) }
                .background(Color(0xFF1A73E8))
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        mapHeightFraction =
                            (mapHeightFraction + dragAmount.y / boxHeight).coerceIn(0.2f, 0.8f)
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
                .background(
                    Color.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp), // Add horizontal padding around the row
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pickup Location Dropdown
                ExposedDropdownMenuBox(
                    expanded = pickupDropdownExpanded,
                    onExpandedChange = { pickupDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = pickupLocation,
                        onValueChange = { },
                        label = { Text("Pick-Up") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pickupDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth(0.5f) // Ensures it occupies exactly 50% width
                            .menuAnchor(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            unfocusedBorderColor = Color(0xFF1A73E8),
                            focusedBorderColor = Color(0xFF3EDBF0)
                        )
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

                Spacer(modifier = Modifier.width(4.dp)) // Space between the two dropdowns

                // Drop-Off Location Dropdown
                ExposedDropdownMenuBox(
                    expanded = dropOffDropdownExpanded,
                    onExpandedChange = { dropOffDropdownExpanded = it && pickupLocation.isNotEmpty() }
                ) {
                    OutlinedTextField(
                        value = dropOffLocation,
                        onValueChange = { },
                        label = { Text("Drop-Off") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropOffDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth(1f) // Remaining 50% width (completes the row)
                            .menuAnchor(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            unfocusedBorderColor = Color(0xFF1A73E8),
                            focusedBorderColor = Color(0xFF3EDBF0)
                        )
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
            }


            // Exact Pickup Location Autocomplete
            Spacer(modifier = Modifier.height(16.dp))

            LaunchedEffect(exactDropoffLocation) {
                if (exactDropoffLocation.isNotBlank()) {
                    // Fetch suggestions based on the input
                    dropOffSuggestions = fetchSuggestions(exactDropoffLocation)
                    val apiKey = "AIzaSyBcGEnvmXH8Aztnq5Kjx2vfz3XmWwPgfsA"
                    val coordinates = geocode(exactDropoffLocation, apiKey)
                    pinDropOffLocation = LatLng(coordinates.first, coordinates.second)
                    dropOffMarkerState = MarkerState(position = pinDropOffLocation)

                } else {
                    dropOffSuggestions = emptyList()
                }
            }

            LaunchedEffect(exactPickupLocationBox) {
                if (exactPickupLocationBox.isNotBlank()) {
                    // Fetch suggestions based on the input
                    pickupSuggestions = fetchSuggestions(exactPickupLocationBox)
                    val apiKey = "AIzaSyBcGEnvmXH8Aztnq5Kjx2vfz3XmWwPgfsA"
                    val coordinates = geocode(exactPickupLocationBox, apiKey)
                    exactPickupLocation = LatLng(coordinates.first, coordinates.second)
                    exactPickupMarkerState = MarkerState(position = exactPickupLocation)
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
                    val apiKey = "AIzaSyBcGEnvmXH8Aztnq5Kjx2vfz3XmWwPgfsA"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp), // Padding for the entire row
                horizontalArrangement = Arrangement.Start, // No extra spacing between components
                verticalAlignment = Alignment.CenterVertically // Center align vertically
            ) {
                // Date Picker with 60% width
                Box(
                    modifier = Modifier
                        .weight(1f) // Allocate 60% of the row's width
                ) {
                    SpinnerDatePicker(
                        initialDay = currentDateTime.dayOfMonth,
                        initialMonth = currentDateTime.monthValue - 1,
                        initialYear = currentDateTime.year,
                        initialHour = currentDateTime.hour,
                        initialMinute = currentDateTime.minute
                    ) { day, month, year,hour,minute ->
                        pickupDate = String.format("%02d-%02d-%04d", day, month + 1, year)
                        pickupTime = String.format("%02d:%02d", hour, minute)
                    }
                }
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
                                exactPickupLocationBox = exactPickupLocationBox,
                                exactDropoffLocation = exactDropoffLocation
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pickupLocation.isNotEmpty() && dropOffLocation.isNotEmpty() ,
                    colors = ButtonDefaults.buttonColors(Color(0xFF00FFFF))
                ) {
                    Text("Book A Ride")
                }
            }
        }
    }
    }
}



@Composable
fun BottomNavigationBar(
    onHomeClicked: () -> Unit,
    onMyTripsClicked: () -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFFE3FDFD)
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = Color(0xFF64B6FF)) },
            label = { Text("Home", color = Color(0xFF64B6FF)) },
            selected = true,
            onClick = onHomeClicked
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = "My Trips", tint = Color(0xFF64B6FF)) },
            label = { Text("My Trips", color = Color(0xFF64B6FF)) },
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
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = Color(0xFF1A73E8),
                focusedBorderColor = Color(0xFF3EDBF0)
            ),
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
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = Color(0xFF1A73E8),
                focusedBorderColor = Color(0xFF3EDBF0)
            ),
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


@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    colWidth: Int,
) {
    // LazyListState to control the scrolling position
    val listState = rememberLazyListState()

    // Side-effect to scroll to the selected index when the composable is first displayed
    LaunchedEffect(selectedIndex) {
        listState.scrollToItem(selectedIndex)
    }

    LazyColumn(
        state = listState, // Attach the state
        modifier = Modifier
            .width(colWidth.dp) // Set a fixed width for better alignment
            .height(80.dp), // Adjusted height for better usability
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(items) { index, item ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    // Draw a circular background around the selected text
                    Box(
                        modifier = Modifier
                            .size(36.dp) // Adjust the size of the circle
                            .background(Color.White, shape = CircleShape)
                    )
                }
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.Gray
                    ),
                    modifier = Modifier
                        .clickable { onItemSelected(index) }, // Handle click to select the item
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Composable
fun SpinnerDatePicker(
    initialDay: Int = 1,
    initialMonth: Int = 0, // 0 for January
    initialYear: Int = 2023,
    initialHour: Int = 12,
    initialMinute: Int = 30,
    onDateSelected: (Int, Int, Int, Int, Int) -> Unit
) {
    val days = (1..31).toList()
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val years = (1900..2100).toList()
    val hours = (0..24).map { String.format("%02d", it) }.toList()
    val minutes = (0..59).map { String.format("%02d", it) }.toList()

    var selectedDay by remember { mutableStateOf(initialDay) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0FFFF), shape = RoundedCornerShape(16.dp)) // Cream background
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Day Picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Day", color = Color.Black, style = MaterialTheme.typography.bodySmall)
                WheelPicker(
                    items = days.map { it.toString() },
                    selectedIndex = days.indexOf(selectedDay),
                    onItemSelected = {
                        selectedDay = days[it]
                        onDateSelected(selectedDay, selectedMonth, selectedYear, selectedHour, selectedMinute)
                    },
                    colWidth = 40
                )
            }

            // Month Picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Month", color = Color.Black, style = MaterialTheme.typography.bodySmall)
                WheelPicker(
                    items = months,
                    selectedIndex = selectedMonth,
                    onItemSelected = {
                        selectedMonth = it
                        onDateSelected(selectedDay, selectedMonth, selectedYear, selectedHour, selectedMinute)
                    },
                    colWidth = 60
                )
            }

            // Year Picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Year", color = Color.Black, style = MaterialTheme.typography.bodySmall)
                WheelPicker(
                    items = years.map { it.toString() },
                    selectedIndex = years.indexOf(selectedYear),
                    onItemSelected = {
                        selectedYear = years[it]
                        onDateSelected(selectedDay, selectedMonth, selectedYear, selectedHour, selectedMinute)
                    },
                    colWidth = 80
                )
            }

            // Hour Picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Hour", color = Color.Black, style = MaterialTheme.typography.bodySmall)
                WheelPicker(
                    items = hours,
                    selectedIndex = hours.indexOf(selectedHour.toString().padStart(2, '0')),
                    onItemSelected = {
                        selectedHour = hours[it].toInt()
                        onDateSelected(selectedDay, selectedMonth, selectedYear, selectedHour, selectedMinute)
                    },
                    colWidth = 60
                )
            }

            // Minute Picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Minute", color = Color.Black, style = MaterialTheme.typography.bodySmall)
                WheelPicker(
                    items = minutes,
                    selectedIndex = minutes.indexOf(selectedMinute.toString().padStart(2, '0')),
                    onItemSelected = {
                        selectedMinute = minutes[it].toInt()
                        onDateSelected(selectedDay, selectedMonth, selectedYear, selectedHour, selectedMinute)
                    },
                    colWidth = 60
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp)) // Add spacing between rows
    }
}


suspend fun reverseGeocode(position: LatLng, apiKey: String): String {
    val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=${position.latitude},${position.longitude}&key=$apiKey"
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    // Perform the network request in the background
    return withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                if (responseBody != null) {
                    // Parse the response to extract the formatted address
                    val json = JSONObject(responseBody)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        return@withContext firstResult.optString("formatted_address", "Unknown Location")
                    }
                }
            }
            "Unknown Location" // Default fallback if response or parsing fails
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: Unable to fetch location"
        }
    }
}

suspend fun geocode(address: String, apiKey: String): Pair<Double, Double> {
    val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${address.replace(" ", "+")}&key=$apiKey"
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    return withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                if (!responseBody.isNullOrEmpty()) {
                    return@withContext parseGeocodeResponse(responseBody)
                }
            }
            Pair(0.0, 0.0) // Default fallback if response or parsing fails
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(0.0, 0.0) // Error fallback
        }
    }
}


fun parseGeocodeResponse(json: String): Pair<Double, Double> {
    println("going to parse")
    val regexLat = """"lat" : ([0-9.-]+)""".toRegex()
    val regexLng = """"lng" : ([0-9.-]+)""".toRegex()

    // Find latitude and longitude inside the geometry.location object
    val latMatch = regexLat.find(json)
    val lngMatch = regexLng.find(json)
    // Extract latitude and longitude from the matched regex groups, or use 0.0 as fallback
    val lat = latMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    val lng = lngMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    println(lat)
    println(lng)
    return Pair(lat, lng)
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RequestLocationPermission(
    @SuppressLint("RestrictedApi") currentUser: FirebaseUser?,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    onLocationUpdate: (LatLng) -> Unit  // Added callback to update location
) {
    var contactNumber by remember { mutableStateOf("") }
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    // Remember the launcher for requesting location permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, fetch the location
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    onLocationUpdate(LatLng(location.latitude, location.longitude))
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

    LaunchedEffect(currentUser) {
        contactNumber = currentUser?.phoneNumber ?: ""
        currentDateTime = LocalDateTime.now()

        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted, fetch location
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    onLocationUpdate(LatLng(location.latitude, location.longitude))
                } else {
                    Toast.makeText(context, "Unable to fetch current location.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Launch permission request
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}


//fun main(){
//    println(geocode_1("Khargapur Railway Station","AIzaSyBcGEnvmXH8Aztnq5Kjx2vfz3XmWwPgfsA"))
////    println(reverseGeocode_1(19.11112f,20.123f,"AIzaSyBcGEnvmXH8Aztnq5Kjx2vfz3XmWwPgfsA"))
//}

