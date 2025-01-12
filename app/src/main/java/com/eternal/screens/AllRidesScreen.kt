package com.eternal.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eternal.models.Ride
import com.eternal.fetchRides

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRidesScreen(userUid: String, onClick: () -> Unit) {
    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedFilter) {
        isLoading = true
        fetchRides(userUid, selectedFilter) { fetchedRides ->
            rides = fetchedRides
            isLoading = false
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBarForAllRides(
                onHomeClicked = onClick,
                onMyTripsClicked = { /* Already on MyTrips */ }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE3FDFD), Color(0xFFFFE6FA))
                    )
                )
        ) {
            DropdownMenuFilter(selectedFilter = selectedFilter) { filter ->
                selectedFilter = filter
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(rides) { ride ->
                        RideTile(ride = ride)
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownMenuFilter(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val filters = listOf(
        "All" to Icons.Default.FilterList,
        "Pending Offer" to Icons.Default.HourglassEmpty,
        "Ride Offered" to Icons.Default.DirectionsCar,
        "Pending Payment" to Icons.Default.Payment,
        "Waiting for Other Parties" to Icons.Default.Group,
        "Completed" to Icons.Default.CheckCircle
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
    ) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.align(Alignment.Center),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF64B6FF),
                containerColor = Color.White
            )
        ) {
            Text(
                text = selectedFilter,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            filters.forEach { (filter, icon) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF64B6FF)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(filter)
                        }
                    },
                    onClick = {
                        expanded = false
                        onFilterSelected(filter)
                    }
                )
            }
        }
    }
}

@Composable
fun RideTile(ride: Ride) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF013131))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text("Pickup: ${ride.pickupLocation}", style = MaterialTheme.typography.bodyLarge,color = Color(0xFFE0FFFF))
                Text("Dropoff: ${ride.dropoffLocation}", style = MaterialTheme.typography.bodyLarge,color = Color(0xFFE0FFFF))
                Text("Pickup Time: ${ride.pickupTime}", style = MaterialTheme.typography.bodyLarge,color = Color(0xFFE0FFFF))
                Text("Pickup Date: ${ride.pickupDate}", style = MaterialTheme.typography.bodyLarge,color = Color(0xFFE0FFFF))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ${ride.currentStatus}", style = MaterialTheme.typography.bodyMedium,color = Color(0xFFE0FFFF))
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusIndicator(currentStatus = ride.currentStatus)
                }
            }

            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "Ride Icon",
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterVertically),
                tint = Color(0xFF64B6FF)
            )
        }
    }
}

@Composable
fun StatusIndicator(currentStatus: String) {
    val statusColor = when (currentStatus.lowercase()) {
        "completed" -> Color.Green
        "pending offer" -> Color.Red
        else -> Color(0xFFFFA500)
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(statusColor)
    )
}

@Composable
fun BottomNavigationBarForAllRides(
    onHomeClicked: () -> Unit,
    onMyTripsClicked: () -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFFE3FDFD)
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = Color(0xFF64B6FF)) },
            label = { Text("Home", color = Color(0xFF64B6FF)) },
            selected = false,
            onClick = onHomeClicked
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = "My Trips", tint = Color(0xFF64B6FF)) },
            label = { Text("My Trips", color = Color(0xFF64B6FF)) },
            selected = true,
            onClick = onMyTripsClicked
        )
    }
}
