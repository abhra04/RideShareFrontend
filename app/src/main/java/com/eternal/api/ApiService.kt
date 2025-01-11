package com.eternal.api

import com.eternal.models.RideRequest
import com.eternal.models.User
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/user")
    fun addUser(@Body user: User): Call<Map<String, String>>

    @GET("/user/{phone}")
    fun getUser(
        @Path("phone") phone: String,
        @Header("Authorization") authHeader: String
    ): Call<User>

    @POST("/createRideRequest")
    fun submitRideRequest(@Body rideRequest: RideRequest): Call<Map<String, String>>

    // New endpoint for fetching rides
    @GET("/user/{userUid}/allRides")
    fun getRides(
        @Path("userUid") userUid: String,
        @Query("currentStatus") currentStatus: String? = null
    ): Call<List<String>>

}
