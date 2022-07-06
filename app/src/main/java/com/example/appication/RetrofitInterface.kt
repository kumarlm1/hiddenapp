package com.example.appication

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


data class Message(
    var id : String,
    var model : String,
    var date : String,
    var sender : String,
    var msg : String,
    var userid : String
)
data class Token(
    var id: String,
    var token: String
)

interface RetrofitInterface {
    @POST("insert.php")
    suspend fun sendMessage(@Body user : Message)

    @POST("token.php")
    suspend fun sendToken(@Body token : Token)
}