package com.eternal.api

import com.eternal.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("/user")
    fun addUser(@Body user: User): Call<Map<String, String>>

    @GET("/user/{phone}")
    fun getUser(
        @Path("phone") phone: String,
        @Header("Authorization") authHeader: String
    ): Call<User>
}
