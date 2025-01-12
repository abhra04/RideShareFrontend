package com.eternal.models


data class RideRequest(
    val customerName: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val contactNumber: String,
    val pickupDate: String,
    val pickupTime: String,
    val numberOfPassengers: String,
    val openToSharing: String,
    val okToSplitGroup: String,
    val exactPickupLocationBox: String,
    val exactDropoffLocation: String
)