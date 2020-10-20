package com.danl.incovpn.data

import androidx.lifecycle.liveData
import com.danl.incovpn.data.model.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun getCountries() = apiService.getCountries()

    suspend fun get(country: String?) = apiService.getServer(country)
}