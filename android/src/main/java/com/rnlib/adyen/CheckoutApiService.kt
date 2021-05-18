package com.rnlib.adyen

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import com.rnlib.adyen.PaymentMethodsRequest

interface CheckoutApiService {
    @POST("api/v1/adyen/payment_methods")
    fun paymentMethods(@HeaderMap headerMap: Map<String, String>,@Body paymentMethodsRequest: PaymentMethodsRequest): Call<ResponseBody>

    @POST("payments")
    fun payments(@HeaderMap headerMap: Map<String, String>,@Body paymentsRequest: RequestBody): Call<ResponseBody>

    @POST("/api/v1/adyen/add_cards")
    fun addCards(@HeaderMap headerMap: Map<String, String>,@Body paymentsRequest: RequestBody): Call<ResponseBody>

    @POST("api/v1/adyen/trip_payments")
    fun tripPayments(@HeaderMap headerMap: Map<String, String>,@Body paymentsRequest: RequestBody): Call<ResponseBody>

    @POST("api/v1/adyen/user_credit_payments")
    fun userCredit(@HeaderMap headerMap: Map<String, String>,@Body paymentsRequest: RequestBody): Call<ResponseBody>

    @POST("api/v1/adyen/payment_details")
    fun details(@HeaderMap headerMap: Map<String, String>,@Body detailsRequest: RequestBody): Call<ResponseBody>
}
