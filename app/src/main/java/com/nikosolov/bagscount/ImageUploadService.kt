package com.nikosolov.bagscount

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

data class CodeResponse(
    val code: String
)

data class StatusResponse(
    val status: String          = "N/A",
    val handbags: String        = "0",
    val suitcases: String       = "0",
    val backpacks: String       = "0"
)


interface ImageUploadService {

    // 1. Получить код доступа
    @GET("/")
    suspend fun getAccessCode(): Response<CodeResponse>

    // Загрузка и скачивание
    @Multipart
    @POST("/{code}/image")
    suspend fun uploadImage(
        @Path("code") code: String,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @Multipart
    @POST("/{code}/video")
    suspend fun uploadVideo(
        @Path("code") code: String,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @GET("/{code}/download")
    @Streaming
    suspend fun downloadFile(@Path("code") code: String): Response<ResponseBody>

    // 4. Получить статус (списки + конкретные поля)
    @GET("/{code}")
    suspend fun getStatus(@Path("code") code: String): Response<StatusResponse>
}
