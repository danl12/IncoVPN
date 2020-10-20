package com.danl.incovpn.data

import com.danl.incovpn.data.model.Server
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("countries")
    suspend fun getCountries(): List<String>

    @GET("server")
    suspend fun getServer(@Query("country") country: String?): Server

}