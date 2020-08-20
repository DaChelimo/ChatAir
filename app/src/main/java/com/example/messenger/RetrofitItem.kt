package com.example.messenger

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

//
const val BASE_URL = "https://fcm.googleapis.com/"
const val CONTENT_TYPE = "application/json"
const val AUTHORIZATION_KEY = "AAAA_zCT9kE:APA91bEOB8tVvEKjleP8e6BP9N2RjL9t7c36rdTN4Zu2A8Gll6XZUyfF-jl1zD7w_SxUz378dSVV03nZmB7u0vhtDaSS40dmJQ5kwiE8fS0b0zd1EIUjcI890pv4g7DrRqiHVEf1XZcT"
//
//const val FULL_CONTENT_TYPE: String = "Content-Type:$CONTENT_TYPE"
//const val FULL_AUTHORIZATION_KEY: String = "Authorization:key=$AUTHORIZATION_KEY"
//
//val LIST_OF_HEADERS = listOf("Content-Type:$CONTENT_TYPE", "Authorization:key=$AUTHORIZATION_KEY")

val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

var logging = HttpLoggingInterceptor().apply {
    this.level = HttpLoggingInterceptor.Level.BODY
}
val httpClient = OkHttpClient.Builder().addInterceptor(logging)

val retrofit: Retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .baseUrl(BASE_URL)
    .client(httpClient.build())
    .build()

interface ApiService {

    @Headers(
        "Authorization: key=$AUTHORIZATION_KEY"
      )

    // @Header("Content-Type") contentType: String = CONTENT_TYPE
    @POST("fcm/send")
    fun sendNotificationInApi(
        @Body notification: FCMData,
        @Header("Content-Type") contentType: String = CONTENT_TYPE
    ): Call<FCMData>
}

object RetrofitItem {
    val postData: ApiService = retrofit.create(ApiService::class.java)
}