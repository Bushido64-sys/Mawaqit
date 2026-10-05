package com.mawaqit.app.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * CountriesNow (https://countriesnow.space) — free, no key (PHASE-8.1).
 * Shapes below are the only fields we read.
 */
interface CountriesNowApiService {

    @GET("api/v0.1/countries")
    suspend fun getCountries(): CountriesResponse

    @POST("api/v0.1/countries/cities/q")
    suspend fun getCities(@Body body: CountryRequest): CitiesResponse
}

data class CountryRequest(val country: String)

data class CountriesResponse(
    val error: Boolean?,
    val msg: String?,
    val data: List<CountryInfo>?
)

data class CountryInfo(
    val iso2: String?,
    val iso3: String?,
    val country: String?
)

data class CitiesResponse(
    val error: Boolean?,
    val msg: String?,
    val data: List<String>?
)
