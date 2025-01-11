package com.eternal.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.eternal.models.Ride
import com.eternal.fetchRides

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRidesScreen(userUid: String, onBack: () -> Unit) {
    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(selectedFilter) {
        isLoading = true
        fetchRides(userUid, selectedFilter) { fetchedRides ->
            rides = fetchedRides
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Rides") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Dropdown
            DropdownMenuFilter(selectedFilter = selectedFilter) { filter ->
                selectedFilter = filter
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rides List
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
            .wrapContentSize(Alignment.Center) // Align the box to the center
    ) {
        // Trigger Button
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.align(Alignment.Center), // Center button in the Box
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
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

        // Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .align(Alignment.Center) // Ensure the dropdown aligns to the center
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
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
                                tint = MaterialTheme.colorScheme.primary
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
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text("Pickup: ${ride.pickupLocation}", style = MaterialTheme.typography.bodyLarge)
                Text("Dropoff: ${ride.dropoffLocation}", style = MaterialTheme.typography.bodyLarge)
                Text("Pickup Time: ${ride.pickupTime}", style = MaterialTheme.typography.bodyLarge)
                Text("Pickup Date: ${ride.pickupDate}", style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ${ride.currentStatus}", style = MaterialTheme.typography.bodyMedium)
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
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatusIndicator(currentStatus: String) {
    val statusColor = when (currentStatus.lowercase()) {
        "completed" -> Color.Green
        "pending offer" -> Color.Red
        else -> Color(0xFFFFA500) // Orange
    }

    Box(
        modifier = Modifier
            .size(12.dp) // Small circle
            .clip(CircleShape)
            .background(statusColor)
    )
}