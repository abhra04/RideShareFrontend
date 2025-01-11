package com.eternal.models

data class Ride(
    val pickupLocation: String,
    val dropoffLocation: String,
    val pickupDate: String,
    val pickupTime: String,
    val currentStatus: String
)