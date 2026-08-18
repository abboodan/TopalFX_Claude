package com.topaloglu.topalfx.network

import retrofit2.http.GET
import retrofit2.http.Query

data class FrankfurterResponse(
    val base: String,
    val date: String?,
    val rates: Map<String, Double>,
)

interface FrankfurterApi {
    @GET("latest")
    suspend fun latest(
        @Query("base") base: String,
        @Query("symbols") symbols: String,
    ): FrankfurterResponse
}
