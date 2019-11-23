package com.rnlib.adyen

import retrofit2.Retrofit
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.rnlib.adyen.CheckoutApiService

object ApiService {
    private val TAG = "--ApiService"

    // post request builder
    fun checkoutApi(baseURL : String) = Retrofit.Builder()
            .baseUrl(baseURL)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(ApiWorker.gsonConverter)
            .client(ApiWorker.client)
            .build()
            .create(CheckoutApiService::class.java)!!
}