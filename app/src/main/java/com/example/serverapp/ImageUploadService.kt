package com.example.serverapp

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ImageUploadService {
    // 1. GET-запрос для получения кода доступа
    @GET("/")
    suspend fun getAccessCode(): Response<String>

    // 2. POST-запрос для отправки изображения
    @Multipart
    @POST("/{code}")
    suspend fun uploadImage(
        @Path("code") code: String,
        @Part image: MultipartBody.Part
    ): Response<Unit>

    // 3. GET-запрос для получения информации об изображении
    @GET("/{code}")
    suspend fun getImageInfo(
        @Path("code") code: String
    ): Response<String>
}